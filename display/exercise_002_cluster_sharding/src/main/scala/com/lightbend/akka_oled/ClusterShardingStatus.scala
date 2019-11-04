/**
  * Copyright © 2016-2019 Lightbend, Inc.
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

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, LeaderChanged, ReachabilityEvent}
import akka.cluster.sharding.ShardRegion.{CurrentRegions, CurrentShardRegionState, GetShardRegionState}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka_oled.Logo
import com.lightbend.akka_oled.Client.{Get, PostTransaction, TransactionAdded}
import com.lightbend.akka_oled.ClusterShardingStatus.Notification
import eroled.{OLEDWindow, SmartOLED}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.ExecutionContext

object ClusterShardingStatus{

   case class Notification(name:String, total:Int)

   def props() =  Props(new ClusterShardingStatus())
   val ACTOR_NAME = "cluster-Sharding-status"
}

class ClusterShardingStatus  extends Actor with ActorLogging with Logo {
   var oled: SmartOLED = new SmartOLED()
   val window: OLEDWindow = new OLEDWindow(oled, 0, 0, 256, 64)
   implicit val ec: ExecutionContext = context.dispatcher
   var showingLogo = true
   val clients = mutable.Map[String, Int]()

   implicit val timeout: Timeout = 3.seconds

   val extractEntityId: ShardRegion.ExtractEntityId = {
      case msg @ PostTransaction(name, _) => (name, msg)
      case msg @ Get(name) => (name, msg)
   }

   val numberOfShards = 100

   val extractShardId: ShardRegion.ExtractShardId = {
      case PostTransaction(id, _)       => (id.hashCode % numberOfShards).toString
      case Get(id)                     => (id.hashCode % numberOfShards).toString
      case ShardRegion.StartEntity(id) =>
         // StartEntity is used by remembering entities feature
         (id.hashCode % numberOfShards).toString
   }

   val counterRegion: ActorRef = ClusterSharding(context.system).start(
      typeName = "BankAccount",
      entityProps = Client.props(self),
      settings = ClusterShardingSettings(context.system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId)

   val dur: FiniteDuration = 5.seconds

   override def preStart(): Unit = {
      log.info("BasicOLED initialized!")
      renderLogo()
      //context.system.scheduler.scheduleOnce(2.seconds, self, CurrentShardRegionState(Set()))
      context.system.scheduler.scheduleOnce(2.seconds, counterRegion, ShardRegion.getShardRegionStateInstance)

   }

   private def renderState: Unit = {
      if (showingLogo) {
         oled.clearRam()
         showingLogo = false
      }

      if (!clients.isEmpty)
         oled.drawMultilineString(clients.map[String] { case (key, value) => key + ": " + value + "        " }.mkString("\n"))
      else
         oled.drawString(0, 0, "No data")

   }


   private def renderLogo() {
      oled.clearRam()
      window.drawBwImage(30, 2, 200, 60, 0xFF.toByte, logoBytes, 0)
      window.drawScreenBuffer()
      showingLogo = true
      import context.dispatcher
      context.system.scheduler.scheduleOnce(2 second, self, SWITCH_FROM_LOGO_TO_SCREEN)

   }

   override def postStop(): Unit = {
      oled.resetOLED()
      Cluster(context.system).unsubscribe(self)
   }

   override def receive: Actor.Receive = {
      case SWITCH_FROM_LOGO_TO_SCREEN =>
         renderState
      case Notification(name,total) =>
         clients += name -> total
         renderState
      case get@Get(name) =>
         //(counterRegion ? ShardRegion.getShardRegionStateInstance).pipeTo(self)
         (counterRegion ? get).pipeTo(sender())
      case post@PostTransaction(name,amount) =>
         (counterRegion ? PostTransaction(name,amount)).pipeTo(sender())
      case CurrentShardRegionState(set) =>
         //add new shards that were rebalanced
         val entityIds = set.flatMap(_.entityIds)
         println("entities" + entityIds.mkString(","))
         entityIds.foreach(a => if(clients.get(a).isEmpty) clients += a -> 0)
         //remove old shards
         clients.foreach{case (k,v) => if(!entityIds.contains(k)) clients -= k}
         println("state = "+clients.mkString(","))
         renderState
         context.system.scheduler.scheduleOnce(2.seconds, counterRegion, ShardRegion.getShardRegionStateInstance)
   }
}
