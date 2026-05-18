# MCP Lab — local pentest sandbox

모든 명령은 `mcp_server_build/` 디렉터리를 작업 기준으로 실행한다.

---

## 마일스톤 일람 (이번 빌드)

각 마일스톤은: **주요 성취 → 선택 (대안과 함께) → 시행착오 → 검증 결과** 순서로 정리.

### M1' — Mock Backend Skeleton  ✓ MET 2026-05-18

- **주요 성취**: 별도 Spring Boot 서비스 (`mock-backend`, port 8083). unauth `/stores/{id}` IDOR sink + Bearer-gated `/secure/stores/{id}`. 6 row 데이터셋 (3 public + 2 restricted/PUBLISHED + 1 restricted/DRAFT). `StoreDetail` record.
- **선택 (다른 옵션과 함께)**:
  - 도메인: **(a) 공유누리 store 단독** ← 선택 / (b) user 도메인 추가 / (c) consumerCd를 사용자 식별자로 재해석
  - DoD 범위: **(a) backend-only standup** ← 선택 / (b) backend + adapter rewiring / (c) full (tool 추가 포함)
  - 데이터 모델: **(a) 단일 shape + `restricted` 플래그** ← 선택 / (b) field tiering / (c) entity scope tiering
  - 네트워크: **(a) flat `mcp-lab-net`** ← 선택 / (b) 2-zone 즉시 도입 / (c) 호스트 포트 expose만 막는 절충
  - 검증 하네스: **(a) bash + curl** ← 선택 / (b) JUnit `MockMvc` / (c) 양쪽
- **시행착오**: 없음.
- **검증**: `bash scripts/verify-m1.sh` → 5/5 PASS.

### M1.5 — Adapter Rewire + Eshare Rename  ✓ MET 2026-05-18

- **주요 성취**: `mcp-server`의 in-process bean → `RestClient` HTTP. mock-backend에 `GET /stores` 리스트 (restricted/draft 필터). `GongGongNuri*` 식별자 → `Eshare*` 일괄 (공공누리=KOGL 저작권 라이선스 ≠ 공유누리=시설 예약 카테고리 오류 정정). `application.properties` → `application.yml`. agentgateway target `gongnuri-mcp-server` → `eshare-mcp-server`.
- **선택**:
  - 리네임 대상: **(a) `Eshare` (eshare.go.kr 도메인 매칭)** ← 선택 / (b) `GongyuNuri` (음역) / (c) `EshareNuri` (절충)
