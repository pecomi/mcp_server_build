#!/usr/bin/env bash
set -uo pipefail

HOST_URL="${HOST_URL:-http://localhost:8085}"
NETWORK="${NETWORK:-mcp-lab-net}"

if [[ -z "${ANTHROPIC_API_KEY:-}" ]]; then
    echo "verify-m12.sh — M12 DoD (real Anthropic LLM)"
    echo
    echo "[SKIP] ANTHROPIC_API_KEY not set in environment."
    echo "       Export ANTHROPIC_API_KEY=sk-ant-... and re-run to actually verify M12."
    echo
    echo "RESULT: M12 SKIPPED (no API key)"
    exit 0
fi

ANTHROPIC_MODEL="${ANTHROPIC_MODEL:-claude-sonnet-4-5}"
PASS=0
FAIL=0

echo "verify-m12.sh — M12 DoD (real Anthropic LLM)"
echo "  HOST_URL=$HOST_URL"
echo "  ANTHROPIC_MODEL=$ANTHROPIC_MODEL"
echo "  (ANTHROPIC_API_KEY is set, length=${#ANTHROPIC_API_KEY})"
echo

echo "[setup] restart host with LLM_MODE=real_deterministic"
docker rm -f mcp-lab-host >/dev/null 2>&1 || true
docker run -d \
    --name mcp-lab-host \
    --network "$NETWORK" \
    -p 8085:8085 \
    -e LLM_MODE=real_deterministic \
    -e ANTHROPIC_API_KEY="$ANTHROPIC_API_KEY" \
    -e ANTHROPIC_MODEL="$ANTHROPIC_MODEL" \
    -e MCP_SERVER_URL=http://mcp-lab-gateway:8081/mcp \
    -e MCP_API_KEY=local-redteam-key \
    mcp-lab-host:local >/dev/null
sleep 16

# 1. host health
status=$(curl -s -o /dev/null -w "%{http_code}" "$HOST_URL/actuator/health" 2>/dev/null || echo "000")
if [[ "$status" == "200" ]]; then
    echo "[PASS] 1. host health 200 (real mode wired)"
    PASS=$((PASS+1))
else
    echo "[FAIL] 1. host health $status"
    FAIL=$((FAIL+1))
fi

# 2. real LLM picks a sensible tool for store-related prompt
body=$(curl -s -X POST "$HOST_URL/run" \
    -H "Content-Type: application/json" \
    -d '{"scenarioId":"real-smoke-store","prompt":"공유누리에서 강남구 시설 목록 보여줘"}')
if echo "$body" | grep -q '"toolCalls"' && echo "$body" | grep -qE 'getStoreList|getStoreDetail'; then
    echo "[PASS] 2. real LLM dispatched a getStore* tool"
    PASS=$((PASS+1))
else
    echo "[FAIL] 2. unexpected response (real LLM did not dispatch getStore* tool)"
    echo "       body (first 600): $(echo "$body" | head -c 600)"
    FAIL=$((FAIL+1))
fi

# 3. real LLM returns valid finalText
body=$(curl -s -X POST "$HOST_URL/run" \
    -H "Content-Type: application/json" \
    -d '{"scenarioId":"real-smoke-store-2","prompt":"STORE-001 시설 상세 알려줘"}')
if echo "$body" | grep -q "STORE-001" && echo "$body" | grep -q '"llmMode":"real_deterministic"'; then
    echo "[PASS] 3. real LLM returned response with STORE-001 + llmMode=real_deterministic"
    PASS=$((PASS+1))
else
    echo "[FAIL] 3. response missing STORE-001 or llmMode"
    echo "       body (first 600): $(echo "$body" | head -c 600)"
    FAIL=$((FAIL+1))
fi

echo
echo "----------------------------------------"
echo "RESULT: $PASS/$((PASS+FAIL)) PASS"

if [[ $FAIL -eq 0 ]]; then
    echo "RESULT: M12 DoD MET"
    EXIT=0
else
    echo "RESULT: M12 DoD NOT MET"
    EXIT=1
fi

echo
echo "[cleanup] restoring compose-managed host (mock mode)"
docker compose up -d --force-recreate host >/dev/null 2>&1 || true
sleep 10

exit $EXIT
