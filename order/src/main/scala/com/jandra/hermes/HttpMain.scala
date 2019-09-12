package com.jandra.hermes

/**
  * @Author: adria
  * @Description:
  * @Date: 14:47 2018/9/29
  * @Modified By:
  */

import akka.actor.{ActorSystem, AddressFromURIString, RootActorPath}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.jandra.hermes.util._
import akka.pattern.ask
import akka.util.Timeout
import com.jandra.hermes.order.{JobResult, ReceiveOrder}

import scala.concurrent.duration._


object HttpMain {
  def main(args: Array[String]): Unit = {

    new WebServer
  }


  class WebServer extends JsonSupport {
    val config = ConfigFactory.load("application_http.conf")
    implicit val system = ActorSystem("http", config)
    implicit val materializer = ActorMaterializer()
    implicit val dispatcher = system.dispatcher

    import scala.collection.JavaConverters._

    val initialContacts = config.getStringList("contact-points").asScala
      .map { case AddressFromURIString(addr) => RootActorPath(addr) / "system" / "receptionist" }.toSet
    val cClient = system.actorOf(ClusterClient.props(ClusterClientSettings(system).withInitialContacts(initialContacts)), "client")

    implicit val timeout: Timeout = 2 seconds

    val routes =
      pathSingleSlash {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Hello...</h1>"))
        }
      } ~
        path("order1") {
          post {
            entity(as[ReceiveOrder]) { receiveOrder =>
              complete {
                (cClient ? ClusterClient.Send("/user/orderEntry", receiveOrder, localAffinity = true)).mapTo[JobResult]
              }
            }
          }
        }

    //    val requestHandler: HttpRequest => Future[HttpResponse] = {
    //      case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
    //        Future(HttpResponse(200, entity = HttpEntity(
    //          ContentTypes.`text/html(UTF-8)`,
    //          "<html><body>Hello world!</body></html>")))
    //    }

    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8073)

    println(s"Started at http://localhost:8073/")

    //    val onceAllConnectionsTerminated: Future[Http.HttpTerminated] =
    //      Await.result(bindingFuture, 10.seconds)
    //      .terminate(hardDeadline = 3.seconds
    // )
    //
    //    onceAllConnectionsTerminated.flatMap{ _ =>
    //      system.terminate()
    //    }
  }

}
