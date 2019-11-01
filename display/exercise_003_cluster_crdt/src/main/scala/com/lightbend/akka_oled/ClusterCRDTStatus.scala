package com.lightbend.akka_oled

import akka.actor.{Actor, ActorLogging, Props}
import akka_oled.Logo

object  ClusterCRDTStatus{
   val ACTOR_NAME = "cluster-Sharding-status"
   def props() = Props(new ClusterCRDTStatus)

}
class ClusterCRDTStatus extends Actor with ActorLogging with Logo{
   override def receive: Actor.Receive = ???
}
