package com.jandra.hermes.stream

import java.util.UUID

import akka.actor.{ActorContext, ActorRef}
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.stream.alpakka.slick.scaladsl.Slick
import akka.stream.scaladsl._
import akka.util.{ByteString, Timeout}
import com.jandra.hermes.cluster.StreamFeedback
import com.jandra.hermes.domain.{HttpTest, MappingResult, Medicine}
import com.jandra.hermes.util._
import slick.jdbc.{GetResult, PositionedParameters, SetParameter}
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * @Author: adria
  * @Description:
  * @Date: 14:10 2018/10/18
  * @Modified By:
  */
object MedicineStream extends JsonSupport {

  def mappingSteam(context: ActorContext, sourceType: String, downActor: ActorRef, log: LoggingAdapter)(implicit session: SlickSession): Unit = {
    implicit val system = context.system
    implicit val mat = ActorMaterializer()
    import session.profile.api._
    implicit val getMedBaseResult = GetResult(r => Medicine(UUID.fromString(r.nextString), r.<<, r.<<))

    implicit object SetUUID extends SetParameter[UUID] {
      override def apply(v: UUID, pp: PositionedParameters): Unit = {
        pp.setString(v.toString)
      }
    }

    def sql(p1: String) =
      sql"SELECT idmapping, value->'CommodityName' as CommodityName, value->'ProductName' as ProductName FROM mapping where type = $p1"
        .as[Medicine]

    implicit val timeout = Timeout(5 seconds)
    val source = Slick.source(sql(sourceType))
      .map { m =>
        Medicine(m.id, m.commodityName.replace("\"", ""), m.productName)
      }
      .log("mappingStream")
    val runnable = source.ask[StreamFeedback](parallelism = 2)(downActor)
      .runWith(Sink.ignore)

    implicit val ec = context.system.dispatcher
    runnable.onComplete(_ => log.info("mappingStream Finished!..."))
  }

  def baseStream(context: ActorContext, sourceType: String, mappingItem: Medicine, log: LoggingAdapter, requestActor: ActorRef)(implicit session: SlickSession): Unit = {
    implicit val system = context.system
    implicit val mat = ActorMaterializer()
    //    private implicit val materializer: Materializer = ActorMaterializer(ActorMaterializerSettings(system))
    implicit val ex: ExecutionContext = context.dispatcher
    import session.profile.api._
    //    implicit val getMedBaseResult = GetResult(r => Medicine(r.nextString, r.nextString, r.nextString))
    implicit val getMappingResult = GetResult(r => MappingResult(UUID.fromString(r.nextString), UUID.fromString(r.nextString), r.<<))

    implicit object SetUUID extends SetParameter[UUID] {
      override def apply(v: UUID, pp: PositionedParameters): Unit = {
        pp.setString(v.toString)
      }
    }

    class Results(tag: Tag) extends Table[(UUID, UUID, Double)](tag, "mapping") {
      def id = column[UUID]("idmapping")

      def idresult = column[UUID]("idresult")

      def score = column[Double]("score")

      def * = (id, idresult, score)
    }

    def search(i: Medicine) = sql"SELECT ${i.id}, idbase, 1 FROM base WHERE type = $sourceType and name = ${i.commodityName}"

    def update(m: MappingResult) = {
      val results = TableQuery[Results]
      val q = for {c <- results if c.id === m.mappingId} yield (c.idresult, c.score)
      q.update(m.baseId, m.score)
    }


    val source = Source.single(mappingItem)
    implicit val timeout = Timeout(5 seconds)
    val result =
      source.via(
        Slick.flowWithPassThrough(item =>
          search(item).as[MappingResult]
        )
      )
        .map{t =>
          val httpTest = HttpTest(t.head.baseId.toString)
          val s = httpTest.asJson.noSpaces
          HttpRequest(method = HttpMethods.POST, uri = "https://7b7b5e76-0974-4262-8ff0-0494fd2ae92a.mock.pstmn.io/hello", entity = HttpEntity(s))
        }
        .ask[MappingResult](parallelism = 1)(requestActor)
        .log("baseStream")
        .runWith(Slick.sink(mappingResult => update(mappingResult)))

    implicit val ec = context.system.dispatcher
    result.onComplete(_ => log.info("baseStream Finished!..."))
  }
}




