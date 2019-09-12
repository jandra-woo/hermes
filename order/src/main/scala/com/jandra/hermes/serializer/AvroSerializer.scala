package com.jandra.hermes.serializer

/**
  * @Author: adria
  * @Description:
  * @Date: 11:13 2018/9/19
  * @Modified By:
  */

import java.io.ByteArrayOutputStream

import akka.serialization.SerializerWithStringManifest
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, AvroSchema}

import com.jandra.hermes.order._

class AvroSerializer extends SerializerWithStringManifest {

  val JobResultManifest = "JobResult"
  val ReceiveOrderManifest = "ReceiveOrder"

  override def identifier: Int = 12306

  override def manifest(o: AnyRef): String =
    o match {
      case _: JobResult => JobResultManifest
      case _: ReceiveOrder => ReceiveOrderManifest
    }

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
     case _: JobResult =>
        val schema = AvroSchema[JobResult]
        val output = new ByteArrayOutputStream
        val avro = AvroOutputStream.binary[JobResult].to(output).build(schema)
        avro.write(o.asInstanceOf[JobResult])
        avro.close()
        output.toByteArray
      case _: ReceiveOrder =>
        val schema = AvroSchema[ReceiveOrder]
        val output = new ByteArrayOutputStream
        val avro = AvroOutputStream.binary[ReceiveOrder].to(output).build(schema)
        avro.write(o.asInstanceOf[ReceiveOrder])
        avro.close()
        output.toByteArray
    }
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    manifest match {
     case JobResultManifest =>
        val schema = AvroSchema[JobResult]
        val is = AvroInputStream.binary[JobResult].from(bytes).build(schema)
        val events = is.iterator.toList
        is.close()
        events(0)
      case ReceiveOrderManifest =>
        val schema = AvroSchema[ReceiveOrder]
        val is = AvroInputStream.binary[ReceiveOrder].from(bytes).build(schema)
        val events = is.iterator.toList
        is.close()
        events(0)
    }
  }
}
