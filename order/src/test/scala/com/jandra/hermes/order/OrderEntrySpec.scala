package com.jandra.hermes.order

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * @Author: adria
  * @Description:
  * @Date: 10:38 2019/9/20
  * @Modified By:
  */


class OrderEntrySpec()
  extends TestKit(ActorSystem("HermesSpec", ConfigFactory.parseString("akka.cluster.roles = [OrderEntry]").withFallback(ConfigFactory.load("application"))))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An Echo actor" must {

    "send back messages unchanged" in {
      val echo = system.actorOf(TestActors.echoActorProps)
      echo ! "hello world"
      expectMsg("hello world")
    }

  }

//  "Order Entry Actor" must {
//    "print string messages" in {
//
//      system.actorOf(
//        ClusterSingletonManager.props(
//          singletonProps = Props(classOf[OrderEntryCounter]),
//          terminationMessage = PoisonPill,
//          settings = ClusterSingletonManagerSettings(system).withRole("OrderEntry")),
//        name = "orderEntryCounter"
//      )
//
//      val orderEntry = system.actorOf(Props(classOf[OrderEntry]), name = "orderEntry")
//      Cluster(system) registerOnMemberUp {
//        ClusterClientReceptionist(system).registerService(orderEntry)
//      }
//      orderEntry ! CreateOrderInfo("ttt", List(("123", 1), ("1234", 2)), "aa", "bb", "cc")
//      expectMsg("OK")
//    }
//  }
}
