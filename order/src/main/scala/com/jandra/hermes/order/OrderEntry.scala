package com.jandra.hermes.order

/**
  * @Author: adria
  * @Description:
  * @Date: 17:46 2019/6/27
  * @Modified By:
  */

import akka.actor.{Actor, ActorPath, ActorSystem, PoisonPill, Props}
import com.typesafe.config.ConfigFactory
import com.jandra.hermes.util.{SharedLeveldb, SnowFlakeId}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}

class OrderEntry extends Actor {

  override def preStart(): Unit = {
    val counterProxy = context.system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = "/user/orderEntryCounter",
        settings = ClusterSingletonProxySettings(context.system).withRole("OrderEntry")
      ),
      name = "counterProxy"
    )
    counterProxy ! RegisterOrderEntry(self)
  }

  var orderEntryId = 0L

  override def receive = {
    case OrderEntryId(_, id) => orderEntryId = id
    case ReceiveOrder(_) =>
      sender() ! JobResult(1,new SnowFlakeId(orderEntryId, context.system).get_id().toString)
    case o => println(o)
  }
}

object OrderEntry {

  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)

    val config = ConfigFactory.parseString(s"akka.remote.artery.canonical.port=$port")
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = [OrderEntry]"))
      .withFallback(ConfigFactory.load("application"))

    val system = ActorSystem("hermes", config)
    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(classOf[OrderEntryCounter]),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system).withRole("OrderEntry")),
      name = "orderEntryCounter"
    )

    SharedLeveldb.startupSharedJournal(system, startStore = (port == "2551"), path =
      ActorPath.fromString("akka://hermes@127.0.0.1:2551/user/store"))


    val orderEntry = system.actorOf(Props(classOf[OrderEntry]), name = "orderEntry")
    Cluster(system) registerOnMemberUp {
      ClusterClientReceptionist(system).registerService(orderEntry)
    }
  }
}
