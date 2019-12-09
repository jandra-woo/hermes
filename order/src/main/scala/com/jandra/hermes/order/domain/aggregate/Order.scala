package com.jandra.hermes.order.domain.aggregate

import java.text.SimpleDateFormat

import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.util.Timeout
import com.jandra.hermes.common.util.{CborSerializable, SharedLeveldb}

import scala.concurrent.duration._
import com.jandra.hermes.order.application.MockActorRef
import com.jandra.hermes.order.application.OrderRestRoutes.CreateOrderItemData
import com.jandra.hermes.order.domain.entity.OrderItem
import com.jandra.hermes.order.domain.entity.OrderItem.{ItemCreateConfirmed, OrderItemCommand}
import com.jandra.hermes.order.domain.protocol._
import com.jandra.hermes.order.domain.valueobject.OrderState._
import com.jandra.hermes.order.domain.valueobject.{CustomerInfo, GetCustomerInfo, GetPriceInfo, GetProductInfo, OrderInfo}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * @Author: adria
  * @Description:
  * @Date: 10:54 2019/10/28
  * @Modified By:
  */

/**
  * FSM convert order
  * empty -> created
  * created -> approved/processing/cancelled/completed/rejected
  * processing -> approved/pending/cancelled/rejected
  * approved -> processing/pending/cancelled/completed/paid
  * pending -> processing/approved/cancelled
  * paid -> cancelled/delivered/completed
  * delivered -> cancelled/completed
  * completed ->
  */

object Order {

  val TypeKey: EntityTypeKey[OrderCommand] =
    EntityTypeKey[OrderCommand]("Order")

  // persist command
  sealed trait OrderFSMCommand[T <: OrderReply] extends OrderCommand {
    val replyTo: ActorRef[T]
  }

  private case class OrderPersist(replyTo: ActorRef[CreateOrderReply]) extends OrderFSMCommand[CreateOrderReply]

  case class ApproveOrder(replyTo: ActorRef[OrderReply]) extends OrderFSMCommand[OrderReply]

  case class ProcessOrder(replyTo: ActorRef[OrderReply]) extends OrderFSMCommand[OrderReply]

  case class PendOrder(replyTo: ActorRef[OrderReply]) extends OrderFSMCommand[OrderReply]

  case class PayOrder(paymentId: String, replyTo: ActorRef[OrderReply]) extends OrderFSMCommand[OrderReply]

  case class DeliverOrder(deliveryId: String, replyTo: ActorRef[OrderReply]) extends OrderFSMCommand[OrderReply]

  case class CompleteOrder(replyTo: ActorRef[OrderReply]) extends OrderFSMCommand[OrderReply]

  case class CancelOrder(replyTo: ActorRef[OrderReply]) extends OrderFSMCommand[OrderReply]

  case class RejectOrder(replyTo: ActorRef[OrderReply]) extends OrderFSMCommand[OrderReply]

  // type alias to reduce boilerplate
  type ReplyEffect = akka.persistence.typed.scaladsl.ReplyEffect[OrderEvent, Order]

  // State
  sealed trait Order extends CborSerializable {
    def applyCommand(cmd: OrderCommand): ReplyEffect

    def applyEvent(event: OrderEvent): Order
  }

  case class EmptyOrder(orderId: String, ctx: ActorContext[OrderCommand]) extends Order {
    val mockEntity = ctx.spawn(MockActorRef(), "MockActorRef")
    var createOrder: Option[CreateOrder] = None
    var customerInfo: Option[CustomerInfo] = None
    var createItemReady: Boolean = false
    var orderInfo: Option[OrderInfo] = None
    var itemConfirmList: List[OrderItem.ItemCreateConfirmed] = List.empty


    def confirmBehavior(orderId: String, ctx: ActorContext[OrderCommand]) =
      (customerInfo, createItemReady) match {
        case (Some(c), true) =>
          val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
          val orderStartDate = sdf.format(System.currentTimeMillis())
          val orderModifyDate = orderStartDate
          orderInfo = Some(OrderInfo(orderId,
            createOrder.get.createOrderData.orderName,
            orderStartDate,
            orderModifyDate,
            createOrder.get.createOrderData.storeId,
            createOrder.get.createOrderData.salesChannelId,
            createOrder.get.createOrderData.orderMode,
            createOrder.get.createOrderData.comments,
            Created,
            c,
            createOrder.get.createOrderData.orderAttrs,
            None,
            None))
          ctx.self ! OrderPersist(createOrder.get.replyTo)
        case (_, _) =>
      }

    override def applyCommand(cmd: OrderCommand): ReplyEffect =
      cmd match {
        case c: CreateOrder =>
          createOrder = Some(c)
          mockEntity ! GetCustomerInfo(c.createOrderData.customerId, ctx.self)
          for (item <- c.createOrderData.itemList) {
            val itemEntity = ctx.spawn(OrderItem(item.productId, item.quantity, ctx.self), item.productId)
            ctx.watch(itemEntity)
            mockEntity ! GetProductInfo(item.productId, itemEntity)
            mockEntity ! GetPriceInfo(item.productId, itemEntity)
          }
          Effect.noReply

        case itemCreateConfirmed: OrderItem.ItemCreateConfirmed =>
          itemConfirmList = itemCreateConfirmed :: itemConfirmList
          if (itemConfirmList.size == createOrder.get.createOrderData.itemList.size) {
            createItemReady = true
            confirmBehavior(orderId, ctx)
          }
          Effect.noReply

        case c: CustomerInfo =>
          customerInfo = Some(c)
          confirmBehavior(orderId, ctx)
          Effect.noReply

        case OrderPersist(replyTo) =>
          Effect.persist(OrderCreated(orderInfo.get, createOrder.get.createOrderData.itemList)).thenReply(replyTo)(_ => CreateOrderReply(orderInfo.get.orderId))

        case GetOrderInfo(_, replyTo) =>
          Effect.reply(replyTo)(OrderRejectedReply("Empty Order Can't Get Info"))
      }

    override def applyEvent(event: OrderEvent): Order =
      event match {
        case OrderCreated(orderInfo, itemList) => CreatedOrder(orderInfo, ctx, itemList)

        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [EmptyOrder]")
      }
  }



