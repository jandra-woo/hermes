package com.jandra.hermes.order.domain.valueobject

import com.jandra.hermes.util.Validate

/**
  * @Author: adria
  * @Description:
  * @Date: 9:57 2019/9/25
  * @Modified By:
  */

final case class CreateOrderInfo(orderName: String,
                           customerId: String,
                           orderMode: String,
                           storeId: String,
                           itemList: List[CreateOrderItemInfo],
                           salesChannelId: String,
                           orderAttrs: String,
                           comments: String)

object CreateOrderInfo extends Validate {
  def apply(orderName: String,
            customerId: String,
            orderMode: String,
            storeId: String,
            itemList: List[CreateOrderItemInfo],
            salesChannelId: String,
            orderAttrs: String,
            comments: String): CreateOrderInfo = {
    idValidate(customerId, "customerId")
    idValidate(storeId, "storeId")
    idValidate(salesChannelId, "salesChannelId")
    new CreateOrderInfo(orderName, customerId, orderMode, storeId, itemList, salesChannelId, orderAttrs, comments)
  }

}


