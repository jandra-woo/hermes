package com.jandra.hermes.order.domain.entity

/**
  * @Author: adria
  * @Description:
  * @Date: 15:58 2019/9/2
  * @Modified By:
  */

import org.scalatest.FunSpec

class OrderItemSpec extends FunSpec {

  describe("Order Item") {
    it("orderItemId should not be null") {
      assertThrows[IllegalArgumentException] {
        OrderItem(null, "", "abc", 1, 1)
      }
    }
    it("orderItemId should not be blank") {
      assertThrows[IllegalArgumentException] {
        OrderItem("", "", "abc", 1, 1)
      }
    }
    it("productId should not be null") {
      assertThrows[IllegalArgumentException] {
        OrderItem("123", null, "abc", 1, 1)
      }
    }
    it("productId should not be blank") {
      assertThrows[IllegalArgumentException] {
        OrderItem("123", "", "abc", 1, 1)
      }
    }
    it("quantity should not less than or equal to 0") {
      assertThrows[IllegalArgumentException] {
        OrderItem("123", "456", "abc", -1, 1)
      }
    }
    it("unitPrice should not less than or equal to 0") {
      assertThrows[IllegalArgumentException] {
        OrderItem("123", "456", "abc", 1, 0)
      }
    }
  }
}