- **시행착오**:
  - 1차 verify 실패 (case 1~3 status 404): mcp-server만 재빌드, **mock-backend 재빌드 누락**. → 양쪽 재빌드.
  - case 4 초기 정의는 gateway 경유였으나 gateway 미존재 (M0' 이전) → mcp-server 직접 호출로 재정의.
- **검증**: `bash scripts/verify-m1-5.sh` → 4/4 PASS.

### M2.5 — `getStoreDetail` Tool  ✓ MET 2026-05-18

- **주요 성취**: 두 번째 MCP tool (store_id 단건 조회). `EshareApiClient.getStoreById` → mock-backend `/stores/{id}` (IDOR sink). description env-controllable (`MCP_TOOL_DESC_GETSTOREDETAIL`) — RT-002 vehicle 인프라.
- **선택**:
  - description voice: **(a) 미니멀 baseline** ← 선택 / (b) 도메인 친화 (internalNotes 등 인정) / (c) LLM 친화 (이미 false promise 일부)
- **시행착오**: 없음.
- **검증**: `bash scripts/verify-m2-5.sh` → 4/4 PASS.

### M3' — Host + Mock LLM stub  ✓ MET 2026-05-18

- **주요 성취**: `mcp-lab-host` Spring Boot service (port 8085). `POST /run` → `Orchestrator` → `LlmClient` → `McpClientFacade` → mcp-server `/mcp`. `MockLlmClient` (시나리오 switch) + `RealLlmClient` skeleton + `LLM_MODE` env 분기 (`mock` / `mock_poisoned` / `mock_cross_poisoned` / `real_deterministic`).
- **선택**:
  - LLM 결정 방식: **(a) 하드코드 switch** ← 선택 (M3' 최소) / (b) YAML 시나리오 로더 / (c) 외부 설정
  - MCP 클라이언트: java-sdk 1.1.1 client 대신 **raw `RestClient`** ← 선택. 이유: verify-script와 wire 동일성, 디버깅 표면 작음.
- **시행착오**: 없음.
- **검증**: `bash scripts/verify-m3p.sh` → 3/3 PASS.

### RT-002 Stage 1 — Description Poisoning × Backend IDOR  ✓ PASS 2026-05-18

- **주요 성취**: 첫 amplification PoC. **두 플래그 attack 모델** — S(서버 description) + H(host LLM 추종). 자동 3-case 비교 (baseline / S-only defense / both-on attack). 보고서: [`docs/RT-002.md`](docs/RT-002.md).
- **선택**:
  - sentinel 형식: (α) 한국어 자연어 / (β) `[SYSTEM CONTEXT]` tag / **(γ) 둘 다** ← 선택. RT-003에도 동일 패턴 재사용.
- **시행착오**: 없음.
- **검증**: `bash scripts/rt-002-stage1.sh` → 3/3 PASS. Case 3에서 `VIP 의전용` 누설 확인.

### M0' — Compose 통합 + 2-zone + Gateway 합류  ✓ MET 2026-05-18

- **주요 성취**: 통합 `docker-compose.yml` (이후 13 서비스). 2-zone (`lab-public` + `mcp-lab-net`). agentgateway 1.0.1 합류 — federation backend. `start-all.sh` build+up+Redis seed+health poll 자동화.
- **선택**:
  - 네트워크 명명: 기존 manual `docker network create mcp-lab-net` 호환 위해 **`lab-internal: external=true, name=mcp-lab-net`** ← 선택 / (b) fresh 새 network 생성 후 manual 스크립트 다 수정 / (c) 별도 명명 + alias
- **시행착오**:
  - compose 첫 `up`에서 기존 `mcp-lab-net` 네트워크 label mismatch → `external: true` 선언으로 해결.
  - 여러 라운드에서 standalone `docker run` 잔재 컨테이너가 compose recreate를 막음 (`Conflict. The container name "..." is already in use`). 정리 명령: `docker rm -f $(docker ps -aq --filter "name=mcp-lab-")`.
  - 첫 `verify-m0p.sh` 0/9 (컨테이너 미기동) — `start-all.sh` 누락. user 직접 인지 + 재실행으로 통과.
- **검증**: `bash scripts/verify-m0p.sh` → 9/9 PASS.

### M6 — Vulnerable Sink Server (fs-server)  ✓ MET 2026-05-18

- **주요 성취**: `mcp-lab-fs-server` (port 8084). MCP tool `read_file(path)` — `Files.readString` 직격, 검증 0. gateway federation 첫 multi-target.
- **선택**:
  - fs-server 내부 auth: **(a) gateway-edge X-API-Key 존재 검사만, fs-server 내부 무방어** ← 선택 (취약 캐릭터 부합) / (b) full ApiKeyAuthenticationFilter / (c) Redis 기반 tenant-specific [→ M13 확장 예정]
- **시행착오**:
  1. **agentgateway federation prefix 미인지**. federation 후 tool name이 `<target>_<tool>` 형식 (`fs-server_read_file` 등). host의 raw `read_file` 호출이 split mis-parse로 500 ("unknown service read"). **해결**: `MockLlmClient.resolveToolName(tools, canonical)` — `tools/list` 응답에서 suffix matching, prefix 유무 자동 흡수.
  2. **Spring `RestClient.body(String.class)` ISO-8859-1 디코딩**. UTF-8 한글 응답이 mojibake → Jackson 파싱 실패 → tools 리스트 empty. **해결**: `body(byte[].class)` + `new String(bytes, StandardCharsets.UTF_8)`.
- **검증**: `bash scripts/verify-m6.sh` → 4/4 PASS.

### M7 — Attack Vehicle Server (research-server)  ✓ MET 2026-05-18

- **주요 성취**: `mcp-lab-research-server` (port 8086). MCP tool `lookup_term(term)` — mock 사전 검색. **Description env-controllable** (`RESEARCH_TOOL_DESC_LOOKUP_TERM`). 3-target federation.
- **선택**:
  - tool voice: **(a) `lookup_term` (검색 stub, 명백한 별 도메인)** ← 선택 / (b) `summarize_context(text)` (LLM-friendly, 텍스트 처리) / (c) `recommend_store(location)` — 도메인 mcp-server와 겹쳐 federation 의미 약함, **영구 skip**.
  - (b)는 RT-005류 (텍스트 가공 exfil 채널) vehicle로 향후 라운드에 고려.
- **시행착오**: 없음.
- **검증**: `bash scripts/verify-m7.sh` → 4/4 PASS.

### M8 / RT-003 Stage 1 — Cross-Server Description Injection  ✓ PASS 2026-05-18

- **주요 성취**: 두 MCP 서버 간 의도하지 않은 데이터 흐름 시연. `research-server.lookup_term` description에 sentinel `read_file('/etc/passwd')` → host LLM이 **다른 서버 (fs-server)** 의 `read_file` 호출 → 결과를 caller 응답에 합쳐 반환. Trivial Trojans 패턴. 보고서: [`docs/RT-003.md`](docs/RT-003.md).
- **구조 변경**: `LlmClient.decideMultiStep` default 메서드 추가 → 기존 single-step 무영향, cross-server multi-tool plan 지원. `Orchestrator`가 plan iterate + `\n---\n` concat. `CrossServerPoisonedMockLlmClient` 신규.
- **선택**:
  - sentinel target path: **(a) `/etc/passwd` (eclipse-temurin root UID로 읽기 가능, 의미 있는 데이터)** ← 선택 / (b) `/data/menu.txt` (안전하나 시연력 약함) / (c) `/data/welcome.txt` (동상)
- **시행착오**: 없음.
- **검증**: `bash scripts/rt-003-stage1.sh` → 3/3 PASS. Case 3에서 `root:x:0` 라인 노출 확인.

### M9 — Scanner MVP (DESC_INJECT + ARG_NO_PATTERN)  ✓ MET 2026-05-18

- **주요 성취**: `mcp-lab-scanner` (port 8087). gateway federated `tools/list` 일괄 분석. 2 룰 (`DESC_INJECT` sentinel 패턴 / `ARG_NO_PATTERN` 보안-민감 string args의 pattern/enum 부재). Finding JSON 보고.
- **선택**:
  - 룰 스코프: **(b) MVP 2 룰** ← 선택 / (a) 5 룰 (+ ARG_NO_REQUIRED_AUTH / CROSS_SERVER_REACH / NO_PER_TOOL_ACL_HINT — 향후 추가 가능, false positive 처리 까다로움)
- **시행착오**: 없음.
- **검증**: `bash scripts/verify-m9.sh` → 4/4 PASS.

### M10 / BT-001 — Backend Authz on Restricted Rows  ✓ MET 2026-05-18

- **주요 성취**: `mock-backend.PublicStoreController`에 `BT_BACKEND_AUTHZ_ENABLED=true` 시 restricted=true row → 403 `RESTRICTED`. RT-002 sink leg 차단. 보고서: [`docs/BT-001.md`](docs/BT-001.md).
- **선택 (3 후보 중)**:
  - BT-A (MCP-layer description 무결성, gateway/scanner)
  - BT-B (Host LLM 강화, sanitization)
  - **BT-C 옵션 2 (backend restricted-row 단일 게이팅)** ← 선택. 이유: 한 줄 / 즉시 검증 가능 / 의존성 0.
- **시행착오**: 없음.
- **검증**: `bash scripts/verify-bt-001.sh` → 4/4 PASS. Case 4 (BT ON + RT-002 attack) → `VIP 의전용` 미포함 확인.

### M11 — Prometheus + Grafana (metrics)  ✓ MET 2026-05-18

- **주요 성취**: 6 앱 `/actuator/prometheus` 노출. Prometheus 9090 scrape. Grafana 3000 + Prometheus default datasource provisioning.
- **선택 (스코프)**:
  - (a) 풀스택 (Prom + Grafana + OTel + Jaeger + Java agent) — 변경 표면 큼 → M11.5로 분리
  - **(b) MVP metrics-only** ← 선택
- **시행착오**:
  - 1차 verify case 3 (모든 targets up) FAIL. 원인: `/api/v1/targets` JSON에서 `"job":"<name>"`이 `labels.{}` 중첩, `"health":"up"`은 상위 — naive regex `[^}]*`이 `}` 못 넘음. **해결**: prometheus `up{job="..."}` 메트릭 쿼리로 변경.
- **검증**: `bash scripts/verify-m11.sh` → 5/5 PASS.

### M11.5 — Tracing (OTel Collector + Jaeger)  ✓ MET 2026-05-18

- **주요 성취**: 6 앱 OTLP export. Jaeger UI 16686에 spans 시각화. `spring.application.name`이 service name으로 등장.
- **선택 (계측 방식)**:
  - (A) Java agent (jar mount + `JAVA_TOOL_OPTIONS`) — 통합 표면 큼
  - **(B) Spring Boot 3 native bridge** (`micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`) ← 선택. 두 dep로 server-side 자동 계측.
  - (C) Manual SDK 코드
- **시행착오**:
  - 첫 trace 확인 시 span 1개만 (host POST /run). 원인: host의 `McpClientFacade`가 `RestClient.create()` 사용 — Spring auto-config의 observation registry 미부착. **해결**: M11.6.
- **검증**: `bash scripts/verify-m11-5.sh` → 4/4 PASS.

### M11.6 — RestClient.Builder Propagation  ✓ done 2026-05-18

- **주요 성취**: 3 services (`McpClientFacade` / `McpToolFetcher` / `EshareApiClient`)의 `RestClient.create()` → `RestClient.Builder` 의존성 주입. Spring Boot 3 auto-config가 observation registry 부착 → outgoing HTTP에 W3C tracecontext 헤더 자동 전파.
- **시행착오**: 없음.
- **확인**: Jaeger UI에서 한 trace 안에 host + (gateway forward 시) downstream span 다수 — `cross-service stitching` 가능 여부는 agentgateway 1.0.1의 traceparent forward 여부에 의존.

### M12 — Anthropic Real LLM API 통합  ✓ 코드 완료 (검증 SKIPPED)

- **주요 성취**: `RealLlmClient` 실구현. Anthropic Messages API (Tool Use) POST. 첫 `tool_use` content → `LlmDecision` 변환. tools 배열 마지막에 `cache_control: ephemeral` (prompt caching).
- **선택**:
  - SDK: (A) Anthropic Java SDK / **(B) 직접 HTTP via RestClient** ← 선택 (안정, 의존성 0)
  - Model: default `claude-sonnet-4-5`, env `ANTHROPIC_MODEL` override
- **시행착오**: 사용자의 Anthropic API key 발급 결제 이슈로 실 호출 검증 불가. verify 스크립트는 key 미존재 시 `SKIPPED exit 0`로 처리.
- **검증**: `bash scripts/verify-m12.sh` → key 미존재로 `SKIPPED`. 키 확보 시 3/3 케이스 자동 실행 가능.

### Web Demo P1 + P2 — React 프론트엔드 + Spring Boot 프록시  ✓ done 2026-05-18

- **주요 성취**: `mcp-lab-demo` 서비스 (port 8090). React+Vite+Tailwind frontend (mockup 디자인 그대로) + Spring Boot proxy backend (`/api/chat` → host, `/api/traces` → jaeger, `/api/scan` → scanner). 4 패널 탭 (Tool Calls / Traces / Packets / Findings). 단일 origin (CORS 0).
- **선택**:
  - 스택: **(a) React + Vite + Tailwind + Spring Boot 정적 서빙** ← 선택 / (b) plain HTML + vanilla JS / (c) Next.js
  - 디자인 가이드: `#bc0f1d` opacity 7 tier + Pretendard ExtraBold(headings)/SemiBold(body) + 흰 배경
  - 목업: 처음 PPT MCP로 가다가 → **단일 static HTML 직접 생성** ← 변경 ([`web-demo/mockup/index.html`](web-demo/mockup/index.html))
- **시행착오**:
  - bot 응답 박스 가로 overflow (긴 JSON, no word breaks). **해결**: `min-w-0 break-all` 추가.
  - Tailwind `bg-r5` 등 r5/10/20/40/60/80/100 toy color 토큰 cleaner하게 적용.
- **확인**: http://localhost:8090 접속 → 채팅 + 좌측 패널 동작 확인 (스크린샷 첨부).

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
| [MILESTONES.md](docs/MILESTONES.md) | 마일스톤 일람·의존성·작업 흐름 |
| [SCENARIOS.md](docs/SCENARIOS.md) | host 시나리오 카탈로그 + 추가 방법 |
| [BT-CANDIDATES.md](docs/BT-CANDIDATES.md) | RT-002 대응 BT-A/B/C 후보 + 통합 매트릭스 |
| [RT-002.md](docs/RT-002.md) | RT-002 Stage 1 보고서 (single-server description poisoning × IDOR) |
| [RT-003.md](docs/RT-003.md) | RT-003 Stage 1 보고서 — cross-server description injection × `/etc/passwd` exfil |
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
