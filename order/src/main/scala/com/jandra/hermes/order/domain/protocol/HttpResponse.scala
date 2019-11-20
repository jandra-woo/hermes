package com.jandra.hermes.order.domain.protocol

/**
  * @Author: adria
  * @Description:
  * @Date: 14:29 2019/9/25
  * @Modified By:
  */

trait HttpResponse {

  val resultCode: Int

  val resultMessage: String
}
