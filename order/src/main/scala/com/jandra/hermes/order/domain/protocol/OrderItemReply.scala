package com.jandra.hermes.order.domain.protocol

import com.jandra.hermes.order.domain.valueobject.{PriceInfo, ProductInfo}

/**
  * @Author: adria
  * @Description:
  * @Date: 15:56 2019/10/29
  * @Modified By:
  */

sealed trait OrderItemReply

final case class UpdateProductInfo(productInfo: ProductInfo) extends OrderItemReply

final case class UpdatePriceInfo(priceInfo: PriceInfo) extends OrderItemReply

case object RepositoryConfirmed extends OrderItemReply


