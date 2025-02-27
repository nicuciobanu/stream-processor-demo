package main.scala.stream.demo.service

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Merge, Source}
import main.scala.stream.demo.model.Constants.{Buffer_Size, Charging_Source_Delay, Redis_TTL_Seconds}
import main.scala.stream.demo.model.{BatteryData, ChargingData, OutputData}
import redis.clients.jedis.Jedis
import spray.json._

import scala.concurrent.Future

case class ChargingBatteryService(jedis: Jedis) {
  def process(chargingSource: Source[ChargingData, NotUsed], batterySource: Source[BatteryData, NotUsed]): Source[OutputData, NotUsed] = {
    // Combine the two sources into a single stream
    val combinedSource: Source[Either[ChargingData, BatteryData], NotUsed] =
      Source
        .combine(chargingSource.delay(Charging_Source_Delay).map(Left(_)), batterySource.map(Right(_)))(Merge(_))
        .buffer(Buffer_Size, OverflowStrategy.dropHead)

    // Process the combined stream
    combinedSource
      .mapAsync(1)(data => updateLocalCache(jedis, data))
      .collect {
        case outputData: OutputData
            if outputData.socketId.nonEmpty && outputData.powerInWatts != 0 && outputData.stateOfChargeInPercent > 0 =>
          outputData
      }
  }

  private def updateLocalCache(jedis: Jedis, chargingBatteryData: Either[ChargingData, BatteryData]): Future[OutputData] =
    chargingBatteryData match {
      case Left(chargingData) =>
        // Use timestamp as redis key
        val key         = chargingData.vehicleId
        val cachedValue = jedis.get(key)

        if (cachedValue != null) {
          // Key exists: Update the cached OutputData with ChargingData
          val combinedData = cachedValue.parseJson.convertTo[OutputData]

          val updatedCombinedData = combinedData.copy(
            socketId = chargingData.socketId,
            powerInWatts = chargingData.powerInWatts
          )

          // Use timestamp as the Redis key
          setKeyAndTtl(jedis, key, updatedCombinedData)

          Future.successful(updatedCombinedData)

        } else {
          // Key does not exist: Create a new OutputData with ChargingData
          val newCombinedData = OutputData(
            timestamp = chargingData.timestamp,
            socketId = chargingData.socketId,
            vehicleId = chargingData.vehicleId,
            powerInWatts = chargingData.powerInWatts,
            stateOfChargeInPercent = 0 // Default value
          )

          // Use timestamp as the Redis key
          setKeyAndTtl(jedis, key, newCombinedData)

          Future.successful(newCombinedData)
        }

      case Right(batteryData) =>
        val outputData = OutputData(
          timestamp = batteryData.timestamp,
          socketId = "",
          vehicleId = batteryData.vehicleId,
          powerInWatts = 0,
          stateOfChargeInPercent = batteryData.stateOfChargeInPercent
        )

        // Use timestamp as the Redis key
        setKeyAndTtl(jedis, batteryData.vehicleId, outputData)

        Future.successful(outputData)
    }

  private def setKeyAndTtl(jedis: Jedis, key: String, outputData: OutputData): Unit = {
    jedis.set(key, outputData.toJson.toString)
    // Set TTL for the key
    jedis.expire(key, Redis_TTL_Seconds)
  }
}
