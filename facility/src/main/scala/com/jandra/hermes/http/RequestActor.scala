package com.jandra.hermes.http

/**
  * @Author: adria
  * @Description:
  * @Date: 16:55 2018/11/7
  * @Modified By:
  */

import java.util.UUID

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import com.jandra.hermes.cluster.StreamFeedback
import com.jandra.hermes.domain.MappingResult

class RequestActor extends Actor with ActorLogging {
  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)

//  override def preStart() = {
//    http.singleRequest(HttpRequest(uri = "http://akka.io"))
//      .pipeTo(self)
//  }

  def receive = {
    case request @ HttpRequest(_, _, _, _, _) =>
      http.singleRequest(request).pipeTo(self)
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        log.info("Got response, body: " + body.utf8String)
      }
      sender() ! MappingResult(UUID.fromString("12345"),UUID.fromString("12345"), 1)
    case resp @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }
}
