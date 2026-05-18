#!/usr/bin/env bash
set -uo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8081/mcp}"
RESEARCH_SERVER_URL="${RESEARCH_SERVER_URL:-http://localhost:8086}"
HOST_URL="${HOST_URL:-http://localhost:8085}"
API_KEY="${API_KEY:-local-redteam-key}"

PASS=0
FAIL=0

echo "verify-m7.sh — M7 DoD (research-server + 3-target federation)"
echo "  GATEWAY_URL=$GATEWAY_URL"
echo "  RESEARCH_SERVER_URL=$RESEARCH_SERVER_URL"
echo "  HOST_URL=$HOST_URL"
echo

# 1. research-server health
status=$(curl -s -o /dev/null -w "%{http_code}" "$RESEARCH_SERVER_URL/actuator/health" 2>/dev/null || echo "000")
if [[ "$status" == "200" ]]; then
    echo "[PASS] 1. research-server health 200"
    PASS=$((PASS+1))
else
    echo "[FAIL] 1. research-server health $status"
    FAIL=$((FAIL+1))
fi

# 2. gateway tools/list federation: all 3 tools present
SESSION_ID=$(curl -i -s -X POST "$GATEWAY_URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json, text/event-stream" \
    -H "MCP-Protocol-Version: 2025-06-18" \
    -H "X-API-Key: $API_KEY" \
    -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"verify-m7","version":"0.0.1"}}}' \
    | grep -i '^mcp-session-id:' | awk '{print $2}' | tr -d '\r')

if [[ -z "${SESSION_ID:-}" ]]; then
    echo "[FAIL] 2. could not obtain session id"
    FAIL=$((FAIL+1))
else
    tools_body=$(curl -s -X POST "$GATEWAY_URL" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json, text/event-stream" \
        -H "MCP-Protocol-Version: 2025-06-18" \
        -H "X-API-Key: $API_KEY" \
        -H "Mcp-Session-Id: $SESSION_ID" \
        -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}')

    has_lookup=0
    has_readfile=0
    has_storedetail=0
    echo "$tools_body" | grep -q "lookup_term" && has_lookup=1
    echo "$tools_body" | grep -q "read_file" && has_readfile=1
    echo "$tools_body" | grep -q "getStoreDetail" && has_storedetail=1

    if [[ $has_lookup -eq 1 && $has_readfile -eq 1 && $has_storedetail -eq 1 ]]; then
        echo "[PASS] 2. 3-target federation: lookup_term + read_file + getStoreDetail"
        PASS=$((PASS+1))
    else
        echo "[FAIL] 2. federation incomplete (lookup=$has_lookup readfile=$has_readfile storedetail=$has_storedetail)"
        echo "       body: $tools_body"
        FAIL=$((FAIL+1))
    fi
fi

# 3. host /run smoke-lookup
body=$(curl -s -X POST "$HOST_URL/run" \
    -H "Content-Type: application/json" \
    -d '{"scenarioId":"smoke-lookup","prompt":"MCP가 뭐야?"}')
if echo "$body" | grep -q "정의" && echo "$body" | grep -q "MCP"; then
    echo "[PASS] 3. host smoke-lookup returns dictionary-style response"
    PASS=$((PASS+1))
else
    echo "[FAIL] 3. host smoke-lookup"
    echo "       body: $body"
    FAIL=$((FAIL+1))
fi

# 4. regression: smoke-readfile + smoke-storedetail still work
body=$(curl -s -X POST "$HOST_URL/run" \
    -H "Content-Type: application/json" \
    -d '{"scenarioId":"smoke-readfile","prompt":"welcome 파일"}')
ok_readfile=0
echo "$body" | grep -q "hello from fs-server" && ok_readfile=1

body=$(curl -s -X POST "$HOST_URL/run" \
    -H "Content-Type: application/json" \
    -d '{"scenarioId":"smoke-storedetail","prompt":"내 시설 정보"}')
ok_storedetail=0
echo "$body" | grep -q "STORE-001" && ok_storedetail=1

if [[ $ok_readfile -eq 1 && $ok_storedetail -eq 1 ]]; then
    echo "[PASS] 4. regression: smoke-readfile + smoke-storedetail both PASS"
    PASS=$((PASS+1))
else
    echo "[FAIL] 4. regression (readfile=$ok_readfile storedetail=$ok_storedetail)"
    FAIL=$((FAIL+1))
fi

echo
echo "----------------------------------------"
echo "RESULT: $PASS/$((PASS+FAIL)) PASS"

if [[ $FAIL -eq 0 ]]; then
    echo "RESULT: M7 DoD MET"
    exit 0
else
    echo "RESULT: M7 DoD NOT MET"
    exit 1
fi