  case class CreatedOrder(orderInfo: OrderInfo, ctx: ActorContext[OrderCommand], itemList: List[CreateOrderItemData]) extends OpenedOrder {

    override def applyCommand(cmd: OrderCommand): ReplyEffect =
      cmd match {
        case GetOrderInfo(_, replyTo) =>
          chaseOrderInfo(replyTo)
          Effect.noReply

        case ApproveOrder(replyTo) =>
          Effect.persist(OrderApproved).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Approved))

        case ProcessOrder(replyTo) =>
          Effect.persist(OrderProcessing).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Processing))

        case RejectOrder(replyTo) =>
          Effect.persist(OrderRejected).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Rejected))

        case CompleteOrder(replyTo) =>
          Effect.persist(OrderCompleted).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Completed))

        case CancelOrder(replyTo) =>
          Effect.persist(OrderCancelled).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Cancelled))
      }

    override def applyEvent(event: OrderEvent): Order =
      event match {
        case OrderApproved =>
          ApprovedOrder(orderInfo.copy(orderState = Approved), ctx, itemList)

        case OrderProcessing =>
          ProcessingOrder(orderInfo.copy(orderState = Processing), ctx, itemList)

        case OrderRejected =>
          RejectedOrder(orderInfo.copy(orderState = Rejected), ctx, itemList)

        case OrderCompleted =>
          CompletedOrder(orderInfo.copy(orderState = Completed), ctx, itemList)

        case OrderCancelled =>
          CancelledOrder(orderInfo.copy(orderState = Cancelled), ctx, itemList)

        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [CreatedOrder]")
      }
  }

  case class ApprovedOrder(orderInfo: OrderInfo, ctx: ActorContext[OrderCommand], itemList: List[CreateOrderItemData]) extends OpenedOrder {

    override def applyCommand(cmd: OrderCommand): ReplyEffect =
      cmd match {
        case GetOrderInfo(_, replyTo) =>
          chaseOrderInfo(replyTo)
          Effect.noReply

        case ProcessOrder(replyTo) =>
          Effect.persist(OrderProcessing).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Processing))

        case PendOrder(replyTo) =>
          Effect.persist(OrderPending).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Pending))

        case PayOrder(paymentId, replyTo) =>
          Effect.persist(OrderPaid(paymentId)).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Paid))

        case CompleteOrder(replyTo) =>
          Effect.persist(OrderCompleted).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Completed))

        case CancelOrder(replyTo) =>
          Effect.persist(OrderCancelled).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Cancelled))
      }

    override def applyEvent(event: OrderEvent): Order =
      event match {
        case OrderProcessing =>
          ProcessingOrder(orderInfo.copy(orderState = Processing), ctx, itemList)

        case OrderPending =>
          PendingOrder(orderInfo.copy(orderState = Pending), ctx, itemList)

        case OrderPaid(p) =>
          PaidOrder(orderInfo.copy(orderState = Paid, paymentId = Some(p)), ctx, itemList)

        case OrderCompleted =>
          CompletedOrder(orderInfo.copy(orderState = Completed), ctx, itemList)

        case OrderCancelled =>
          CancelledOrder(orderInfo.copy(orderState = Cancelled), ctx, itemList)

        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [PendingOrder]")
      }
  }

  case class ProcessingOrder(orderInfo: OrderInfo, ctx: ActorContext[OrderCommand], itemList: List[CreateOrderItemData]) extends OpenedOrder {

    override def applyCommand(cmd: OrderCommand): ReplyEffect =
      cmd match {
        case GetOrderInfo(_, replyTo) =>
          chaseOrderInfo(replyTo)
          Effect.noReply

        case ApproveOrder(replyTo) =>
          Effect.persist(OrderApproved).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Approved))

        case PendOrder(replyTo) =>
          Effect.persist(OrderPending).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Pending))

        case RejectOrder(replyTo) =>
          Effect.persist(OrderRejected).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Rejected))

        case CancelOrder(replyTo) =>
          Effect.persist(OrderCancelled).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Cancelled))
      }

    override def applyEvent(event: OrderEvent): Order =
      event match {
        case OrderApproved =>
          ApprovedOrder(orderInfo.copy(orderState = Approved), ctx, itemList)

        case OrderPending =>
          PendingOrder(orderInfo.copy(orderState = Pending), ctx, itemList)

        case OrderRejected =>
          RejectedOrder(orderInfo.copy(orderState = Rejected), ctx, itemList)

        case OrderCancelled =>
          CancelledOrder(orderInfo.copy(orderState = Cancelled), ctx, itemList)

        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [PendingOrder]")
      }
  }

  case class PendingOrder(orderInfo: OrderInfo, ctx: ActorContext[OrderCommand], itemList: List[CreateOrderItemData]) extends OpenedOrder {

    override def applyCommand(cmd: OrderCommand): ReplyEffect =
      cmd match {
        case GetOrderInfo(_, replyTo) =>
          chaseOrderInfo(replyTo)
          Effect.noReply

        case ApproveOrder(replyTo) =>
          Effect.persist(OrderApproved).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Approved))

        case ProcessOrder(replyTo) =>
          Effect.persist(OrderProcessing).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Processing))

        case CancelOrder(replyTo) =>
          Effect.persist(OrderCancelled).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Cancelled))
      }

    override def applyEvent(event: OrderEvent): Order =
      event match {
        case OrderApproved =>
          ApprovedOrder(orderInfo.copy(orderState = Approved), ctx, itemList)

        case OrderProcessing =>
          ProcessingOrder(orderInfo.copy(orderState = Processing), ctx, itemList)

        case OrderCancelled =>
          CancelledOrder(orderInfo.copy(orderState = Cancelled), ctx, itemList)

        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [PendingOrder]")
      }
  }

  case class PaidOrder(orderInfo: OrderInfo, ctx: ActorContext[OrderCommand], itemList: List[CreateOrderItemData]) extends OpenedOrder {

    override def applyCommand(cmd: OrderCommand): ReplyEffect =
      cmd match {
        case GetOrderInfo(_, replyTo) =>
          chaseOrderInfo(replyTo)
          Effect.noReply

        case DeliverOrder(deliveryId, replyTo) =>
          Effect.persist(OrderDelivered(deliveryId)).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Delivered))

        case CompleteOrder(replyTo) =>
          Effect.persist(OrderCompleted).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Completed))

        case CancelOrder(replyTo) =>
          Effect.persist(OrderCancelled).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Cancelled))
      }

    override def applyEvent(event: OrderEvent): Order =
      event match {
        case OrderDelivered(d) =>
          DeliveredOrder(orderInfo.copy(orderState = Delivered, deliveryId = Some(d)), ctx, itemList)

        case OrderCompleted =>
          CompletedOrder(orderInfo.copy(orderState = Completed), ctx, itemList)

        case OrderCancelled =>
          CancelledOrder(orderInfo.copy(orderState = Cancelled), ctx, itemList)

        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [PendingOrder]")
      }
  }

  case class DeliveredOrder(orderInfo: OrderInfo, ctx: ActorContext[OrderCommand], itemList: List[CreateOrderItemData]) extends OpenedOrder {

    override def applyCommand(cmd: OrderCommand): ReplyEffect =
      cmd match {
        case GetOrderInfo(_, replyTo) =>
          chaseOrderInfo(replyTo)
          Effect.noReply

        case CompleteOrder(replyTo) =>
          Effect.persist(OrderCompleted).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Completed))

        case CancelOrder(replyTo) =>
          Effect.persist(OrderCancelled).thenReply(replyTo)(_ => CurrentOrderStatus(orderInfo.orderId, Cancelled))
      }

    override def applyEvent(event: OrderEvent): Order =
      event match {
        case OrderCompleted =>
          CompletedOrder(orderInfo.copy(orderState = Completed), ctx, itemList)

        case OrderCancelled =>
          CancelledOrder(orderInfo.copy(orderState = Cancelled), ctx, itemList)

        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [PendingOrder]")
      }
  }

  case class RejectedOrder(orderInfo: OrderInfo, ctx: ActorContext[OrderCommand], itemList: List[CreateOrderItemData]) extends OpenedOrder {

    override def applyCommand(cmd: OrderCommand): ReplyEffect =
      cmd match {
        case GetOrderInfo(_, replyTo) =>
          Effect.reply(replyTo)(OrderRejectedReply("Order Cancelled!"))
        case x: OrderFSMCommand[OrderReply] =>
          Effect.reply(x.replyTo)(OrderRejectedReply("Rejected Order Don't Accept Any Command!"))
      }

    override def applyEvent(event: OrderEvent): Order =
      throw new IllegalStateException(s"unexpected event [$event] in state [CancelledOrder]")
  }


  case class CompletedOrder(orderInfo: OrderInfo, ctx: ActorContext[OrderCommand], itemList: List[CreateOrderItemData]) extends OpenedOrder {

    override def applyCommand(cmd: OrderCommand): ReplyEffect =
      cmd match {
        case GetOrderInfo(_, replyTo) =>
          chaseOrderInfo(replyTo)
          Effect.noReply
        case x: OrderFSMCommand[OrderReply] =>
          Effect.reply(x.replyTo)(OrderRejectedReply("Completed Order Don't Accept Any Command!"))
      }

    override def applyEvent(event: OrderEvent): Order =
      throw new IllegalStateException(s"unexpected event [$event] in state [CompletedOrder]")
  }

  case class CancelledOrder(orderInfo: OrderInfo, ctx: ActorContext[OrderCommand], itemList: List[CreateOrderItemData]) extends OpenedOrder {

    override def applyCommand(cmd: OrderCommand): ReplyEffect =
      cmd match {
        case GetOrderInfo(_, replyTo) =>
          Effect.reply(replyTo)(OrderRejectedReply("Order Cancelled!"))
        case x: OrderFSMCommand[OrderReply] =>
          Effect.reply(x.replyTo)(OrderRejectedReply("Cancelled Order Don't Accept Any Command!"))
      }

    override def applyEvent(event: OrderEvent): Order =
      throw new IllegalStateException(s"unexpected event [$event] in state [CancelledOrder]")
  }

  // initial item actor & get order information
  trait OpenedOrder extends Order{
    val orderInfo: OrderInfo
    val itemList: List[CreateOrderItemData]
    val ctx: ActorContext[OrderCommand]
    var itemRefList = List[ActorRef[OrderItemCommand]]()
    for (item <- itemList) {
      if (ctx.child(item.productId).isEmpty) {
        val itemEntity = ctx.spawn(OrderItem(item.productId, item.quantity, ctx.self), item.productId)
        itemRefList = itemEntity :: itemRefList
        ctx.watch(itemEntity)
      } else {
        itemRefList = ctx.child(item.productId).get.asInstanceOf[ActorRef[OrderItem.OrderItemCommand]] :: itemRefList
      }
    }

    def chaseOrderInfo(replyTo: ActorRef[OrderReply]): Unit = {
      implicit val timeout: Timeout = 2.seconds
      implicit val ec = ctx.system.executionContext
      implicit val scheduler: Scheduler = ctx.system.scheduler
      val listFuture: List[Future[OrderItem.OrderItemReply]] = itemRefList.map { i =>
        val f: Future[OrderItem.OrderItemReply] = i.ask(ref => OrderItem.GetOrderItemInfo(ref))
        f
      }
      val futureList: Future[List[OrderItem.OrderItemReply]] = Future.sequence(listFuture).map(_.distinct)
      futureList.onComplete {
        case Success(l) =>
          if (l.exists(x => x.getClass == classOf[OrderItem.OrderItemRejected])) {
            replyTo ! OrderRejectedReply("get item info error!")
          }
          else {
            replyTo ! CurrentOrderInfo(orderInfo, l.map(_.asInstanceOf[OrderItem.CurrentOrderItemInfo]))
          }
        case _ => Effect.noReply
      }
    }
  }

  def apply(orderShardId: String, orderId: String): Behavior[OrderCommand] = {
    Behaviors.setup[OrderCommand] { context =>
      context.log.info(context.self.path.name + " order created!")
      import akka.actor.typed.scaladsl.adapter._
      val classicSystem: akka.actor.ActorSystem = context.system.toClassic
      SharedLeveldb.startupSharedJournal(classicSystem, false, context.self.path.root)
      EventSourcedBehavior.withEnforcedReplies[OrderCommand, OrderEvent, Order](
        PersistenceId.apply(orderShardId, orderId),
        EmptyOrder(orderId, context),
        (state, cmd) => state.applyCommand(cmd),
        (state, event) => state.applyEvent(event))
    }
  }

}
