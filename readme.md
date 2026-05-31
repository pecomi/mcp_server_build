# MCP Lab — local pentest sandbox

모든 명령은 `mcp_server_build/` 디렉터리를 작업 기준으로 실행한다.

---

## 마일스톤 

각 마일스톤은: **선택 (대안과 함께) → 유의할 점 → 검증 결과**으로 정리.

### M1' — Mock Backend Skeleton  

-  별도 Spring Boot 서비스 (`mock-backend`, port 8083). unauth `/stores/{id}` IDOR sink + Bearer-gated `/secure/stores/{id}`. 6 row 데이터셋 (3 public + 2 restricted/PUBLISHED + 1 restricted/DRAFT). `StoreDetail` record.
- **검증**: `bash scripts/verify-m1.sh` → 5/5 PASS.

### M1.5 — Adapter Rewire + Eshare Rename  

-  `mcp-server`의 in-process bean → `RestClient` HTTP. mock-backend에 `GET /stores` 리스트 (restricted/draft 필터). `GongGongNuri*` 식별자 → `Eshare*` 일괄 (공공누리=KOGL 저작권 라이선스 ≠ 공유누리=시설 예약 카테고리 오류 정정). `application.properties` → `application.yml`. agentgateway target `gongnuri-mcp-server` → `eshare-mcp-server`.
- **선택**:
  - 리네임 대상: **(a) `Eshare` (eshare.go.kr 도메인 매칭)** ← 선택 / (b) `GongyuNuri` (음역) / (c) `EshareNuri` (절충)
- **검증**: `bash scripts/verify-m1-5.sh` → 4/4 PASS.

### M2.5 — `getStoreDetail` Tool  

- 두 번째 MCP tool (store_id 단건 조회). `EshareApiClient.getStoreById` → mock-backend `/stores/{id}` (IDOR sink). description env-controllable (`MCP_TOOL_DESC_GETSTOREDETAIL`) — RT-002 vehicle 인프라.
- **검증**: `bash scripts/verify-m2-5.sh` → 4/4 PASS.

### M3' — Host + Mock LLM stub 

- `mcp-lab-host` Spring Boot service (port 8085). `POST /run` → `Orchestrator` → `LlmClient` → `McpClientFacade` → mcp-server `/mcp`. `MockLlmClient` (시나리오 switch) + `RealLlmClient` skeleton + `LLM_MODE` env 분기 (`mock` / `mock_poisoned` / `mock_cross_poisoned` / `real_deterministic`).
- LLM 결정 방식: **(a) 하드코드 switch** ← 선택
  - MCP 클라이언트: java-sdk 1.1.1 client 대신 **raw `RestClient`** ← 선택. 이유: verify-script와 wire 동일성, 디버깅 표면 작음.
- **검증**: `bash scripts/verify-m3p.sh` → 3/3 PASS.

### RT-002 Stage 1 — Description Poisoning × Backend IDOR 
- **검증**: `bash scripts/rt-002-stage1.sh` → 3/3 PASS. Case 3에서 `VIP 의전용` 누설 확인.

### M0' — Compose 통합 + 2-zone + Gateway 합류  

- 통합 `docker-compose.yml`/ 2-zone (`lab-public` + `mcp-lab-net`). agentgateway 1.0.1 합류 — federation backend. `start-all.sh` build+up+Redis seed+health poll 자동화.
- compose 첫 `up`에서 기존 `mcp-lab-net` 네트워크 label mismatch → `external: true` 선언으로 해결.
- 여러 라운드에서 standalone `docker run` 잔재 컨테이너가 compose recreate를 막음 (`Conflict. The container name "..." is already in use`). 정리 명령: `docker rm -f $(docker ps -aq --filter "name=mcp-lab-")`.
- **검증**: `bash scripts/verify-m0p.sh` → 9/9 PASS.

### M6 — Vulnerable Sink Server (fs-server) 
- `mcp-lab-fs-server` (port 8084). MCP tool `read_file(path)` — `Files.readString` 직격, 검증 0. gateway federation 첫 multi-target.
- **시행착오**:
  1. **agentgateway federation prefix 미인지**. federation 후 tool name이 `<target>_<tool>` 형식 (`fs-server_read_file` 등). host의 raw `read_file` 호출이 split mis-parse로 500 ("unknown service read"). **해결**: `MockLlmClient.resolveToolName(tools, canonical)` — `tools/list` 응답에서 suffix matching, prefix 유무 자동 흡수.
  2. **Spring `RestClient.body(String.class)` ISO-8859-1 디코딩**. UTF-8 한글 응답이 mojibake → Jackson 파싱 실패 → tools 리스트 empty. **해결**: `body(byte[].class)` + `new String(bytes, StandardCharsets.UTF_8)`.
