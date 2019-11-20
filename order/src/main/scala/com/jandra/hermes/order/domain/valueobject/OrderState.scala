package com.jandra.hermes.order.domain.valueobject

/**
  * @Author: adria
  * @Description:
  * @Date: 14:44 2019/9/16
  * @Modified By:
  */

object OrderState extends Enumeration{

  type OrderState = Value
  val CREATED, PAID, CLOSED, CANCELLED, COMPLETED, PENDING, DELIVERED = Value
}

case class ChangeState(from: OrderState.OrderState, to: OrderState.OrderState)

object ChangeState {

  import OrderState._

  def apply(from: OrderState,
            to: OrderState): ChangeState = {
    if(check(from, to)) ChangeState(from, to)
    else throw new Exception("abc")
  }

  def check(from: OrderState, to: OrderState): Boolean = {
    // undo
    true
  }
}

