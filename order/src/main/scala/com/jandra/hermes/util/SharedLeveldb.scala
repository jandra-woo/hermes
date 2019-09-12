package com.jandra.hermes.util

/**
  * @Author: adria
  * @Description:
  * @Date: 9:25 2019/7/10
  * @Modified By:
  */

import akka.actor.{Actor, ActorIdentity, ActorPath, ActorSystem, Identify, Props}
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object SharedLeveldb {

  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
    // Start the shared journal one one node (don't crash this SPOF)
    // This will not be needed with a distributed journal
    if (startStore) {
      system.actorOf(Props[SharedLeveldbStore], "store")
    }
    // register the shared journal
    import system.dispatcher
    implicit val timeout = Timeout(15.seconds)
    val f = (system.actorSelection(path) ? Identify(1))
    f.onComplete {
      case Success(ActorIdentity(_, Some(ref))) =>
        SharedLeveldbJournal.setStore(ref, system)
      case Failure(exception) =>
        system.log.error("Lookup of shared journal at {} timed out, exception: {}", path, exception)
        system.terminate()
    }
  }
}
