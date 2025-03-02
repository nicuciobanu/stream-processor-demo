## Electrical Charging Station App

Write a program that consumes two source streams, charging data (timestamp, socketId, vehicleId, and powerInWatts) and battery data (timestamp, vehicleId, and stateOfChargeInPercent).
Then, they are combined and produce output data (timestamp, socketId, vehicleId, powerInWatts, and stateOfChargeInPercent).

Only emit output when charging data arrives. Output contains the last received timestamp for the same `vehicleId`.

#### Technologies
Scala, Akka-Streams, Redis, and Docker.

### Setting up Redis
```
docker run -d --name redis-stack-server -p 6379:6379 redis/redis-stack-server:latest
```

### Explanation
```
https://medium.com/@ciobanunicu.boris/akka-streams-processing-with-redis-bb210598fa64
```
