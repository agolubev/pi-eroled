package com.lightbend.akka_oled

import java.lang.management.ManagementFactory
import java.text.DecimalFormat

import com.sun.management.OperatingSystemMXBean
import org.apache.commons.io.FileUtils

import scala.collection.mutable

trait JavaMetricsScreen {
   def getJavaMetrics(): Array[Array[String]] = {
      val bean = ManagementFactory.getPlatformMXBean(classOf[OperatingSystemMXBean])
      val formatter = new DecimalFormat("#0.00")
      val map: Map[String, String] = mutable.LinkedHashMap[String, String](
         "Max mem:" -> FileUtils.byteCountToDisplaySize( ManagementFactory.getMemoryMXBean.getHeapMemoryUsage.getMax),
         "Curr mem:" -> FileUtils.byteCountToDisplaySize(ManagementFactory.getMemoryMXBean.getHeapMemoryUsage.getUsed),
         "CPU:" -> (formatter.format(bean.getSystemCpuLoad) + "%"),
         "Threads:" -> ManagementFactory.getThreadMXBean.getThreadCount.toString,
         "Classes:" -> ManagementFactory.getClassLoadingMXBean.getLoadedClassCount.toString).toMap
      map.toArray.map(x => Array(x._1, x._2))
   }
}