- **검증**: `bash scripts/verify-m6.sh` → 4/4 PASS.

### M7 — Attack Vehicle Server (research-server) 

- `mcp-lab-research-server` (port 8086). MCP tool `lookup_term(term)` — mock 사전 검색. **Description env-controllable** (`RESEARCH_TOOL_DESC_LOOKUP_TERM`). 3-target federation.
- **검증**: `bash scripts/verify-m7.sh` → 4/4 PASS.

### M8 / RT-003 Stage 1 — Cross-Server Description Injection  

- 두 MCP 서버 간 의도하지 않은 데이터 흐름 시연. `research-server.lookup_term` description에 sentinel `read_file('/etc/passwd')` → host LLM이 **다른 서버 (fs-server)** 의 `read_file` 호출 → 결과를 caller 응답에 합쳐 반환. Trivial Trojans 패턴. 보고서: [`docs/RT-003.md`](docs/RT-003.md).
- **구조 변경**: `LlmClient.decideMultiStep` default 메서드 추가 → 기존 single-step 무영향, cross-server multi-tool plan 지원. `Orchestrator`가 plan iterate + `\n---\n` concat. `CrossServerPoisonedMockLlmClient` 신규.
- sentinel target path: **(a) `/etc/passwd` (eclipse-temurin root UID로 읽기 가능, 의미 있는 데이터)** ← 선택 
- **검증**: `bash scripts/rt-003-stage1.sh` → 3/3 PASS. Case 3에서 `root:x:0` 라인 노출 확인.

### M9 — Scanner MVP (DESC_INJECT + ARG_NO_PATTERN)  
- `mcp-lab-scanner` (port 8087). gateway federated `tools/list` 일괄 분석. 2 룰 (`DESC_INJECT` sentinel 패턴 / `ARG_NO_PATTERN` 보안-민감 string args의 pattern/enum 부재). Finding JSON 보고.
- **검증**: `bash scripts/verify-m9.sh` → 4/4 PASS.

### RT-006 Stage 1 — Tool-Output Prompt Injection

- `research-server.lookup_term` description은 정상으로 두고, 반환되는 tool output 뒤에 `read_file('/etc/passwd')` follow-up instruction을 주입. `OutputPoisonedMockLlmClient`가 tool result를 다음 planning context로 신뢰하면 `fs-server.read_file` 후속 호출이 발생. 보고서: [`docs/RT-006.md`](docs/RT-006.md).
- **검증**: `bash scripts/rt-006-stage1.sh` → case 3에서 `root:x:0` 노출 기대.

### M10 / BT-001 — Backend Authz on Restricted Rows 

- `mock-backend.PublicStoreController`에 `BT_BACKEND_AUTHZ_ENABLED=true` 시 restricted=true row → 403 `RESTRICTED`. RT-002 sink leg 차단. 보고서: [`docs/BT-001.md`](docs/BT-001.md).
- BT-opt-C (backend restricted-row 단일 게이팅)
- **검증**: `bash scripts/verify-bt-001.sh` → 4/4 PASS. Case 4 (BT ON + RT-002 attack) → `VIP 의전용` 미포함 확인.

### M11 — Prometheus + Grafana (metrics) 
- 앱 `/actuator/prometheus` 노출. Prometheus 9090 scrape. Grafana 3000 + Prometheus default datasource provisioning.
- **검증**: `bash scripts/verify-m11.sh` → 5/5 PASS.

### M11.5 — Tracing (OTel Collector + Jaeger)  
- 앱 OTLP export. Jaeger UI 16686에 spans 시각화. `spring.application.name`이 service name으로 등장.
- **검증**: `bash scripts/verify-m11-5.sh` → 4/4 PASS.

### M11.6 — RestClient.Builder Propagation  
- 3 services (`McpClientFacade` / `McpToolFetcher` / `EshareApiClient`)의 `RestClient.create()` → `RestClient.Builder` 의존성 주입. Spring Boot 3 auto-config가 observation registry 부착 → outgoing HTTP에 W3C tracecontext 헤더 자동 전파.
- **확인**: Jaeger UI에서 한 trace 안에 host + (gateway forward 시) downstream span 다수 — `cross-service stitching` 가능 여부는 agentgateway 1.0.1의 traceparent forward 여부에 의존.

