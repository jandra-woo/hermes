package com.jandra.hermes.order.application

import scala.util.{Failure, Success}
import scala.concurrent.duration._

import akka.actor.typed.ActorSystem
import akka.actor.CoordinatedShutdown
import akka.{Done, actor => classic}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

/**
  * @Author: adria
  * @Description:
  * @Date: 16:33 2019/11/14
  * @Modified By:
  */

final class OrderRestService(routes: Route, port: Int, system: ActorSystem[_]) {
  // Akka HTTP still needs a classic ActorSystem to start
  import akka.actor.typed.scaladsl.adapter._
  implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
  private val shutdown = CoordinatedShutdown(classicSystem)
  import system.executionContext

  def start(): Unit = {
    val futureBinding = Http().bindAndHandle(routes, "localhost", port)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("HttpServer online at http://{}:{}/", address.getHostString, address.getPort)
        shutdown.addTask(CoordinatedShutdown.PhaseServiceRequestsDone, "http-graceful-terminate") { () =>
          binding.terminate(10.seconds).map { _ =>
            system.log.info("HttpServer http://{}:{}/ graceful shutdown completed", address.getHostString, address.getPort)
            Done
          }
        }
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
}
