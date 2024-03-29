package com.lightbend.akka_oled

import java.text.SimpleDateFormat
import java.util.Date

import akka.actor.{Actor, ActorRef, Props}
import com.lightbend.akka_oled.TestActor.Ping

import scala.concurrent.duration._



object TestActor{
   case object Ping
   def props(notification:ActorRef) = Props(new TestActor(notification))
}

class TestActor(notification:ActorRef) extends Actor {
   var state:String = ""
   override def receive: Receive = {
      case Ping => state = System.currentTimeMillis().toString
         import context.dispatcher
         context.system.scheduler.scheduleOnce(1 second, self, Ping)
         notification!MetricsActor.ACTOR_STATE(self.path.name,"\n{updated="+
            new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date(state.toLong))+"}")
   }

}
