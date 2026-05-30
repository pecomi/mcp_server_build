#!/usr/bin/env bash
set -uo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8081/mcp}"
FS_SERVER_URL="${FS_SERVER_URL:-http://localhost:8084}"
HOST_URL="${HOST_URL:-http://localhost:8085}"
API_KEY="${API_KEY:-local-redteam-key}"

PASS=0
FAIL=0

echo "verify-m6.sh — M6 DoD (fs-server + federation)"
echo "  GATEWAY_URL=$GATEWAY_URL"
echo "  FS_SERVER_URL=$FS_SERVER_URL"
echo "  HOST_URL=$HOST_URL"
echo

# 1. fs-server health UP
status=$(curl -s -o /dev/null -w "%{http_code}" "$FS_SERVER_URL/actuator/health" 2>/dev/null || echo "000")
if [[ "$status" == "200" ]]; then
    echo "[PASS] 1. fs-server health 200"
    PASS=$((PASS+1))
else
    echo "[FAIL] 1. fs-server health $status"
    FAIL=$((FAIL+1))
fi

# 2. gateway tools/list federation — both read_file and getStoreDetail present
SESSION_ID=$(curl -i -s -X POST "$GATEWAY_URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json, text/event-stream" \
    -H "MCP-Protocol-Version: 2025-06-18" \
    -H "X-API-Key: $API_KEY" \
    -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"verify-m6","version":"0.0.1"}}}' \
    | grep -i '^mcp-session-id:' | awk '{print $2}' | tr -d '\r')

if [[ -z "${SESSION_ID:-}" ]]; then
    echo "[FAIL] 2. could not obtain session id from gateway"
    FAIL=$((FAIL+1))
else
    tools_body=$(curl -s -X POST "$GATEWAY_URL" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json, text/event-stream" \
        -H "MCP-Protocol-Version: 2025-06-18" \
        -H "X-API-Key: $API_KEY" \
        -H "Mcp-Session-Id: $SESSION_ID" \
        -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}')

    has_readfile=0
    has_storedetail=0
    # agentgateway 1.0.1 federation prefixes tool names with <target>_, so accept either form.
    echo "$tools_body" | grep -qE 'read_file' && has_readfile=1
    echo "$tools_body" | grep -qE 'getStoreDetail' && has_storedetail=1

    if [[ $has_readfile -eq 1 && $has_storedetail -eq 1 ]]; then
        echo "[PASS] 2. gateway federation: tools/list has both read_file + getStoreDetail"
        PASS=$((PASS+1))
    else
        echo "[FAIL] 2. federation incomplete (read_file=$has_readfile getStoreDetail=$has_storedetail)"
        echo "       body: $tools_body"
        FAIL=$((FAIL+1))
    fi
fi

# 3. host /run smoke-readfile — full chain through gateway federation
body=$(curl -s -X POST "$HOST_URL/run" \
    -H "Content-Type: application/json" \
    -d '{"scenarioId":"smoke-readfile","prompt":"welcome 파일 읽어줘"}')
if echo "$body" | grep -q "hello from fs-server"; then
    echo "[PASS] 3. host smoke-readfile returns welcome.txt content"
    PASS=$((PASS+1))
else
    echo "[FAIL] 3. host smoke-readfile"
    echo "       body: $body"
    FAIL=$((FAIL+1))
fi

# 4. regression: smoke-storedetail still STORE-001 (federation didn't break eshare path)
body=$(curl -s -X POST "$HOST_URL/run" \
    -H "Content-Type: application/json" \
    -d '{"scenarioId":"smoke-storedetail","prompt":"내 시설 정보"}')
if echo "$body" | grep -q "STORE-001"; then
    echo "[PASS] 4. regression: smoke-storedetail still returns STORE-001"
    PASS=$((PASS+1))
else
    echo "[FAIL] 4. regression smoke-storedetail"
    echo "       body: $body"
    FAIL=$((FAIL+1))
fi

echo
echo "----------------------------------------"
echo "RESULT: $PASS/$((PASS+FAIL)) PASS"

if [[ $FAIL -eq 0 ]]; then
    echo "RESULT: M6 DoD MET"
    exit 0
else
    echo "RESULT: M6 DoD NOT MET"
    exit 1
fi
