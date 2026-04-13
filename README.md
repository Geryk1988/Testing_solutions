# idq-gsnowintg — Kafka Consumer Microservice

Rebuilt from the original `EventsConsumerwithSASLSSL` core Java application
into a Spring Boot 2.7.18 microservice, using the exact pom.xml, versioning,
and project identity from the original source.

---

## Project structure

```
idq-gsnowintg/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/ubs/dqs/idq_gsnowintg/
    │   │   ├── IdqGsnowintgApplication.java
    │   │   ├── config/
    │   │   │   ├── ConsumerProperties.java       ← reads all props from ReadPropFileService
    │   │   │   └── KafkaConsumerConfig.java      ← SASL/SSL Kafka beans
    │   │   ├── model/
    │   │   │   ├── IncidentRecord.java           ← 10-field domain object + toHashRow()
    │   │   │   └── ConsumerRunStats.java         ← run statistics snapshot
    │   │   ├── service/
    │   │   │   ├── ReadPropFileService.java      ← UNCHANGED from original
    │   │   │   ├── KafkaConsumerService.java     ← @KafkaListener (replaces poll loop)
    │   │   │   ├── MessageProcessorService.java  ← parse + filter logic (sections 1-10)
    │   │   │   ├── FileOutputService.java        ← writes hash rows + JSON dump files
    │   │   │   └── ConsumerStatsService.java     ← thread-safe counters + summary log
    │   │   ├── controller/
    │   │   │   └── ConsumerStatusController.java ← REST: /api/consumer/status & /health
    │   │   └── util/
    │   │       ├── JsonParserUtil.java
    │   │       └── DateTimeFormatterUtil.java
    │   └── resources/
    │       └── application.properties            ← port 8079 + runprops.path
    └── test/
        └── java/com/ubs/dqs/idq_gsnowintg/
            ├── ReadPropFileServiceTest.java
            ├── JsonParserUtilTest.java
            └── DateTimeFormatterUtilTest.java
```

---

## Configuration flow

```
application.properties
  └── runprops.path = P:\Documents\SNOW2026\...\GSNOW_RunProperties_CFLNT.properties
        │
        ▼
  ReadPropFileService.load()     ← @PostConstruct, reads the .properties file line by line
        │
        ▼
  ConsumerProperties.init()      ← @PostConstruct, maps keys into typed fields
        │
        ▼
  KafkaConsumerConfig            ← builds Kafka consumer factory with SASL/SSL settings
  KafkaConsumerService           ← @KafkaListener, processes each record
```

---

## Required keys in GSNOW_RunProperties_CFLNT.properties

```properties
# Kafka connection
CONSUMER_BROKERS=your-broker:9093
CONSUMER_GROUPID=your-group-id
CONSUMER_TOPIC=your-topic
CONSUMER_API_KEY=your-api-key
CONSUMER_API_SECRET=your-api-secret

# TLS
TRUST_STORE=/path/to/truststore.jks

# Schema Registry (optional)
CONSUMER_SR_URL=https://schema-registry.example.com
CONSUMER_SR_API_KEY=sr-key
CONSUMER_SR_API_SECRET=sr-secret

# Detection filters
DETECT_STRING=actimize
DETECT_SWC=SWC1,SWC2,SWC3

# Output files
CONSUMER_OUTPUT_FILE=/output/consumer_output.txt
GSNOW_OUTPUT_FILE=/output/gsnow_output_

# Run duration  (unit: s=seconds, m=minutes, h=hours)
DURATION=60
DURATION_UNIT=m

# Poll tuning (optional - defaults shown)
POLL_DURATION_SECONDS=1
MAX_POLL_RECORDS=100
SESSION_TIMEOUT_MS=30000
HEARTBEAT_INTERVAL_MS=10000
MAX_POLL_INTERVAL_MS=600000
```

---

## Building and running

```bash
# Build
mvn clean package -DskipTests

# Run (uses runprops.path from application.properties)
java -jar target/idq-gsnowintg-0.0.1-SNAPSHOT.jar

# Override the properties file path at runtime
java -jar target/idq-gsnowintg-0.0.1-SNAPSHOT.jar \
     --runprops.path=C:/your/path/GSNOW_RunProperties_CFLNT.properties

# Run tests
mvn test
```

---

## REST endpoints  (port 8079)

```
GET /api/consumer/status  →  ConsumerRunStats JSON (counters, duration, file info)
GET /api/consumer/health  →  {"status":"RUNNING"|"STOPPED", "runId":"...", "started":"..."}
                             200 while running, 503 after stopped
```

---

## Versioning — matches original pom.xml exactly

| Dependency | Version |
|---|---|
| Spring Boot parent | 2.7.18 |
| Java source/target | 1.8 |
| Lombok | 1.18.30 (provided) |
| spring-context | 5.3.31 |
| jackson-databind | managed by Spring Boot 2.7.18 |
| kafka-clients | managed by Spring Boot 2.7.18 |
| kafka-streams | managed by Spring Boot 2.7.18 |
| spring-kafka | managed by Spring Boot 2.7.18 |

---

## What changed vs the original

| Original | Spring Boot version |
|---|---|
| Manual `Properties` + `FileInputStream` | `ReadPropFileService` (unchanged) wired as a Spring bean |
| `KafkaConsumer` + `while(!closed)` poll loop | `@KafkaListener` — Spring manages threading and offset commits |
| All logic in one 600-line class | Separated into Service / Config / Model / Util / Controller layers |
| `System.exit(-1)` on error | Spring exception handling |
| No HTTP layer | REST endpoints on port 8079 |
| Static helper methods | Spring `@Component` beans — injectable and unit-testable |
| No tests | JUnit 5 unit tests for ReadPropFileService, JsonParserUtil, DateTimeFormatterUtil |
