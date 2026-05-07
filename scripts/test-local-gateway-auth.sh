#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://localhost:8081/mcp"

echo "[1] no-key initialize: expect 401"
curl -s -i -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"no-key-test","version":"0.0.1"}}}' \
  | head -n 10

echo
echo "[2] inactive-key initialize: expect 401"
curl -s -i -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -H "X-API-Key: inactive-key" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"inactive-test","version":"0.0.1"}}}' \
  | head -n 10

echo
echo "[3] local-redteam-key initialize: expect 200 and session"
SESSION_ID=$(curl -i -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -H "X-API-Key: local-redteam-key" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"session-test","version":"0.0.1"}}}' \
  | grep -i '^mcp-session-id:' \
  | awk '{print $2}' \
  | tr -d '\r')

echo "SESSION_ID=$SESSION_ID"

echo
echo "[4] local-redteam-key tools/call: expect 200"
curl -s -i -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -H "X-API-Key: local-redteam-key" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"getStoreList","arguments":{"page":1,"size":20,"consumerCd":"local-test","sido":"11","sigungu":"강남구","searchFreeYn":"Y","searchSbclsCd":"011000","searchMnclsCd":null}}}' \
  | head -n 20

echo
echo "[5] blocked-key tools/call: expect 403"
BLOCKED_SESSION_ID=$(curl -i -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -H "X-API-Key: blocked-key" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"blocked-session-test","version":"0.0.1"}}}' \
  | grep -i '^mcp-session-id:' \
  | awk '{print $2}' \
  | tr -d '\r')

curl -s -i -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -H "X-API-Key: blocked-key" \
  -H "Mcp-Session-Id: $BLOCKED_SESSION_ID" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"getStoreList","arguments":{"page":1,"size":20,"consumerCd":"local-test"}}}' \
  | head -n 20