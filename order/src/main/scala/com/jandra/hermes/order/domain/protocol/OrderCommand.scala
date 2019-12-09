package com.jandra.hermes.order.domain.protocol

import akka.actor.typed.ActorRef
import com.jandra.hermes.common.util.CborSerializable
import com.jandra.hermes.order.application.OrderRestRoutes.CreateOrderData
import com.jandra.hermes.order.domain.valueobject.OrderState

/**
  * @Author: adria
  * @Description:
  * @Date: 15:45 2019/11/12
  * @Modified By:
  */

trait OrderCommand extends CborSerializable

final case class CreateOrder(createOrderData: CreateOrderData, replyTo: ActorRef[CreateOrderReply]) extends OrderCommand

final case class ChangeOrderStatus(orderId: String, orderState: OrderState, replyTo: ActorRef[OrderReply]) extends OrderCommand

final case class GetOrderInfo(orderId: String, replyTo: ActorRef[OrderReply]) extends OrderCommand


