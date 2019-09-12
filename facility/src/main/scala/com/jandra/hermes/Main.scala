package com.jandra.hermes

import com.jandra.hermes.cluster.{Master, MasterSharding}

/**
  * @Author: adria
  * @Description:
  * @Date: 10:53 2018/10/15
  * @Modified By:
  */
object Main {
  def main(args: Array[String]): Unit = {
    MasterSharding.main(Seq("2551").toArray)
    MasterSharding.main(Seq("2552").toArray)
    HttpMain.main(Array.empty)
//    Worker.main(Seq("2553").toArray)
//    Worker.main(Array.empty)
  }
}
