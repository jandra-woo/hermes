package com.jandra.hermes.cluster

/**
  * @Author: adria
  * @Description:
  * @Date: 18:19 2018/10/23
  * @Modified By:
  */

case class StartJob(jobType: String)

case object WorkerRegistration

case class StreamFeedback(feedbackType: Int)

case class Job(jobType: String)

case class JobResult(resultCode: Int, resultMessage: String)

final case class OrderRequest(seqId: String, partyId: String, productId: String, quantity: Int)

final case class InventoryChange(productId: String, quantity: Int)
