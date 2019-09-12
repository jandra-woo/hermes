package com.jandra.hermes.order

/**
  * @Author: adria
  * @Description:
  * @Date: 9:27 2019/6/28
  * @Modified By:
  */

import akka.actor.{ActorLogging, ActorRef}
import akka.persistence.{PersistentActor, SnapshotOffer}

class OrderEntryCounter extends PersistentActor with ActorLogging {

  import OrderEntryCounter._

  log.info("OrderEntryCounter Starting!")
  override def persistenceId: String = "OrderEntryCounter"

  private var id = 0L
  private var orderEntries = Map[ActorRef, Long]()

  private def register(ref: ActorRef) = {
    id += 1
    orderEntries += (ref -> id)
  }

  override def receiveRecover: Receive = {
    case evt: AddOrderEntry =>
      register(evt.ref)
      log.info("Recovered order actor: {}, id: {}", evt.ref, id)
    case SnapshotOffer(_, snapshot: Map[ActorRef, Long]) => orderEntries = snapshot
  }

  val snapShotInterval = 5

  def receiveCommand = {
    case RegisterOrderEntry(ref) =>
      if (orderEntries contains (ref)) {
        sender() ! OrderEntryId(ref, orderEntries(ref))
      } else {
        persist(AddOrderEntry(ref)) { event =>
          register(ref)
          log.info("Order Actor Added: {}", event.ref)
          sender() ! OrderEntryId(event.ref, id)
          if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
            saveSnapshot(orderEntries)
        }
      }
    case _ =>
      log.error("Unrecognized message in order entry counter!")
  }
}

object OrderEntryCounter {

  case class AddOrderEntry(ref: ActorRef)

}