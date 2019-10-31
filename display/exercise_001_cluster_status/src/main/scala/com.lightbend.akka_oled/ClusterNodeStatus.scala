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

import scala.collection.mutable
import scala.concurrent.duration._

object ClusterNodeStatus {

   val UPDATE_DELAY = 1 second


   case class SWITCH_FROM_LOGO_TO_SCREEN(screen: Screens.Value)

   val ACTOR_NAME = "cluster-node-status"

   def props(): Props = {
      Props(new MetricsActor())
   }

}

class ClusterNodeStatus extends Actor with ActorLogging with Logo {
   var oled: SmartOLED = new SmartOLED()
   val window: OLEDWindow = new OLEDWindow(oled, 0, 0, 256, 64)

   var currentScreen: Screens.Value = Screens.CLUSTER_STATE
   var state: Option[mutable.LinkedHashMap[String, String]] = None
   var showingLogo = true

   override def preStart(): Unit = {
      log.info("BasicOLED initialized!")
      Cluster(context.system)
         .subscribe(self,
            InitialStateAsEvents,
            classOf[LeaderChanged],
            classOf[ReachabilityEvent],
            classOf[MemberEvent]
         )
      context.become(running())
      renderLogo()
   }

   override def postStop(): Unit = {
      oled.resetOLED()
      Cluster(context.system).unsubscribe(self)
   }

   override def receive: Receive = idle

   def idle: Receive = akka.actor.Actor.emptyBehavior

   def mapHostToName(ip: String): String = {
      ip match {
         case "192.168.1.100" => "Node 0"
         case "192.168.1.101" => "Node 1"
         case "192.168.1.102" => "Node 2"
         case _ => "Node X"
      }
   }

   private def nodeStatus(member: Member, status: String): Unit = {
      if (state.isEmpty) {
         state = Some(
            mutable.LinkedHashMap[String, String]("Node 0" -> "N/A", "Node 1" -> "N/A", "Node 2" -> "N/A"))
         oled.clearRam()
      }

      state.get += mapHostToName(member.address.host.get) -> status

      renderState
   }

   private def renderState: Unit = {
      if (showingLogo) {
         oled.clearRam()
         showingLogo = false
      }

      if (state.isDefined)
         state.foreach(s =>
            oled.drawMultilineString(s.map(a => a._1 + ": " + a._2).mkString("\n"))
         )
      else
         oled.drawString(0, 21, "Joining cluster")
   }

   def running(): Receive = {
      case SWITCH_FROM_TITLE_TO_SCREEN(screen) =>
         renderState

      case msg@MemberUp(member) =>
         nodeStatus(member, "Up")
         log.debug(s"$msg")

      case msg@MemberLeft(member) =>
         nodeStatus(member, "Left")
         log.debug(s"$msg")

      case msg@MemberExited(member) =>
         nodeStatus(member, "Exited")
         log.debug(s"$msg")

      case msg@MemberJoined(member) =>
         nodeStatus(member, "Joined")
         log.debug(s"$msg")

      case msg@MemberRemoved(member, previousStatus) =>
         nodeStatus(member, "Removed")
         log.debug(s"$msg")

      case msg@MemberWeaklyUp(member) =>
         nodeStatus(member, "WeaklyUp")
         log.debug(s"$msg")

      case msg@ReachableMember(member) if member.status == Up =>
         nodeStatus(member, "Reachable")
         log.debug(s"$msg")

      case msg@ReachableMember(member) if member.status == WeaklyUp =>
         nodeStatus(member, "Reachable")
         log.debug(s"$msg")

      case msg@UnreachableMember(member) =>
         nodeStatus(member, "Unreachable")
         log.debug(s"$msg")

      case msg@LeaderChanged(Some(leader)) =>
         changeLeader(leader)
         log.debug(s"$msg")

      case msg@LeaderChanged(None) =>
         changeLeader(self.path.address)
         log.debug(s"$msg")

      case event =>
         log.debug(s"~~~> UNHANDLED CLUSTER DOMAIN EVENT: $event")

   }

   private def changeLeader(address: Address): Unit = {
      if (state.isEmpty) {
         oled.clearRam()
         state = Some(
            mutable.LinkedHashMap[String, String]("Node 0" -> "N/A", "Node 1" -> "N/A", "Node 2" -> "N/A"))
      }
      state(Screens.CLUSTER_STATE).asInstanceOf[mutable.Map[String, String]] += "Leader" -> mapHostToName(address.host.getOrElse("N/A"))

      renderState
   }


   private def renderLogo() {
      oled.clearRam()
      window.drawBwImage(30, 2, 200, 60, 0xFF.toByte, logoBytes, 0)
      window.drawScreenBuffer()
      showingLogo = true
      context.system.scheduler.scheduleOnce(2 second, self, SWITCH_FROM_TITLE_TO_SCREEN(currentScreen))
   }

}
