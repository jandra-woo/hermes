package com.jandra.hermes.order.domain.entity

/**
  * @Author: adria
  * @Description:
  * @Date: 17:07 2019/8/22
  * @Modified By:
  */

import com.jandra.hermes.common.domain.Entity
import com.jandra.hermes.order.domain.valueobject.{PriceInfo, ProductInfo}

case class OrderItem private(orderItemId: String,
                             productInfo: ProductInfo,
                             priceInfo: PriceInfo,
                             quantity: Int) extends Entity {
  override protected var identity: String = orderItemId
  private def copy(): Unit = ()
}

object OrderItem {
  def apply(orderItemId: String,
            productId: String,
            productName: String,
            quantity: Int,
            unitPrice: Float): OrderItem = {
    orderItemId match {
      case null => throw new IllegalArgumentException("The orderItemId may not be set to null.")
      case "" => throw new IllegalArgumentException("The orderItemId may not be set to blank.")
      case _ =>
    }
    productId match {
      case null => throw new IllegalArgumentException("The productId may not be set to null.")
      case "" => throw new IllegalArgumentException("The productId may not be set to blank.")
      case _ =>
    }
    productName match {
      case null => throw new IllegalArgumentException("The productName may not be set to null.")
      case "" => throw new IllegalArgumentException("The productName may not be set to blank.")
      case _ =>
    }
    quantity match {
      case x => if(x <= 0) throw new IllegalArgumentException("The quantity may not less than or equal to 0.")
      case _ =>
    }
    unitPrice match {
      case x => if(x <= 0) throw new IllegalArgumentException("The unitPrice may not less than or equal to 0.")
      case _ =>
    }
    new OrderItem(orderItemId, productId, productName, quantity, unitPrice)
  }
}

