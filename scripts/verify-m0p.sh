#!/usr/bin/env bash
set -uo pipefail

PASS=0
FAIL=0

check_running() {
    local name="$1"
    local state
    state=$(docker inspect -f '{{.State.Status}}' "$name" 2>/dev/null || echo "missing")
    if [[ "$state" == "running" ]]; then
        echo "[PASS] container $name running"
        PASS=$((PASS+1))
    else
        echo "[FAIL] container $name state=$state"
        FAIL=$((FAIL+1))
    fi
}

check_health() {
    local name="$1"
    local url="$2"
    local status
    status=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    if [[ "$status" == "200" ]]; then
        echo "[PASS] $name health 200"
        PASS=$((PASS+1))
    else
        echo "[FAIL] $name health $status ($url)"
        FAIL=$((FAIL+1))
    fi
}

echo "verify-m0p.sh — M0' DoD checks (integrated stack)"
echo

echo "----- 5 containers running -----"
check_running mcp-lab-redis
check_running mcp-lab-mock-backend
check_running mcp-lab-mcp-server
check_running mcp-lab-gateway
check_running mcp-lab-host

echo
echo "----- health endpoints -----"
check_health mock-backend http://localhost:8083/actuator/health
check_health mcp-server http://localhost:8080/actuator/health
check_health host http://localhost:8085/actuator/health

echo
echo "----- E2E smoke through full chain (host → gateway → mcp-server → mock-backend) -----"
body=$(curl -s -X POST http://localhost:8085/run \
    -H "Content-Type: application/json" \
    -d '{"scenarioId":"smoke-storedetail","prompt":"내 시설 정보 보여줘"}')
if echo "$body" | grep -q "STORE-001"; then
    echo "[PASS] E2E: STORE-001 returned through full 4-hop chain"
    PASS=$((PASS+1))
else
    echo "[FAIL] E2E chain"
    echo "       body: $body"
    FAIL=$((FAIL+1))
fi

echo
echo "----------------------------------------"
echo "RESULT: $PASS/$((PASS+FAIL)) PASS"

if [[ $FAIL -eq 0 ]]; then
    echo "RESULT: M0' DoD MET"
    exit 0
else
    echo "RESULT: M0' DoD NOT MET"
    exit 1
fi
