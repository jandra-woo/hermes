package com.jandra.hermes.order.domain.protocol

import com.jandra.hermes.common.util.CborSerializable
import com.jandra.hermes.order.application.OrderRestRoutes.CreateOrderItemData
import com.jandra.hermes.order.domain.valueobject.OrderInfo

/**
  * @Author: adria
  * @Description:
  * @Date: 10:52 2019/11/27
  * @Modified By:
  */

trait OrderEvent extends CborSerializable

final case class OrderCreated(orderInfo: OrderInfo, itemRefList: List[CreateOrderItemData]) extends OrderEvent

final case object OrderApproved extends OrderEvent

final case object OrderProcessing extends OrderEvent

final case object OrderPending extends OrderEvent

final case object OrderRejected extends OrderEvent

final case object OrderCompleted extends OrderEvent

final case object OrderCancelled extends OrderEvent

final case class OrderPaid(paymentId: String) extends OrderEvent

final case class OrderDelivered(deliveryId: String) extends OrderEvent
