package com.jandra.hermes.serializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.jackson

/**
  * @Author: adria
  * @Description:
  * @Date: 18:19 2019/12/3
  * @Modified By:
  */

trait JsonCodec extends Json4sSupport{
  import org.json4s.DefaultFormats
  import org.json4s.ext.JodaTimeSerializers

  implicit val serializer = jackson.Serialization
  implicit val formats = DefaultFormats ++ JodaTimeSerializers.all

}
