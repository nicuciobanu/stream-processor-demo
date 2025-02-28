package stream.demo

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.testkit.TestKit
import com.dimafeng.testcontainers.{ForAllTestContainer, RedisContainer}
import main.scala.stream.demo.model.OutputData
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import redis.clients.jedis.Jedis
import spray.json._
import stream.demo.model.{BatteryData, ChargingData}
import stream.demo.service.ChargingBatteryService

import scala.concurrent.Await
import scala.concurrent.duration._

class ChargingBatteryServiceSpec
    extends TestKit(ActorSystem("ChargingBatteryServiceSpec"))
    with AnyWordSpecLike
    with Matchers
    with ForAllTestContainer
    with BeforeAndAfterEach {

  override val container: RedisContainer = RedisContainer()
  var jedis: Jedis                       = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    // Initialize the Jedis client with the Redis container's host and port
    jedis = new Jedis(container.containerIpAddress, container.mappedPort(6379))
  }

  override def afterEach(): Unit = {
    jedis.close() // Close the Jedis client
    super.afterEach()
  }

  "ChargingBatteryService" should {
    "combine charging data and battery data into output data and cache" in {
      val chargingBatteryService = ChargingBatteryService(jedis)
      // Mock data
      val chargingSource = Source(
        List(
          ChargingData("2023-11-02T17:08:49Z", "s123", "v123", 60),
          ChargingData("2023-11-02T17:09:00Z", "s456", "v456", 70)
        )
      )

      val batterySource = Source(
        List(
          BatteryData("2023-11-02T17:08:49Z", "v123", 51),
          BatteryData("2023-11-02T17:09:00Z", "v456", 55)
        )
      )

      // Combine the streams
      val outputStream = chargingBatteryService.process(chargingSource, batterySource)

      // Run the stream and collect results
      val result        = outputStream.runWith(Sink.seq)
      val outputResults = Await.result(result, 7.seconds)

      // Verify the results
      outputResults should contain theSameElementsAs Seq(
        OutputData("2023-11-02T17:08:49Z", "s123", "v123", 60, 51),
        OutputData("2023-11-02T17:09:00Z", "s456", "v456", 70, 55)
      )

      // Verify Redis cache
      val cachedValue1 = jedis.get("v123")
      cachedValue1 should not be null

      val cachedOutputData1 = cachedValue1.parseJson.convertTo[OutputData]
      cachedOutputData1 shouldEqual OutputData("2023-11-02T17:08:49Z", "s123", "v123", 60, 51)

      val cachedValue2 = jedis.get("v456")
      cachedValue2 should not be null

      val cachedOutputData2 = cachedValue2.parseJson.convertTo[OutputData]
      cachedOutputData2 shouldEqual OutputData("2023-11-02T17:09:00Z", "s456", "v456", 70, 55)
    }

    "cache charging with and battery data and return empty outut" in {
      val chargingBatteryService = ChargingBatteryService(jedis)
      // Mock data
      val chargingSource = Source(
        List(
          ChargingData("2023-11-02T17:08:49Z", "s123", "v124", 60),
          ChargingData("2023-11-02T17:09:00Z", "s456", "v351", 70)
        )
      )

      val batterySource = Source(
        List(
          BatteryData("2023-11-02T17:08:49Z", "v189", 51),
          BatteryData("2023-11-02T17:09:00Z", "v567", 55)
        )
      )

      // Combine the streams
      val outputStream = chargingBatteryService.process(chargingSource, batterySource)

      // Run the stream and collect results
      val result        = outputStream.runWith(Sink.seq)
      val outputResults = Await.result(result, 7.seconds)

      // Verify the results
      outputResults shouldBe empty

      // Verify Redis cache
      val cachedValue1 = jedis.get("v124")
      cachedValue1 should not be null

      val cachedOutputData1 = cachedValue1.parseJson.convertTo[OutputData]
      cachedOutputData1 shouldEqual OutputData("2023-11-02T17:08:49Z", "s123", "v124", 60, 0)

      val cachedValue2 = jedis.get("v351")
      cachedValue2 should not be null

      val cachedOutputData2 = cachedValue2.parseJson.convertTo[OutputData]
      cachedOutputData2 shouldEqual OutputData("2023-11-02T17:09:00Z", "s456", "v351", 70, 0)

      val cachedValue3 = jedis.get("v189")
      cachedValue3 should not be null

      val cachedOutputData3 = cachedValue3.parseJson.convertTo[OutputData]
      cachedOutputData3 shouldEqual OutputData("2023-11-02T17:08:49Z", "", "v189", 0, 51)

      val cachedValue4 = jedis.get("v567")
      cachedValue4 should not be null

      val cachedOutputData4 = cachedValue4.parseJson.convertTo[OutputData]
      cachedOutputData4 shouldEqual OutputData("2023-11-02T17:09:00Z", "", "v567", 0, 55)
    }
  }
}
