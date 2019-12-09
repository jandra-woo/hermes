package com.jandra.hermes.order.domain.service

import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.util.Timeout
import com.jandra.hermes.order.domain.aggregate.Order
import com.jandra.hermes.order.domain.aggregate.Order._
import com.jandra.hermes.order.domain.protocol._
import com.jandra.hermes.order.domain.valueobject.OrderState._
import com.jandra.hermes.util.SnowFlakeIdService

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

// entry point to order domain cluster and sharded order entity
object OrderDomainService {
  implicit val timeout: Timeout = 5.seconds

  def apply(): Behavior[OrderCommand] = {
    Behaviors.setup { context =>
      val sharding = ClusterSharding(context.system)
      sharding.init(Entity(Order.TypeKey) { entityContext =>
        Order(entityContext.entityTypeKey.name, entityContext.entityId)
      })


      val snowFlakeService: ActorRef[SnowFlakeIdService.SnowFlakeCommand] = context.spawn(SnowFlakeIdService(1L), "SnowFlakeService")

      Behaviors.receiveMessage[OrderCommand] {
        case c: CreateOrder =>
          implicit val ec = context.system.executionContext
          implicit val scheduler: Scheduler = context.system.scheduler

          val snowFlakeIdFuture: Future[OrderCommand] = snowFlakeService.ask(ref => SnowFlakeIdService.GetId(ref))

          snowFlakeIdFuture.onComplete {
            case Success(SnowFlakeIdService.SnowFlakeId(id)) =>
              val orderEntity = sharding.entityRefFor(Order.TypeKey, id.toString)
              orderEntity ! c
            case Failure(ex) => context.system.log.error("snowFlakeIdFuture error: " + ex.toString)
          }
          Behaviors.same
        case ChangeOrderStatus(orderId, orderStatus, replyTo) =>
          val orderEntity = sharding.entityRefFor(Order.TypeKey, orderId)
          orderStatus match {
            case Approved =>
              orderEntity ! ApproveOrder(replyTo)
            case Processing =>
              orderEntity ! ProcessOrder(replyTo)
            case Pending =>
              orderEntity ! PendOrder(replyTo)
            case Completed =>
              orderEntity ! CompleteOrder(replyTo)
            case Cancelled =>
              orderEntity ! CancelOrder(replyTo)
            case _ =>
              replyTo ! OrderRejectedReply("order status error!")
          }
          Behaviors.same
        case getOrder: GetOrderInfo =>
          val orderEntity = sharding.entityRefFor(Order.TypeKey, getOrder.orderId)
          orderEntity ! getOrder
          Behaviors.same
      }
    }
  }

}
