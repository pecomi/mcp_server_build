#!/usr/bin/env bash
# verify-all.sh — master end-to-end verification for the whole MCP Lab.
#
# Brings up the full compose stack, then runs every milestone + red-team (RT)
# harness in dependency order, capturing PASS/FAIL per stage. Designed for a
# reviewer who just cloned the repo and wants one command + one verdict.
#
#   bash scripts/verify-all.sh                 # full run
#   FAST=1 bash scripts/verify-all.sh          # skip slow infra checks (m11/m11.5)
#   SKIP_BOOT=1 bash scripts/verify-all.sh     # assume stack already up
#
# Requirements: docker, docker compose, curl, bash. Run from repo root or scripts/.
# Each sub-script self-heals the containers it needs; this wrapper just sequences
# them and aggregates the verdicts.
set -uo pipefail

cd "$(dirname "$0")/.."   # repo root
ROOT="$(pwd)"

FAST="${FAST:-0}"
SKIP_BOOT="${SKIP_BOOT:-0}"

GREEN=$'\033[32m'; RED=$'\033[31m'; YEL=$'\033[33m'; DIM=$'\033[2m'; RST=$'\033[0m'

TOTAL=0; OK=0
declare -a RESULTS

run_stage() {
    local label="$1"; shift
    TOTAL=$((TOTAL+1))
    echo
    echo "${YEL}══════════════════════════════════════════════════════════════${RST}"
    echo "${YEL}▶ [$TOTAL] $label${RST}"
    echo "${DIM}  \$ $*${RST}"
    echo "${YEL}══════════════════════════════════════════════════════════════${RST}"
    if "$@"; then
        echo "${GREEN}✔ $label${RST}"
        RESULTS+=("${GREEN}PASS${RST}  $label")
        OK=$((OK+1))
    else
        local rc=$?
        echo "${RED}x $label (exit $rc)${RST}"
        RESULTS+=("${RED}FAIL${RST}  $label")
    fi
}

echo "MCP Lab — master verification"
echo "  root=$ROOT  FAST=$FAST  SKIP_BOOT=$SKIP_BOOT"
echo "  $(docker --version 2>/dev/null || echo 'docker: NOT FOUND')"

# 0. Boot the integrated stack ------------------------------------------------
if [[ "$SKIP_BOOT" != "1" ]]; then
    run_stage "Boot full stack (start-all.sh)" bash start-all.sh
else
    echo "${DIM}[skip] SKIP_BOOT=1 — assuming stack already running${RST}"
fi

# 1. Integrated DoD: 5 containers + health + E2E smoke ------------------------
run_stage "M0' integrated stack DoD (verify-m0p.sh)" bash scripts/verify-m0p.sh

# 2. Per-server tool surfaces -------------------------------------------------
run_stage "M6  fs-server read_file sink (verify-m6.sh)"          bash scripts/verify-m6.sh
run_stage "M7  research-server lookup_term (verify-m7.sh)"       bash scripts/verify-m7.sh
run_stage "M9  scanner DESC_INJECT/ARG_NO_PATTERN (verify-m9.sh)" bash scripts/verify-m9.sh

# 3. Red-team scenarios (the actual PoCs) -------------------------------------
run_stage "RT-002 description poisoning × IDOR (rt-002-stage1.sh)"      bash scripts/rt-002-stage1.sh
run_stage "RT-003 cross-server desc injection × /etc/passwd (rt-003-stage1.sh)" bash scripts/rt-003-stage1.sh
run_stage "RT-004 tool-output injection × PII sink (rt-004-stage1.sh)"  bash scripts/rt-004-stage1.sh
run_stage "RT-006 tool-output prompt injection (rt-006-stage1.sh)"      bash scripts/rt-006-stage1.sh

# 4. Blue-team mitigation -----------------------------------------------------
run_stage "BT-001 backend authz on restricted rows (verify-bt-001.sh)" bash scripts/verify-bt-001.sh

# 5. Observability (slow; skip with FAST=1) -----------------------------------
if [[ "$FAST" != "1" ]]; then
    run_stage "M11  Prometheus + Grafana (verify-m11.sh)"     bash scripts/verify-m11.sh
    run_stage "M11.5 OTel + Jaeger tracing (verify-m11-5.sh)" bash scripts/verify-m11-5.sh
else
    echo "${DIM}[skip] FAST=1 — skipping M11 / M11.5 observability checks${RST}"
fi

# Summary ---------------------------------------------------------------------
echo
echo "════════════════════════════════════════════════════════════════"
echo " VERIFY-ALL SUMMARY"
echo "════════════════════════════════════════════════════════════════"
for line in "${RESULTS[@]}"; do echo "  $line"; done
echo "----------------------------------------------------------------"
echo "  $OK / $TOTAL stages PASS"
echo
echo "  UIs: demo :8090 · Prometheus :9090 · Grafana :3000 (admin/admin) · Jaeger :16686"
echo "════════════════════════════════════════════════════════════════"

if [[ "$OK" == "$TOTAL" ]]; then
    echo "${GREEN}RESULT: ALL STAGES PASS${RST}"
    exit 0
else
    echo "${RED}RESULT: $((TOTAL-OK)) STAGE(S) FAILED${RST}"
    exit 1
fi
