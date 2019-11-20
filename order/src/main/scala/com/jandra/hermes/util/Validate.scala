package com.jandra.hermes.util

/**
  * @Author: adria
  * @Description:
  * @Date: 11:22 2019/9/25
  * @Modified By:
  */

trait Validate {

  def idValidate(id: String, idName: String) = id match {
    case null => throw new IllegalArgumentException("The " + idName + " may not be set to null.")
    case "" => throw new IllegalArgumentException("The " + idName + " may not be set to blank.")
    case _ =>
  }

}
