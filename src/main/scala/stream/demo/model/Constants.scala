package main.scala.stream.demo.model

import scala.concurrent.duration.DurationInt

object Constants {
  // or move to config file
  val Buffer_Size           = 1000
  val Redis_TTL_Seconds     = 3600 // TTL in seconds
  val Charging_Source_Delay = 1.seconds
  val Host                  = "localhost"
  val Port                  = 6379
}
