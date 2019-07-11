package com.lightbend.akka_oled

import akka.actor.{ActorRef, Props, ReceiveTimeout}
import akka.persistence.PersistentActor
import com.lightbend.akka_oled.BankAccount.{CounterChanged, Decrement, Get, Increment, Stop}
import com.lightbend.akka_oled.MetricsActor.{ACTOR_STATE, PERSISTENT_ACTOR_STATE, UPDATE_DELAY, UPDATE_STATE}

import scala.concurrent.duration._

object BankAccount {

   case object Increment
   case object Decrement
   final case class Get(name: String)
   final case class EntityEnvelope(name: String, payload: Any)

   case object Stop
   final case class CounterChanged(delta: Int)
   def props(notification:ActorRef) = Props(new BankAccount(notification))


}



class BankAccount(notification:ActorRef) extends PersistentActor {
   import akka.cluster.sharding.ShardRegion.Passivate
   import context.dispatcher

   context.setReceiveTimeout(120.seconds)

   // self.path.name is the entity identifier (utf-8 URL-encoded)
   override def persistenceId: String = self.path.name
   val rand = scala.util.Random
   var count = rand.nextInt(100)

   def updateState(event: CounterChanged): Unit = {
      count += event.delta

      context.system.scheduler.scheduleOnce(1 second, self,
         if (rand.nextInt(100) < 50) Increment else Decrement )
      notification ! PERSISTENT_ACTOR_STATE(self.path.name,s"{balance=$count}")

   }
   override def receiveRecover: Receive = {
      case evt: CounterChanged => updateState(evt)
   }

   override def receiveCommand: Receive = {
      case Increment      => persist(CounterChanged(+1))(updateState)
      case Decrement      => persist(CounterChanged(-1))(updateState)
      case Get(_)         => sender() ! count
      case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
      case Stop           => context.stop(self)
   }
}