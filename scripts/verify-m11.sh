#!/usr/bin/env bash
set -uo pipefail

PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"
PASS=0
FAIL=0

echo "verify-m11.sh — M11 DoD (Prometheus + Grafana metrics MVP)"
echo "  PROMETHEUS_URL=$PROMETHEUS_URL"
echo "  GRAFANA_URL=$GRAFANA_URL"
echo

# 1. prometheus self-health
status=$(curl -s -o /dev/null -w "%{http_code}" "$PROMETHEUS_URL/-/healthy" 2>/dev/null || echo "000")
if [[ "$status" == "200" ]]; then
    echo "[PASS] 1. prometheus self-healthy"
    PASS=$((PASS+1))
else
    echo "[FAIL] 1. prometheus /-/healthy $status"
    FAIL=$((FAIL+1))
fi

# 2. grafana health
status=$(curl -s -o /dev/null -w "%{http_code}" "$GRAFANA_URL/api/health" 2>/dev/null || echo "000")
if [[ "$status" == "200" ]]; then
    echo "[PASS] 2. grafana health OK"
    PASS=$((PASS+1))
else
    echo "[FAIL] 2. grafana /api/health $status"
    FAIL=$((FAIL+1))
fi

# 3. prometheus targets — all 6 app jobs UP
expected_jobs=(mock-backend mcp-server fs-server research-server host scanner)
all_up=1
for job in "${expected_jobs[@]}"; do
    q=$(curl -s --data-urlencode "query=up{job=\"$job\"}" "$PROMETHEUS_URL/api/v1/query" 2>/dev/null)
    if echo "$q" | grep -q "\"job\":\"$job\"" && echo "$q" | grep -oE '"value":\[[0-9.]+,"1"\]' | head -1 | grep -q "1"; then
        :
    else
        echo "       job=$job up!=1"
        all_up=0
    fi
done
if [[ $all_up -eq 1 ]]; then
    echo "[PASS] 3. all 6 app scrape jobs report up=1"
    PASS=$((PASS+1))
else
    echo "[FAIL] 3. some scrape jobs not up"
    FAIL=$((FAIL+1))
fi

# 4. prometheus has actual JVM metric data from at least one app
query=$(curl -s "$PROMETHEUS_URL/api/v1/query?query=jvm_threads_live_threads" 2>/dev/null)
if echo "$query" | grep -q '"status":"success"' && echo "$query" | grep -q '"value"'; then
    echo "[PASS] 4. prometheus jvm_threads_live_threads has data"
    PASS=$((PASS+1))
else
    echo "[FAIL] 4. prometheus query returned no data"
    echo "       body: $(echo "$query" | head -c 200)"
    FAIL=$((FAIL+1))
fi

# 5. grafana datasource API: Prometheus provisioned
ds=$(curl -s -u admin:admin "$GRAFANA_URL/api/datasources" 2>/dev/null)
if echo "$ds" | grep -q '"type":"prometheus"' && echo "$ds" | grep -q '"isDefault":true'; then
    echo "[PASS] 5. grafana has Prometheus default datasource"
    PASS=$((PASS+1))
else
    echo "[FAIL] 5. grafana datasource missing or not default"
    echo "       body: $(echo "$ds" | head -c 200)"
    FAIL=$((FAIL+1))
fi

echo
echo "----------------------------------------"
echo "RESULT: $PASS/$((PASS+FAIL)) PASS"

if [[ $FAIL -eq 0 ]]; then
    echo "RESULT: M11 DoD MET"
    exit 0
else
    echo "RESULT: M11 DoD NOT MET"
    exit 1
fi
