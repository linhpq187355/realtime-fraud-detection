---
trigger: always_on
---

# Real-Time Fraud Detection Project Rules

## Project Context

This is a Java 21 + Spring Boot multi-module Maven project for a real-time fraud detection system.

The project uses microservices architecture with a monorepo.

Modules:

- common
- tx-simulator
- feature-service
- decision-service
- dashboard

Supporting projects:

- model-training
- load-test

Infrastructure:

- Apache Kafka using KRaft
- Redis
- Docker Compose

## Architecture

### tx-simulator

Responsibilities:

- Provide the Simulator web UI.
- Create manual transactions.
- Generate automatic transactions.
- Run preset fraud scenarios.
- Publish transaction events to Kafka.
- Display transaction decisions and live feed.

Port:
- 8080

### feature-service

Responsibilities:

- Consume transaction events from Kafka.
- Process transaction streams.
- Calculate transaction features.
- Maintain rolling/windowed statistics.
- Store current feature state in Redis.

It must NOT make the final fraud decision.

### decision-service

Responsibilities:

- Expose POST /check-transaction.
- Read current features from Redis.
- Evaluate rules from rules.yaml.
- Execute ONNX model when no blocking/review rule matches.
- Produce CHO_QUA / XEM_XET / CHAN.
- Return risk score, triggered rule, features and latency.

Port:
- 8082

### dashboard

Responsibilities:

- Display real-time transaction statistics.
- Display CHAN/XEM_XET/CHO_QUA statistics.
- Display throughput and latency metrics.
- Do not contain fraud decision logic.

Port:
- 8081

### common

Only contain genuinely shared contracts and neutral utilities.

Allowed examples:

- DTOs
- enums
- constants
- event schemas

Do NOT put business services or fraud decision logic into common.

## Important Design Decisions

Use:

- Java 21
- Spring Boot
- Maven
- Apache Kafka
- Kafka Streams
- Redis
- Docker Compose
- Vanilla HTML/CSS/JavaScript for simulator and dashboard unless there is a strong reason otherwise.

Do NOT introduce unnecessary technologies such as:

- Spring Cloud Stream
- Kubernetes
- API Gateway
- Service Discovery
- Config Server
- ZooKeeper
- Kafka Connect
- unnecessary third-party rule engines

unless explicitly requested.

Kafka must use KRaft rather than ZooKeeper.

## Coding Rules

1. Inspect the existing project before editing files.
2. Never overwrite or recreate the whole project unnecessarily.
3. Preserve existing working code.
4. Make the smallest reasonable change for each task.
5. Follow the existing package naming conventions.
6. Prefer simple, readable Spring Boot code.
7. Do not invent APIs, classes or infrastructure that are not needed.
8. Do not add dependencies unless they are required.
9. Do not add authentication/security unless explicitly requested.
10. Do not add database infrastructure unless explicitly requested.
11. Do not hard-code secrets.
12. Use configuration properties/environment variables for configurable infrastructure settings.

## Development Process

For every task:

1. Inspect the relevant files.
2. Explain the implementation plan briefly.
3. Implement the requested change.
4. Run the relevant tests/build.
5. Fix compilation/test failures.
6. Summarize changed files and verification results.

Do not implement future milestones unless explicitly requested.

## Git Safety

Before making large changes:

- inspect git status
- do not reset or discard user changes
- do not force-push
- do not rewrite git history

Keep commits logically separated by feature.

## Current Development Philosophy

This is a learning project.

Prefer understandable architecture over unnecessary production complexity.

Do not optimize prematurely.

Implement one milestone at a time and keep the system runnable after each milestone.