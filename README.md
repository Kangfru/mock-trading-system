# Mock Trading System

Kotlin + Spring Boot 기반의 모의 주식 거래 시스템

## Tech Stack

- **Language**: Kotlin (Java 21)
- **Framework**: Spring Boot 4.0.1
- **Build**: Gradle Kotlin DSL
- **Messaging**: Apache Kafka
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
│   └── StockPriceController.kt # 시세 API
├── domain/
│   ├── Order.kt                # 주문 도메인
│   ├── OrderBook.kt            # 호가창 도메인
│   ├── Execution.kt            # 체결 도메인
│   └── StockQuote.kt           # 시세 도메인
├── producer/
│   └── OrderProducer.kt        # 주문 메시지 발행
├── service/
│   ├── OrderBookService.kt     # 호가창/매칭 서비스
│   └── ExecutionService.kt     # 체결 서비스
└── util/
    └── SnowflakeIdGenerator.kt # 분산 ID 생성
```

## Infrastructure

```yaml
# docker-compose.yml
- Zookeeper: Kafka 코디네이션
- Kafka: localhost:9092
- Kafka UI: http://localhost:38080
```

## TODO

- [X] 사용자/계좌 시스템
- [X] 포트폴리오 관리 (보유 종목, 잔고)
- [X] 매도 시 보유 수량 검증
- [ ] Kotlin Multiplatform Compose로 간단한 HTS 앱 개발
