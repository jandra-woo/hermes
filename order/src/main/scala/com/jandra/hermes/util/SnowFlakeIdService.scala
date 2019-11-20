package com.jandra.hermes.util

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.LoggerOps
import com.jandra.hermes.order.domain.protocol.OrderCommand

/**
  * @Author: adria
  * @Description:
  * @Date: 15:26 2019/6/27
  * @Modified By:
  */

object SnowFlakeIdService {
//  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
//    def genString(t: AnyRef): String = t.getClass.getName
//
//    override def getClazz(t: AnyRef): Class[_] = t.getClass
//  }

  //  def props(workerId: Long,
  //            system: ActorSystem,
  //            sequence: Long = 0L): Props = Props(new SnowFlakeIdService(workerId, system, sequence))

  // Snow Flake Command
  sealed trait SnowFlakeCommand

  case class GetId(replyTo: ActorRef[OrderCommand]) extends SnowFlakeCommand

  case class GetWorkId(replyTo: ActorRef[OrderCommand]) extends SnowFlakeCommand

  case class GetTimestamp(replyTo: ActorRef[OrderCommand]) extends SnowFlakeCommand

  // Snow Flake Reply
  sealed trait SnowFlakeReply extends OrderCommand

  case class SnowFlakeId(id: Long) extends SnowFlakeReply

  case class SnowFlakeWorkId(workId: Long) extends SnowFlakeReply

  def apply(workerId: Long,
            sequence: Long = 0L
           ): Behavior[SnowFlakeCommand] = {
    Behaviors.setup[SnowFlakeCommand] { context =>

      val twepoch = 1288834974657L

      val workerIdBits = 10L

      val maxWorkerId = -1L ^ (-1L << workerIdBits)

      val sequenceBits = 12L

      val workerIdShift = sequenceBits

      val timestampLeftShift = sequenceBits + workerIdBits

      val sequenceMask = -1L ^ (-1L << sequenceBits)

      var lastTimestamp = -1L

      if (workerId > maxWorkerId || workerId < 0) {
        throw new IllegalArgumentException("worker Id can't be greater than %d or less than 0".format(maxWorkerId))
      }

      context.log.infoN(s"worker starting, timestamp left shift: {}, worker id bits: {}, sequence bits: {}, workerId: {}", timestampLeftShift, workerIdBits, sequenceBits, workerId)

      def nextId(): Long = {
        var timestamp = timeGen()

        var newSequence = sequence

        if (timestamp < lastTimestamp) {
          context.log.error("clock is moving backwards. rejecting requests until {}.", lastTimestamp)
          throw new IllegalArgumentException("Clock moved backwards. Refusing to generate id for %d milliseconds.".format(lastTimestamp - timestamp))
        }

        if (lastTimestamp == timestamp) {
          newSequence = (sequence + 1) & sequenceMask
          if (sequence == 0) {
            timestamp = tilNextMillis(lastTimestamp)
          }
        } else {
          newSequence = 0
        }

        lastTimestamp = timestamp
        ((timestamp - twepoch) << timestampLeftShift) |
          (workerId << workerIdShift) |
          newSequence
      }

      def tilNextMillis(lastTimestamp: Long): Long = {
        var timestamp = timeGen()
        while (timestamp <= lastTimestamp) {
          timestamp = timeGen()
        }
        timestamp
      }

      def timeGen(): Long = System.currentTimeMillis()

      Behaviors.receiveMessage[SnowFlakeCommand] {
        case getId: GetId =>
          val id = nextId()
          getId.replyTo ! SnowFlakeId(id)
          Behaviors.same
        case getWorkId: GetWorkId =>
          getWorkId.replyTo ! SnowFlakeWorkId(workerId)
          Behaviors.same
      }
    }

  }
}