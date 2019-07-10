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

import java.lang.management.ManagementFactory
import java.text.DecimalFormat

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, LeaderChanged, MemberEvent, MemberExited, MemberJoined, MemberLeft, MemberRemoved, MemberUp, MemberWeaklyUp, ReachabilityEvent, ReachableMember, UnreachableMember}
import com.lightbend.akka_oled.MetricsActor.{ACTOR_STATE, NEXT_SCREEN, PREVIOUS_SCREEN, SWITCH_FROM_TITLE_TO_SCREEN, Screens, UPDATE_DELAY, UPDATE_STATE, UPDATE_STATISTICS}
import com.sun.management.OperatingSystemMXBean
import eroled.SmartOLED
import org.apache.commons.io.FileUtils
import akka.actor.Props

import scala.concurrent.duration._

object MetricsActor {

   val UPDATE_DELAY = 1 second

   case object NEXT_SCREEN

   case object PREVIOUS_SCREEN
   case class SWITCH_FROM_TITLE_TO_SCREEN(screen:Screens.Value)
   case object UPDATE_STATISTICS
   case class ACTOR_STATE(path:String, State:String)
   case object UPDATE_STATE
   val ACTOR_NAME = "akka-status-tracker"

   def props(): Props = {
      Props(new MetricsActor())
   }

   object Screens extends Enumeration {

      import scala.language.implicitConversions

      implicit def valueToPlanetVal(x: Value): Val = x.asInstanceOf[Val]

      protected case class Val(str: String) extends super.Val

      val JAVA_METRICS = Val("Java Metrics")
      val ACTOR_STATE = Val("Actor State")
      val CLUSTER_STATE = Val("Cluster State")
      val NODE_STATE = Val("Cluster Node state")
      val CLUSTER_SHARDING = Val("Sharding state")
   }

}

class MetricsActor extends Actor with ActorLogging with ButtonPushHandlers with JavaMetricsScreen {
   var oled: SmartOLED = new SmartOLED()
   import context.dispatcher

   var currentScreen: Screens.Value = Screens.JAVA_METRICS
   var state = Map.empty[Screens.Value , List[(String, String)]]
   var showingTitle = true
   override def preStart(): Unit = {
      //oled.init()
      log.info("OLED initialized!")
      Cluster(context.system)
         .subscribe(self,
            InitialStateAsEvents,
            classOf[LeaderChanged],
            classOf[ReachabilityEvent],
            classOf[MemberEvent]
         )
      //imers.startPeriodicTimer("heartbeat-timer", Heartbeat, heartbeatIndicatorInterval)
      context.become(running())
      renderTitle()

      initButtonPush(self)
   }

   override def postStop(): Unit = {
      oled.resetOLED()
      Cluster(context.system).unsubscribe(self)
   }

   override def receive: Receive = idle

   def idle: Receive = akka.actor.Actor.emptyBehavior

