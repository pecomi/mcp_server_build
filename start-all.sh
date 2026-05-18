#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

START_TIME=$SECONDS
MAX_WAIT="${MAX_WAIT:-180}"

echo "[1/3] docker compose up -d --build"
docker compose up -d --build

echo
echo "[2/3] seeding Redis API keys"
for i in $(seq 1 10); do
    if docker exec mcp-lab-redis redis-cli ping >/dev/null 2>&1; then
        break
    fi
    sleep 1
done

docker exec -i mcp-lab-redis redis-cli <<'EOF'
SET api-key:local-redteam-key '{"apiKey":"local-redteam-key","clientId":"local-redteam-client","status":"ACTIVE","allowedTools":["getStoreList","getStoreDetail"]}'
SET api-key:blocked-key '{"apiKey":"blocked-key","clientId":"blocked-client","status":"ACTIVE","allowedTools":[]}'
SET api-key:inactive-key '{"apiKey":"inactive-key","clientId":"inactive-client","status":"INACTIVE","allowedTools":["getStoreList","getStoreDetail"]}'
EOF

echo
echo "[3/3] waiting for stack to become healthy (max ${MAX_WAIT}s)"
WAITED=0
while [[ $WAITED -lt $MAX_WAIT ]]; do
    MOCK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8083/actuator/health 2>/dev/null || echo "000")
    MCP=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "000")
    FS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8084/actuator/health 2>/dev/null || echo "000")
    RES=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8086/actuator/health 2>/dev/null || echo "000")
    HOST=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8085/actuator/health 2>/dev/null || echo "000")
    SCAN=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8087/actuator/health 2>/dev/null || echo "000")
    PROM=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9090/-/healthy 2>/dev/null || echo "000")
    GRAF=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/api/health 2>/dev/null || echo "000")
    JAEG=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:16686/ 2>/dev/null || echo "000")
    OTEL=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:13133/ 2>/dev/null || echo "000")
    DEMO=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/actuator/health 2>/dev/null || echo "000")
    if [[ "$MOCK" == "200" && "$MCP" == "200" && "$FS" == "200" && "$RES" == "200" && "$HOST" == "200" && "$SCAN" == "200" && "$PROM" == "200" && "$GRAF" == "200" && "$JAEG" == "200" && "$OTEL" == "200" && "$DEMO" == "200" ]]; then
        ELAPSED=$((SECONDS - START_TIME))
        echo
        echo "Stack is up (mock=$MOCK mcp=$MCP fs=$FS research=$RES host=$HOST scanner=$SCAN prom=$PROM grafana=$GRAF jaeger=$JAEG otel=$OTEL demo=$DEMO) — total ${ELAPSED}s"
        echo
        docker compose ps
        exit 0
    fi
    printf '.'
    sleep 3
    WAITED=$((WAITED + 3))
done

echo
echo "Timed out after ${MAX_WAIT}s. Current state:"
docker compose ps
echo
echo "Tail last 30 lines of each service:"
for svc in mcp-lab-mcp-server mcp-lab-mock-backend mcp-lab-fs-server mcp-lab-research-server mcp-lab-scanner mcp-lab-host mcp-lab-gateway mcp-lab-redis mcp-lab-prometheus mcp-lab-grafana mcp-lab-jaeger mcp-lab-otel-collector mcp-lab-demo; do
    echo "--- $svc ---"
    docker logs --tail 30 "$svc" 2>&1 || true
    echo
done
exit 1
