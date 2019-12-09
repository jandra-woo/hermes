package com.jandra.hermes.facility.domain.aggregate

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import com.jandra.hermes.facility.domain.protocol.ProductCommand

/**
  * @Author: adria
  * @Description:
  * @Date: 17:43 2019/12/6
  * @Modified By:
  */

object Product {
  val TypeKey: EntityTypeKey[ProductCommand] =
    EntityTypeKey[ProductCommand]("Product")
}
