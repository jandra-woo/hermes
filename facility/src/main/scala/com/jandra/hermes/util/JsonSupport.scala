package com.jandra.hermes.util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.jandra.hermes.cluster.{Job, JobResult, OrderRequest}
import com.jandra.hermes.domain.HttpTest
import spray.json.DefaultJsonProtocol

/**
  * @Author: adria
  * @Description:
  * @Date: 19:04 2018/11/7
  * @Modified By:
  */

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val jobFormat = jsonFormat1(Job)
    implicit val orderRequestFormat = jsonFormat4(OrderRequest)
    implicit val jobResultFormat = jsonFormat2(JobResult)
    implicit val httpTestFormat = jsonFormat1(HttpTest)
}
