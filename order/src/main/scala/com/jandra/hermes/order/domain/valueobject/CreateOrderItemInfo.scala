package com.jandra.hermes.order.domain.valueobject

import com.jandra.hermes.util.Validate

/**
  * @Author: adria
  * @Description:
  * @Date: 9:58 2019/9/25
  * @Modified By:
  */

case class CreateOrderItemInfo(productId: String, quantity: Int)

object CreateOrderItemInfo extends Validate {
  def apply(productId: String, quantity: Int): CreateOrderItemInfo = {
    idValidate(productId, "productId")
    new CreateOrderItemInfo(productId, quantity)
  }
}
