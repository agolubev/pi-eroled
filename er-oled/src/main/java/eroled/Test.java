package eroled;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static java.lang.Math.round;

public class Test {

    public static String getString(){
        OperatingSystemMXBean bean = ManagementFactory.getPlatformMXBean(
                OperatingSystemMXBean.class);
        NumberFormat formatter = new DecimalFormat("#0.00");
        return   "Max mem:   "+FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory())
            +  "\nCurr mem:  "+FileUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory())+
               "\nCPU:       "+formatter.format(bean.getSystemCpuLoad()) +"%" +
               "\nThreads:   "+ManagementFactory.getThreadMXBean().getThreadCount() ;


    }

    public static void onShutdown(final OLED oled){
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                try {
                    oled.resetOLED();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] str) throws IOException, InterruptedException {
        SmartOLED oled = null;
        oled = new SmartOLED();
        oled.drawString(0, 21, "  Screen 1: Java Statistics");
        Thread.sleep(2000);
        oled.clearRam();

        onShutdown(oled);
        final GpioController gpio = GpioFactory.getInstance();

        final GpioPinDigitalInput downButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_12,
                PinPullResistance.PULL_UP);

        downButton.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if(event.getState().isLow()) {
                    // display pin state on console
                    System.out.println(" --> Down ");

                }
            }

        });


        final GpioPinDigitalInput upButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_16,
                PinPullResistance.PULL_UP);

        upButton.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if(event.getState().isLow()) {
                    // display pin state on console
                    System.out.println(" --> Up ");

                }
            }

        });

        while(true) {
            try {

                int v = 0;
                for (String s : getString().split("\n")) {
                    oled.drawString(0, v, s);
                    v = v + 16;
                }
                Thread.sleep(1000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        /*try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.print("Waiting");
        oled.resetOLED();*/
    }
}
