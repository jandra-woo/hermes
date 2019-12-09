package com.jandra.hermes.order.domain.valueobject

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, SerializerProvider}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
  * @Author: adria
  * @Description:
  * @Date: 14:44 2019/9/16
  * @Modified By:
  */

@JsonSerialize(using = classOf[OrderStateJsonSerializer])
@JsonDeserialize(using = classOf[OrderStateJsonDeserializer])
sealed trait OrderState

object OrderState {

  case object Created extends OrderState
  case object Approved extends OrderState
  case object Paid extends OrderState
  case object Rejected extends OrderState
  case object Cancelled extends OrderState
  case object Completed extends OrderState
  case object Pending extends OrderState
  case object Processing extends OrderState
  case object Delivered extends OrderState
}

class OrderStateJsonSerializer extends StdSerializer[OrderState](classOf[OrderState]){
  import OrderState._

  override def serialize(value: OrderState, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    val strValue = value match {
      case Created => "Created"
      case Approved => "Approved"
      case Paid => "Paid"
      case Rejected => "Rejected"
      case Cancelled => "Cancelled"
      case Completed => "Completed"
      case Pending => "Pending"
      case Processing => "Processing"
      case Delivered => "Delivered"
    }
    gen.writeString(strValue)
  }
}

class OrderStateJsonDeserializer extends StdDeserializer[OrderState](classOf[OrderState]) {
  import OrderState._

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): OrderState = {
    p.getText match {
      case "Created" => Created
      case "Approved" => Approved
      case "Paid" => Paid
      case "Rejected" => Rejected
      case "Cancelled" => Cancelled
      case "Completed" => Completed
      case "Pending" => Pending
      case "Processing" => Processing
      case "Delivered" => Delivered
    }
  }
}

case class ChangeState(from: OrderState, to: OrderState)

object ChangeState {

  def apply(from: OrderState,
            to: OrderState): ChangeState = {
    if(check(from, to)) ChangeState(from, to)
    else throw new Exception("abc")
  }

  private def check(from: OrderState, to: OrderState): Boolean = {
    // undo
    true
  }
}

