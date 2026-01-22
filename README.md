# Mock Trading System

Kotlin + Spring Boot 기반의 모의 주식 거래 시스템

## Tech Stack

- **Language**: Kotlin (Java 21)
- **Framework**: Spring Boot 4.0.1
- **Build**: Gradle Kotlin DSL
- **Messaging**: Apache Kafka
- **RPC**: gRPC (Kotlin Coroutines)
- **Market Data**: Stooq API (미국 주식 시세)

## Features

### 주문 처리
- 지정가 주문 (LIMIT)
- 시장가 주문 (MARKET) - Stooq API 실시간 시세 연동
- 주문 취소/정정
- 대량 주문 처리

### 호가창 & 체결
- 종목별 호가창 관리
- 가격/시간 우선 원칙 매칭
- 연속 체결 처리
- 상대 주문 상태 자동 업데이트

### 사용자 & 계좌 관리
- 사용자 CRUD
- 계좌 생성/관리
- 입출금 처리
- 주식 보유 현황 관리

### gRPC API
- 주문 조회 (Unary)
- 주문 상태 실시간 스트리밍 (Server Streaming)
- Reflection 지원 (grpcurl 사용 가능)

### 시세 조회
- Stooq API 연동 (API Key 불필요)
- Rate Limiting (1초 간격 throttling)

## API Endpoints

### 주문
```bash
# 시장가 매수 주문
POST /api/orders
{
  "stockCode": "AAPL",
  "orderType": "BUY",
  "quantity": 10,
  "priceType": "MARKET"
}

# 지정가 매도 주문
POST /api/orders
{
  "stockCode": "TSLA",
  "orderType": "SELL",
  "quantity": 5,
  "price": 450.00,
  "priceType": "LIMIT"
}

# 주문 취소
POST /api/orders/cancel
{
  "originalOrderNumber": 123456789
}

# 대량 주문
POST /api/orders/bulk?count=100
```

### 시세 조회
```bash
# 미국 주식 시세
GET /api/stocks/{symbol}/quote
GET /api/stocks/AAPL/quote
GET /api/stocks/TSLA/quote?market=us
```

### 호가창
```bash
# 종목별 호가창 조회
GET /api/orderbook/{stockCode}

# 전체 호가창 조회
GET /api/orderbook
```

### 사용자
```bash
# 사용자 생성
POST /api/users
{
  "username": "홍길동",
  "email": "hong@example.com"
}

# 사용자 조회
GET /api/users/{seq}
GET /api/users/email/{email}
GET /api/users

# 사용자 수정
PUT /api/users/{seq}

# 사용자 삭제
DELETE /api/users/{seq}

# 사용자 계좌 목록
GET /api/users/{seq}/accounts
```

### 계좌
```bash
# 계좌 생성
POST /api/accounts
{
  "userId": 1,
  "initialBalance": 10000000
}

# 계좌 조회
GET /api/accounts/{accountNumber}
GET /api/accounts

# 입금/출금
POST /api/accounts/{accountNumber}/deposit
POST /api/accounts/{accountNumber}/withdraw

# 주식 매수/매도
POST /api/accounts/{accountNumber}/stocks/buy
{
  "stockCode": "AAPL",
  "quantity": 10,
  "price": 150.00
}

POST /api/accounts/{accountNumber}/stocks/sell

# 보유 종목 조회
GET /api/accounts/{accountNumber}/holdings
GET /api/accounts/{accountNumber}/holdings/{stockCode}
```

### gRPC (localhost:9090)
```bash
# 주문 조회
grpcurl -plaintext -d '{"order_number": 123456789}' \
  localhost:9090 order.OrderService/GetOrder

# 주문 상태 스트리밍
grpcurl -plaintext -d '{"order_number": 123456789}' \
  localhost:9090 order.OrderService/WatchOrderStatus

# 서비스 목록 조회
grpcurl -plaintext localhost:9090 list
```

## Quick Start

```bash
# 1. Kafka 인프라 실행
docker-compose up -d

# 2. 애플리케이션 빌드
./gradlew build

# 3. 애플리케이션 실행
./gradlew bootRun

# 4. 테스트
curl http://localhost:8080/api/stocks/AAPL/quote
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"stockCode":"AAPL","orderType":"BUY","quantity":10,"priceType":"MARKET"}'
```

## Project Structure

```
src/main/kotlin/com/kangfru/mocktradingsystem/
├── client/
│   └── StooqClient.kt          # Stooq API 클라이언트 (Rate Limiting)
├── config/
│   ├── KafkaConfig.kt          # Kafka 설정
│   └── WebClientConfig.kt      # WebClient 설정
├── consumer/
│   └── OrderConsumer.kt        # 주문 메시지 소비
├── controller/
│   ├── OrderController.kt      # 주문 API
│   ├── OrderBookController.kt  # 호가창 API
│   ├── StockPriceController.kt # 시세 API
│   ├── UserController.kt       # 사용자 API
│   └── AccountController.kt    # 계좌 API
├── domain/
│   ├── Order.kt                # 주문 도메인
│   ├── OrderBook.kt            # 호가창 도메인
│   ├── Execution.kt            # 체결 도메인
│   ├── StockQuote.kt           # 시세 도메인
│   ├── User.kt                 # 사용자 도메인
│   ├── Account.kt              # 계좌 도메인
│   └── StockHolding.kt         # 보유 종목 도메인
├── grpc/
│   ├── GrpcServerStarter.kt    # gRPC 서버 시작
│   ├── GrpcOrderService.kt     # gRPC 주문 서비스
│   └── OrderMapper.kt          # Domain ↔ Proto 변환
├── producer/
│   └── OrderProducer.kt        # 주문 메시지 발행
├── repository/
│   ├── UserRepository.kt       # 사용자 저장소
│   └── AccountRepository.kt    # 계좌 저장소
├── service/
│   ├── OrderBookService.kt     # 호가창/매칭 서비스
│   ├── ExecutionService.kt     # 체결 서비스
│   ├── UserService.kt          # 사용자 서비스
│   └── AccountService.kt       # 계좌 서비스
└── util/
    └── SnowflakeIdGenerator.kt # 분산 ID 생성

src/main/proto/
└── order.proto                 # gRPC 서비스 정의
```

## Infrastructure

```yaml
# docker-compose.yml
- Zookeeper: Kafka 코디네이션
- Kafka: localhost:9092
- Kafka UI: http://localhost:38080
```

## Ports

| Service | Port  |
|---------|-------|
| REST API | 44732 |
| gRPC | 9090  |
| Kafka | 9092  |
| Kafka UI | 38080 |

## Architecture

```
┌─────────────┐     ┌─────────────┐
│  REST API   │     │  gRPC API   │
│   :8080     │     │   :9090     │
└──────┬──────┘     └──────┬──────┘
       │                   │
       ▼                   ▼
┌─────────────────────────────────┐
│        OrderProducer            │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│            Kafka                │
│           :9092                 │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│        OrderConsumer            │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│       ExecutionService          │
│    (주문 저장 & 상태 관리)        │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│       OrderBookService          │
│     (호가창 & 매칭 엔진)          │
└─────────────────────────────────┘
```

## Notes

- **Eventual Consistency**: REST API로 주문 제출 후 gRPC로 조회 시, Kafka 처리 지연으로 인해 즉시 조회되지 않을 수 있음
- **Snowflake ID**: 분산 환경에서 고유한 주문번호 생성
- **Coroutines**: 사용자/계좌 서비스는 Kotlin Coroutines 기반 비동기 처리
