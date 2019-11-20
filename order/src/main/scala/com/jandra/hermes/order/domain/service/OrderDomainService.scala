package com.jandra.hermes.order.domain.service

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.util.Timeout
import com.jandra.hermes.order.domain.aggregate.OrderTyped
import com.jandra.hermes.order.domain.protocol.{CreateOrder, OrderCommand}
import com.jandra.hermes.util.SnowFlakeIdService

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

// entry point to order domain cluster and sharded order entity
object OrderDomainService {
  implicit val timeout: Timeout = 3.seconds

  def apply(): Behavior[OrderCommand] = {
    Behaviors.setup { context =>

      val sharding = ClusterSharding(context.system)
      sharding.init(Entity(OrderTyped.TypeKey) { entityContext =>
        OrderTyped(entityContext.entityTypeKey.name, entityContext.entityId)
      })

      val snowFlakeService: ActorRef[SnowFlakeIdService.SnowFlakeCommand] = context.spawn(SnowFlakeIdService(1L), "SnowFlakeService")

      Behaviors.receiveMessage[OrderCommand] {
        case c: CreateOrder =>
          implicit val ec = context.system.executionContext
          implicit val scheduler: Scheduler = context.system.scheduler

          val snowFlakeIdFuture: Future[OrderCommand] = snowFlakeService.ask(ref => SnowFlakeIdService.GetId(ref))

          snowFlakeIdFuture.onComplete{
            case Success(SnowFlakeIdService.SnowFlakeId(id)) =>
              val orderEntity = sharding.entityRefFor(OrderTyped.TypeKey, id.toString)
              orderEntity ! c
//              shardRegion ! ShardingEnvelope(id.toString, c)
            case Failure(ex) => println("~~~~~~failure~~~~~~~~" + ex)
          }
          Behaviors.same
      }
    }
  }

}
