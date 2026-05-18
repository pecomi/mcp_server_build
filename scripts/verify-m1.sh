#!/usr/bin/env bash
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8083}"
PASS=0
FAIL=0

run_case() {
    local name="$1"
    local expected_status="$2"
    local expected_grep="$3"
    local url="$4"
    shift 4

    local tmp_body
    tmp_body=$(mktemp)
    local status
    status=$(curl -s -o "$tmp_body" -w "%{http_code}" "$url" "$@")

    local body
    body=$(cat "$tmp_body")
    rm -f "$tmp_body"

    if [[ "$status" == "$expected_status" ]] && echo "$body" | grep -q -- "$expected_grep"; then
        echo "[PASS] $name (status=$status)"
        PASS=$((PASS+1))
    else
        echo "[FAIL] $name (status=$status, expected=$expected_status, grep='$expected_grep')"
        echo "       body: $body"
        FAIL=$((FAIL+1))
    fi
}

echo "verify-m1.sh — BASE_URL=$BASE_URL"
echo

run_case \
    "1. unauth public store" \
    200 \
    "STORE-001" \
    "$BASE_URL/stores/STORE-001"

run_case \
    "2. unauth restricted store (IDOR sink)" \
    200 \
    "VIP 의전용" \
    "$BASE_URL/stores/STORE-INTERNAL-001"

run_case \
    "3. secure missing Authorization" \
    401 \
    "missing_auth" \
    "$BASE_URL/secure/stores/STORE-INTERNAL-001"

run_case \
    "4. secure wrong bearer" \
    401 \
    "invalid_token" \
    "$BASE_URL/secure/stores/STORE-INTERNAL-001" \
    -H "Authorization: Bearer wrong-token"

run_case \
    "5. secure valid bearer" \
    200 \
    "STORE-INTERNAL-001" \
    "$BASE_URL/secure/stores/STORE-INTERNAL-001" \
    -H "Authorization: Bearer secret-token-abc"

echo
echo "----------------------------------------"
echo "RESULT: $PASS/$((PASS+FAIL)) PASS"

if [[ $FAIL -eq 0 ]]; then
    echo "RESULT: M1 DoD MET"
    exit 0
else
    echo "RESULT: M1 DoD NOT MET"
    exit 1
fi
