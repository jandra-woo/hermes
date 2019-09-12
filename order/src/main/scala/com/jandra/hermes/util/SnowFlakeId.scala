package com.jandra.hermes.util

import akka.actor.ActorSystem
import akka.event.{LogSource, Logging}

/**
  * @Author: adria
  * @Description:
  * @Date: 15:26 2019/6/27
  * @Modified By:
  */

object SnowFlakeId {
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(t: AnyRef): String = t.getClass.getName

    override def getClazz(t: AnyRef): Class[_] = t.getClass
  }
}

class SnowFlakeId(
                   val workerId: Long,
                   val system: ActorSystem,
                   var sequence: Long = 0L
                 ) {

  private[this] val log = Logging(system, this)

  val twepoch = 1288834974657L

  private[this] val workerIdBits = 10L

  private[this] val maxWorkerId = -1L ^ (-1L << workerIdBits)

  private[this] val sequenceBits = 12L

  private[this] val workerIdShift = sequenceBits

  private[this] val timestampLeftShift = sequenceBits + workerIdBits

  private[this] val sequenceMask = -1L ^ (-1L << sequenceBits)

  private[this] var lastTimestamp = -1L

  if (workerId > maxWorkerId || workerId < 0) {
    throw new IllegalArgumentException("worker Id can't be greater than %d or less than 0".format(maxWorkerId))
  }

  log.info(s"worker starting, timestamp left shift: {}, worker id bits: {}, sequence bits: {}, workerId: {}", timestampLeftShift, workerIdBits, sequenceBits, workerId)

  def get_id(): Long = {
    val id = nextId()
    id
  }

  def get_worker_id(): Long = workerId

  def get_timestamp(): Long = System.currentTimeMillis()

  protected[util] def nextId(): Long = synchronized {
    var timestamp = timeGen()

    if (timestamp < lastTimestamp) {
      log.error("clock is moving backwards. rejecting requests until {}.", lastTimestamp)
      throw new IllegalArgumentException("Clock moved backwards. Refusing to generate id for %d milliseconds.".format(lastTimestamp - timestamp))
    }

    if (lastTimestamp == timestamp) {
      sequence = (sequence + 1) & sequenceMask
      if (sequence == 0) {
        timestamp = tilNextMillis(lastTimestamp)
      }
    } else {
      sequence = 0
    }

    lastTimestamp = timestamp
    ((timestamp - twepoch) << timestampLeftShift) |
      (workerId << workerIdShift) |
      sequence
  }

  protected def tilNextMillis(lastTimestamp: Long): Long = {
    var timestamp = timeGen()
    while (timestamp <= lastTimestamp) {
      timestamp = timeGen()
    }
    timestamp
  }

  protected def timeGen(): Long = System.currentTimeMillis()
}
