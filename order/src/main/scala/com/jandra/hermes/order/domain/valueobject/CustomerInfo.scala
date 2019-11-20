package com.jandra.hermes.order.domain.valueobject

import akka.actor.typed.ActorRef
import com.jandra.hermes.order.domain.protocol.{MockProtocol, OrderCommand}

/**
  * @Author: adria
  * @Description:
  * @Date: 16:28 2019/9/5
  * @Modified By:
  */

case class CustomerInfo(customerId: String, customerName: String, telephone: String) extends OrderCommand

case class GetCustomerInfo(customerId: String, replyTo: ActorRef[CustomerInfo]) extends MockProtocol