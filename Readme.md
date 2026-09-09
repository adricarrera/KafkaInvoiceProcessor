# Marosa Invoice Processor

A Spring Boot microservice that processes electronic invoices through Apache Kafka.

The service consumes invoice JSON messages from Kafka, validates them, transforms valid invoices into a simplified XML representation and publishes them to a second Kafka topic.

Invalid invoices are persisted in an H2 database for traceability.

---

## Architecture

The application separates Kafka infrastructure from business logic:

* `InvoiceConsumer` receives invoice events from Kafka.
* `InvoiceProcessingService` orchestrates the processing flow.
* `InvoiceValidationService` validates invoice data.
* `InvoiceTransformationService` converts valid invoices to XML.
* `InvoiceProducer` publishes validated invoices to Kafka.
* `InvalidInvoiceRepository` persists invalid invoices in H2.
* `KafkaConfig` configures JSON deserialization and Kafka error handling.


### Project structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/adricarrera/marosatest/
│   │       ├── config/
│   │       ├── consumer/
│   │       ├── domain/
│   │       ├── exception/
│   │       ├── producer/
│   │       ├── repository/
│   │       ├── service/
│   │       └── validation/
│   │
│   └── resources/
│       └── application.yaml
│
├── test/
│   └── java/
│       └── com/adricarrera/marosatest/
│
├── samples/
│   ├── valid-invoice.json
│   └── invalid-invoice.json
│
├── docker-compose.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

## Technology Stack

* Java 17
* Spring Boot 4.1.1
* Spring Kafka
* Apache Kafka
* Spring Data JPA
* H2
* Maven
* Docker Compose

---

## Spring Boot version

The technical test specifies Spring Boot 3.x.

I am aware of this requirement. The implementation uses Spring Boot 4.1.1 because the latest Spring Boot 3.x line, 3.5, reached the end of Open Source Support on June 30, 2026, with Spring Boot 3.5.16 being the final OSS release of the 3.5.x generation.

This version choice is therefore intentional and documented.

## Requirements

To run the application locally you need:

* Java 17 or later
* Docker
* Docker Compose

Maven does not need to be installed globally because the project includes the Maven Wrapper.

---

## Running the application

### 1. Start Kafka

From the project root, run:

```bash
docker compose up -d
```

The Docker Compose configuration starts Kafka and automatically creates the required topics:

* `incoming-invoices`
* `validated-invoices`

You can verify that the containers are running with:

```bash
docker compose ps
```

You can also verify the topics with:

```bash
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

Expected output:

```text
incoming-invoices
validated-invoices
```

### 2. Start the Spring Boot application

Run app:

```bash
./mvnw spring-boot:run
```


The application will connect to Kafka at:

```text
localhost:9092
```

---

## Testing the Kafka flow

Once Kafka and the Spring Boot application are running, the invoice flow can be tested manually using Kafka's console producer and consumer.

### 1. Start a Kafka consumer

Open a new terminal and listen to the `validated-invoices` topic:

```bash
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic validated-invoices
```

This terminal displays the XML messages produced by the application.

### 2. Start the Kafka producer

Open another terminal and start a producer for the `incoming-invoices` topic:

```bash
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic incoming-invoices
```

This terminal is used to send invoice JSON messages to the application.

### 3. Send a valid invoice

A sample valid invoice is provided in:

```text
samples/valid-invoice.json
```

The file contains:

```json
{"invoiceId":"INV-20240101-001","customerId":"DE987654321","country":"DE","issueDate":"2024-01-01","lines":[{"description":"Laptop","amount":1000,"vatRate":0.19}]}
```

Paste the JSON into the Kafka producer terminal and press Enter.

The application validates the invoice, transforms it into XML and publishes the result to `validated-invoices`.

The consumer terminal should display:

```xml
<Invoice>
    <Id>INV-20240101-001</Id>
    <Country>DE</Country>
    <Total>1190.00</Total>
</Invoice>
```

### 4. Send an invalid invoice

A sample invalid invoice is provided in:

```text
samples/invalid-invoice.json
```

The file contains:

```json
{"invoiceId":"INV-INVALID-002","customerId":"DE987654321","country":"DE","issueDate":"2024-01-01","lines":[{"description":"Laptop","amount":0,"vatRate":0.19}]}
```

Paste the JSON into the Kafka producer terminal and press Enter.

The invoice fails business validation and is persisted in H2.

It is not published to:

```text
validated-invoices
```

## H2 Database

Invalid invoices are persisted in an in-memory H2 database for traceability.

The H2 console is enabled and can be accessed at:

http://localhost:8080/h2-console

Use the following connection settings:

| Property | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:invoices` |
| User Name | `sa` |
| Password | *(empty)* |

After connecting, the invalid invoices can be inspected in the `INVALID_INVOICE` table.


## Invalid Invoices

If an invoice fails business validation, it is not published to `validated-invoices`.

Instead, the invoice ID and validation reason are persisted in H2.

The processing flow is:

```text
incoming-invoices
        │
        ▼
    Validation
        │
        ▼
InvalidInvoiceException
        │
        ▼
       H2
```

Technical errors are deliberately not converted into business validation errors. They are propagated so that Kafka's error handling mechanism can deal with them appropriately.

For example:

* Invalid line amount → business validation error → persisted in H2.
* Invalid customer ID → business validation error → persisted in H2.
* Malformed JSON → deserialization error → handled by Kafka error handling.

---

## Kafka Error Handling

