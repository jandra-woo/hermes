package com.jandra.hermes.order.infrastructure.repositories

/**
  * @Author: adria
  * @Description:
  * @Date: 17:05 2019/9/30
  * @Modified By:
  */

import akka.actor.Props
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.journal.leveldb.SharedLeveldbStore
import akka.persistence.journal.leveldb.SharedLeveldbJournal
import com.jandra.hermes.order.domain.protocol.OrderCommand
import com.jandra.hermes.order.domain.valueobject.OrderState._
import com.jandra.hermes.order.domain.valueobject.OrderInfo
import com.jandra.hermes.util.{CborSerializable, SharedLeveldb}

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
          copy(orderInfo = orderInfo.copy(paymentId = Some(pId), orderState = PAID))
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
          copy(orderInfo = orderInfo.copy(deliveryId = Some(dId), orderState = DELIVERED))
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
          copy(orderInfo = orderInfo.copy(orderState = COMPLETED))
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
      implicit val classicSystem: akka.actor.ActorSystem = context.system.toClassic
      val store = context.actorOf(Props[SharedLeveldbStore], "store")
      SharedLeveldbJournal.setStore(store, classicSystem)
      EventSourcedBehavior.withEnforcedReplies[Command[_], Event, OrderRepository](
        persistenceId,
        EmptyOrder,
        (state, cmd) => state.applyCommand(cmd),
        (state, event) => state.applyEvent(event))
    }
  }
}


//object AccountEntity {
//
//  // Command
//  sealed trait Command[Reply <: CommandReply] extends CborSerializable {
//    def replyTo: ActorRef[Reply]
//  }
//
//  final case class CreateAccount(replyTo: ActorRef[OperationResult]) extends Command[OperationResult]
//
//  final case class Deposit(amount: BigDecimal, replyTo: ActorRef[OperationResult]) extends Command[OperationResult]
//
//  final case class Withdraw(amount: BigDecimal, replyTo: ActorRef[OperationResult]) extends Command[OperationResult]
//
//  final case class GetBalance(replyTo: ActorRef[CurrentBalance]) extends Command[CurrentBalance]
//
//  final case class CloseAccount(replyTo: ActorRef[OperationResult]) extends Command[OperationResult]
//
//  // Reply
//  sealed trait CommandReply extends CborSerializable
//
//  sealed trait OperationResult extends CommandReply
//
//  case object Confirmed extends OperationResult
//
//  final case class Rejected(reason: String) extends OperationResult
//
//  final case class CurrentBalance(balance: BigDecimal) extends CommandReply
//
//  // Event
//  sealed trait Event extends CborSerializable
//
//  case object AccountCreated extends Event
//
//  case class Deposited(amount: BigDecimal) extends Event
//
//  case class Withdrawn(amount: BigDecimal) extends Event
//
//  case object AccountClosed extends Event
//
//  val Zero = BigDecimal(0)
//
//  // type alias to reduce boilerplate
//  type ReplyEffect = akka.persistence.typed.scaladsl.ReplyEffect[Event, Account]
//
//  // State
//  sealed trait Account extends CborSerializable {
//    def applyCommand(cmd: Command[_]): ReplyEffect
//
//    def applyEvent(event: Event): Account
//  }
//
//  case object EmptyAccount extends Account {
//    override def applyCommand(cmd: Command[_]): ReplyEffect =
//      cmd match {
//        case CreateAccount(replyTo) =>
//          Effect.persist(AccountCreated).thenReply(replyTo)(_ => Confirmed)
//        case _ =>
//          // CreateAccount before handling any other commands
//          Effect.unhandled.thenNoReply()
//      }
//
//    override def applyEvent(event: Event): Account =
//      event match {
//        case AccountCreated => OpenedAccount(Zero)
//        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [EmptyAccount]")
//      }
//  }
//
//  case class OpenedAccount(balance: BigDecimal) extends Account {
//    require(balance >= Zero, "Account balance can't be negative")
//
//    override def applyCommand(cmd: Command[_]): ReplyEffect =
//      cmd match {
//        case Deposit(amount, replyTo) =>
//          Effect.persist(Deposited(amount)).thenReply(replyTo)(_ => Confirmed)
//
//        case Withdraw(amount, replyTo) =>
//          if (canWithdraw(amount))
//            Effect.persist(Withdrawn(amount)).thenReply(replyTo)(_ => Confirmed)
//          else
//            Effect.reply(replyTo)(Rejected(s"Insufficient balance $balance to be able to withdraw $amount"))
//
//        case GetBalance(replyTo) =>
//          Effect.reply(replyTo)(CurrentBalance(balance))
//
//        case CloseAccount(replyTo) =>
//          if (balance == Zero)
//            Effect.persist(AccountClosed).thenReply(replyTo)(_ => Confirmed)
//          else
//            Effect.reply(replyTo)(Rejected("Can't close account with non-zero balance"))
//
//        case CreateAccount(replyTo) =>
//          Effect.reply(replyTo)(Rejected("Account is already created"))
//
//      }
//
//    override def applyEvent(event: Event): Account =
//      event match {
//        case Deposited(amount) => copy(balance = balance + amount)
//        case Withdrawn(amount) => copy(balance = balance - amount)
//        case AccountClosed => ClosedAccount
//        case AccountCreated => throw new IllegalStateException(s"unexpected event [$event] in state [OpenedAccount]")
//      }
//
//    def canWithdraw(amount: BigDecimal): Boolean = {
//      balance - amount >= Zero
//    }
//
//  }
//
//  case object ClosedAccount extends Account {
//    override def applyCommand(cmd: Command[_]): ReplyEffect =
//      cmd match {
//        case c@(_: Deposit | _: Withdraw) =>
//          Effect.reply(c.replyTo)(Rejected("Account is closed"))
//        case GetBalance(replyTo) =>
//          Effect.reply(replyTo)(CurrentBalance(Zero))
//        case CloseAccount(replyTo) =>
//          Effect.reply(replyTo)(Rejected("Account is already closed"))
//        case CreateAccount(replyTo) =>
//          Effect.reply(replyTo)(Rejected("Account is already created"))
//      }
//
//    override def applyEvent(event: Event): Account =
//      throw new IllegalStateException(s"unexpected event [$event] in state [ClosedAccount]")
//  }
//
//  val TypeKey: EntityTypeKey[Command[_]] =
//    EntityTypeKey[Command[_]]("Account")
//
//  def apply(persistenceId: PersistenceId): Behavior[Command[_]] = {
//    EventSourcedBehavior.withEnforcedReplies[Command[_], Event, Account](
//      persistenceId,
//      EmptyAccount,
//      (state, cmd) => state.applyCommand(cmd),
//      (state, event) => state.applyEvent(event))
//  }
//
//}