package com.lightbend.akka_oled

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.lightbend.akka_oled.MetricsActor.{NEXT_SCREEN, PREVIOUS_SCREEN}
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import com.pi4j.io.gpio._

object MetricsExtension extends ExtensionId[MetricsExtensionImpl] with ExtensionIdProvider {
   override def lookup = MetricsExtension

   override def createExtension(system: ExtendedActorSystem) = new MetricsExtensionImpl(system)
}

class MetricsExtensionImpl(system: ExtendedActorSystem) extends Extension {
   val gpio: GpioController = GpioFactory.getInstance
   val metricsActor = system.actorSelection("/user/" + MetricsActor.ACTOR_NAME)
   val downButton: GpioPinDigitalInput = gpio.provisionDigitalInputPin(RaspiPin.GPIO_12, PinPullResistance.PULL_UP)

   downButton.addListener(new GpioPinListenerDigital() {
      override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
         if (event.getState.isLow) { // display pin state on console
            metricsActor ! NEXT_SCREEN
         }
      }
   })


   val upButton: GpioPinDigitalInput = gpio.provisionDigitalInputPin(RaspiPin.GPIO_16, PinPullResistance.PULL_UP)

   upButton.addListener(new GpioPinListenerDigital() {
      override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
         metricsActor ! PREVIOUS_SCREEN
      }
   })
}
