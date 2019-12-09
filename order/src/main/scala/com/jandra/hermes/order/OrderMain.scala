package com.jandra.hermes.order

import java.net.{DatagramSocket, InetSocketAddress}
import java.nio.channels.DatagramChannel

import akka.actor.{AddressFromURIString, Props}
import akka.actor.typed.ActorSystem
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.journal.leveldb.SharedLeveldbStore
import com.jandra.hermes.order.application.{OrderRestRoutes, OrderRestService}
import com.jandra.hermes.order.domain.service.OrderDomainService
import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Random
import scala.util.control.NonFatal

/**
  * @Author: adria
  * @Description:
  * @Date: 14:29 2019/11/19
  * @Modified By:
  */

object OrderMain {
  private val random = new Random()

  def main(args: Array[String]): Unit = {
    val seedNodes = akka.japi.Util
      .immutableSeq(ConfigFactory.load("application.conf").getStringList("akka.cluster.seed-nodes"))
      .flatMap { case AddressFromURIString(s) => s.port }

    val ports = args.headOption match {
      case Some(port) => Seq(port.toInt)
      case _ =>
        // In a production application you wouldn't typically start multiple ActorSystem instances in the
        // same JVM, here we do it to easily demonstrate these ActorSystems (which would be in separate JVM's)
        // talking to each other.
        seedNodes ++ Seq(0)
    }

    val from = (akkaPort: Int) => if (!seedNodes.contains(akkaPort)) 8081 else s"80${random.nextInt(80)}".toInt

    for {
      akkaPort <- ports
      restPort <- findHttpPort(from(akkaPort))
    } startNode(config(akkaPort), restPort)

    startNode(config(2557),8190)
  }


  private def startNode(config: Config, httpPort: Int): Unit = {

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      if(httpPort == 8190) {
        //start leveldb store
        import akka.actor.typed.scaladsl.adapter._
        val store = context.actorOf(Props[SharedLeveldbStore], "store")
        val levelDbStoreKey = ServiceKey("levelDbStoreKey")
        context.system.receptionist ! Receptionist.Register(levelDbStoreKey, store)
      }

      //start orderMainService
      val orderDomainService = context.spawn(OrderDomainService(), "OrderDomainService")
      context.watch(orderDomainService)

      val routes = new OrderRestRoutes(orderDomainService)(context.system)
      new OrderRestService(routes.order, httpPort, context.system).start()

      Behaviors.empty
    }
    ActorSystem[Nothing](rootBehavior, "hermes", config)
  }

  private def config(port: Int): Config =
    ConfigFactory.parseString(
      s"""
       akka.remote.artery.canonical.port = $port
        """).withFallback(ConfigFactory.load("application.conf"))

  private def findHttpPort(attempt: Int): Option[Int] = {
    val ds: DatagramSocket = DatagramChannel.open().socket()
    try {
      ds.bind(new InetSocketAddress("localhost", attempt))
      Some(attempt)
    } catch {
      case NonFatal(e) =>
        ds.close()
        println(s"Unable to bind to port $attempt for http server to send data: ${e.getMessage}")
        None
    } finally
      ds.close()
  }
}
