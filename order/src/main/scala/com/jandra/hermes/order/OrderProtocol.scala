package com.jandra.hermes.order

/**
  * @Author: adria
  * @Description:
  * @Date: 17:51 2019/7/2
  * @Modified By:
  */

import akka.actor.ActorRef
import aecor.macros.boopickleWireProtocol
import cats.tagless.autoFunctorK
import boopickle.Default._

//@boopickleWireProtocol
//@autoFunctorK(false)
//trait Order[F[_]] {
//  def createOrder(userId: String, productId: String, productNumber: Int, orderDate: String): F[Unit]
//}

// Messages from OrderEntry
case class RegisterOrderEntry(ref: ActorRef)

// Messages to OrderEntry
case object OrderEntryIsReady

case class OrderEntryId(ref: ActorRef, id: Long)

case class ReceiveOrder(orderDate: String)

//
case class JobResult(resultCode: Int, resultMessage: String)

