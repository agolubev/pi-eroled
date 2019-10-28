package com.lightbend.akka_oled

import akka.actor.ActorRef
import com.lightbend.akka_oled.MetricsActor.{NEXT_SCREEN, PREVIOUS_SCREEN, RESET_SCREEN}
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import com.pi4j.io.gpio._

trait ButtonPushHandlers {
   val gpio: GpioController = GpioFactory.getInstance
   val DELAY = 300
   var counter = 0
   val lastClick = 0
   val RESET_DELAY = 1000
   def onStop(): Unit = gpio.removeAllListeners()

   private[this] def diff(last: Option[Long]): Long = System.currentTimeMillis() - last.getOrElse(0L)

   def initButtonPush(actor: ActorRef): Unit = {
      val upButton: GpioPinDigitalInput = gpio.provisionDigitalInputPin(RaspiPin.GPIO_16, PinPullResistance.PULL_UP)

      upButton.addListener(new GpioPinListenerDigital() {
         var lastPush: Option[Long] = None

         override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
            counter += 1
            println(counter)
            if (event.getState.isLow && diff(lastPush) > DELAY) { // display pin state on console
               if(diff(lastPush) < RESET_DELAY){
                  actor ! RESET_SCREEN
               }else{
                  actor ! NEXT_SCREEN
               }
               lastPush = Some(System.currentTimeMillis())
            }
         }
      })
   }
}
