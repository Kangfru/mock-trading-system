---
description: 주가 조회
allowed-tools: "*"
---

stooq-mcp 서버의 get_stock_price 도구를 사용하여 $ARGUMENTS 주가를 조회해줘.

- 티커만 입력된 경우: 미국(us) 시장으로 기본 조회
- "삼성전자", "005930" 등 한국 주식인 경우: 한국(jp가 아닌 경우 지원 여부 확인 필요)
- 시장을 명시한 경우: 해당 시장으로 조회 (예: "AAPL us", "7203 jp")