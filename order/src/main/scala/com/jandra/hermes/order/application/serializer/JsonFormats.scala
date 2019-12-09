package com.jandra.hermes.serializer

import com.jandra.hermes.order.application.OrderRestRoutes._
import com.jandra.hermes.order.domain.entity.OrderItem.CurrentOrderItemInfo
import com.jandra.hermes.order.domain.protocol._
import com.jandra.hermes.order.domain.valueobject._
import com.jandra.hermes.order.domain.valueobject.OrderState._
import spray.json.{JsString, JsValue, JsonFormat, deserializationError}

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
  implicit val changeOrderStatusDataFormat: RootJsonFormat[ChangeOrderStatusData] = jsonFormat2(ChangeOrderStatusData)

  implicit val orderCreatedFormat: RootJsonFormat[CreateOrderReply] = jsonFormat1(CreateOrderReply)

  implicit val customerInfoFormat: RootJsonFormat[CustomerInfo] = jsonFormat3(CustomerInfo)
  implicit val orderInfoFormat: RootJsonFormat[OrderInfo] = jsonFormat13(OrderInfo)
  implicit val productInfoFormat: RootJsonFormat[ProductInfo] = jsonFormat3(ProductInfo)
  implicit val priceInfoFormat: RootJsonFormat[PriceInfo] = jsonFormat4(PriceInfo)
  implicit val orderItemInfoFormat: RootJsonFormat[OrderItemInfo] = jsonFormat4(OrderItemInfo)
  implicit val currentOrderItemInfoFormat: RootJsonFormat[CurrentOrderItemInfo] = jsonFormat1(CurrentOrderItemInfo)
  implicit val currentOrderInfoFormat: RootJsonFormat[CurrentOrderInfo] = jsonFormat2(CurrentOrderInfo)

  implicit val currentOrderStatusFormat: RootJsonFormat[CurrentOrderStatus] = jsonFormat2(CurrentOrderStatus)

  implicit val orderRejectedReplyFormat: RootJsonFormat[OrderRejectedReply] = jsonFormat1(OrderRejectedReply)

  //add customize json format
  implicit object OrderStateJsonFormat extends JsonFormat[OrderState] {
    def write(x: OrderState) = x match {
      case Created => JsString("Created")
      case Approved => JsString("Approved")
      case Paid => JsString("Paid")
      case Rejected => JsString("Rejected")
      case Cancelled => JsString("Cancelled")
      case Completed => JsString("Completed")
      case Pending => JsString("Pending")
      case Processing => JsString("Processing")
      case Delivered => JsString("Delivered")
    }
    def read(value: JsValue) = value match {
      case JsString("Created") => Created
      case JsString("Approved") => Approved
      case JsString("Paid") => Paid
      case JsString("Rejected") => Rejected
      case JsString("Cancelled") => Cancelled
      case JsString("Completed") => Completed
      case JsString("Pending") => Pending
      case JsString("Processing") => Processing
      case JsString("Delivered") => Delivered
      case x => deserializationError("Expected String as JsString, but got " + x)
    }
  }



}
