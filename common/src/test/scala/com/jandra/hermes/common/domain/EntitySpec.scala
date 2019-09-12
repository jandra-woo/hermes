package com.jandra.hermes.common.domain

/**
  * @Author: adria
  * @Description:
  * @Date: 10:56 2019/9/2
  * @Modified By:
  */
import org.scalatest.FunSpec

class EntitySpec extends FunSpec {

  describe("An Entity") {
    val entity = new Entity {
        override protected var identity: String = "test"
      }
    it("get id") {
      assert(entity.id == "test")
    }
  }
}
