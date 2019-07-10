package com.lightbend.akka_oled

import akka.actor.ActorRef
import com.lightbend.akka_oled.MetricsActor.{NEXT_SCREEN, PREVIOUS_SCREEN}
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import com.pi4j.io.gpio._

trait ButtonPushHandlers {
   val gpio: GpioController = GpioFactory.getInstance
   val DELAY = 300
   def onStop(): Unit = gpio.removeAllListeners()

   private[this] def diff(last: Option[Long]): Long = System.currentTimeMillis() - last.getOrElse(0L)

   def initButtonPush(actor: ActorRef): Unit = {
      val downButton: GpioPinDigitalInput = gpio.provisionDigitalInputPin(RaspiPin.GPIO_12, PinPullResistance.PULL_UP)

      downButton.addListener(new GpioPinListenerDigital() {
         var lastPush: Option[Long] = None

         override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
            if (event.getState.isLow && diff(lastPush) > DELAY) { // display pin state on console
               lastPush = Some(System.currentTimeMillis())
               actor ! NEXT_SCREEN
            }
         }
      })

      val upButton: GpioPinDigitalInput = gpio.provisionDigitalInputPin(RaspiPin.GPIO_16, PinPullResistance.PULL_UP)

      upButton.addListener(new GpioPinListenerDigital() {
         var lastPush: Option[Long] = None

         override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
            if (event.getState.isLow && diff(lastPush) > DELAY) { // display pin state on console
               lastPush = Some(System.currentTimeMillis())
               actor ! PREVIOUS_SCREEN
            }
         }
      })
   }
}
