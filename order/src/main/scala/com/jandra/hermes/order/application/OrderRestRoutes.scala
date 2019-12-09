package com.jandra.hermes.order.application

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.{actor => classic}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.jandra.hermes.common.util.CborSerializable

import scala.concurrent.Future
import scala.concurrent.duration._
import com.jandra.hermes.order.domain.protocol._
import com.jandra.hermes.order.domain.valueobject.OrderState

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

  final case class ChangeOrderStatusData(orderId: String,
                                     stateId: OrderState) extends RestCommand

}

class OrderRestRoutes(orderDomainService: ActorRef[OrderCommand])(implicit system: ActorSystem[_]) {

  import OrderRestRoutes._
  import akka.actor.typed.scaladsl.adapter._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import akka.http.scaladsl.server.Directives._
  import com.jandra.hermes.serializer.JsonFormats._

  implicit val classicSystem: classic.ActorSystem = system.toClassic
  implicit val timeout: Timeout = 5.seconds

  val order: Route =
    concat(
      post {
        path("createOrder") {
          entity(as[CreateOrderData]) { data =>
            system.log.info("create order request: " + data)
            val f: Future[CreateOrderReply] = orderDomainService.ask(replyTo => CreateOrder(data, replyTo))
            onSuccess(f) { orderResult =>
              complete(StatusCodes.Created -> orderResult)
            }
          }
        }
      },
      get {
        pathPrefix("order"/ LongNumber) { id =>
          system.log.info("get order request: " + id)
          val f: Future[OrderReply] = orderDomainService.ask(replyTo => GetOrderInfo(id.toString, replyTo))
          onSuccess(f) {
            case c: CurrentOrderInfo =>
//              Marshal(c).to[MessageEntity]
              complete(StatusCodes.OK -> c)
            case r: OrderRejectedReply =>
              complete(StatusCodes.NotAcceptable -> r)
          }
        }
      },
      post {
        path("changeOrderStatus") {
          entity(as[ChangeOrderStatusData]) {data =>
            system.log.info("change order status request: " + data)
            val f: Future[OrderReply] = orderDomainService.ask(ref => ChangeOrderStatus(data.orderId, data.stateId, ref))
            onSuccess(f) {
              case c: CurrentOrderStatus =>
                complete(StatusCodes.OK -> c)
              case r: OrderRejectedReply =>
                complete(StatusCodes.NotAcceptable -> r)
            }
          }
        }
      }
    )

}
