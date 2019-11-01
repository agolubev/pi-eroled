/**
  * Copyright © 2019 Lightbend, Inc
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *
  * NO COMMERCIAL SUPPORT OR ANY OTHER FORM OF SUPPORT IS OFFERED ON
  * THIS SOFTWARE BY LIGHTBEND, Inc.
  *
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package com.lightbend.akka_oled

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.management.scaladsl.AkkaManagement
import com.lightbend.akka_oled.BankAccount.{EntityEnvelope, Get, Increment}
import com.typesafe.config.ConfigFactory


object AkkaMetricsMain {

   def main(args: Array[String]): Unit = {
      val baseConfig = ConfigFactory.load()

      val system = ActorSystem("akka-oled", baseConfig)
      val clusterStatusTracker = system.actorOf(MetricsActor.props(),MetricsActor.ACTOR_NAME)
      val testActor = system.actorOf(TestActor.props(clusterStatusTracker),"TestActor")


      val extractEntityId: ShardRegion.ExtractEntityId = {
         case EntityEnvelope(name, payload) => (name, payload)
         case msg @ Get(name)               => (name, msg)
      }

      val numberOfShards = 100

      val extractShardId: ShardRegion.ExtractShardId = {
         case EntityEnvelope(id, _)       => (id.hashCode % numberOfShards).toString
         case Get(id)                     => (id.hashCode % numberOfShards).toString
         case ShardRegion.StartEntity(id) =>
            // StartEntity is used by remembering entities feature
            (id.hashCode % numberOfShards).toString
      }

      val counterRegion: ActorRef = ClusterSharding(system).start(
         typeName = "Account",
         entityProps = BankAccount.props(clusterStatusTracker),
         settings = ClusterShardingSettings(system),
         extractEntityId = extractEntityId,
         extractShardId = extractShardId)

      counterRegion ! EntityEnvelope("Michael", Increment)
      counterRegion ! EntityEnvelope("John", Increment)

      testActor ! TestActor.Ping
      AkkaManagement(system).start
   }
}
