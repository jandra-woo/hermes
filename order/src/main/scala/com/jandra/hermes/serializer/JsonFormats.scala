package com.jandra.hermes.serializer

import com.jandra.hermes.order.application.OrderRestRoutes.{CreateOrderData, CreateOrderItemData, CreateOrderRestReply}
import com.jandra.hermes.order.domain.aggregate.OrderTyped.OrderCreated
import com.jandra.hermes.order.domain.protocol.OrderReply

/**
  * @Author: adria
  * @Description:
  * @Date: 15:37 2019/11/18
  * @Modified By:
  */

object JsonFormats {

  import spray.json.RootJsonFormat
  // import the default encoders for primitive types (Int, String, Lists etc)
  import spray.json.DefaultJsonProtocol._

  implicit val createOrderItemDataFormat: RootJsonFormat[CreateOrderItemData] = jsonFormat2(CreateOrderItemData)
  implicit val createOrderDataFormat: RootJsonFormat[CreateOrderData] = jsonFormat8(CreateOrderData)

  implicit val orderCreatedFormat: RootJsonFormat[OrderCreated] = jsonFormat1(OrderCreated)
  implicit val createOrderRestReplyFormat: RootJsonFormat[CreateOrderRestReply] = jsonFormat1(CreateOrderRestReply)

}
