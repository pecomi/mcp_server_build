1. 로컬 개발 환경 고정
2. MCP Server 로컬 실행
3. getStoreList Mock Tool 구현
4. REST Adapter 방어 구조 구현
5. API Key 인증/인가 구현
6. Redis 8.6 연동
7. Rate Limit 구현
8. 공유누리 API 14개 Tool 확장
9. 개인정보/DSP 암복호화 Mock 구현
10. Agentgateway v1.0.1 추가
11. TLS 1.3 적용
12. red teaming 테스트셋 작성
13. 서버 배포
14. red teaming 수행 및 결과 정리

# MCP Lab 실행 절차

## 1. Docker network 생성

이미 있으면 생략 가능.

```bash
docker network create mcp-lab-net
```
## 2. Redis 실행

```bash
docker run -d \
  --name mcp-lab-redis \
  --network mcp-lab-net \
  -p 6379:6379 \
  redis:8.6
```

## 3. Redis API Key 등록

```bash
docker exec -it mcp-lab-redis redis-cli

SET api-key:local-redteam-key '{"apiKey":"local-redteam-key","clientId":"local-redteam-client","status":"ACTIVE","allowedTools":["getStoreList"]}'
SET api-key:blocked-key '{"apiKey":"blocked-key","clientId":"blocked-client","status":"ACTIVE","allowedTools":[]}'
SET api-key:inactive-key '{"apiKey":"inactive-key","clientId":"inactive-client","status":"INACTIVE","allowedTools":["getStoreList"]}'
exit
```

## 4. MCP Server 이미지 빌드

```bash
cd ~/mcp-lab/mcp-server
docker build -t mcp-lab-server:local .
```

## 5. MCP Server 실행

```bash
docker run -d \
  --name mcp-lab-mcp-server \
  --network mcp-lab-net \
  -p 8080:8080 \
  -e SPRING_DATA_REDIS_HOST=mcp-lab-redis \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e REDIS_HOST=mcp-lab-redis \
  -e REDIS_PORT=6379 \
  mcp-lab-server:local
```

## 6. Health 확인

```bash
curl http://localhost:8080/actuator/health
```

나와야하는 결과 : {"status":"UP"}

## 7. MCP Inspector 접속
```bash
npx @modelcontextprotocol/inspector
```

Transport: Streamable HTTP
URL: http://localhost:8080/mcp
Header: X-API-Key: local-redteam-key

```bash
curl http://localhost:8080/actuator/health
```