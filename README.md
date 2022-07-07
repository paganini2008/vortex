# Vortex Series

**Vortex** is a lightweight distributed streaming computing framework. It is based on memory computing, which is suitable for high availability, high concurrency and real-time computing business scenarios.

**Vortex** is developed based on <code>SpringBoot</code> framework. It relies on [tridenter](https://github.com/paganini2008/tridenter-spring-boot-starter.git) component to build cluster. Vortex application expose extra independent TCP server port to accept external connections. Vortex application cluster member discover for each other and establish long-term connections through the net  multicast to achieve  high availability, decentralization and load balancing.

## Components

**The vortex series consists of three parts:**

* vortex-common
  client side, vortex common classes, it makes a  Java application as vortex client.
  
* vortex-spring-boot-starter
  server side, the core class of the vortex framework. it makes spring application  as vortex  cluster member.
  
* vortex-metrics-api
  a time series computing  tool library, it is used to do relevant  programming on  time series computing based on memory

* vortex-metrics
  a web application based on time series computing tool. It is easy to be scalable and have high performance in realtime situation

## Compatibility

1. Jdk8 (or later)
2. <code>SpringBoot</code> Framework 2.2.x (or later)
3. <code>Redis</code> 3.x (or later)
4. <code>Netty</code> 4.x (or later)

## Install

**Server Side**
```xml
<dependency>
	<groupId>com.github.paganini2008.atlantis</groupId>
	<artifactId>vortex-spring-boot-starter</artifactId>
	<version>1.0-RC1</version>
</dependency>
```
**Client Side**
```xml
<dependency>
   <groupId>com.github.paganini2008.atlantis</groupId>
   <artifactId>vortex-common</artifactId>
   <version>1.0-RC1</version>
</dependency>
```

At present, following open source projects based on vortex framework: 
1. [Jellyfish](https://github.com/paganini2008/jellyfish.git)
2. [Greenfinger](https://github.com/paganini2008/greenfinger.git)
3. Vortex metrics

## Quick Start

#### How to use the vortex API in your application?

Server Side: 

Implement <code>Handler</code> interface

``` java

@Slf4j
public class TestHandler implements Handler{

    @Override
    public void onData(Tuple tuple) {
        log.info(tuple.toString());
    }

}

```

Client Side:
``` java
String brokerUrl = "http://localhost:10010"; // Expose the location of Server Side
TransportClient transportClient = new HttpTransportClient(brokerUrl);
Tuple tuple = Tuple.newOne();
tuple.setField("clusterName", clusterName);
tuple.setField("applicationName", applicationName);
tuple.setField("host", host);
tuple.setField("message", "Hello World!");
transportClient.write(tuple);

```

####   How to retain historical metrics data ?

Default setting shows statistical time window is 1 minute and rollingly retain recent 60 records, discarding historical records once exceeding the size. If you want to save historical metrics data, You need to:

* Implementing interface <code>io.atlantisframework.vortex.metric.api.MetricEvictionHandler</code>  to customize your persistence policy
* Implementing interface <code>io.atlantisframework.jellyfish.http.MetricSequencerFactory</code> or expanding class <code>io.atlantisframework.jellyfish.http.DefaultMetricSequencerFactory</code> and redefine a <code>GenericUserMetricSequencer</code>






