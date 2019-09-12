package com.jandra.hermes.util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.jandra.hermes.order.{JobResult, ReceiveOrder}
import spray.json.DefaultJsonProtocol

/**
  * @Author: adria
  * @Description:
  * @Date: 19:04 2018/11/7
  * @Modified By:
  */

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

    implicit val jobResultFormat = jsonFormat2(JobResult)
    implicit val receiveOrderFormat = jsonFormat1(ReceiveOrder)
}
