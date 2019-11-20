package com.jandra.hermes.order.domain.entity

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import com.jandra.hermes.order.domain.aggregate.OrderTyped
import com.jandra.hermes.order.domain.protocol.OrderCommand
import com.jandra.hermes.order.domain.valueobject.{PriceInfo, ProductInfo}
import com.jandra.hermes.order.infrastructure.repositories.{OrderItemInfo, OrderItemRepository}

/**
  * @Author: adria
  * @Description:
  * @Date: 10:34 2019/10/29
  * @Modified By:
  */

object OrderItemTyped {

  //Command
  trait OrderItemCommand

//  final case class CreateOrderItem(productId: String,
//                                   quantity: Int,
//                                   replyTo: ActorRef[CommandReply]) extends Command

  final case class DeliverOrderItem(replyTo: ActorRef[CommandReply]) extends OrderItemCommand

  final case class GetItemInfo(itemId: String,
                               replyTo: ActorRef[CommandReply]) extends OrderItemCommand

  //Reply
  sealed trait CommandReply extends OrderCommand

  case object ItemConfirmed extends CommandReply

  final case class CreatedItem(itemId: String) extends CommandReply

  final case class ItemInfo(productId: String,
                            productInfo: ProductInfo,
                            priceInfo: PriceInfo,
                            deliveryId: String) extends CommandReply

  def apply(productId: String, quantity: Int, replyTo: ActorRef[CommandReply]): Behavior[OrderItemCommand] = {
    Behaviors.setup[OrderItemCommand] { context =>
      context.log.info(context.self.path.name + " order item created.")

      var productInfo: Option[ProductInfo] = None

      var priceInfo: Option[PriceInfo] = None

      def nextBehavior(): Behavior[OrderItemCommand] =
        (productInfo, priceInfo) match {
          case (Some(m), Some(n)) =>
            println("++++" + context.self.path.parent.name)
            val orderItemRepository = context.spawn(OrderItemRepository(PersistenceId(context.self.path.parent.name, productId)), "orderItemRepository")
            val orderItemInfo = OrderItemInfo(productId, m, n, quantity)
            orderItemRepository ! OrderItemRepository.CreateOrderItem(orderItemInfo, context.self)
            Behaviors.same
          case _ =>
            Behaviors.same
        }

      Behaviors.receiveMessage[OrderItemCommand] {
        case m: ProductInfo =>
          productInfo = Some(m)
          nextBehavior()
        case n: PriceInfo =>
          priceInfo = Some(n)
          nextBehavior()
        case OrderItemRepository.Confirmed =>
          replyTo ! ItemConfirmed
          Behaviors.same
      }
    }
  }
}
