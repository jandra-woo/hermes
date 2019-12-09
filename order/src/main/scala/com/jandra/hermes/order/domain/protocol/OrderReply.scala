package com.jandra.hermes.order.domain.protocol

import com.jandra.hermes.common.util.CborSerializable
import com.jandra.hermes.order.domain.entity.OrderItem
import com.jandra.hermes.order.domain.valueobject._

/**
  * @Author: adria
  * @Description:
  * @Date: 11:02 2019/11/15
  * @Modified By:
  */

trait OrderReply extends CborSerializable

final case class CreateOrderReply(orderId: String) extends OrderReply

final case class CurrentOrderInfo(orderInfo: OrderInfo, itemList: List[OrderItem.CurrentOrderItemInfo]) extends OrderReply

final case class CurrentOrderStatus(orderId: String, orderState: OrderState) extends OrderReply

final case class OrderRejectedReply(reason: String) extends OrderReply
