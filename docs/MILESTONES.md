# Milestones — mcp_server_build

본 환경 (`airlab/mcp_server_build/`)의 모든 마일스톤 + RT 산출물 일람. 모두 2026-05-18 작업 세션 (single-day plow-through).

| ID | 범위 | 상태 | DoD 스크립트 | 핵심 산출물 |
|---|---|---|---|---|
| **M0' on-intake** | 인계받은 환경 상태 평가 | — | (n/a) | docker-compose는 redis만, mcp-server·gateway는 manual `docker run`. M2·M4·M5 부분 완성. M1·M3 누락. 도메인 식별자 `GongGongNuri` (공공누리, 저작권 라이선스) 카테고리 오류 — 실제 도메인은 공유누리 (eshare.go.kr 시설 예약). |
| **M1'** | mock-backend 별도 서비스 + sink 쌍 + 데이터셋 | **MET 2026-05-18** | `scripts/verify-m1.sh` 5/5 | `mock-backend/` 신규 Spring Boot. `/stores/{id}` (unauth IDOR sink) + `/secure/stores/{id}` (Bearer). 6 row 데이터셋 (`restricted` 플래그). `StoreDetail` record. |
| **M1.5** | HTTP 어댑터 재배선 + `Eshare*` 리네임 + application config 정리 | **MET 2026-05-18** | `scripts/verify-m1-5.sh` 4/4 | mcp-server의 in-process bean → `RestClient` HTTP. mock-backend `GET /stores` 리스트 엔드포인트 (restricted 필터). `GongGongNuri*` → `Eshare*` 일괄 리네임. `application.properties` → `application.yml`. `gongnuri-mcp-server` → `eshare-mcp-server`. |
| **M2.5** | `getStoreDetail` tool 등록 (RT-002 vehicle 인프라) | **MET 2026-05-18** | `scripts/verify-m2-5.sh` 4/4 | mcp-server `dto/StoreDetail` + `EshareApiClient.getStoreById` + `EshareTools.getStoreDetail` + `McpServerConfig` 두 번째 tool 등록. description은 정직 baseline (env-controllable로 M0'에서 RT-002 변조 가능). |
| **M3'** | host + Mock LLM stub | **MET 2026-05-18** | `scripts/verify-m3p.sh` 3/3 | `mcp-lab-host/` 신규. `POST /run` → Orchestrator → `LlmClient` → `McpClientFacade` → mcp-server. `MockLlmClient` hardcoded 시나리오. `LLM_MODE` env로 mock·mock_poisoned·real_deterministic 분기. |
| **RT-002 Stage 1** | tool description poisoning × backend IDOR PoC | **PASS 2026-05-18** | `scripts/rt-002-stage1.sh` 3/3 | 두 플래그 attack 모델 (server-side description env + host-side `PoisonedMockLlmClient`). 3 케이스 (baseline / S only / both) 자동 실행 + baseline 복원. 보고서: `docs/RT-002.md`. |
| **M0'** | 통합 docker-compose + start-all + 2-zone + gateway 합류 | **MET 2026-05-18** | `scripts/verify-m0p.sh` 9/9 | `docker-compose.yml` (5 서비스 + 2-zone), `start-all.sh` (build+up+Redis seed+120s health poll), gateway 합류 (`agentgateway:v1.0.1`), host의 기본 경로가 gateway 경유 4-hop chain. |

## 의존성 관계

```
M0' on-intake
   │
   ├─→ M1' (mock-backend 신규)
   │     │
   │     └─→ M1.5 (어댑터 HTTP + 리네임)
   │           │
   │           └─→ M2.5 (getStoreDetail tool)
   │                 │
   │                 └─→ RT-002 Stage 1
   │
   └─→ M3' (host + Mock LLM)  ←─── M2.5 결과를 시연 대상으로 사용
         │
         └─→ M0' (통합 부팅, 모든 컴포넌트 합쳐서 4-hop chain)
```

M3'는 M2.5에 의존하지 않지만 RT-002 Stage 1은 M3' + M2.5 둘 다 필요. M0'는 마지막 — 모든 컴포넌트가 만들어진 뒤 compose로 통합.

## 단일 세션 작업 흐름 (2026-05-18 시간순)

1. 환경 진단 (`docker ps`, 메모리 인덱스 확인, `GongGongNuri` 카테고리 오류 식별).
2. M1' co-design → MET.
3. M1.5 co-design → MET. 트러블슈팅: 옛 env (`mcp-pentest-lab`)의 host가 8080 점유 중 → `docker compose down`으로 일시 정지; Redis seeding 누락 → 별도 단계; mcp-server Created-state 컨테이너 port spec 손상 → recreate; mock-backend 재빌드 누락 (M1.5 변경 시 한 번 더 build 필요).
4. M2.5 → MET.
5. M3' → MET.
6. RT-002 Stage 1 → PASS.
7. M0' → MET. 트러블슈팅: 기존 manual `docker network create mcp-lab-net` 와 compose 레이블 충돌 → `external: true` 처리.
8. 문서화 (본 문서 포함).

## 다음 단계 (예정)

- **RT-001 포팅**: 옛 env의 보고서를 새 env 맥락으로 재서술 (gateway flavor mismatch — 새 env는 Streamable HTTP만 사용해서 자연 회피된 형태).
- **RT-003 포팅**: 두 번째 MCP server 추가 (research-server 류) → cross-server description injection 시연.
- **BT-001 구현**: BT-A/B/C 중 하나 시작 (`docs/BT-CANDIDATES.md` 참조).
- **M-mig/M-comp** 재정의 (옛 env의 invalidated milestones — RT-001 재현용 변형 branch로 부활 검토).
- **M4 X-API-Key boundary**: 새 env에 이미 부분 적용. ACL 정책 확장 필요 시.
- **M6 취약 sink** 별도 서버: path traversal 등 classical 단독 sink (RT vehicle 다양화).
- **M7 공격용 MCP 서버**: cross-server RT 시나리오 (RT-003 동반).
- **M11 모니터링**: OTel + Prometheus + Grafana 통합.
- **M12 real LLM**: Anthropic API + `RealLlmClient` 구현. RT-002 Stage 2 실행 가능.

## 검증 스크립트 운영 노트

- 모든 verify 스크립트는 `scripts/` 디렉터리 안. 작업 기준은 `mcp_server_build/`.
- M0' 통합 부팅 후 모든 verify는 `bash scripts/verify-XXX.sh` 형식으로 실행.
- RT-002 스크립트는 자체 `docker run`으로 컨테이너 재기동 — 끝나면 baseline 복원하므로 다른 검증과 격리됨. 단, 이후 compose state로 복귀하려면 `docker compose up -d --force-recreate mcp-lab-mcp-server mcp-lab-host`.