### M12 — Anthropic Real LLM API 통합  
- `RealLlmClient` 실구현. Anthropic Messages API (Tool Use) POST. 첫 `tool_use` content → `LlmDecision` 변환. tools 배열 마지막에 `cache_control: ephemeral` (prompt caching).
- default `claude-sonnet-4-5`, env `ANTHROPIC_MODEL` override
- **검증**: `bash scripts/verify-m12.sh` → key 미존재로 `SKIPPED`. 키 확보 시 3/3 케이스 자동 실행 가능.

### Web Demo P1 + P2 — React 프론트엔드 + Spring Boot 프록시 

 `mcp-lab-demo` 서비스 (port 8090). React+Vite+Tailwind frontend (mockup 디자인 그대로) + Spring Boot proxy backend (`/api/chat` → host, `/api/traces` → jaeger, `/api/scan` → scanner). 4 패널 탭 (Tool Calls / Traces / Packets / Findings). 단일 origin (CORS 0)..
- **확인**: http://localhost:8090 접속 → 채팅 + 좌측 패널 동작 확인

---

## 빠른 부팅

```bash
bash start-all.sh
bash scripts/verify-m0p.sh
```

- `start-all.sh`: docker compose 빌드·기동·Redis 키 시드·health 폴링까지 일괄 (≤180s).
- `verify-m0p.sh`: 컨테이너 + health + E2E smoke 검증.

데모 UI: http://localhost:8090 · Prometheus: :9090 · Grafana: :3000 (admin/admin) · Jaeger: :16686

수동 단계별 빌드는 §1~§23 참조.

## 문서 인덱스 (`docs/`)

| 문서 | 내용 |
|---|---|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | 서비스 구성·2-zone·MCP 흐름·신뢰 경계·코드 구조 |
| [THREAT-MODEL.md](docs/THREAT-MODEL.md) | Actor·ability·layer별 attack surface·RT 매핑·thesis |
| [SCENARIOS.md](docs/SCENARIOS.md) | host 시나리오 카탈로그 + 추가 방법 |
| [RT-002.md](docs/RT-002.md) | RT-002 Stage 1 보고서 (single-server description poisoning × IDOR) |
| [RT-003.md](docs/RT-003.md) | RT-003 Stage 1 보고서 — cross-server description injection × `/etc/passwd` exfil |
| [RT-006.md](docs/RT-006.md) | RT-006 Stage 1 보고서 — tool-output prompt injection × intent flow subversion |
| [BT-001.md](docs/BT-001.md) | BT-001 보고서 — backend authz on restricted rows |

---

## 수동 절차 (각 마일스톤 검증용)

## 1. Docker 네트워크 생성

이미 있으면 생략.

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
docker exec -i mcp-lab-redis redis-cli <<'EOF'
SET api-key:local-redteam-key '{"apiKey":"local-redteam-key","clientId":"local-redteam-client","status":"ACTIVE","allowedTools":["getStoreList","getStoreDetail"]}'
SET api-key:blocked-key '{"apiKey":"blocked-key","clientId":"blocked-client","status":"ACTIVE","allowedTools":[]}'
SET api-key:inactive-key '{"apiKey":"inactive-key","clientId":"inactive-client","status":"INACTIVE","allowedTools":["getStoreList","getStoreDetail"]}'
EOF
```

## 4. MCP Server 빌드 & 실행

> **선행 조건 (M1.5 이후)**: `getStoreList` Tool이 HTTP로 `mock-backend`를 호출하므로 §6의 mock-backend 컨테이너가 먼저 떠 있어야 한다.

```bash
cd mcp-server
docker build -t mcp-lab-server:local .
cd ..

docker run -d \
  --name mcp-lab-mcp-server \
  --network mcp-lab-net \
  -p 8080:8080 \
  -e SPRING_DATA_REDIS_HOST=mcp-lab-redis \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e REDIS_HOST=mcp-lab-redis \
  -e REDIS_PORT=6379 \
  -e MOCK_BACKEND_URL=http://mcp-lab-mock-backend:8083 \
  mcp-lab-server:local
