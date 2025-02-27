package main.scala.stream.demo

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import main.scala.stream.demo.model.Constants.{Host, Port}
import main.scala.stream.demo.model.{BatteryData, ChargingData, OutputData}
import main.scala.stream.demo.service.ChargingBatteryService
import redis.clients.jedis.Jedis

import scala.util.Random

object ChargingBatteryApp extends App {

  implicit val system: ActorSystem = ActorSystem("ChargingBattery")

  val jedis                  = new Jedis(Host, Port)
  val chargingBatteryService = ChargingBatteryService(jedis)

  val hardcodedTimestamps = Array(
    "2023-11-02T17:08:48Z",
    "2023-11-02T17:08:49Z",
    "2023-11-02T17:09:00Z",
    "2023-11-02T17:09:15Z",
    "2023-11-02T17:09:30Z",
    "2023-11-02T17:09:45Z"
  )

  // Helper function to generate ChargingData
  private def generateChargingData(): ChargingData = {
    val timestamp    = hardcodedTimestamps(Random.between(0, hardcodedTimestamps.size - 1)) // Reuse a timestamp from the hardcoded list
    val socketId     = s"s${Random.between(1, 10)}"                                         // Cycle through 10 socket IDs
    val vehicleId    = s"v${Random.between(1, 100)}"                                        // Cycle through 100 vehicle IDs
    val powerInWatts = 50 + Random.between(0, 49)                                           // Vary power between 50 and 99 watts

    ChargingData(timestamp, socketId, vehicleId, powerInWatts)
  }

  // Helper function to generate BatteryData
  private def generateBatteryData(): BatteryData = {
    val timestamp = hardcodedTimestamps(Random.between(0, hardcodedTimestamps.size - 1)) // Reuse a timestamp from the hardcoded list
    val vehicleId = s"v${Random.between(1, 100)}"                                        // Cycle through 100 vehicle IDs
    val stateOfChargeInPercent = 20 + Random.between(0, 79) // Vary charge between 20% and 99%

    BatteryData(timestamp, vehicleId, stateOfChargeInPercent)
  }

  // Infinite source streams with different elements and hardcoded timestamps

  private val iterator = Iterator.range(start = 0, end = 1000, step = 1)
  val chargingSource: Source[ChargingData, NotUsed] =
    Source.fromIterator(() => iterator).map(_ => generateChargingData())

  val batterySource: Source[BatteryData, NotUsed] =
    Source.fromIterator(() => iterator).map(_ => generateBatteryData())

  val processedStream: Source[OutputData, NotUsed] =
    chargingBatteryService.process(chargingSource = chargingSource, batterySource = batterySource)

  processedStream.runWith(Sink.foreach(println))
}
