#!/usr/bin/env bash
set -uo pipefail

HOST_URL="${HOST_URL:-http://localhost:8085}"
PASS=0
FAIL=0

run_case() {
    local name="$1"
    local expected_status="$2"
    local body_json="$3"
    local must_grep="$4"

    local tmp_body
    tmp_body=$(mktemp)
    local status
    status=$(curl -s -o "$tmp_body" -w "%{http_code}" -X POST "$HOST_URL/run" \
        -H "Content-Type: application/json" \
        -d "$body_json")
    local body
    body=$(cat "$tmp_body")
    rm -f "$tmp_body"

    local ok=1
    if [[ "$status" != "$expected_status" ]]; then
        ok=0
    fi
    if [[ -n "$must_grep" ]] && ! echo "$body" | grep -q -- "$must_grep"; then
        ok=0
    fi

    if [[ $ok -eq 1 ]]; then
        echo "[PASS] $name (status=$status)"
        PASS=$((PASS+1))
    else
        echo "[FAIL] $name (status=$status, expect=$expected_status, want='$must_grep')"
        echo "       body: $body"
        FAIL=$((FAIL+1))
    fi
}

echo "verify-m3p.sh — HOST_URL=$HOST_URL"
echo

run_case \
    "1. smoke-storedetail → host → mcp-server → mock-backend chain, STORE-001" \
    200 \
    '{"scenarioId":"smoke-storedetail","prompt":"내 시설 정보 보여줘"}' \
    "STORE-001"

run_case \
    "2. smoke-storelist → list chain, STORE-001 in list" \
    200 \
    '{"scenarioId":"smoke-storelist","prompt":"강남구 시설 알려줘"}' \
    "STORE-001"

run_case \
    "3. unknown scenario → 400 BAD_REQUEST" \
    400 \
    '{"scenarioId":"nonexistent-xxx","prompt":"test"}' \
    "BAD_REQUEST"

echo
echo "----------------------------------------"
echo "RESULT: $PASS/$((PASS+FAIL)) PASS"

if [[ $FAIL -eq 0 ]]; then
    echo "RESULT: M3' DoD MET"
    exit 0
else
    echo "RESULT: M3' DoD NOT MET"
    exit 1
fi
