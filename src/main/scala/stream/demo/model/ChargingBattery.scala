package stream.demo.model

sealed trait ChargingBattery
case class ChargingData(timestamp: String, socketId: String, vehicleId: String, powerInWatts: Int) extends ChargingBattery
case class BatteryData(timestamp: String, vehicleId: String, stateOfChargeInPercent: Int) extends ChargingBattery

