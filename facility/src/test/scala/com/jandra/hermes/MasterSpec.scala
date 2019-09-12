package com.jandra.hermes

/**
  * @Author: adria
  * @Description:
  * @Date: 16:43 2018/10/25
  * @Modified By:
  */
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import com.jandra.hermes.cluster.StartJob
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class MasterSpec() extends TestKit(ActorSystem("MappingSystem")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit ={
    TestKit.shutdownActorSystem(system)
  }

  "An Master Actor" must {

    "send start job" in {
      val master = system.actorOf(TestActors.echoActorProps)
      master ! StartJob("疾病")
      expectMsg(StartJob("疾病"))
    }
  }
}
