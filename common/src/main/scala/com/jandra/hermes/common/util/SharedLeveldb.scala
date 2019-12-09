package com.jandra.hermes.common.util

import akka.actor.{ActorIdentity, ActorPath, ActorSystem, Identify, Props}
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import akka.pattern.ask

import scala.util.{Failure, Success}
import scala.concurrent.duration._

/**
  * @Author: adria
  * @Description:
  * @Date: 17:37 2019/12/9
  * @Modified By:
  */
object SharedLeveldb {

  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
    // Start the shared journal one one node (don't crash this SPOF)
    // This will not be needed with a distributed journal
    if (startStore) {
      system.actorOf(Props[SharedLeveldbStore], "store")
    }
    // register the shared journal
    implicit val dis = system.dispatcher
    implicit val timeout = Timeout(5.seconds)
    val store = system.actorSelection("akka://hermes@127.0.0.1:2557/user/store")

    val f = (store ? Identify(1))
    f.onComplete {
      case Success(ActorIdentity(_, Some(ref))) =>
        SharedLeveldbJournal.setStore(ref, system)
        println(ref + " set store")
      case Failure(exception) =>
        system.log.error("Lookup of shared journal at {} timed out, exception: {}", path, exception)
        system.terminate()
    }
  }

}
