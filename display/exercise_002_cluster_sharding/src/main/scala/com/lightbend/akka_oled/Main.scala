package com.lightbend.akka_oled

import akka.actor.ActorSystem
import akka.management.scaladsl.AkkaManagement
import com.typesafe.config.ConfigFactory

object Main {

   def main(args: Array[String]): Unit = {
      val baseConfig = ConfigFactory.load()

      val system = ActorSystem("akka-oled", baseConfig)
      val clusterStatusTracker = system.actorOf(ClusterNodeStatus.props(),ClusterNodeStatus.ACTOR_NAME)

      AkkaManagement(system).start

   }
}