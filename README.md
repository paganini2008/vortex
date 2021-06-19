# Vortex Series
### Description
**Vortex** is a lightweight distributed streaming computing framework. Vortex, the word means that the datas stream continuously flows into the vortex and then are output smoothly.

**Vortex** is a streaming framework based on memory computing, which is suitable for high availability, high concurrency and real-time computing business scenarios.

**Vortex** is developed based on <code>SpringBoot</code> framework. It relies on [tridenter](https://github.com/paganini2008/tridenter-spring-boot-starter.git), a distributed collaboration framework for microservices, to realize cluster characteristics. Vortex microservice has an independent TCP server embedded in it (netty4 is used by default). Applications in vortex microservice cluster discover each other and establish long-term connections through the multicast function of tridenter to realize high availability, decentralization and load balancing, So that the whole spring application cluster has the ability of real-time computing.

### Architecture
**The vortex series consists of three parts:**
1. vortex-common
   **the agent side** jar package of vortex framework
2. vortex-spring-boot-starter
   the core jar package of the vortex framework is added to the springboot application to make it the **vortex server side**
3. vortex-metrics
   a distributed timing computing framework based on vortex is an important independent subproject of vortex

### Compatibility
1. jdk8 (or later)
2. <code>SpringBoot</code> Framework 2.2.x (or later)
3. <code>Redis</code> 4.x (or later)
4. <code>Netty</code> 4.x (or later)

### Install
**Server Side**
```xml
<dependency>
	<groupId>indi.atlantis.framework</groupId>
	<artifactId>vortex-spring-boot-starter</artifactId>
	<version>1.0-RC1</version>
</dependency>
```
**Agent Side**
```xml
<dependency>
   <groupId>indi.atlantis.framework</groupId>
   <artifactId>vortex-common</artifactId>
   <version>1.0-RC1</version>
</dependency>
```

At present, there are two open source projects based on vortex framework: 
1. [Jellyfish](https://github.com/paganini2008/jellyfish.git)
   , the distributed microservice monitoring system jellyfish
2. Vortex metrics, a distributed time series computing framework

### How to use the vortex API in your application?
The server side of vortex receives data, and the agent side of vortex sends data. Vortex provides HTTP and TCP protocols to receive and send data.
1. The server side of vortex needs to realize the <code>Handler</code> interface, for example:
``` java
@Slf4j
public class TestHandler implements Handler{

    @Override
    public void onData(Tuple tuple) {
        log.info(tuple.toString());
    }

}

```
2. Agent side sends data through <code>TransportClient</code> object
The following takes the log collection module in jellyfish as an example, with reference to the source code:
** Jelly Server Side:**
``` java
public class Slf4jHandler implements Handler {

    private static final String TOPIC_NAME = "slf4j";

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private LogEntryService logEntryService;

    @Value("${atlantis.framework.jellyfish.handler.interferedCharacter:}")
    private String interferedCharacterRegex;

    @Override
    public void onData(Tuple tuple) {
        LogEntry logEntry = new LogEntry();
        logEntry.setId(idGenerator.generateId());
        logEntry.setClusterName(tuple.getField("clusterName", String.class));
        logEntry.setApplicationName(tuple.getField("applicationName", String.class));
        logEntry.setHost(tuple.getField("host", String.class));
        logEntry.setIdentifier(tuple.getField("identifier", String.class));
        logEntry.setLoggerName(tuple.getField("loggerName", String.class));
        logEntry.setMessage(tuple.getField("message", String.class));
        logEntry.setLevel(tuple.getField("level", String.class));
        logEntry.setReason(tuple.getField("reason", String.class));
        logEntry.setMarker(tuple.getField("marker", String.class));
        logEntry.setCreateTime(tuple.getField("timestamp", Long.class));
        if (StringUtils.isNotBlank(interferedCharacterRegex)) {
            logEntry.setMessage(logEntry.getMessage().replaceAll(interferedCharacterRegex, ""));
            logEntry.setReason(logEntry.getReason().replaceAll(interferedCharacterRegex, ""));
        }
        logEntryService.bulkSaveLogEntries(logEntry);
    }

    @Override
    public String getTopic() {
        return TOPIC_NAME;
    }

}
```

For the **vortex agent side**, you need to implement an agent side by yourself to continuously send datas to the **vortex server side**. Please refer to the <code>TransportClientAppenderBase.java</code> source code comes from <code>jellyfish-slf4j</code>

``` java
@Override
    protected void append(ILoggingEvent eventObject) {
        if (transportClient == null) {
            return;
        }
        Tuple tuple = Tuple.newOne(GLOBAL_TOPIC_NAME);
        tuple.setField("clusterName", clusterName);
        tuple.setField("applicationName", applicationName);
        tuple.setField("host", host);
        tuple.setField("identifier", identifier);
        tuple.setField("loggerName", eventObject.getLoggerName());
        String msg = eventObject.getFormattedMessage();
        tuple.setField("message", msg);
        tuple.setField("level", eventObject.getLevel().toString());
        String reason = ThrowableProxyUtil.asString(eventObject.getThrowableProxy());
        tuple.setField("reason", reason);
        tuple.setField("marker", eventObject.getMarker() != null ? eventObject.getMarker().getName() : "");
        tuple.setField("timestamp", eventObject.getTimeStamp());
        Map<String, String> mdc = eventObject.getMDCPropertyMap();
        if (MapUtils.isNotEmpty(mdc)) {
            tuple.append(mdc);
        }
        transportClient.write(tuple);
    }
```
To illustrate, the interactive data between **the server side** and **the agent side** of vortex can be Map</code>, <code>Tuple</code> object or JSON string, but they are eventually wrapped as tuple object.


