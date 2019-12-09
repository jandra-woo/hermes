package com.jandra.hermes.common.domain

/**
  * @Author: adria
  * @Description:
  * @Date: 17:57 2019/8/22
  * @Modified By:
  */

abstract trait Entity {

  //
  protected var identity: String

  // get ID
  def id = identity

  // set ID
  protected def id_=(newId: String) = {
    identity = newId
  }
}
