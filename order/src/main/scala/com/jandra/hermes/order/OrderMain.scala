package com.jandra.hermes.order

import com.jandra.hermes.HttpMain

/**
  * @Author: adria
  * @Description:
  * @Date: 14:31 2019/7/8
  * @Modified By:
  */

object OrderMain {
  def main(args: Array[String]): Unit = {
    OrderEntry.main(Seq("2551").toArray)
    OrderEntry.main(Seq("2552").toArray)
    HttpMain.main(Array.empty)
  }
}
