package com.jandra.hermes.serializer

/**
  * @Author: adria
  * @Description:
  * @Date: 11:13 2018/9/19
  * @Modified By:
  */

import java.io.ByteArrayOutputStream

import akka.serialization.SerializerWithStringManifest
import com.jandra.hermes.cluster.InventoryChange
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, AvroSchema}
import com.jandra.hermes.cluster._

class AvroSerializer extends SerializerWithStringManifest {

  val OrderRequestManifest = "OrderRequest"
  val InventoryChangeManifest = "InventoryChange"
  val JobResultManifest = "JobResult"

  override def identifier: Int = 12306

  override def manifest(o: AnyRef): String =
    o match {
      case _: OrderRequest => OrderRequestManifest
      case _: InventoryChange => InventoryChangeManifest
      case _: JobResult => JobResultManifest
    }

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case _: OrderRequest =>
        val schema = AvroSchema[OrderRequest]
        val output = new ByteArrayOutputStream
        val avro = AvroOutputStream.binary[OrderRequest].to(output).build(schema)
        avro.write(o.asInstanceOf[OrderRequest])
        avro.close()
        output.toByteArray
      case _: InventoryChange =>
        val schema = AvroSchema[InventoryChange]
        val output = new ByteArrayOutputStream
        val avro = AvroOutputStream.binary[InventoryChange].to(output).build(schema)
        avro.write(o.asInstanceOf[InventoryChange])
        avro.close()
        output.toByteArray
      case _: JobResult =>
        val schema = AvroSchema[JobResult]
        val output = new ByteArrayOutputStream
        val avro = AvroOutputStream.binary[JobResult].to(output).build(schema)
        avro.write(o.asInstanceOf[JobResult])
        avro.close()
        output.toByteArray
    }
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    manifest match {
      case OrderRequestManifest =>
        val schema = AvroSchema[OrderRequest]
        val is = AvroInputStream.binary[OrderRequest].from(bytes).build(schema)
        val events = is.iterator.toList
        is.close()
        events(0)
      case InventoryChangeManifest =>
        val schema = AvroSchema[InventoryChange]
        val is = AvroInputStream.binary[InventoryChange].from(bytes).build(schema)
        val events = is.iterator.toList
        is.close()
        events(0)
      case JobResultManifest =>
        val schema = AvroSchema[JobResult]
        val is = AvroInputStream.binary[JobResult].from(bytes).build(schema)
        val events = is.iterator.toList
        is.close()
        events(0)
    }
  }
}
