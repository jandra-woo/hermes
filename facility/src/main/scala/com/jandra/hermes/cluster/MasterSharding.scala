package com.jandra.hermes.cluster

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberUp, UnreachableMember}
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding._
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import java.util.UUID

import com.jandra.hermes.domain._
import com.typesafe.config.ConfigFactory

import scala.util.{Failure, Success}

/**
  * @Author: adria
  * @Description:
  * @Date: 16:29 2018/10/23
  * @Modified By:
  */
class MasterSharding extends Actor with ActorLogging {
  val cluster = Cluster(context.system)
  implicit val mat = ActorMaterializer()

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberUp], classOf[UnreachableMember])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg@InventoryChange(productId, _) => (productId.toString, msg)
  }

  val numberOfShards = 6

  val extractShardId: ShardRegion.ExtractShardId = {
    case InventoryChange(productId, _) => (productId.hashCode() % numberOfShards).toString
    case ShardRegion.StartEntity(id) ⇒
      // StartEntity is used by remembering entities feature
      (id.toLong % numberOfShards).toString
  }


  val productRegion: ActorRef = ClusterSharding(context.system).start(
    typeName = "Product",
    entityProps = Props[Product],
    settings = ClusterShardingSettings(context.system),
    extractEntityId = extractEntityId,
    extractShardId = extractShardId
  )

  override def receive: Receive = {
    case o: OrderRequest =>
      import context.dispatcher
      implicit val timeout: Timeout = Timeout(2 seconds)
      val s = sender()
      val future = productRegion ? InventoryChange(o.productId, o.quantity)
      future.onComplete {
        case Success(v) =>
          if (v.toString == "Success") {
            val uuid = UUID.randomUUID.toString
            println("~~~~~~~~~~~~Success~~~~~~~~~~~")
            s ! JobResult(200, uuid)
          }else if(v.toString == "Failure") {
            s ! JobResult(200, "Not enough inventory")
          }else s ! JobResult(999, v.toString)
        case Failure(e) =>
          e.printStackTrace
      }
    case f: StreamFeedback =>
      log.info("------------------Result: " + f.feedbackType)
  }
}

object MasterSharding {
  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.artery.canonical.port=$port")
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = [mastersharding]"))
      .withFallback(ConfigFactory.load("application"))
    val system = ActorSystem("hermes", config)

    val master = system.actorOf(Props(classOf[MasterSharding]), name = "mastersharding")
    Cluster(system) registerOnMemberUp {
      ClusterClientReceptionist(system).registerService(master)
    }

    AkkaManagement(system).start()
  }
}
