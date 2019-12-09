package com.jandra.hermes.order.infrastructure.repositories

/**
  * @Author: adria
  * @Description:
  * @Date: 17:05 2019/9/30
  * @Modified By:
  */

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.jandra.hermes.common.util.{CborSerializable, SharedLeveldb}
import com.jandra.hermes.order.domain.protocol.OrderCommand
import com.jandra.hermes.order.domain.valueobject.{OrderInfo, OrderState}

@Deprecated
object OrderRepository {

  // Command
  sealed trait Command[Reply <: CommandReply] extends CborSerializable {
    def replyTo: ActorRef[Reply]
  }

  final case class CreateOrder(orderInfo: OrderInfo, replyTo: ActorRef[OperationResult]) extends Command[OperationResult]

  final case class GetOrderInfo(replyTo: ActorRef[OperationResult]) extends Command[OperationResult]

  final case class Paid(paymentId: String, replyTo: ActorRef[OperationResult]) extends Command[OperationResult]

  final case class Delivered(deliveryId: String, replyTo: ActorRef[OperationResult]) extends Command[OperationResult]

  final case class Completed(replyTo: ActorRef[OperationResult]) extends Command[OperationResult]

  // Reply
  sealed trait CommandReply extends CborSerializable with OrderCommand

  sealed trait OperationResult extends CommandReply

  case object OrderRepositoryConfirmed extends OperationResult

  final case class CurrentOrderInfo(orderInfo: OrderInfo) extends OperationResult

  final case class Rejected(reason: String) extends OperationResult

  // Event
  sealed trait Event extends CborSerializable

  case class OrderCreated(orderInfo: OrderInfo) extends Event

  case class OrderPaid(paymentId: String) extends Event

  case class OrderDelivered(deliveryId: String) extends Event

  case object OrderCompleted extends Event

  // type alias to reduce boilerplate
  type ReplyEffect = akka.persistence.typed.scaladsl.ReplyEffect[Event, OrderRepository]

  // State
  sealed trait OrderRepository extends CborSerializable {
    def applyCommand(cmd: Command[_]): ReplyEffect

    def applyEvent(event: Event): OrderRepository
  }

  case object EmptyOrder extends OrderRepository {
    override def applyCommand(cmd: Command[_]): ReplyEffect =
      cmd match {
        case CreateOrder(orderInfo, replyTo) =>
          Effect.persist(OrderCreated(orderInfo)).thenReply(replyTo)(_ => OrderRepositoryConfirmed)

        case GetOrderInfo(replyTo) =>
          Effect.reply(replyTo)(Rejected("Order has not been created!"))

        case _ =>
          // CreateOrder before handling any other commands
          Effect.unhandled.thenNoReply()
      }

    override def applyEvent(event: Event): OrderRepository =
      event match {
        case OrderCreated(orderInfo) => OpenedOrder(orderInfo)

        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [EmptyOrder]")
      }
  }

  case class OpenedOrder(orderInfo: OrderInfo) extends OrderRepository {
    override def applyCommand(cmd: Command[_]): ReplyEffect =
      cmd match {
        case GetOrderInfo(replyTo) =>
          Effect.reply(replyTo)(CurrentOrderInfo(orderInfo))

        case Paid(paymentId, replyTo) =>
          Effect.persist(OrderPaid(paymentId)).thenReply(replyTo)(_ => OrderRepositoryConfirmed)

        case CreateOrder(_, replyTo) =>
          Effect.reply(replyTo)(Rejected("Order is already created!"))
      }

    override def applyEvent(event: Event): OrderRepository =
      event match {
        case OrderPaid(pId) =>
          copy(orderInfo = orderInfo.copy(paymentId = Some(pId), orderState = OrderState.Paid))
          PaidOrder(orderInfo)
      }
  }

  case class PaidOrder(orderInfo: OrderInfo) extends OrderRepository {
    override def applyCommand(cmd: Command[_]): ReplyEffect =
      cmd match {
        case GetOrderInfo(replyTo) =>
          Effect.reply(replyTo)(CurrentOrderInfo(orderInfo))

        case Delivered(deliveryId, replyTo) =>
          Effect.persist(OrderDelivered(deliveryId)).thenReply(replyTo)(_ => OrderRepositoryConfirmed)
      }

    override def applyEvent(event: Event): OrderRepository =
      event match {
        case OrderDelivered(dId) =>
          copy(orderInfo = orderInfo.copy(deliveryId = Some(dId), orderState = OrderState.Delivered))
          DeliveredOrder(orderInfo)
      }
  }

  case class DeliveredOrder(orderInfo: OrderInfo) extends OrderRepository {
    override def applyCommand(cmd: Command[_]): ReplyEffect =
      cmd match {
        case GetOrderInfo(replyTo) =>
          Effect.reply(replyTo)(CurrentOrderInfo(orderInfo))

        case Completed(replyTo) =>
          Effect.persist(OrderCompleted).thenReply(replyTo)(_ => OrderRepositoryConfirmed)
      }

    override def applyEvent(event: Event): OrderRepository =
      event match {
        case OrderCompleted =>
          copy(orderInfo = orderInfo.copy(orderState = OrderState.Completed))
      }
  }

  case class CompletedOrder(orderInfo: OrderInfo) extends OrderRepository {
    override def applyCommand(cmd: Command[_]): ReplyEffect =
      cmd match {
        case GetOrderInfo(replyTo) =>
          Effect.reply(replyTo)(CurrentOrderInfo(orderInfo))
      }

    override def applyEvent(event: Event): OrderRepository =
      throw new IllegalStateException(s"unexpected event [$event] in state [CompletedOrder]")
  }

  def apply(persistenceId: PersistenceId): Behavior[Command[_]] = {
    Behaviors.setup[Command[_]] { context =>
      import akka.actor.typed.scaladsl.adapter._
      val classicSystem: akka.actor.ActorSystem = context.system.toClassic
      SharedLeveldb.startupSharedJournal(classicSystem, false, context.self.path.root)
      EventSourcedBehavior.withEnforcedReplies[Command[_], Event, OrderRepository](
        persistenceId,
        EmptyOrder,
        (state, cmd) => state.applyCommand(cmd),
        (state, event) => state.applyEvent(event))
    }
  }
}


