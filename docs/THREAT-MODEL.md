# Threat Model — mcp_server_build

본 PoC의 **MCP × classical 증폭** 위협 모델. actor·ability·knowledge·attack surface·in/out of scope를 명시하고 RT-001~003에 매핑.

## 1. Actor 카탈로그

| Actor | 권한 | 능력 |
|---|---|---|
| **Citizen** (정당 caller) | host `/run` 호출 가능. X-API-Key는 host 컨테이너가 보유, citizen은 보지 못함. | prompt + scenarioId 임의로 보낼 수 있음. host·gateway·mcp-server·mock-backend 어디에도 직접 네트워크 접근 못함 (lab-internal 격리). |
| **Tool-description Attacker** | mcp-server의 tool description 메타데이터 변조 가능 (supply-chain compromise OR insider OR `MCP_TOOL_DESC_*` env 변조 OR poisoned MCP server 배포). 다른 컴포넌트 접근 X. | description 한 문자열 변경만으로 LLM 행동 좌우. caller·gateway·backend 정상 운영 중에도 공격 성립. |
| **Cross-server Attacker** (RT-003 모델) | 페더레이션된 두 번째 MCP server 운영자 (예: malicious research-server). | 자기 도구의 description에 `[SYSTEM CONTEXT]` 주입. primary server 운영자는 description 검증 안 함. |
| **Insider** (운영자) | 모든 컴포넌트 접근. 의도적 약점 도입 가능. | out of scope (사내 위협은 본 PoC의 thesis 아님). |

## 2. Citizen이 가지는 것 / 못 가지는 것

**Citizen Has**:
- host `/run` 호출 가능 (인증 X — 본 PoC sandbox 한정).
- 임의 prompt + scenarioId 송신.
- 응답으로 돌아온 자연어 텍스트 수신.

**Citizen Lacks**:
- gateway·mcp-server·mock-backend의 네트워크 접근 (lab-internal 격리).
- X-API-Key 값 (host 컨테이너의 env에만 존재).
- mcp-server·mock-backend의 내부 데이터 모델·필드 정보.
- LLM provider·모델 type·temperature 정보.

→ Citizen은 **신뢰 가능한 caller**로 모델링됨. 자기 데이터를 자기 권한 안에서 요청할 뿐. RT-002의 본질은 "정직한 citizen이 의도하지 않게 zone 경계 침범의 trigger가 됨".

## 3. Layer별 attack surface

| Layer | 정상 동작 | 가능한 침범 |
|---|---|---|
| host (caller-facing) | prompt 수신 → Orchestrator | (sandbox 한정) auth 부재 → 임의 trigger. M3'에선 in scope. |
| host LLM | tool description 읽고 args 선택 | **B5**: description 안의 지시문을 추종 (PoisonedMockLlmClient 모델). 실제 LLM 시 description이 prompt context에 들어가 영향. |
| gateway | X-API-Key 헤더 검사 + 라우팅 | RT-001 (다른 환경): SSE-legacy 백엔드와의 flavor mismatch로 세션 격리 실패. 새 env는 Streamable HTTP만 사용해 회피. |
| mcp-server | X-API-Key + `allowedTools` ACL + tool 호출 핸들러 | description ↔ handler 바인딩 검증 부재. 핸들러는 description의 약속을 보장하지 않음. |
| mock-backend | `/stores/{id}` 응답 | **B4**: `/stores/{id}`가 `restricted` 플래그·caller identity 검사 없음 → IDOR. |
| Redis | ACL 정책 보관 | (out of scope) 직접 침범 시 권한 상승 가능하나 본 PoC 위협 아님. |

## 4. RT 매핑

| RT | Layer 침범 | 본 PoC env 상태 |
|---|---|---|
| **RT-001** (gateway flavor mismatch) | gateway × mcp-server transport flavor | 새 env에선 (i) Streamable HTTP만 사용 → 본 env에선 재현 안 함. 보고는 `mcp-pentest-lab` 측의 `docs/RT-001.md` 유지. |
| **RT-002** (description poisoning × IDOR) | B5 (host LLM) × B4 (mock-backend) | **Stage 1 PASS 2026-05-18**. 본 env의 first amplification PoC. 보고: `docs/RT-002.md`. |
| **RT-003** (cross-server exfil) | 페더레이션 시 second-server description × primary server context | 본 env엔 아직 미반영. M3'에서 두 번째 MCP server 추가 필요. 다음 라운드. |

## 5. Thesis 정리

**MCP × classical 증폭의 핵심**: 단일 layer에서는 막혀 있는 공격이 두 layer 간 *의미론적 신뢰 가정* 의 어긋남을 통해 도달 가능해진다.

- 단독 classical (IDOR): citizen이 lab-internal에 도달 불가 → 사용 못 함.
- 단독 MCP-layer (description poisoning): 핸들러가 description 약속과 다르게 동작 → caller가 의심.
- 결합: gateway가 정상 라우팅 + mcp-server가 정상 호출 + backend가 정상 반환 → caller가 정상 응답 형태로 restricted 데이터 수령. **각 layer 모두 자기 규칙대로 정상 동작**.

이게 *new* vector인 이유: layer 단위 보안 리뷰는 각자 통과시킴. 다중 layer 간 의미론적 가정 일치성을 따로 검사하지 않으면 발견되지 않음.

## 6. In scope / Out of scope

**In scope**:
- MCP-layer: tool description poisoning, false promise (α), `[SYSTEM CONTEXT]` injection (γ), cross-server orchestration (계획).
- Classical: IDOR, missing function-level authz, network-zone trust assumption.
- 검증: 두 stage (mock harness + real LLM).

**Out of scope (본 PoC에선 시연 안 함)**:
- Insider threat (운영자 공격).
- Sandbox auth bypass (host `/run`의 무인증 자체는 sandbox 단순화 한정).
- Real LLM provider API key 탈취 (Stage 2 변경 사항이지만 별 문제).
- Gateway·mcp-server·mock-backend의 메모리 손상·언어 런타임 익스플로잇.
- Redis 자체 침해.
- TLS·암호화 (M11 미적용).

## 7. Defense thesis (개요 — 상세는 `docs/BT-CANDIDATES.md`)

각 RT는 두 개 이상의 BT 후보로 단독 차단 가능. 결합 방어는 defense-in-depth.

- RT-002 단독 차단: BT-A (description 무결성) OR BT-B (host LLM 강화) OR BT-C (backend authz).
- 통합 방어: A+B (MCP-layer DiD) + C (cross-layer DiD).