   def running(): Receive = {
      case NEXT_SCREEN =>
         currentScreen = if(currentScreen.id == Screens.maxId - 1) Screens(0) else Screens(currentScreen.id + 1)
         log.info("Next screen")
         renderTitle()
      case PREVIOUS_SCREEN =>
         currentScreen = if(currentScreen.id == 0) Screens(Screens.maxId - 1) else Screens(currentScreen.id - 1)
         log.info("Previous screen")
         renderTitle()
      case SWITCH_FROM_TITLE_TO_SCREEN(screen)  =>
         if(screen == currentScreen) {
            showingTitle = false
            log.info("Render screen")
            oled.clearRam()
            renderScreen
         }
      case UPDATE_STATISTICS  if currentScreen == Screens.JAVA_METRICS =>
               oled.drawKeyValues(getJavaMetrics)
               import context.dispatcher
               context.system.scheduler.scheduleOnce(UPDATE_DELAY, self, UPDATE_STATISTICS)
      case UPDATE_STATE  if currentScreen == Screens.ACTOR_STATE =>
         renderActorState()
         context.system.scheduler.scheduleOnce(UPDATE_DELAY, self, UPDATE_STATE)
      case ACTOR_STATE(path,actorState) =>
         state += (Screens.ACTOR_STATE -> List[(String,String)](("Actor:", path),("State:", actorState)))
/*
   case Heartbeat if hearbeatLEDOn =>
      setPixelColorAndShow(strip, logicalToPhysicalLEDMapping(HeartbeatLedNumber), Black)
      context.become(running(hearbeatLEDOn = false))

   case Heartbeat =>
      setPixelColorAndShow(strip, logicalToPhysicalLEDMapping(HeartbeatLedNumber), heartbeartIndicatorColor)
      context.become(running(hearbeatLEDOn = true))

   case msg @ MemberUp(member) =>
      setPixelColorAndShow(strip, mapHostToLED(member.address.host.get), nodeUpColor)
      log.debug(s"$msg")

   case msg @ MemberLeft(member) =>
      setPixelColorAndShow(strip, mapHostToLED(member.address.host.get), nodeLeftColor)
      log.debug(s"$msg")

   case msg @ MemberExited(member) =>
      setPixelColorAndShow(strip, mapHostToLED(member.address.host.get), nodeExitedColor)
      log.debug(s"$msg")

   case msg @ MemberJoined(member) =>
      setPixelColorAndShow(strip, mapHostToLED(member.address.host.get), nodeJoinedColor)
      log.debug(s"$msg")

   case msg @ MemberRemoved(member, previousStatus) =>
      setPixelColorAndShow(strip, mapHostToLED(member.address.host.get), nodeDownColor)
      log.debug(s"$msg")

   case msg @ MemberWeaklyUp(member) =>
      setPixelColorAndShow(strip, mapHostToLED(member.address.host.get), nodeWeaklyUpColor)
      log.debug(s"$msg")

   case msg @ ReachableMember(member) if member.status == Up =>
      setPixelColorAndShow(strip, mapHostToLED(member.address.host.get), nodeUpColor)
      log.debug(s"$msg")

   case msg @ ReachableMember(member) if member.status == WeaklyUp =>
      setPixelColorAndShow(strip, mapHostToLED(member.address.host.get), nodeWeaklyUpColor)
      log.debug(s"$msg")

   case msg @ UnreachableMember(member) =>
      setPixelColorAndShow(strip, mapHostToLED(member.address.host.get), nodeUnreachableColor)
      log.debug(s"$msg")

   case msg @ LeaderChanged(Some(leader)) if leader.host.getOrElse("") == thisHost =>
      setPixelColorAndShow(strip, logicalToPhysicalLEDMapping(LeaderLedNumber), leaderIndicatorColor)
      log.debug(s"$msg")

   case msg @ LeaderChanged(Some(leader)) =>
      setPixelColorAndShow(strip, logicalToPhysicalLEDMapping(LeaderLedNumber), Black)
      log.debug(s"$msg")

   case msg @ LeaderChanged(None) =>
      setPixelColorAndShow(strip, logicalToPhysicalLEDMapping(LeaderLedNumber), Black)
      log.debug(s"$msg")

   case event =>
      log.debug(s"~~~> UNHANDLED CLUSTER DOMAIN EVENT: $event")
*/
   }

   private def renderScreen() {
      println("RenderState "+currentScreen)
      currentScreen match {
         case Screens.JAVA_METRICS =>
            import context.dispatcher
            oled.drawKeyValues(getJavaMetrics)
            context.system.scheduler.scheduleOnce(UPDATE_DELAY, self, UPDATE_STATISTICS)
         case Screens.ACTOR_STATE =>
            if (state.get(currentScreen).isEmpty) renderEmptyScreen
            else {
               renderActorState()
               context.system.scheduler.scheduleOnce(UPDATE_DELAY, self, UPDATE_STATE)
            }
         case Screens.CLUSTER_STATE => if (state.get(currentScreen).isEmpty) renderEmptyScreen
         case Screens.NODE_STATE => if (state.get(currentScreen).isEmpty) renderEmptyScreen
         case Screens.CLUSTER_SHARDING => if (state.get(currentScreen).isEmpty) renderEmptyScreen
      }
   }

   private def renderEmptyScreen() {
      oled.clearRam()
      oled.drawString(0, 21, "No data")
   }

   private def renderActorState(): Unit = {
      state.get(currentScreen).foreach(s =>
         oled.drawMultilineString(s.map(a => a._1 + a._2).mkString("\n"))
      )
   }


   private def renderTitle() {
      oled.clearRam()
      oled.drawString(0, 21, "Screen " + (currentScreen.id + 1) + ": " + currentScreen.str)
      log.info("Rendered title")
      showingTitle = true
      context.system.scheduler.scheduleOnce(2 second, self, SWITCH_FROM_TITLE_TO_SCREEN(currentScreen))
   }

}
