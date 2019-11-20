package com.jandra.hermes.order.domain.protocol

/**
  * @Author: adria
  * @Description:
  * @Date: 14:40 2019/9/25
  * @Modified By:
  */

case class CreateOrderResult(resultCode: Int, orderId: String) extends HttpResponse {
  override val resultMessage: String = orderId
}
