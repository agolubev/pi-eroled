
name := "pi-eroled"

version := "0.1"
import com.typesafe.sbt.packager.docker._
resolvers ++= Seq(
   Resolver.sonatypeRepo("public"),
   "Confluent Maven Repo" at "http://packages.confluent.io/maven/"
)


lazy val `er-oled` = (project in file("er-oled"))
   .settings(libraryDependencies ++=
      Seq(
         "com.pi4j" % "pi4j-parent" % "1.2" pomOnly(),
         "ch.qos.logback" % "logback-classic" % "1.2.3",
         "org.apache.commons" % "commons-lang3" % "3.1",
         "commons-io" % "commons-io" % "2.5",
         "org.scalatest" %% "scalatest" % "3.0.8" % Test,
         "org.scalamock" %% "scalamock" % "4.1.0" % Test,
         "org.powermock" % "powermock-api-mockito2" % "2.0.2" % Test

      ),
      mainClass in assembly := Some("eroled.Test"),
      assemblyJarName in assembly := "pi-eroled.jar",
      assemblyExcludedJars in assembly := {
         val cp = (fullClasspath in assembly).value
         cp filter {_.data.getName().indexOf("javadoc.jar") > 0}
      }
   )

lazy val `akka-metrics` = (project in file("akka-metrics"))
   .enablePlugins(JavaAppPackaging,AshScriptPlugin)
   .settings(libraryDependencies ++=
      Seq(
         "com.typesafe.akka" %% "akka-cluster" % "2.5.23",
         "com.lightbend.akka.management" %% "akka-management" % "1.0.1",
         "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.23"
      ),
      assemblyJarName in assembly := "oled-akka.jar",
      assemblyExcludedJars in assembly := {
         val cp = (fullClasspath in assembly).value
         cp filter {_.data.getName().indexOf("javadoc.jar") > 0}
      },
      dockerCommands += ExecCmd("RUN","apt-get", "install", "wiringpi"),
      mainClass in assembly := Some("com.lightbend.akka_oled.AkkaMetricsMain"),
      packageName in Docker := "oled-akka",
      dockerUpdateLatest := true,
      dockerBaseImage := "hypriot/rpi-java",
   ).dependsOn(`er-oled`)