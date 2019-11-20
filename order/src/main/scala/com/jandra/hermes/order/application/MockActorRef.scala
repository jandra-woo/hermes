package com.jandra.hermes.order.application

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.jandra.hermes.order.domain.protocol.MockProtocol
import com.jandra.hermes.order.domain.valueobject.{CustomerInfo, GetCustomerInfo, GetPriceInfo, GetProductInfo, PriceInfo, ProductInfo}

/**
  * @Author: adria
  * @Description:
  * @Date: 18:33 2019/11/19
  * @Modified By:
  */

object MockActorRef {

  def apply(): Behavior[MockProtocol] = {
    Behaviors.setup[MockProtocol] { context =>
      Behaviors.receiveMessage[MockProtocol] {
        case getCustomerInfo: GetCustomerInfo =>
          getCustomerInfo.replyTo ! CustomerInfo(getCustomerInfo.customerId, "jason", "13800000000")
          Behaviors.same
        case getProductInfo: GetProductInfo =>
          getProductInfo.replyTo ! ProductInfo(getProductInfo.productId, "iPhone", "mobile")
          Behaviors.same
        case getPriceInfo: GetPriceInfo =>
          getPriceInfo.replyTo ! PriceInfo(getPriceInfo.productId, 100, "999", 88)
          Behaviors.same
      }
    }
  }
}
