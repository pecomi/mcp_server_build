#!/usr/bin/env bash
set -uo pipefail

MCP_SERVER_URL="${MCP_SERVER_URL:-http://localhost:8080/mcp}"
API_KEY="${API_KEY:-local-redteam-key}"
PASS=0
FAIL=0

echo "verify-m2-5.sh — MCP_SERVER_URL=$MCP_SERVER_URL API_KEY=$API_KEY"
echo

init_session() {
    curl -i -s -X POST "$MCP_SERVER_URL" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json, text/event-stream" \
        -H "MCP-Protocol-Version: 2025-06-18" \
        -H "X-API-Key: $API_KEY" \
        -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"verify-m2-5","version":"0.0.1"}}}' \
        | grep -i '^mcp-session-id:' \
        | awk '{print $2}' \
        | tr -d '\r'
}

call_get_store_detail() {
    local session_id="$1"
    local store_id="$2"
    local req_id="$3"
    curl -s -X POST "$MCP_SERVER_URL" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json, text/event-stream" \
        -H "MCP-Protocol-Version: 2025-06-18" \
        -H "X-API-Key: $API_KEY" \
        -H "Mcp-Session-Id: $session_id" \
        -d "{\"jsonrpc\":\"2.0\",\"id\":$req_id,\"method\":\"tools/call\",\"params\":{\"name\":\"getStoreDetail\",\"arguments\":{\"store_id\":\"$store_id\"}}}"
}

run_case() {
    local name="$1"
    local store_id="$2"
    local req_id="$3"
    local must_grep="$4"
    local must_not_grep="$5"
    local want_iserror="$6"

    body=$(call_get_store_detail "$SESSION_ID" "$store_id" "$req_id")

    local ok=1
    if [[ -n "$must_grep" ]] && ! echo "$body" | grep -q -- "$must_grep"; then
        ok=0
    fi
    if [[ -n "$must_not_grep" ]] && echo "$body" | grep -q -- "$must_not_grep"; then
        ok=0
    fi
    if [[ "$want_iserror" == "true" ]] && ! echo "$body" | grep -q '"isError":true'; then
        ok=0
    fi
    if [[ "$want_iserror" == "false" ]] && echo "$body" | grep -q '"isError":true'; then
        ok=0
    fi

    if [[ $ok -eq 1 ]]; then
        echo "[PASS] $name"
        PASS=$((PASS+1))
    else
        echo "[FAIL] $name"
        echo "       body: $body"
        FAIL=$((FAIL+1))
    fi
}

SESSION_ID=$(init_session)
if [[ -z "${SESSION_ID:-}" ]]; then
    echo "[FATAL] could not obtain session id from mcp-server. Is mcp-server up + API key valid?"
    exit 1
fi
echo "SESSION_ID=$SESSION_ID"
echo

run_case \
    "1. getStoreDetail(STORE-001) → public store returned" \
    "STORE-001" 2 \
    "STORE-001" \
    "" \
    "false"

run_case \
    "2. getStoreDetail(STORE-INTERNAL-001) → RESTRICTED leaked via tool (IDOR sink reached)" \
    "STORE-INTERNAL-001" 3 \
    "VIP 의전용" \
    "" \
    "false"

run_case \
    "3. getStoreDetail(STORE-DRAFT-001) → DRAFT entry returned" \
    "STORE-DRAFT-001" 4 \
    "DRAFT" \
    "" \
    "false"

run_case \
    "4. getStoreDetail(STORE-NOEXIST-999) → backend 404 → tool surface isError:true" \
    "STORE-NOEXIST-999" 5 \
    "" \
    "VIP" \
    "true"

echo
echo "----------------------------------------"
echo "RESULT: $PASS/$((PASS+FAIL)) PASS"

if [[ $FAIL -eq 0 ]]; then
    echo "RESULT: M2.5 DoD MET"
    exit 0
else
    echo "RESULT: M2.5 DoD NOT MET"
    exit 1
fi
