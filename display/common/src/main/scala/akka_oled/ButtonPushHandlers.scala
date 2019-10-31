package akka_oled

import akka.actor.ActorRef
import akka_oled.ButtonPushHandlers.{NEXT_SCREEN, RESET_SCREEN}
import com.pi4j.io.gpio.{GpioController, GpioFactory, GpioPinDigitalInput, PinPullResistance, RaspiPin}
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}

object ButtonPushHandlers{
   case object NEXT_SCREEN
   case object RESET_SCREEN
   case object PREVIOUS_SCREEN
}

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
