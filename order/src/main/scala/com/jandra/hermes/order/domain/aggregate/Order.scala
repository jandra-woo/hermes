package com.jandra.hermes.order.domain.aggregate

/**
  * @Author: adria
  * @Description:
  * @Date: 17:43 2019/8/30
  * @Modified By:
  */

import java.util.Date

import com.jandra.hermes.common.domain.Entity
import com.jandra.hermes.order.domain.entity.OrderItem
import com.jandra.hermes.order.domain.valueobject.CustomerInfo

case class Order(orderId: String,
                 orderDate: Date,
                 itemList: List[OrderItem],
                 customerInfo: CustomerInfo) extends Entity {

  override protected var identity: String = orderId
}
