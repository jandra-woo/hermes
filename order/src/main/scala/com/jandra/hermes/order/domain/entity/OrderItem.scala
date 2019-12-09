package com.jandra.hermes.order.domain.entity

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.jandra.hermes.common.util.{CborSerializable, SharedLeveldb}
import com.jandra.hermes.order.domain.valueobject.{OrderItemInfo, PriceInfo, ProductInfo}
import com.jandra.hermes.common.util.CborSerializable
import com.jandra.hermes.order.domain.protocol.OrderCommand

/**
  * @Author: adria
  * @Description:
  * @Date: 10:34 2019/10/29
  * @Modified By:
  */

object OrderItem {

  //Command
  trait OrderItemCommand extends CborSerializable

  final case class CreateOrderItem(orderItemInfo: OrderItemInfo, replyTo: ActorRef[OrderItemReply]) extends OrderItemCommand

  final case class CloseOrderItem(replyTo: ActorRef[OrderItemReply]) extends OrderItemCommand

  final case class GetOrderItemInfo(replyTo: ActorRef[OrderItemReply]) extends OrderItemCommand

  //Reply
  sealed trait OrderItemReply extends OrderCommand

  final case class ItemCreateConfirmed(productId: String, ref: ActorRef[OrderItemCommand]) extends OrderItemReply

  final case class ItemCloseConfirmed(productId: String) extends OrderItemReply

  //  final case class CreatedItem(itemId: String) extends OrderItemReply

  final case class CurrentOrderItemInfo(orderItemInfo: OrderItemInfo) extends OrderItemReply

  final case class OrderItemRejected(reason: String) extends OrderItemReply

  //  final case class ItemInfo(productId: String,
  //                            productInfo: ProductInfo,
  //                            priceInfo: PriceInfo,
  //                            quantity: Int,
  //                            deliveryId: String) extends OrderItemReply

  // Event
  sealed trait OrderItemEvent extends CborSerializable

  case class ItemCreated(orderItemInfo: OrderItemInfo) extends OrderItemEvent

  case object ItemClosed extends OrderItemEvent

  private var productInfo: Option[ProductInfo] = None

  private var priceInfo: Option[PriceInfo] = None

  // type alias to reduce boilerplate
  type ReplyEffect = akka.persistence.typed.scaladsl.ReplyEffect[OrderItemEvent, OrderItem]

  // State
  sealed trait OrderItem extends CborSerializable {
    def applyCommand(cmd: OrderItemCommand): ReplyEffect

    def applyEvent(event: OrderItemEvent): OrderItem
  }

  case class EmptyOrderItem(productId: String, quantity: Int, replyTo: ActorRef[OrderItemReply], ctx: ActorContext[OrderItemCommand]) extends OrderItem {
    override def applyCommand(cmd: OrderItemCommand): ReplyEffect =
      cmd match {
        case CreateOrderItem(orderItemInfo, replyTo) =>
          Effect.persist(ItemCreated(orderItemInfo)).thenReply(replyTo)(_ => ItemCreateConfirmed(orderItemInfo.productId, ctx.self))
        case m: ProductInfo =>
          productInfo = Some(m)
          checkInfo(productId, quantity, replyTo, ctx)
          Effect.noReply
        case n: PriceInfo =>
          priceInfo = Some(n)
          checkInfo(productId, quantity, replyTo, ctx)
          Effect.noReply
        case _ =>
          // CreateOrder before handling any other commands
          Effect.unhandled.thenNoReply()
      }

    override def applyEvent(event: OrderItemEvent): OrderItem =
      event match {
        case ItemCreated(orderItemInfo) => OpenedOrderItem(orderItemInfo)
        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [EmptyOrder]")
      }
  }


  case class OpenedOrderItem(orderItemInfo: OrderItemInfo) extends OrderItem {
    override def applyCommand(cmd: OrderItemCommand): ReplyEffect =
      cmd match {
        case GetOrderItemInfo(replyTo) =>
          Effect.reply(replyTo)(CurrentOrderItemInfo(orderItemInfo))
        case CloseOrderItem(replyTo) =>
          Effect.persist(ItemClosed).thenReply(replyTo)(_ => ItemCloseConfirmed(orderItemInfo.productId))
        case _ =>
          Effect.unhandled.thenNoReply()
      }

    override def applyEvent(event: OrderItemEvent): OrderItem =
      event match {
        case ItemClosed => ClosedOrderItem
        case ItemCreated(_) => throw new IllegalStateException(s"unexpected event [$event] in state [OpenedOrderItem]")
      }
  }

  case object ClosedOrderItem extends OrderItem {
    override def applyCommand(cmd: OrderItemCommand): ReplyEffect =
      cmd match {
        case GetOrderItemInfo(replyTo) =>
          Effect.reply(replyTo)(OrderItemRejected("Item is closed"))
        case CloseOrderItem(replyTo) =>
          Effect.reply(replyTo)(OrderItemRejected("Order Item is already closed"))
        case CreateOrderItem(_, replyTo) =>
          Effect.reply(replyTo)(OrderItemRejected("Order Item is already created"))
      }

    override def applyEvent(event: OrderItemEvent): OrderItem =
      throw new IllegalStateException(s"unexpected event [$event] in state [ClosedOrderItem]")
  }

  def checkInfo(productId: String, quantity: Int, replyTo: ActorRef[OrderItemReply], ctx: ActorContext[OrderItemCommand]) =
    (productInfo, priceInfo) match {
      case (Some(m), Some(n)) =>
        //            val orderItemRepository = context.spawn(OrderItemRepository(PersistenceId(context.self.path.parent.name, productId)), "orderItemRepository")
        val orderItemInfo = OrderItemInfo(productId, m, n, quantity)
        //            orderItemRepository ! OrderItemRepository.CreateOrderItem(orderItemInfo, context.self)

        ctx.self ! CreateOrderItem(orderItemInfo, replyTo)
      case (_, _) =>
    }

  def apply(productId: String, quantity: Int, replyTo: ActorRef[OrderItemReply]): Behavior[OrderItemCommand] = {
    Behaviors.setup[OrderItemCommand] { context =>
      context.log.info(context.self.path.parent.name + "-" + context.self.path.name + " order item created.")
      import akka.actor.typed.scaladsl.adapter._
      val classicSystem: akka.actor.ActorSystem = context.system.toClassic
      SharedLeveldb.startupSharedJournal(classicSystem, false, context.self.path.root)


      EventSourcedBehavior.withEnforcedReplies[OrderItemCommand, OrderItemEvent, OrderItem](
        PersistenceId(context.self.path.parent.name, productId),
        EmptyOrderItem(productId, quantity, replyTo, context),
        (state, cmd) => state.applyCommand(cmd),
        (state, event) => state.applyEvent(event))


      //      Behaviors.receiveMessage[OrderItemCommand] {
      //        case m: ProductInfo =>
      //          productInfo = Some(m)
      //          nextBehavior()
      //        case n: PriceInfo =>
      //          priceInfo = Some(n)
      //          nextBehavior()
      //        case OrderItemRepository.Confirmed(proId) =>
      //          replyTo ! ItemConfirmed(proId)
      //          Behaviors.same
      //        case GetItemInfo(replyTo) =>
      //          replyTo ! ItemInfo(productId, productInfo.get, priceInfo.get, quantity, deliveryId.getOrElse(""))
      //          Behaviors.same
      //        case c: CurrentItem =>
      //          Behaviors.same
      //      }
    }
  }
}
