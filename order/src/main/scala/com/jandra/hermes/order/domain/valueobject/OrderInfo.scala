package com.jandra.hermes.order.domain.valueobject

import com.jandra.hermes.order.domain.valueobject.OrderState.OrderState

/**
  * @Author: adria
  * @Description:
  * @Date: 15:48 2019/10/12
  * @Modified By:
  */

case class OrderInfo(orderId: String,
                     orderName: String,
                     orderStartDate: String,
                     orderModifyDate: String,
                     storeId: String,
                     salesChannelId: String,
                     orderMode: String,
                     comments: String,
                     orderState: OrderState,
                     customerInfo: CustomerInfo,
                     orderAttrs: String,
                     paymentId: Option[String],
                     deliveryId: Option[String]
                    ) {

}
