package com.lightbend.akka_oled

import akka.actor.{ActorRef, Props}
import akka.persistence.PersistentActor
import com.lightbend.akka_oled.Client.{Get, PostTransaction, Stop, TransactionAdded}
import com.lightbend.akka_oled.ClusterShardingStatus.Notification

object Client {

   case class PostTransaction(name: String, amount: Int)

   case object Stop

   case class Get(name: String)

   final case class TransactionAdded(name: String, amount: Int)

   def props(ref: ActorRef) = Props(new Client(ref))
}

class Client(ref: ActorRef) extends PersistentActor {
   override def persistenceId: String = self.path.name

   def updateState(event: TransactionAdded): Unit = {
      name = name.orElse(Some(event.name))
      total += event.amount
   }


   var name: Option[String] = None
   var total: Int = 0

   override def receiveRecover: Receive = {
      case evt: TransactionAdded => updateState(evt)
   }

   override def receiveCommand: Receive = {
      case PostTransaction(name, amount) =>
         persist(TransactionAdded(name, amount))(updateState)
         ref ! Notification(name, total)
      case Get(name) =>
         sender() ! total
         ref ! Notification(name, total)
      case Stop => context.stop(self)
   }
}
