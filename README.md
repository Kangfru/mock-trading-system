# Mock Trading System

Kotlin + Spring Boot 기반의 모의 주식 거래 시스템

## Tech Stack

- **Language**: Kotlin (Java 21)
- **Framework**: Spring Boot 4.0.1
- **Build**: Gradle Kotlin DSL
- **Messaging**: Apache Kafka
- **RPC**: gRPC (Kotlin Coroutines)
- **WebSocket**: Spring WebFlux WebSocket (Reactive)
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

### gRPC API (4가지 통신 패턴)
- **Unary**: 주문 조회 (`GetOrder`)
- **Server Streaming**: 주문 상태 실시간 모니터링 (`WatchOrderStatus`)
- **Client Streaming**: 대량 주문 일괄 처리 (`CreateBulkOrder`)
- **Bidirectional Streaming**: 실시간 주문 관리 세션 (`ManageOrders`)
- Reflection 지원 (grpcurl 사용 가능)

### WebSocket API
- **실시간 양방향 통신**: 단일 연결로 주문/조회/취소 처리
- **메시지 타입**: CREATE_ORDER, QUERY_ORDER, CANCEL_ORDER, GET_PORTFOLIO
- **Rate Limiting**: Fixed Window 방식 (세션당 초당 5회 제한)
- **세션 관리**: 연결별 독립적인 Rate Limiter 및 세션 추적
- **CORS 지원**: 개발 환경 전체 허용

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

#### Unary - 단일 주문 조회
```bash
grpcurl -plaintext -d '{"order_number": 123456789}' \
  localhost:9090 order.OrderService/GetOrder
```

#### Server Streaming - 주문 상태 실시간 모니터링
```bash
# 주문이 FILLED 또는 CANCELLED 될 때까지 상태 업데이트 수신
grpcurl -plaintext -d '{"order_number": 123456789}' \
  localhost:9090 order.OrderService/WatchOrderStatus
```

#### Client Streaming - 대량 주문 일괄 처리
```bash
# 여러 주문을 스트림으로 전송, 마지막에 성공/실패 카운트 수신
# (grpcurl은 client streaming을 직접 지원하지 않음 - 프로그래밍 방식 사용)
```

#### Bidirectional Streaming - 실시간 주문 관리
```bash
# 주문 전송과 동시에 처리 결과를 실시간으로 수신
# (grpcurl은 bidirectional streaming을 직접 지원하지 않음 - 프로그래밍 방식 사용)
```

#### 서비스 조회
```bash
grpcurl -plaintext localhost:9090 list
grpcurl -plaintext localhost:9090 describe order.OrderService
```

### WebSocket (ws://localhost:8080/ws/trading)

#### 연결
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/trading');
```

#### 주문 생성 (CREATE_ORDER)
```json
// 요청
{
  "type": "CREATE_ORDER",
  "requestId": "uuid-1234",
  "stockCode": "AAPL",
  "orderType": "BUY",
  "quantity": 10,
  "price": 150.00,
  "priceType": "LIMIT"
}

// 응답
{
  "type": "ORDER_CREATED",
  "requestId": "uuid-1234",
  "success": true,
  "orderNumber": 123456789
}
```

#### 주문 조회 (QUERY_ORDER)
```json
// 요청
{
  "type": "QUERY_ORDER",
  "requestId": "uuid-1235",
  "orderNumber": 123456789
}

// 응답
{
  "type": "ORDER_QUERIED",
  "requestId": "uuid-1235",
  "success": true,
  "order": { ... }
}
```

#### 포트폴리오 조회 (GET_PORTFOLIO)
```json
// 요청
{
  "type": "GET_PORTFOLIO",
  "requestId": "uuid-1236",
  "accountNumber": "ACC001"
}

