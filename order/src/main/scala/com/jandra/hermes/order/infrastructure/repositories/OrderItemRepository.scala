package com.jandra.hermes.order.infrastructure.repositories

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.jandra.hermes.order.domain.entity.OrderItemTyped
import com.jandra.hermes.order.domain.valueobject.{OrderInfo, PriceInfo, ProductInfo}
import com.jandra.hermes.order.domain.valueobject.OrderState.PAID
import com.jandra.hermes.util.CborSerializable

/**
  * @Author: adria
  * @Description:
  * @Date: 11:19 2019/10/22
  * @Modified By:
  */


case class OrderItemInfo private(productId: String,
                                 productInfo: ProductInfo,
                                 priceInfo: PriceInfo,
                                 quantity: Int) {
}


object OrderItemRepository {

  // Command
  sealed trait Command[Reply <: CommandReply] extends CborSerializable {
    def replyTo: ActorRef[Reply]
  }

  final case class CreateOrderItem(orderItemInfo: OrderItemInfo, replyTo: ActorRef[OperationResult]) extends Command[OperationResult]

  final case class CloseOrderItem(replyTo: ActorRef[OperationResult]) extends Command[OperationResult]

  final case class GetItem(replyTo: ActorRef[OperationResult]) extends Command[OperationResult]

  // Reply
  sealed trait CommandReply extends CborSerializable with OrderItemTyped.OrderItemCommand

  sealed trait OperationResult extends CommandReply

  case object Confirmed extends OperationResult

  final case class Rejected(reason: String) extends OperationResult

  final case class CurrentItem(itemInfo: OrderItemInfo) extends OperationResult

  // Event
  sealed trait Event extends CborSerializable

  case class ItemCreated(orderItemInfo: OrderItemInfo) extends Event

  case object ItemClosed extends Event

  // type alias to reduce boilerplate
  type ReplyEffect = akka.persistence.typed.scaladsl.ReplyEffect[Event, OrderItemRepository]

  // State
  sealed trait OrderItemRepository extends CborSerializable {
    def applyCommand(cmd: Command[_]): ReplyEffect

    def applyEvent(event: Event): OrderItemRepository
  }

  case object EmptyOrderItem extends OrderItemRepository {
    override def applyCommand(cmd: Command[_]): ReplyEffect =
      cmd match {
        case CreateOrderItem(orderItemInfo, replyTo) =>
          Effect.persist(ItemCreated(orderItemInfo)).thenReply(replyTo)(_ => Confirmed)
        case _ =>
          // CreateOrder before handling any other commands
          Effect.unhandled.thenNoReply()
      }

    override def applyEvent(event: Event): OrderItemRepository =
      event match {
        case ItemCreated(orderItemInfo) => OpenedOrderItem(orderItemInfo)
        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [EmptyOrder]")
      }
  }

  case class OpenedOrderItem(orderItemInfo: OrderItemInfo) extends OrderItemRepository {
    override def applyCommand(cmd: Command[_]): ReplyEffect =
      cmd match {
        case GetItem(replyTo) =>
          Effect.reply(replyTo)(CurrentItem(orderItemInfo))
        case CloseOrderItem(replyTo) =>
          Effect.persist(ItemClosed).thenReply(replyTo)(_ => Confirmed)
        case _ =>
          Effect.unhandled.thenNoReply()
      }

    override def applyEvent(event: Event): OrderItemRepository =
      event match {
        case ItemClosed => ClosedOrderItem
        case ItemCreated(_) => throw new IllegalStateException(s"unexpected event [$event] in state [OpenedOrderItem]")
      }
  }

  case object ClosedOrderItem extends OrderItemRepository {
    override def applyCommand(cmd: Command[_]): ReplyEffect =
      cmd match {
        case GetItem(replyTo) =>
          Effect.reply(replyTo)(Rejected("Item is closed"))
        case CloseOrderItem(replyTo) =>
          Effect.reply(replyTo)(Rejected("Account is already closed"))
        case CreateOrderItem(_, replyTo) =>
          Effect.reply(replyTo)(Rejected("Account is already created"))
      }

    override def applyEvent(event: Event): OrderItemRepository =
      throw new IllegalStateException(s"unexpected event [$event] in state [ClosedAccount]")
  }

  def apply(persistenceId: PersistenceId): Behavior[Command[_]] = {
    EventSourcedBehavior.withEnforcedReplies[Command[_], Event, OrderItemRepository](
      persistenceId,
      EmptyOrderItem,
      (state, cmd) => state.applyCommand(cmd),
      (state, event) => state.applyEvent(event))
  }

}
