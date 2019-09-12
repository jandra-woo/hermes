package com.jandra.hermes.cluster

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, RootActorPath}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.{CurrentClusterState, InitialStateAsEvents, MemberUp, UnreachableMember}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.jandra.hermes.domain.Medicine
import com.jandra.hermes.http.RequestActor
import com.jandra.hermes.stream.MedicineStream
import com.typesafe.config.ConfigFactory

/**
  * @Author: adria
  * @Description:
  * @Date: 11:33 2018/10/15
  * @Modified By:
  */

object Worker {
  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = [worker]"))
      .withFallback(ConfigFactory.load("application"))

    val system = ActorSystem("MappingSystem", config)
    system.actorOf(Props[Worker], name = "worker")
  }

}

class Worker extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  implicit val mat = ActorMaterializer()
  implicit val executionContext = context.system.dispatchers.lookup("my-dispatcher")
  implicit val session = SlickSession.forConfig("slick-postgres")
  context.system.registerOnTermination(() => session.close())
  val requestActor = context.actorOf(Props(classOf[RequestActor]))

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberUp], classOf[UnreachableMember])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  def receive: Receive = {
    case m: Medicine =>
      MedicineStream.baseStream(context, "药品", m, log, requestActor)(session)
      sender() ! StreamFeedback(1)
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register
    case MemberUp(member) =>
      register(member)
      log.info("Member is Up: {}", member.address)
    case _ =>
  }

  def register(member: Member): Unit = {
    if (member.hasRole("master")) {
      context.actorSelection(RootActorPath(member.address) / "user" / "master") !
        WorkerRegistration
    }

  }
}
