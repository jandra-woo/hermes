package com.jandra.hermes.order.domain.valueobject

import akka.actor.typed.ActorRef
import com.jandra.hermes.order.domain.entity.OrderItem.OrderItemCommand
import com.jandra.hermes.order.domain.protocol.MockProtocol

/**
  * @Author: adria
  * @Description:
  * @Date: 16:38 2019/9/5
  * @Modified By:
  */

case class PriceInfo(productId: String,
                     listPrice: Float,
                     promotionId: String,
                     finalPrice: Float) extends OrderItemCommand{
//  private def copy(): Unit = ()
}

//object PriceInfo {
//  def apply(productId: String,
//            listPrice: Float,
//            promotionId: String,
//            finalPrice: Float): PriceInfo = {
//    productId match {
//      case null => throw new IllegalArgumentException("The productId may not be set to null.")
//      case "" => throw new IllegalArgumentException("The productId may not be set to blank.")
//      case _ =>
//    }
//    listPrice match {
//      case x: Float => if (x < 0) throw new IllegalArgumentException("The listPrice may not less than 0.")
//    }
//    finalPrice match {
//      case x: Float => if (x < 0) throw new IllegalArgumentException("The finalPrice may not less than 0.")
//    }
//    new PriceInfo(productId, listPrice, promotionId, finalPrice)
//  }
//}

case class GetPriceInfo(productId: String, replyTo: ActorRef[PriceInfo]) extends MockProtocol