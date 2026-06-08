# Invoice Processing Service

This project is a Spring Boot skeleton implementing a typical invoice processing workflow using Kafka for event streaming:

## Flow

- Upload invoice PDF via REST endpoint
- Save file to S3
- Publish Kafka event with S3 location
- Kafka consumer listens for events (event-driven, no polling)
- Download PDF from S3 and extract invoice details (PDFBox)
- Store extracted data in DB (JPA / H2 for demo)

## Quickstart

### Prerequisites
- Java 24
- Kafka running locally (or configure `spring.kafka.bootstrap-servers` in properties)
  - For Mac with Homebrew: `brew install kafka`
  - Start Kafka: `zk-server-start.sh /usr/local/etc/kafka/zookeeper.properties` (in one terminal)
  - Then: `kafka-server-start.sh /usr/local/etc/kafka/server.properties` (in another)

### 1. Build

```bash
mvn -U -DskipTests package
```

### 2. Run (ensure Kafka is running)

```bash
mvn spring-boot:run
```

### 3. Upload an invoice (example)

```bash
curl -v -F file=@/path/to/invoice.pdf http://localhost:8080/invoices/upload
```

The application will:
1. Upload the PDF to S3
2. Publish a message to the `invoice-uploads` Kafka topic
3. The Kafka consumer (listening via `@KafkaListener`) immediately processes the message
4. Extract invoice data and save to DB

## Configuration

- `src/main/resources/application.properties` contains defaults. Key settings:
  - `spring.kafka.bootstrap-servers` — Kafka broker address (default: `localhost:9092`)
  - `app.kafka.topic` — Kafka topic name (default: `invoice-uploads`)
  - `app.kafka.groupId` — Consumer group ID (default: `invoice-consumer-group`)
  - `app.s3.bucket` — S3 bucket name (default: `invoice-uploads`)

### Running with Docker Compose (Optional)

To run Kafka + Zookeeper in containers:

```bash
docker-compose up -d
```

(You would need to create a `docker-compose.yml` file; see notes below.)

## Architecture & Benefits of Kafka

- **Event-Driven:** Consumer reacts immediately to published events (no polling overhead)
- **Scalability:** Can add multiple consumer instances with the same group ID for parallel processing
- **Durability:** Kafka retains messages for replay and recovery
- **Comparison to SNS/SQS:**
  - SNS (pub-sub) + SQS (queue) requires AWS setup and managed services
  - Kafka gives you a self-hosted or cloud-hosted message broker with richer features
  - Both are valid; Kafka is lighter for proof-of-concept / single-service scenarios

## Project Structure

- `InvoiceProcessingServiceApplication` — Main Spring Boot app (no scheduling, event-driven)
- `InvoiceController` — REST `/invoices/upload` endpoint
- `S3Service` — Upload/download PDFs to/from S3
- `KafkaProducerService` — Publishes events to Kafka topic
- `KafkaConsumer` — Listens (`@KafkaListener`) for events and processes them
- `InvoiceExtractor` — PDF text parsing and field extraction
- `Invoice` — JPA entity for database
- `InvoiceRepository` — Spring Data JPA for CRUD
- `AwsConfig` — AWS S3 client configuration

## Notes

- This is a demo skeleton. For production, add:
  - Error handling & dead-letter topics (Kafka has DLT support)
  - Security (TLS for Kafka, API auth for REST)
  - Distributed tracing (Spring Cloud Sleuth)
  - Metrics & monitoring (Micrometer/Prometheus)
  - Robust PDF parsing (OCR, ML models)
  - Idempotency for message processing
  - Consumer offset management configuration

## Testing Locally

### Create the Kafka topic (if not auto-created)

```bash
kafka-topics.sh --create --topic invoice-uploads --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### Produce test message manually (optional)

```bash
echo '{"bucket":"invoice-uploads","key":"test-key.pdf"}' | kafka-console-producer.sh --topic invoice-uploads --bootstrap-server localhost:9092
```

### Consumer Verify

```bash
kafka-console-consumer.sh --topic invoice-uploads --bootstrap-server localhost:9092 --from-beginning
```


