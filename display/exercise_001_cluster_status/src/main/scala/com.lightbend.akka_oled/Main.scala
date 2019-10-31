package com.lightbend.akka_oled

object AkkaMetricsMain {

   def main(args: Array[String]): Unit = {
      val baseConfig = ConfigFactory.load()

      val system = ActorSystem("akka-oled", baseConfig)
      val clusterStatusTracker = system.actorOf(ClusterNodeStatus.props(),ClusterNodeStatus.ACTOR_NAME)

      AkkaManagement(system).start

   }
}