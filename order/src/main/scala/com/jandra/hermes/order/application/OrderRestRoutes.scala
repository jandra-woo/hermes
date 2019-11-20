package com.jandra.hermes.order.application

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.{actor => classic}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.jandra.hermes.order.domain.aggregate.OrderTyped.OrderCreated

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import com.jandra.hermes.order.domain.protocol.{CreateOrder, OrderCommand, OrderReply}
import com.jandra.hermes.util.CborSerializable

/**
  * @Author: adria
  * @Description:
  * @Date: 16:34 2019/11/14
  * @Modified By:
  */

object OrderRestRoutes {
  sealed trait RestCommand extends CborSerializable

  final case class CreateOrderItemData(productId: String, quantity: Int) extends RestCommand

  final case class CreateOrderData(orderName: String,
                                   customerId: String,
                                   orderMode: String,
                                   storeId: String,
                                   itemList: List[CreateOrderItemData],
                                   salesChannelId: String,
                                   orderAttrs: String,
                                   comments: String) extends RestCommand

  final case class CreateOrderRestReply(orderId: String) extends CborSerializable
}

class OrderRestRoutes(orderDomainService: ActorRef[OrderCommand]) (implicit system: ActorSystem[_]) {

  import OrderRestRoutes._
  import akka.actor.typed.scaladsl.adapter._
  implicit val classicSystem: classic.ActorSystem = system.toClassic
  implicit val timeout: Timeout = 3.seconds

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import akka.http.scaladsl.server.Directives._
  import com.jandra.hermes.serializer.JsonFormats._

  val order: Route =
    pathPrefix("order") {
      concat(
        post {
          entity(as[CreateOrderData]) { data =>
            val f: Future[CreateOrderRestReply] = orderDomainService.ask(replyTo => CreateOrder(data, replyTo))
            onSuccess(f) { orderResult =>
              complete(StatusCodes.Created -> orderResult)
            }
          }
        }
      )
    }

}
