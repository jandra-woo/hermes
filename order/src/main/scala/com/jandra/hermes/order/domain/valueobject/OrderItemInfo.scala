package com.jandra.hermes.order.domain.valueobject

import com.jandra.hermes.common.util.CborSerializable

/**
  * @Author: adria
  * @Description:
  * @Date: 13:53 2019/11/22
  * @Modified By:
  */

case class OrderItemInfo(productId: String,
                         productInfo: ProductInfo,
                         priceInfo: PriceInfo,
                         quantity: Int) extends CborSerializable