// 응답
{
  "type": "PORTFOLIO_DATA",
  "requestId": "uuid-1236",
  "success": true,
  "holdings": [ ... ]
}
```

#### Rate Limit 초과 시
```json
{
  "type": "ERROR",
  "requestId": "uuid-1237",
  "success": false,
  "errorCode": "TOO_MANY_REQUESTS",
  "errorMessage": "Too Many Requests"
}
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
├── util/
│   └── SnowflakeIdGenerator.kt # 분산 ID 생성
└── ws/
    ├── config/
    │   └── WebSocketConfig.kt      # WebSocket 라우팅 및 CORS 설정
    ├── handler/
    │   └── TradingWebSocketHandler.kt  # WebSocket 메시지 처리
    ├── message/
    │   ├── WsRequest.kt            # 요청 메시지 (sealed class)
    │   └── WsResponse.kt           # 응답 메시지 (sealed class)
    ├── ratelimit/
    │   ├── RateLimiter.kt          # Fixed Window Rate Limiter
    │   └── RateLimiterManager.kt   # 세션별 Rate Limiter 관리
    └── session/
        └── SessionManager.kt       # WebSocket 세션 관리

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
| REST API | 8080 |
| WebSocket | 8080 (ws://localhost:8080/ws/trading) |
| gRPC | 9090  |
| Kafka | 9092  |
| Kafka UI | 38080 |

## Architecture

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  REST API   │  │  gRPC API   │  │  WebSocket  │
│   :8080     │  │   :9090     │  │ /ws/trading │
└──────┬──────┘  └──────┬──────┘  └──────┬──────┘
       │                │                │
       │                │                ▼
       │                │         ┌─────────────┐
       │                │         │ RateLimiter │
       │                │         │  Manager    │
       │                │         └──────┬──────┘
       │                │                │
       ▼                ▼                ▼
┌─────────────────────────────────────────────────┐
│                 OrderProducer                    │
└──────────────────────┬──────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────┐
│                    Kafka                         │
│                   :9092                          │
└──────────────────────┬──────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────┐
│                 OrderConsumer                    │
└──────────────────────┬──────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────┐
│              ExecutionService                    │
│           (주문 저장 & 상태 관리)                  │
└──────────────────────┬──────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────┐
│              OrderBookService                    │
│            (호가창 & 매칭 엔진)                    │
└─────────────────────────────────────────────────┘
```

## gRPC 통신 패턴

이 프로젝트는 gRPC의 4가지 통신 패턴을 모두 구현하여 학습 목적으로 활용할 수 있습니다.

### 1. Unary RPC
```
Client ──Request──▶ Server
Client ◀──Response── Server
```
- **메서드**: `GetOrder`
- **특징**: 단일 요청, 단일 응답 (REST API와 유사)
- **사용 사례**: 주문 조회

### 2. Server Streaming RPC
```
Client ──Request──▶ Server
Client ◀──Response── Server
Client ◀──Response── Server
Client ◀──Response── Server
```
- **메서드**: `WatchOrderStatus`
- **특징**: 단일 요청, 다중 응답 스트림
- **사용 사례**: 주문 상태 변경 실시간 모니터링
- **구현**: `Flow<T>`를 반환하여 지속적인 데이터 전송

### 3. Client Streaming RPC
```
Client ──Request──▶ Server
Client ──Request──▶ Server
Client ──Request──▶ Server
Client ◀──Response── Server
```
- **메서드**: `CreateBulkOrder`
- **특징**: 다중 요청 스트림, 단일 응답
- **사용 사례**: 대량 주문 일괄 제출
- **구현**: `Flow<T>`를 파라미터로 받아 `collect`로 처리

### 4. Bidirectional Streaming RPC
```
Client ──Request──▶ Server
Client ◀──Response── Server
Client ──Request──▶ Server
Client ──Request──▶ Server
Client ◀──Response── Server
Client ◀──Response── Server
```
- **메서드**: `ManageOrders`
- **특징**: 양방향 동시 스트림 (실시간 양방향 통신)
- **사용 사례**: 실시간 주문 제출 + 즉각적인 처리 결과 수신
- **구현**: `channelFlow` + `launch`로 동시성 처리

### Proto 정의 요약
```protobuf
service OrderService {
  // Unary
  rpc GetOrder(GetOrderRequest) returns (GetOrderResponse);

  // Server Streaming
  rpc WatchOrderStatus(WatchOrderStatusRequest) returns (stream OrderStatusUpdate);

  // Client Streaming
  rpc CreateBulkOrder(stream OrderRequest) returns (OrderResponse);

  // Bidirectional Streaming
  rpc ManageOrders(stream OrderManagementRequest) returns (stream OrderManagementResponse);
}
```

## Notes

- **Eventual Consistency**: REST API로 주문 제출 후 gRPC로 조회 시, Kafka 처리 지연으로 인해 즉시 조회되지 않을 수 있음
- **Snowflake ID**: 분산 환경에서 고유한 주문번호 생성
- **Coroutines**: 사용자/계좌 서비스는 Kotlin Coroutines 기반 비동기 처리
- **gRPC Streaming**: Kotlin Flow와 Coroutines를 활용한 비동기 스트리밍 구현
- **WebSocket Rate Limiting**: Fixed Window 방식으로 세션당 초당 5회 요청 제한 (CAS 기반 Thread-safe 구현)
- **WebSocket Session Management**: ConcurrentHashMap 기반 세션 관리, 연결 종료 시 자동 리소스 정리
