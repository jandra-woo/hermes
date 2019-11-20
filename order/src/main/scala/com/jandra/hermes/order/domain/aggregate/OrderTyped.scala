package com.jandra.hermes.order.domain.aggregate

import java.text.SimpleDateFormat

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import com.jandra.hermes.order.application.MockActorRef
import com.jandra.hermes.order.application.OrderRestRoutes.CreateOrderRestReply
import com.jandra.hermes.order.domain.entity.OrderItemTyped
import com.jandra.hermes.order.domain.entity.OrderItemTyped.ItemConfirmed
import com.jandra.hermes.order.domain.protocol.{CreateOrder, OrderCommand, OrderReply}
import com.jandra.hermes.order.domain.valueobject.{CustomerInfo, GetCustomerInfo, GetPriceInfo, GetProductInfo, OrderInfo}
import com.jandra.hermes.order.infrastructure.repositories.{OrderItemRepository, OrderRepository}
import com.jandra.hermes.order.domain.valueobject.OrderState._
import com.jandra.hermes.order.infrastructure.repositories.OrderRepository.OrderRepositoryConfirmed

/**
  * @Author: adria
  * @Description:
  * @Date: 10:54 2019/10/28
  * @Modified By:
  */

object OrderTyped {

  //Reply
  sealed trait CommandReply extends OrderReply

  case class OrderCreated(orderId: String) extends OrderReply

  val TypeKey: EntityTypeKey[OrderCommand] =
    EntityTypeKey[OrderCommand]("OrderTyped")

  var customerInfo: Option[CustomerInfo] = None

  var itemConfirm: Option[OrderItemTyped.CommandReply] = None

  var createOrder: Option[CreateOrder] = None



  //Behavior
  def apply(orderShardId: String, orderId: String): Behavior[OrderCommand] = {
    Behaviors.setup[OrderCommand] { context =>
      context.log.info(context.self.path.name + " order created!")

      val mockEntity = context.spawn(MockActorRef(), "MockActorRef")

//      val productEntity: ActorRef[GetProductInfo] = ???
//
//      val priceEntity: ActorRef[GetPriceInfo] = ???
//
//      val customerEntity: ActorRef[GetCustomerInfo] = ???

      val orderRepository: ActorRef[OrderRepository.Command[_]] = context.spawn(OrderRepository(PersistenceId(orderShardId, orderId)), "orderRepository")

      def confirmBehavior(): Behavior[OrderCommand] =
        (customerInfo, itemConfirm) match {
          case (Some(c), Some(_)) =>
            val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
            val orderStartDate = sdf.format(System.currentTimeMillis())
            val orderModifyDate = orderStartDate
            val orderInfo = OrderInfo(orderId,
              createOrder.get.createOrderData.orderName,
              orderStartDate,
              orderModifyDate,
              createOrder.get.createOrderData.storeId,
              createOrder.get.createOrderData.salesChannelId,
              createOrder.get.createOrderData.orderMode,
              createOrder.get.createOrderData.comments,
              CREATED,
              c,
              createOrder.get.createOrderData.orderAttrs,
              None,
              None)
            orderRepository ! OrderRepository.CreateOrder(orderInfo, context.self)
            Behaviors.same
          case _ =>
            Behaviors.same
        }

      Behaviors.receiveMessage[OrderCommand] {
        case c: CreateOrder =>
          createOrder = Some(c)
          mockEntity ! GetCustomerInfo(c.createOrderData.customerId, context.self)
          //          customerEntity ! GetCustomerInfo(c.createOrderData.customerId, context.self)
          for (item <- c.createOrderData.itemList) {
            val itemEntity = context.spawn(OrderItemTyped(item.productId, item.quantity, context.self), orderId + "-" + item.productId)
            mockEntity ! GetProductInfo(item.productId, itemEntity)
            mockEntity ! GetPriceInfo(item.productId, itemEntity)
            //            productEntity ! GetProductInfo(item.productId, itemEntity)
            //            priceEntity ! GetPriceInfo(item.productId, itemEntity)
          }
          Behaviors.same
        case ItemConfirmed =>
          itemConfirm = Some(ItemConfirmed)
          confirmBehavior()
        case c: CustomerInfo =>
          customerInfo = Some(c)
          confirmBehavior()
        case OrderRepositoryConfirmed =>
          createOrder.get.replyTo ! CreateOrderRestReply(orderId)
          Behaviors.same
      }
    }
  }

//  def nextBehavior(orderId: String, replyTo: ActorRef[CreateOrderRestReply], orderRepository: ActorRef[OrderRepository.Command[_]]): Behavior[OrderCommand] = {
//    Behaviors.setup[OrderCommand] { context =>
//      (customerInfo, itemConfirm) match {
//        case (Some(c), Some(_)) =>
//          val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
//          val orderStartDate = sdf.format(System.currentTimeMillis())
//          val orderModifyDate = orderStartDate
//          val orderInfo = OrderInfo(orderId,
//            createOrder.get.createOrderData.orderName,
//            orderStartDate,
//            orderModifyDate,
//            createOrder.get.createOrderData.storeId,
//            createOrder.get.createOrderData.salesChannelId,
//            createOrder.get.createOrderData.orderMode,
//            createOrder.get.createOrderData.comments,
//            CREATED,
//            c,
//            createOrder.get.createOrderData.orderAttrs,
//            None,
//            None)
//          orderRepository ! OrderRepository.CreateOrder(orderInfo, context.self)
//          receiveBehavior(orderId, replyTo)
//        case _ => receiveBehavior(orderId, replyTo)
//      }
//    }
//  }
//
//    def receiveBehavior(orderId: String, replyTo: ActorRef[CreateOrderRestReply]): Behavior[OrderCommand] = {
//      println("------receiveBehavior--------")
//      Behaviors.receiveMessage[OrderCommand] {
//        case ItemConfirmed =>
//          itemConfirm = Some(ItemConfirmed)
//          nextBehavior(replyTo)
//        case c: CustomerInfo =>
//          println("~~~~~~~~~customerinfo~~~~~~~~~")
//          customerInfo = Some(c)
//          nextBehavior(replyTo)
//        case OrderRepositoryConfirmed =>
//          replyTo ! CreateOrderRestReply(orderId)
//          Behaviors.same
//      }
//    }

}
