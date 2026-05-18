#!/usr/bin/env bash
set -uo pipefail

SCANNER_URL="${SCANNER_URL:-http://localhost:8087}"
PASS=0
FAIL=0

echo "verify-m9.sh — M9 DoD (scanner MVP — DESC_INJECT + ARG_NO_PATTERN)"
echo "  SCANNER_URL=$SCANNER_URL"
echo

# 1. scanner health
status=$(curl -s -o /dev/null -w "%{http_code}" "$SCANNER_URL/actuator/health" 2>/dev/null || echo "000")
if [[ "$status" == "200" ]]; then
    echo "[PASS] 1. scanner health 200"
    PASS=$((PASS+1))
else
    echo "[FAIL] 1. scanner health $status"
    FAIL=$((FAIL+1))
fi

# 2. POST /scan returns non-empty findings (baseline state: ARG_NO_PATTERN should fire)
body=$(curl -s -X POST "$SCANNER_URL/scan" \
    -H "Content-Type: application/json" \
    -d '{}')
if echo "$body" | grep -q '"scannedTools"' && echo "$body" | grep -q '"findings"'; then
    echo "[PASS] 2. /scan returns valid ScanResponse"
    PASS=$((PASS+1))
else
    echo "[FAIL] 2. /scan response malformed"
    echo "       body: $body"
    FAIL=$((FAIL+1))
fi

# 3. ARG_NO_PATTERN finds at least read_file.path + getStoreDetail.store_id
body=$(curl -s -X POST "$SCANNER_URL/scan" \
    -H "Content-Type: application/json" \
    -d '{}')
has_readfile_path=0
has_storedetail_id=0
echo "$body" | grep -q 'read_file.*ARG_NO_PATTERN' && has_readfile_path=1
echo "$body" | grep -q 'getStoreDetail.*ARG_NO_PATTERN' && has_storedetail_id=1
# fallback: also accept ARG_NO_PATTERN with read_file or getStoreDetail in any order via tool name
if [[ $has_readfile_path -eq 0 ]]; then
    echo "$body" | grep -E '"tool":"[^"]*read_file[^"]*"[^}]*"rule":"ARG_NO_PATTERN"' >/dev/null && has_readfile_path=1
fi
if [[ $has_storedetail_id -eq 0 ]]; then
    echo "$body" | grep -E '"tool":"[^"]*getStoreDetail[^"]*"[^}]*"rule":"ARG_NO_PATTERN"' >/dev/null && has_storedetail_id=1
fi

if [[ $has_readfile_path -eq 1 && $has_storedetail_id -eq 1 ]]; then
    echo "[PASS] 3. ARG_NO_PATTERN detects read_file.path + getStoreDetail.store_id"
    PASS=$((PASS+1))
else
    echo "[FAIL] 3. ARG_NO_PATTERN coverage incomplete (read_file=$has_readfile_path getStoreDetail=$has_storedetail_id)"
    echo "       body: $body"
    FAIL=$((FAIL+1))
fi

# 4. baseline (honest descriptions): DESC_INJECT should NOT fire on default descriptions
body=$(curl -s -X POST "$SCANNER_URL/scan" \
    -H "Content-Type: application/json" \
    -d '{}')
if echo "$body" | grep -q '"rule":"DESC_INJECT"'; then
    echo "[FAIL] 4. unexpected DESC_INJECT hit in baseline (descriptions should be honest)"
    echo "       body: $body"
    FAIL=$((FAIL+1))
else
    echo "[PASS] 4. baseline DESC_INJECT clean (no false positives)"
    PASS=$((PASS+1))
fi

echo
echo "----------------------------------------"
echo "RESULT: $PASS/$((PASS+FAIL)) PASS"

if [[ $FAIL -eq 0 ]]; then
    echo "RESULT: M9 DoD MET"
    exit 0
else
    echo "RESULT: M9 DoD NOT MET"
    exit 1
fi
