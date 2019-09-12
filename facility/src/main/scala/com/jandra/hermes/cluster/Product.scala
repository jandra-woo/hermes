package com.jandra.hermes.cluster

/**
  * @Author: adria
  * @Description:
  * @Date: 21:28 2019/3/22
  * @Modified By:
  */
import akka.actor.{Actor, ActorLogging, Props, RootActorPath}
import akka.cluster.ClusterEvent.{CurrentClusterState, InitialStateAsEvents, MemberUp, UnreachableMember}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.stream.ActorMaterializer
import com.jandra.hermes.http.RequestActor

import scala.util.{Failure, Success}


class Product extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  implicit val mat = ActorMaterializer()
  implicit val executionContext = context.system.dispatchers.lookup("my-dispatcher")

  val requestActor = context.actorOf(Props(classOf[RequestActor]))

  var initialInventory = 0

  override def preStart(): Unit = {
    //ask initial inventory
    initialInventory = 10
  }

  def receive: Receive = {
    case i: InventoryChange =>
      if(initialInventory - i.quantity < 0)
        sender() ! Failure
      else {
        initialInventory = initialInventory - i.quantity
        sender() ! Success
      }
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register
    case MemberUp(member) =>
      register(member)
      log.info("Member is Up: {}", member.address)
    case _ =>
  }

  def register(member: Member): Unit = {
    if (member.hasRole("mastersharding")) {
      context.actorSelection(RootActorPath(member.address) / "user" / "mastersharding") !
        WorkerRegistration
    }

  }
}
