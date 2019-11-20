package com.jandra.hermes.order.domain.protocol

import akka.actor.typed.ActorRef
import com.jandra.hermes.order.application.OrderRestRoutes.{CreateOrderData, CreateOrderRestReply}
import com.jandra.hermes.util.CborSerializable

/**
  * @Author: adria
  * @Description:
  * @Date: 15:45 2019/11/12
  * @Modified By:
  */

trait OrderCommand extends CborSerializable

case class CreateOrderItem(productId: String, quantity: Int) extends OrderCommand

case class CreateOrder(createOrderData: CreateOrderData,
                       replyTo: ActorRef[CreateOrderRestReply]) extends OrderCommand