The Kafka consumer uses:

* `ErrorHandlingDeserializer`
* `DefaultErrorHandler`
* `FixedBackOff`

`ErrorHandlingDeserializer` allows deserialization failures, such as malformed JSON, to be handled by Spring Kafka's error handling mechanism.

Processing failures handled by `DefaultErrorHandler` are retried twice with a one-second delay between attempts.

The configured behaviour is:

```text
Processing failure
        │
        ▼
   wait 1 second
        │
        ▼
      retry
        │
        ▼
   wait 1 second
        │
        ▼
      retry
        │
        ▼
   retries exhausted
```

Business validation errors are handled separately by the application.

For example, an invoice with an invalid amount is persisted as an invalid invoice instead of being retried, because the same input would produce the same validation result.

The application follows an at-least-once processing model. Therefore, duplicate processing can occur in failure/restart scenarios.

The invoice ID is used as the Kafka message key when publishing validated invoices. This makes it easier for downstream systems to associate events with the same invoice and implement idempotent processing if required.

For a production environment, unrecoverable technical failures could additionally be routed to a Dead Letter Topic (DLT).

---

## Monetary Calculations

`BigDecimal` is used for invoice amounts and VAT calculations instead of floating-point types.

This avoids precision problems associated with `double` or `float` when processing financial values.

The calculation for each line is:

```text
line total = amount + (amount × VAT rate)
```

The invoice total is the sum of all line totals.

For example:

```text
amount = 1000.00
VAT    = 19%

1000.00 × 1.19 = 1190.00
```

---

## Database

H2 is used to persist invalid invoices.

The database is configured as an in-memory database because persistence is an optional requirement of the technical test and no external database is necessary for the exercise.

Invalid invoice records contain:

* Generated database ID
* Invoice ID
* Validation reason

The H2 database is intentionally in-memory, so its contents are lost when the application stops.

---

## Testing

The project contains unit tests covering:

* Valid invoices
* Invalid country codes
* Unsupported countries
* Invalid customer IDs
* Missing customer IDs
* Missing invoice lines
* Empty invoice lines
* Zero line amounts
* Missing line amounts
* Negative VAT rates
* Missing VAT rates
* VAT rates greater than 100%
* VAT calculations
* Multiple invoice lines
* Invalid invoice persistence
* Spring application context loading

### Run automated tests

On Linux/macOS:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

The automated tests focus on business logic and application behaviour without requiring a running Kafka broker.

### Manual Kafka test

The Kafka flow can be tested manually using the console producer and consumer described in [Testing the Kafka flow](#testing-the-kafka-flow).

This verifies the complete:

```text
Kafka → Consumer → Validation → Transformation → Producer → Kafka
```

flow against a real Kafka broker.

---

## Technical Decisions

### Spring Kafka

Spring Kafka was selected because the required processing model is a straightforward:

```text
consume → validate → transform → produce
```

pipeline.

Using the standard Spring Kafka consumer and producer approach keeps the processing flow explicit and avoids unnecessary abstractions for this use case.

### Kafka Streams

Kafka Streams was not used because the exercise does not require stateful stream processing, joins, windows or aggregations.

For this pipeline, a standard Kafka consumer and producer provide a simpler implementation.

### H2

H2 was selected because invalid invoice persistence is an optional requirement and an in-memory database is sufficient for the scope of the exercise.

### Simplified XML

The technical test allows a simplified UBL/Facturae-like representation, so the implementation focuses on the required invoice fields instead of implementing a complete electronic invoicing standard.

### At-least-once delivery

The consumer uses Kafka's standard at-least-once processing model.

The application therefore assumes that duplicate processing is possible.

In a production environment, idempotency and/or Kafka transactions could be introduced depending on the required delivery guarantees.

### Separation of responsibilities

Validation, transformation and Kafka infrastructure are separated into different components.

This keeps the business logic independently testable and makes the service easier to extend.

### Country-specific validation rules

Customer ID validation rules are isolated in `CustomerIdValidationRules`.

The regular expression patterns used for country-specific customer ID validation are based on the following open-source repository:

https://github.com/mnestorov/regex-patterns

The patterns were adapted to the scope and requirements of this technical test.

This prevents country-specific regular expressions from being coupled to the main invoice validation flow.

Adding a supported country therefore only requires adding its corresponding validation rule without changing the overall processing pipeline.

---

## Production Considerations

The implementation is intentionally focused on the requirements of the technical test.

For a production environment, the following improvements could be considered:

* Integration tests using Testcontainers and a real Kafka broker
* Idempotent invoice processing
* Dead Letter Topics for unrecoverable Kafka messages
* Kafka transactions if exactly-once semantics are required
* Persistent database such as PostgreSQL
* Structured logging
* Metrics and distributed tracing
* Schema management using Avro or JSON Schema
* More complete country-specific tax identifier validation
* XML schema validation
* Complete UBL/Facturae XML generation
* Authentication and authorization for Kafka

---

## Scope

The implementation covers the requested functionality:

1. Consume invoice JSON messages from `incoming-invoices`.
2. Validate country and customer ID.
3. Validate invoice line amounts and VAT rates.
4. Transform valid invoices into XML.
5. Publish XML to `validated-invoices`.
6. Persist invalid invoices in H2.
7. Provide Kafka error handling.
8. Provide automated unit tests.
9. Provide Docker Compose infrastructure for local Kafka execution.
10. Provide sample valid and invalid invoice messages.
11. Document the main technical decisions.
