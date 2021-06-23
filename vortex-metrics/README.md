# Vortex Metrics
## Distributed timing sequential computing framework
### Description
Vortex Metrics is a lightweight distributed timing sequential computing framework written in Java.

Vortex Metrics is a concrete implementation of distributed streaming computing framework vortex about timing sequential computing scenarios

Compared with <code>OpenTSDB</code>, another timing sequential database, Vortex Metrics is more inclined to real-time calculation, and keeps the delay in seconds as far as possible. However, because it is memory calculation, the calculation results are stored in memory, and the limited storage space makes it not suitable for business scenarios where a large number of indicators are stored. But at the same time, when the window data of an indicator needs to be retained for a long time span, vortex metrics provides a custom interface, which can be saved to external storage, such as database or cache, but it needs to do some extra development work. Also, Vortex Metrics provides a web query interface, which is in continuous improvement.

In fact,  Vortex Metrics is very simple, because it only does one thing, that is to calculate and output the statistical data in the time window in real-time according to the continuous reported data of the client.

### Feature
1. High Availability
2. High Throughput
3. Data eventual consistency
4. Real-time Statistics
5. Memory Storage

### Install
``` xml
<dependency>
	<groupId>com.github.paganini2008.atlantis</groupId>
    <artifactId>vortex-spring-boot-starter</artifactId>
	<version>1.0-RC1</version>
</dependency>
```

### Config
``` properties
# Cluster Configuration
spring.application.name=vortex-metrics
spring.application.cluster.name=vortex-metrics-cluster

# Vortex Configuration
atlantis.framework.vortex.bufferzone.collectionName=metric
atlantis.framework.vortex.bufferzone.pullSize=1000
atlantis.framework.vortex.processor.threads=200
```

### Run

1. http://localhost:6150/metrics/sequence/<code>{dataType}</code>

**Eg:** 
``` Shell
curl -X POST -H "Content-Type: application/json" -d '{name: "car", "metric": "speed", "value": 120, "timestamp": 1613200609281}' 'http://localhost:6150/metrics/sequence/bigint'
```
2. http://localhost:6150/metrics/sequence/<code>{dataType}/{name}/{metric}</code>

**Eg:**
``` Shell
curl -X GET 'http://localhost:6150/metrics/sequence/bigint/car/speed'
```
   Options of Parameter '<code>dataType</code>' may set <code>bigint</code>, numeric or bool




