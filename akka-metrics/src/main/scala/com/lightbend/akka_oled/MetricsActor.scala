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

import akka.actor.{Actor, ActorLogging, Address, Props}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent.{InitialStateAsEvents, LeaderChanged, MemberEvent, MemberExited, MemberJoined, MemberLeft, MemberRemoved, MemberUp, MemberWeaklyUp, ReachabilityEvent, ReachableMember, UnreachableMember}
import akka.cluster.MemberStatus.{Up, WeaklyUp}
import com.lightbend.akka_oled.MetricsActor._
import eroled.SmartOLED

import scala.collection.mutable
import scala.concurrent.duration._

object MetricsActor {

   val UPDATE_DELAY = 1 second

   case object NEXT_SCREEN

   case object PREVIOUS_SCREEN
   case class SWITCH_FROM_TITLE_TO_SCREEN(screen:Screens.Value)
   case object UPDATE_STATISTICS
   case class ACTOR_STATE(path:String, State:String)
   case class PERSISTENT_ACTOR_STATE(path:String, State:String)

   case object UPDATE_STATE
   val ACTOR_NAME = "akka-status-tracker"

   def props(): Props = {
      Props(new MetricsActor())
   }

   object Screens extends Enumeration {

      import scala.language.implicitConversions

      implicit def valueToPlanetVal(x: Value): Val = x.asInstanceOf[Val]

      protected case class Val(str: String) extends super.Val

      val CLUSTER_STATE = Val("Cluster State")
      val JAVA_METRICS = Val("Java Metrics")
      val ACTOR_STATE = Val("Actor State")
      val NODE_STATE = Val("Cluster Node state")
      val CLUSTER_SHARDING = Val("Sharding state")
   }

}

class MetricsActor extends Actor with ActorLogging with ButtonPushHandlers with JavaMetricsScreen {
   var oled: SmartOLED = new SmartOLED()
   import context.dispatcher

   var currentScreen: Screens.Value = Screens.CLUSTER_STATE
   var state = mutable.Map.empty[Screens.Value , Any]
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

   def mapHostToName(ip:String):String={
      ip match {
         case "192.168.1.100" => "Node 0"
         case "192.168.1.101" => "Node 1"
         case "192.168.1.102" => "Node 2"
         case _ => "Node X"
      }
   }

   private def nodeStatus(member:Member,status:String): Unit ={
      if (state.get(Screens.CLUSTER_STATE).isEmpty) state += Screens.CLUSTER_STATE ->
         mutable.LinkedHashMap[String, String]("Node 0" -> "N/A", "Node 1" -> "N/A", "Node 2" -> "N/A")
      state(Screens.CLUSTER_STATE).asInstanceOf[mutable.Map[String,String]] += mapHostToName(member.address.host.get) ->  status
      if(currentScreen == Screens.CLUSTER_STATE) renderClusterState
   }

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
               context.system.scheduler.scheduleOnce(UPDATE_DELAY, self, UPDATE_STATISTICS)

      case UPDATE_STATE  if currentScreen == Screens.ACTOR_STATE =>
         renderActorState()
         context.system.scheduler.scheduleOnce(UPDATE_DELAY, self, UPDATE_STATE)

      case UPDATE_STATE  if currentScreen == Screens.CLUSTER_SHARDING =>
         renderPersistentActorState
         context.system.scheduler.scheduleOnce(UPDATE_DELAY, self, UPDATE_STATE)

      case ACTOR_STATE(path,actorState) =>
         state += (Screens.ACTOR_STATE -> List[(String,String)](("Actor:", path),("State:", actorState)))

      case PERSISTENT_ACTOR_STATE(name, actorState) =>
         var m = state.get(Screens.CLUSTER_SHARDING).getOrElse(mutable.Map.empty[String, String]).asInstanceOf[mutable.Map[String,String]]
         m += ("Persistent Actor:"+name) -> ("State:"+actorState)
         state += (Screens.CLUSTER_SHARDING -> m)
      
   case msg @ MemberUp(member) =>
      nodeStatus(member, "Up")
      log.debug(s"$msg")

   case msg @ MemberLeft(member) =>
      nodeStatus(member, "Left")
      log.debug(s"$msg")

   case msg @ MemberExited(member) =>
      nodeStatus(member, "Exited")
      log.debug(s"$msg")

   case msg @ MemberJoined(member) =>
      nodeStatus(member, "Joined")
      log.debug(s"$msg")

   case msg @ MemberRemoved(member, previousStatus) =>
      nodeStatus(member, "Removed")
      log.debug(s"$msg")

   case msg @ MemberWeaklyUp(member) =>
      nodeStatus(member, "WeaklyUp")
      log.debug(s"$msg")

   case msg @ ReachableMember(member) if member.status == Up =>
      nodeStatus(member, "Reachable")
      log.debug(s"$msg")

   case msg @ ReachableMember(member) if member.status == WeaklyUp =>
      nodeStatus(member, "Reachable")
      log.debug(s"$msg")

   case msg @ UnreachableMember(member) =>
      nodeStatus(member, "Unreachable")
      log.debug(s"$msg")

   case msg @ LeaderChanged(Some(leader)) =>
      changeLeader(leader)
      log.debug(s"$msg")

   case msg @ LeaderChanged(None) =>
      changeLeader(self.path.address)
      log.debug(s"$msg")

   case event =>
      log.debug(s"~~~> UNHANDLED CLUSTER DOMAIN EVENT: $event")

   }

   private def changeLeader(address:Address): Unit ={
      if (state.get(Screens.CLUSTER_STATE).isEmpty) state += Screens.CLUSTER_STATE ->
         mutable.LinkedHashMap[String, String]("Node 0" -> "N/A", "Node 1" -> "N/A", "Node 2" -> "N/A")
      state(Screens.CLUSTER_STATE).asInstanceOf[mutable.Map[String,String]] += "Leader" -> mapHostToName(address.host.getOrElse("N/A"))
      if(currentScreen == Screens.CLUSTER_STATE) renderClusterState
   }

   private def renderScreen() {
      println("RenderState " + currentScreen)
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
         case Screens.CLUSTER_STATE => if (state.get(currentScreen).isEmpty) renderEmptyScreen else {
            renderClusterState()
            context.system.scheduler.scheduleOnce(UPDATE_DELAY, self, UPDATE_STATE)
         }
         case Screens.NODE_STATE => if (state.get(currentScreen).isEmpty) renderEmptyScreen
         case Screens.CLUSTER_SHARDING =>
            if (state.get(currentScreen).isEmpty) renderEmptyScreen
            else {
               renderPersistentActorState()
               context.system.scheduler.scheduleOnce(UPDATE_DELAY, self, UPDATE_STATE)
            }
      }
   }

   private def renderEmptyScreen() {
      oled.clearRam()
      oled.drawString(0, 21, "No data")
   }

   private def renderActorState(): Unit = {
      state.get(Screens.ACTOR_STATE).foreach(s =>
         oled.drawMultilineString(s.asInstanceOf[List[(String,String)]].map(a => a._1 + a._2).mkString("\n"))
      )
   }

   private def renderClusterState(): Unit = {
      state.get(Screens.CLUSTER_STATE).foreach(s =>
         oled.drawMultilineString(s.asInstanceOf[mutable.Map[String,String]].map(a => a._1 +": "+ a._2+"            ").mkString("\n"))
      )
   }
   private def renderPersistentActorState(): Unit = {
      state.get(Screens.CLUSTER_SHARDING).foreach(s =>
         oled.drawMultilineString(s.asInstanceOf[mutable.Map[String,String]].map(a => a._1 +"\n"+ a._2).mkString("\n"))
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
