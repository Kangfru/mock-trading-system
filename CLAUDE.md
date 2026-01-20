# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Rules

- 개발 전 사용자의 요구사항을 상세히 파악하고 확인한 후 진행한다
- 쉘 명령어 실행은 반드시 사용자의 승인을 받은 후 수행한다

## Build & Run Commands

```bash
# Build
./gradlew build

# Run application
./gradlew bootRun

# Run all tests
./gradlew test

# Run single test class
./gradlew test --tests "com.kangfru.mocktradingsystem.SomeTestClass"

# Run single test method
./gradlew test --tests "com.kangfru.mocktradingsystem.SomeTestClass.testMethodName"

# Clean build
./gradlew clean build

# Start Kafka infrastructure (Zookeeper, Kafka, Kafka UI)
docker-compose up -d

# Stop Kafka infrastructure
docker-compose down
```

## Tech Stack

- **Language**: Kotlin (Java 21)
- **Framework**: Spring Boot 4.0.1
- **Build**: Gradle Kotlin DSL
- **Messaging**: Apache Kafka (via Docker Compose)
- **Serialization**: Jackson with Kotlin module

## Architecture

Mock trading system built on Spring Boot with Kafka messaging infrastructure.

**Package**: `com.kangfru.mocktradingsystem`

**Infrastructure (Docker Compose)**:
- Zookeeper: Kafka coordination
- Kafka: Port 9092
- Kafka UI: Port 38080 (http://localhost:38080)
