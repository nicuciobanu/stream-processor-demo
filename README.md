## Charging Battery App

Write a program that consumes 2 source streams, charging data (timestamp, socketId, vehicleId and powerInWatts), and battery data (timestamp, vehicleId and stateOfChargeInPercent),
teh combines them and produce output data (timestamp, socketId, vehicleId, powerInWatts and stateOfChargeInPercent).

Only emit output when charging data arrives, output contains the last received stateOfChargeInPercent for teh same vehicleId.

#### Technologies
Scala, Akka-Streams, Redis, Docker.

### Setting up Redis
```
docker run -d --name redis-stack-server -p 6379:6379 redis/redis-stack-server:latest
```
