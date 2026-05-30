#!/usr/bin/env bash
set -uo pipefail

OTEL_HEALTH_URL="${OTEL_HEALTH_URL:-http://localhost:13133}"
JAEGER_URL="${JAEGER_URL:-http://localhost:16686}"
HOST_URL="${HOST_URL:-http://localhost:8085}"
PASS=0
FAIL=0

echo "verify-m11-5.sh — M11.5 DoD (Tracing — OTel Collector + Jaeger)"
echo "  OTEL_HEALTH_URL=$OTEL_HEALTH_URL"
echo "  JAEGER_URL=$JAEGER_URL"
echo "  HOST_URL=$HOST_URL"
echo

# 1. otel-collector health
status=$(curl -s -o /dev/null -w "%{http_code}" "$OTEL_HEALTH_URL/" 2>/dev/null || echo "000")
if [[ "$status" == "200" ]]; then
    echo "[PASS] 1. otel-collector health 200"
    PASS=$((PASS+1))
else
    echo "[FAIL] 1. otel-collector health $status"
    FAIL=$((FAIL+1))
fi

# 2. jaeger services API responds
ds=$(curl -s -o /tmp/m115-ds -w "%{http_code}" "$JAEGER_URL/api/services" 2>/dev/null || echo "000")
if [[ "$ds" == "200" ]]; then
    echo "[PASS] 2. jaeger /api/services responds 200"
    PASS=$((PASS+1))
else
    echo "[FAIL] 2. jaeger /api/services $ds"
    FAIL=$((FAIL+1))
fi

# Generate spans by triggering one /run scenario (host → gateway → mcp-server → mock-backend)
echo
echo "[setup] triggering POST /run smoke-storedetail to generate trace spans"
curl -s -X POST "$HOST_URL/run" \
    -H "Content-Type: application/json" \
    -d '{"scenarioId":"smoke-storedetail","prompt":"verify-m11-5 trace test"}' > /dev/null

# Wait for batch exporter to flush + jaeger to ingest
echo "[setup] waiting 12s for batch export + jaeger ingest"
sleep 12

# 3. jaeger services list includes our app(s)
services_body=$(curl -s "$JAEGER_URL/api/services" 2>/dev/null)
expected_services=(mcp-lab-host)
found_count=0
for svc in "${expected_services[@]}"; do
    if echo "$services_body" | grep -q "\"$svc\""; then
        found_count=$((found_count+1))
    fi
done
if [[ $found_count -ge 1 ]]; then
    echo "[PASS] 3. jaeger has registered service(s): mcp-lab-host"
    PASS=$((PASS+1))
else
    echo "[FAIL] 3. expected services missing from jaeger"
    echo "       services body: $services_body"
    FAIL=$((FAIL+1))
fi

# 4. traces present for mcp-lab-host
traces_body=$(curl -s "$JAEGER_URL/api/traces?service=mcp-lab-host&limit=20" 2>/dev/null)
trace_count=$(echo "$traces_body" | grep -oE '"traceID":"[^"]+"' | wc -l)
if [[ $trace_count -ge 1 ]]; then
    echo "[PASS] 4. jaeger has $trace_count trace(s) for mcp-lab-host"
    PASS=$((PASS+1))
else
    echo "[FAIL] 4. no traces in jaeger for mcp-lab-host"
    echo "       traces body (first 300): $(echo "$traces_body" | head -c 300)"
    FAIL=$((FAIL+1))
fi

rm -f /tmp/m115-ds

echo
echo "----------------------------------------"
echo "RESULT: $PASS/$((PASS+FAIL)) PASS"

if [[ $FAIL -eq 0 ]]; then
    echo "RESULT: M11.5 DoD MET"
    exit 0
else
    echo "RESULT: M11.5 DoD NOT MET"
    exit 1
fi
