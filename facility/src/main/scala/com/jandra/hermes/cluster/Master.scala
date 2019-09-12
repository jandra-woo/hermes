package com.jandra.hermes.cluster

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.client.ClusterClientReceptionist
import akka.stream.ActorMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.stream.alpakka.slick.scaladsl.Slick
import akka.stream.scaladsl._
import akka.util.Timeout
import com.jandra.hermes.stream.MedicineStream
import com.typesafe.config.ConfigFactory

/**
  * @Author: adria
  * @Description:
  * @Date: 15:29 2018/10/12
  * @Modified By:
  */

object Master {
  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = [master]"))
      .withFallback(ConfigFactory.load("application"))
    val system = ActorSystem("hermes", config)

    val master = system.actorOf(Props(classOf[Master]), name = "master")
    Cluster(system) registerOnMemberUp {
      ClusterClientReceptionist(system).registerService(master)
    }
  }
}

class Master extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  implicit val mat = ActorMaterializer()
  implicit val session = SlickSession.forConfig("slick-postgres")
  context.system.registerOnTermination(() => session.close())

  // implicit val getMedBaseResult = GetResult(r => Medicine(r.nextString, r.nextString, r.nextString))

  var workers = IndexedSeq.empty[ActorRef]
  var jobCounter = 0

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberUp], classOf[UnreachableMember])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
    case UnreachableMember(member) ⇒
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) ⇒
      log.info(
        "Member is Removed: {} after {}",
        member.address, previousStatus)
    case WorkerRegistration if !workers.contains(sender()) =>
      context watch sender()
      workers = workers :+ sender()
    case Terminated(worker) =>
      workers = workers.filterNot(_ == worker)
    case s: StartJob =>
      jobCounter += 1
      MedicineStream.mappingSteam(context, s.jobType, workers(jobCounter % workers.size), log)(session)
      log.info("Message arrived: {}", sender())
    case _ =>
  }

}
