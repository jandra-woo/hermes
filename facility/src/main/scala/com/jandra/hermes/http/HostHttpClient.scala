package com.jandra.hermes.http

/**
  * @Author: adria
  * @Description:
  * @Date: 13:46 2018/11/7
  * @Modified By:
  */

import scala.util.{Failure, Success}
import scala.concurrent.{Future, Promise}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import akka.stream.{OverflowStrategy, QueueOfferResult}

class HostHttpClient(host:String, request: HttpRequest) {

  implicit val system = ActorSystem()

  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  val QueueSize = 10

  val poolClientFlow = Http().cachedHostConnectionPool[Promise[HttpResponse]](host)
  val queue =
    Source.queue[(HttpRequest, Promise[HttpResponse])](QueueSize, OverflowStrategy.dropNew)
      .via(poolClientFlow)
      .toMat(Sink.foreach({
        case ((Success(resp), p)) => p.success(resp)
        case ((Failure(e), p)) => p.failure(e)
      }))(Keep.left)
      .run()

  def queueRequest(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued => responsePromise.future
      case QueueOfferResult.Dropped => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }

  val responseFuture: Future[HttpResponse] = queueRequest(request)
}
