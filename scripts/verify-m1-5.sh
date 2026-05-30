#!/usr/bin/env bash
set -uo pipefail

MOCK_BACKEND_URL="${MOCK_BACKEND_URL:-http://localhost:8083}"
MCP_SERVER_URL="${MCP_SERVER_URL:-http://localhost:8080/mcp}"
API_KEY="${API_KEY:-local-redteam-key}"
PASS=0
FAIL=0

run_case() {
    local name="$1"
    local expected_status="$2"
    local expected_grep="$3"
    local must_not_grep="$4"
    shift 4
    local url="$1"
    shift

    local tmp_body
    tmp_body=$(mktemp)
    local status
    status=$(curl -s -o "$tmp_body" -w "%{http_code}" "$url" "$@")
    local body
    body=$(cat "$tmp_body")
    rm -f "$tmp_body"

    local ok=1
    if [[ "$status" != "$expected_status" ]]; then
        ok=0
    fi
    if [[ -n "$expected_grep" ]] && ! echo "$body" | grep -q -- "$expected_grep"; then
        ok=0
    fi
    if [[ -n "$must_not_grep" ]] && echo "$body" | grep -q -- "$must_not_grep"; then
        ok=0
    fi

    if [[ $ok -eq 1 ]]; then
        echo "[PASS] $name (status=$status)"
        PASS=$((PASS+1))
    else
        echo "[FAIL] $name (status=$status, expect=$expected_status, want='$expected_grep', forbid='$must_not_grep')"
        echo "       body: $body"
        FAIL=$((FAIL+1))
    fi
}

echo "verify-m1-5.sh — MOCK_BACKEND_URL=$MOCK_BACKEND_URL MCP_SERVER_URL=$MCP_SERVER_URL"
echo

run_case \
    "1. list (no filter): 3 public stores, no restricted" \
    200 \
    "STORE-001" \
    "STORE-INTERNAL" \
    "$MOCK_BACKEND_URL/stores"

run_case \
    "2. list filter (sido=11, sigungu=강남구, free=Y): STORE-001+003 only" \
    200 \
    "STORE-003" \
    "STORE-002" \
    "$MOCK_BACKEND_URL/stores?sido=11&sigungu=%EA%B0%95%EB%82%A8%EA%B5%AC&searchFreeYn=Y"

run_case \
    "3. list also excludes STORE-DRAFT (restricted=true status=DRAFT)" \
    200 \
    "count" \
    "STORE-DRAFT" \
    "$MOCK_BACKEND_URL/stores"

echo
echo "----- E2E (mcp-server direct → mock-backend via Eshare HTTP adapter) -----"

SESSION_ID=$(curl -i -s -X POST "$MCP_SERVER_URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json, text/event-stream" \
    -H "MCP-Protocol-Version: 2025-06-18" \
    -H "X-API-Key: $API_KEY" \
    -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"verify-m1-5","version":"0.0.1"}}}' \
    | grep -i '^mcp-session-id:' \
    | awk '{print $2}' \
    | tr -d '\r')

if [[ -z "${SESSION_ID:-}" ]]; then
    echo "[FAIL] 4. could not obtain session id from mcp-server"
    FAIL=$((FAIL+1))
else
    echo "       SESSION_ID=$SESSION_ID"
    tmp_body=$(mktemp)
    status=$(curl -s -o "$tmp_body" -w "%{http_code}" -X POST "$MCP_SERVER_URL" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json, text/event-stream" \
        -H "MCP-Protocol-Version: 2025-06-18" \
        -H "X-API-Key: $API_KEY" \
        -H "Mcp-Session-Id: $SESSION_ID" \
        -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"getStoreList","arguments":{"page":1,"size":20,"consumerCd":"verify-m1-5","sido":"11","sigungu":"강남구","searchFreeYn":"Y"}}}')
    body=$(cat "$tmp_body")
    rm -f "$tmp_body"
    if [[ "$status" == "200" ]] && echo "$body" | grep -q "STORE-001"; then
        echo "[PASS] 4. tools/call getStoreList → EshareApiClient → mock-backend returns STORE-001 (status=$status)"
        PASS=$((PASS+1))
    else
        echo "[FAIL] 4. mcp-server tools/call (status=$status)"
        echo "       body: $body"
        FAIL=$((FAIL+1))
    fi
fi

echo
echo "----------------------------------------"
echo "RESULT: $PASS/$((PASS+FAIL)) PASS"

if [[ $FAIL -eq 0 ]]; then
    echo "RESULT: M1.5 DoD MET"
    exit 0
else
    echo "RESULT: M1.5 DoD NOT MET"
    exit 1
fi