```

### 4.1 Health

```bash
curl http://localhost:8080/actuator/health
```

기대: `{"status":"UP"}`

### 4.2 MCP Inspector

```bash
npx @modelcontextprotocol/inspector
```

- Transport: Streamable HTTP
- URL: `http://localhost:8080/mcp`
- Header: `X-API-Key: local-redteam-key`

## 5. (선택) Gateway 경유 인증 검증

```bash
bash scripts/test-local-gateway-auth.sh
```

## 6. Mock Backend (M1')

```bash
cd mock-backend
docker build -t mcp-lab-mock-backend:local .
cd ..

docker run -d \
  --name mcp-lab-mock-backend \
  --network mcp-lab-net \
  -p 8083:8083 \
  mcp-lab-mock-backend:local
```

검증: `bash scripts/verify-m1.sh` → 5/5 PASS.

## 7. M1.5 검증

```bash
bash scripts/verify-m1-5.sh
```

## 8. M2.5 검증

```bash
docker exec -i mcp-lab-redis redis-cli <<'EOF'
SET api-key:local-redteam-key '{"apiKey":"local-redteam-key","clientId":"local-redteam-client","status":"ACTIVE","allowedTools":["getStoreList","getStoreDetail"]}'
EOF
bash scripts/verify-m2-5.sh
```

## 9. Host (M3')

```bash
cd mcp-lab-host
docker build -t mcp-lab-host:local .
cd ..

docker run -d \
  --name mcp-lab-host \
  --network mcp-lab-net \
  -p 8085:8085 \
  -e LLM_MODE=mock \
  -e MCP_SERVER_URL=http://mcp-lab-mcp-server:8080/mcp \
  -e MCP_API_KEY=local-redteam-key \
  mcp-lab-host:local

bash scripts/verify-m3p.sh
```

## 10. RT-002 Stage 1

```bash
bash scripts/rt-002-stage1.sh
```

## 11. M0' 통합

`docker-compose.yml` + `start-all.sh` + 2-zone + gateway 합류.

```bash
bash start-all.sh
bash scripts/verify-m0p.sh
```

## 12. M6 (fs-server)

`start-all.sh`가 fs-server 포함 자동 빌드. 별도 빌드 시:

```bash
cd fs-server && docker build -t mcp-lab-fs-server:local . && cd ..
bash scripts/verify-m6.sh
```

## 13. M7 (research-server)

```bash
cd research-server && docker build -t mcp-lab-research-server:local . && cd ..
bash scripts/verify-m7.sh
```

## 14. RT-003 Stage 1

```bash
bash scripts/rt-003-stage1.sh
```

## 15. M9 (scanner)

```bash
cd scanner && docker build -t mcp-lab-scanner:local . && cd ..
bash scripts/verify-m9.sh
```

## 16. BT-001 활성화

```bash
# 영구
BT_BACKEND_AUTHZ_ENABLED=true docker compose up -d --force-recreate mock-backend

# 일시 (스크립트가 활성→복원)
bash scripts/verify-bt-001.sh
```

## 17. M11 (metrics)

```bash
bash scripts/verify-m11.sh
```

UI: Prometheus http://localhost:9090 · Grafana http://localhost:3000

## 18. M11.5 (tracing)

```bash
bash scripts/verify-m11-5.sh
```

UI: http://localhost:16686

## 19. M12 (real LLM)

```bash
export ANTHROPIC_API_KEY=sk-ant-...
bash scripts/verify-m12.sh
```

## 20. Web Demo

```bash
# 자동 (start-all.sh 안에 포함)
# 브라우저: http://localhost:8090
```

frontend 단독 dev (vite hot reload):
```bash
cd web-demo/frontend
npm install
npm run dev    # http://localhost:5173, /api/* proxy to :8090
```

---

## 정리 / 재시작

```bash
docker rm -f mcp-lab-host mcp-lab-gateway mcp-lab-mcp-server mcp-lab-fs-server mcp-lab-research-server mcp-lab-scanner mcp-lab-mock-backend mcp-lab-redis mcp-lab-prometheus mcp-lab-grafana mcp-lab-jaeger mcp-lab-otel-collector mcp-lab-demo 2>/dev/null || true
docker network rm mcp-lab-net mcp-lab-public 2>/dev/null || true
```

또는 한 줄 (compose 추적 밖 컨테이너 포함 다 정리):

```bash
docker rm -f $(docker ps -aq --filter "name=mcp-lab-") 2>/dev/null
```
