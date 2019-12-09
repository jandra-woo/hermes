package com.jandra.hermes.order.domain.valueobject

import akka.actor.typed.ActorRef
import com.jandra.hermes.order.domain.entity.OrderItem.OrderItemCommand
import com.jandra.hermes.order.domain.protocol.MockProtocol

/**
  * @Author: adria
  * @Description:
  * @Date: 16:35 2019/9/5
  * @Modified By:
  */

case class ProductInfo(productId: String,
                       productName: String,
                       category: String) extends OrderItemCommand{
//  private def copy(): Unit = ()
}

//object ProductInfo {
//  def apply(productId: String,
//            productName: String,
//            category: String): ProductInfo = {
//    productId match {
//      case null => throw new IllegalArgumentException("The productId may not be set to null.")
//      case "" => throw new IllegalArgumentException("The productId may not be set to blank.")
//      case _ =>
//    }
//    productName match {
//      case null => throw new IllegalArgumentException("The productName may not be set to null.")
//      case "" => throw new IllegalArgumentException("The productName may not be set to blank.")
//      case _ =>
//    }
//    new ProductInfo(productId, productName, category)
//  }
//}

case class GetProductInfo(productId: String, replyTo: ActorRef[ProductInfo]) extends MockProtocol