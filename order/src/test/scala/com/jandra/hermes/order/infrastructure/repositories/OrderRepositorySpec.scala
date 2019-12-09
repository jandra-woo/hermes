package com.jandra.hermes.order.infrastructure.repositories

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LogCapturing, ScalaTestWithActorTestKit}

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * @Author: adria
  * @Description:
  * @Date: 10:35 2019/10/24
  * @Modified By:
  */

class OrderRepositorySpec
  extends ScalaTestWithActorTestKit(
    s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with WordSpecLike
    with LogCapturing
    with BeforeAndAfterAll
    with Matchers {

  //  ConfigFactory.load("application")

    "Order Repository" must {
      "handle create" in {

//        val orderInfo: OrderInfo = OrderInfo("testOrderId-123",
//          "2019-10-24",
//          "2019-10-24",
//          "testStore",
//          "web",
//          "",
//          CREATED,
//          CustomerInfo("testOrderId-123", "testCustomer-234", "testCustomer-abc", "13800000000"))
//
//        val orderRepository = spawn(OrderRepository(PersistenceId(orderInfo.orderId, "1")), "orderRepository")
//
//        val probe = createTestProbe[OrderRepository.OperationResult]()
//
//        orderRepository ! OrderRepository.CreateOrder(orderInfo, probe.ref)
//
//        probe.expectMessage(OrderRepository.Confirmed)
      }
    }
}