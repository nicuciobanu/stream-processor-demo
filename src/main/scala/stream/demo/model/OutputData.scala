package main.scala.stream.demo.model

import spray.json.{DefaultJsonProtocol, JsonFormat}

case class OutputData(timestamp: String, socketId: String, vehicleId: String, powerInWatts: Int, stateOfChargeInPercent: Int)

object OutputData extends DefaultJsonProtocol {
  implicit val outputDataFormat: JsonFormat[OutputData] = jsonFormat5(OutputData.apply)
}
