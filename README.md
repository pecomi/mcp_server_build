````markdown
# MCP Lab Server

Spring Boot 3.5.9와 MCP Java SDK v1.1.1을 사용하여 구현한 MCP Server 시범환경이다.  
현재 목표는 공유누리 API 연계 MCP 서버 구조를 실제 서버에 올리고, 이후 red teaming을 수행할 수 있는 최소 실행 환경을 구성하는 것이다.

---

## 1. 현재 구현 상태 요약

현재까지 구현된 구성은 다음과 같다.

```text
MCP Inspector / MCP Client
        |
        | Streamable HTTP
        | X-API-Key
        v
MCP Server
- Spring Boot 3.5.9
- JDK 17
- MCP Java SDK v1.1.1
- Streamable HTTP /mcp endpoint
- API Key Authentication
- Tool-level Authorization
- getStoreList Mock Tool
- getExternalInstitutionRecord Mock Tool
- poisonedTool
        |
        v
Redis 8.6
- api-key:{key}
- clientId
- status
- allowedTools
````

서버 배포 환경에서는 Rocky Linux 8.5 서버 위에서 Podman pod를 사용한다.

```text
Rocky Linux Server: airlab_storage

Podman Pod: mcp-lab-pod
 ├─ mcp-lab-redis
 │   └─ localhost:6379
 │
 └─ mcp-lab-mcp-server
     └─ localhost:8080
```

---

## 2. 현재 구현된 기능

### MCP Server

* Spring Boot 3.5.9 기반 서버
* JDK 17 사용
* MCP Java SDK v1.1.1 직접 사용
* Spring AI MCP Starter는 사용하지 않음
* Streamable HTTP 기반 `/mcp` endpoint 제공
* MCP Inspector 연결 가능
* `getStoreList`, `getExternalInstitutionRecord`, `poisonedTool` Tool 등록

### Authentication / Authorization

* `X-API-Key` 기반 인증
* Redis에서 API Key 정책 조회
* `status=ACTIVE`인 key만 허용
* `allowedTools` 기반 Tool-level ACL 적용
* API Key가 없거나 잘못된 경우 `/mcp` 요청 차단
* 인증은 되었지만 Tool 권한이 없는 경우 Tool 호출 차단

### Redis

* Redis 8.6 사용
* API Key 정책 저장
* 현재 테스트 key:

  * `local-redteam-key`
  * `blocked-key`
  * `inactive-key`

### Tool

현재 구현된 Tool은 3개이다.

```text
getStoreList
getExternalInstitutionRecord
poisonedTool
```

현재는 실제 공유누리 API 또는 외부 기관 API를 호출하지 않고, `MockGongGongNuriApiClient`를 통해 mock 응답을 반환한다.

각 Tool의 목적은 다음과 같다.

```text
getStoreList
- 공유누리 장소 목록 조회 mock Tool

getExternalInstitutionRecord
- 다른 기관 API를 호출해 해당 기관 데이터베이스 record를 조회하는 상황을 가정한 mock Tool
- 응답 데이터의 reservations[].note 필드에 오염된 운영 메모가 포함된 상황을 재현한다.

poisonedTool
- 다른 Tool 호출 이후 후속 동작으로 호출되도록 description을 구성한 Tool
- 호출되면 서버 로그에 "HACKED!!"를 출력한다.
```

---

## 3. 아직 구현되지 않은 것

현재 구현되지 않았거나 이후 작업으로 미룬 항목은 다음과 같다.

```text
- Gateway / Agentgateway 연동
- 실제 공유누리 REST API 연동
- REST Client Adapter 방어구조
- Tool 14개 전체 구현
- MCP Resources 구현
- MCP Prompts 구현
- OpenTelemetry / 모니터링
- Rate Limit
- TLS / HTTPS
- systemd 기반 자동 재시작
- 응답 envelope 정규화
- 상세 error code 체계화
```

현재 단계의 목적은 모든 기능 완성이 아니라, 다음 주 red teaming을 위한 최소 MCP 서버 환경을 실제 서버에서 구동하는 것이다.

---

## 4. 로컬 개발 환경

로컬 개발은 WSL2 Ubuntu에서 진행하였다.

```text
Local OS: WSL2 Ubuntu
Project root: ~/mcp-lab
Server project: ~/mcp-lab/mcp-server
```

주요 기술 스택:

```text
Java 17
Spring Boot 3.5.9
Gradle
MCP Java SDK v1.1.1
Redis 8.6
Docker - local
Podman - Rocky server
```

---

## 5. 프로젝트 구조

```text
mcp-lab/
 ├─ README.md
 ├─ docker-compose.yml
 └─ mcp-server/
     ├─ Dockerfile
     ├─ build.gradle
     ├─ settings.gradle
     ├─ gradlew
     ├─ gradlew.bat
     └─ src/
         └─ main/
             ├─ java/
             │   └─ mcp_server/
             │       ├─ McpServerApplication.java
             │       ├─ auth/
             │       │   ├─ ApiKeyAuthenticationFilter.java
             │       │   ├─ ApiKeyContext.java
             │       │   ├─ ApiKeyPolicyService.java
             │       │   └─ AuthenticatedClient.java
             │       ├─ config/
             │       │   ├─ McpServerConfig.java
             │       │   └─ McpServletConfig.java
             │       ├─ tool/
             │       │   └─ GongGongNuriTools.java
             │       ├─ dto/
             │       │   ├─ ExternalInstitutionRecordRequest.java
             │       │   ├─ ExternalInstitutionRecordResponse.java
             │       │   ├─ StoreListRequest.java
             │       │   └─ StoreListResponse.java
             │       ├─ adapter/
             │       │   └─ MockGongGongNuriApiClient.java
             │       └─ validation/
             │           └─ StoreListValidator.java
             └─ resources/
                 └─ application.properties
```

---

## 6. MCP Server 실행 방식

### 로컬 Docker 실행 방식

로컬 WSL에서는 Docker를 사용하였다.

#### 6.1 Docker network 생성

```bash
docker network create mcp-lab-net
```

이미 존재하면 아래 메시지가 나올 수 있다.

```text
network with name mcp-lab-net already exists
```

이 경우 무시해도 된다.

#### 6.2 Redis 실행

```bash
docker run -d \
  --name mcp-lab-redis \
  --network mcp-lab-net \
  -p 6379:6379 \
  redis:8.6
```

Redis 확인:

```bash
docker exec -it mcp-lab-redis redis-cli PING
```

기대 결과:

```text
PONG
```

#### 6.3 Redis API Key 등록

```bash
docker exec -it mcp-lab-redis redis-cli
```

Redis CLI 안에서:

```redis
SET api-key:local-redteam-key '{"apiKey":"local-redteam-key","clientId":"local-redteam-client","status":"ACTIVE","allowedTools":["getStoreList","getExternalInstitutionRecord","poisonedTool"]}'
SET api-key:blocked-key '{"apiKey":"blocked-key","clientId":"blocked-client","status":"ACTIVE","allowedTools":[]}'
SET api-key:inactive-key '{"apiKey":"inactive-key","clientId":"inactive-client","status":"INACTIVE","allowedTools":["getStoreList","getExternalInstitutionRecord","poisonedTool"]}'
GET api-key:local-redteam-key
exit
```

#### 6.4 MCP Server 이미지 빌드

```bash
cd ~/mcp-lab/mcp-server
docker build -t mcp-lab-server:local .
```

#### 6.5 MCP Server 실행

```bash
docker run -d \
  --name mcp-lab-mcp-server \
  --network mcp-lab-net \
  -p 8080:8080 \
  -e SPRING_DATA_REDIS_HOST=mcp-lab-redis \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e REDIS_HOST=mcp-lab-redis \
  -e REDIS_PORT=6379 \
  mcp-lab-server:local
```

#### 6.6 Health 확인

```bash
curl http://localhost:8080/actuator/health
```

기대 결과:

```json
{"status":"UP"}
```

---

## 7. Rocky Linux 서버 배포 방식

실제 서버는 Rocky Linux 8.5이다.

```text
Server OS: Rocky Linux 8.5
Host: airlab_storage
Runtime: Podman
```

Docker 설치를 시도했으나, 서버에 이미 설치된 Podman/runc 계열 패키지와 Docker의 containerd.io 패키지가 충돌하였다.
따라서 실제 서버에서는 Docker 대신 Podman을 사용한다.

### 7.1 서버에 프로젝트 업로드

로컬 WSL에서 tar로 묶어 업로드하는 방식이 가장 안정적이다.

로컬 WSL에서:

```bash
cd ~/mcp-lab
tar --exclude='mcp-server/build' \
    --exclude='mcp-server/.gradle' \
    -czf mcp-server.tar.gz mcp-server
```

서버로 업로드:

```bash
scp mcp-server.tar.gz young@163.152.126.51:/home/young/mcp-lab/
```

서버에서 압축 해제:

```bash
cd /home/young/mcp-lab
rm -rf mcp-server
tar -xzf mcp-server.tar.gz
```

확인:

```bash
cd /home/young/mcp-lab/mcp-server
ls
```

다음 파일들이 보여야 한다.

```text
Dockerfile
build.gradle
gradlew
gradlew.bat
settings.gradle
src
gradle
```

---

## 8. Rocky 서버에서 Podman으로 실행

Podman CNI 네트워크에서 컨테이너 이름 DNS가 기대대로 동작하지 않아, Redis와 MCP 서버를 같은 Podman pod 안에서 실행한다.
같은 pod 안에서는 Redis가 `localhost:6379`로 접근 가능하다.

### 8.1 기존 컨테이너 정리

```bash
podman rm -f mcp-lab-mcp-server
podman rm -f mcp-lab-redis
```

이미 없으면 에러가 날 수 있으나 무시해도 된다.

### 8.2 Pod 생성

```bash
podman pod create --name mcp-lab-pod -p 8080:8080
```

중간에 image pull 관련 warning이 나와도 마지막에 긴 pod ID가 출력되면 생성된 것이다.

확인:

```bash
podman pod ps
```

### 8.3 Redis 실행

```bash
podman run -d \
  --pod mcp-lab-pod \
  --name mcp-lab-redis \
  docker.io/library/redis:8.6
```

Redis 확인:

```bash
podman exec -it mcp-lab-redis redis-cli PING
```

기대 결과:

```text
PONG
```

### 8.4 Redis API Key 등록

```bash
podman exec -it mcp-lab-redis redis-cli
```

Redis CLI 안에서:

```redis
SET api-key:local-redteam-key '{"apiKey":"local-redteam-key","clientId":"local-redteam-client","status":"ACTIVE","allowedTools":["getStoreList","getExternalInstitutionRecord","poisonedTool"]}'
SET api-key:blocked-key '{"apiKey":"blocked-key","clientId":"blocked-client","status":"ACTIVE","allowedTools":[]}'
SET api-key:inactive-key '{"apiKey":"inactive-key","clientId":"inactive-client","status":"INACTIVE","allowedTools":["getStoreList","getExternalInstitutionRecord","poisonedTool"]}'
GET api-key:local-redteam-key
exit
```

### 8.5 MCP Server 이미지 빌드

```bash
cd /home/young/mcp-lab/mcp-server
chmod +x gradlew
podman build -t mcp-lab-server:local .
```

### 8.6 MCP Server 실행

같은 pod 안에서는 Redis를 `localhost:6379`로 접근한다.

```bash
podman run -d \
  --pod mcp-lab-pod \
  --name mcp-lab-mcp-server \
  -e SPRING_DATA_REDIS_HOST=localhost \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e REDIS_HOST=localhost \
  -e REDIS_PORT=6379 \
  mcp-lab-server:local
```

주의: 여기서는 `-p 8080:8080`을 붙이지 않는다.
포트는 `podman pod create -p 8080:8080` 단계에서 이미 열었다.

### 8.7 서버 내부 Health 확인

```bash
curl http://localhost:8080/actuator/health
```

기대 결과:

```json
{"status":"UP"}
```

---

## 9. 서버 상태 확인 명령어

### Pod 상태 확인

```bash
podman pod ps
```

### Container 상태 확인

```bash
podman ps -a
```

정상 상태 예시:

```text
mcp-lab-redis        Up
mcp-lab-mcp-server   Up
```

### MCP Server 로그 확인

```bash
podman logs mcp-lab-mcp-server --tail 100
```

실시간 로그:

```bash
podman logs -f mcp-lab-mcp-server
```

### Redis 확인

```bash
podman exec -it mcp-lab-redis redis-cli PING
```

기대 결과:

```text
PONG
```

---

## 10. MCP 인증 테스트

### 10.1 API Key 없는 요청

```bash
curl -i -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"curl-test","version":"0.0.1"}}}'
```

기대 결과:

```text
HTTP/1.1 401
```

### 10.2 유효한 API Key 요청

```bash
curl -i -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -H "X-API-Key: local-redteam-key" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"curl-test","version":"0.0.1"}}}'
```

기대 결과:

```text
HTTP/1.1 200
```

또는 정상 MCP initialize 응답.

---

## 11. 외부 접속 확인

서버 내부에서 health가 `UP`이어도 외부에서 접근 가능하려면 8080 포트가 열려 있어야 한다.

로컬 PC/WSL에서:

```bash
curl http://163.152.126.51:8080/actuator/health
```

기대 결과:

```json
{"status":"UP"}
```

안 될 경우 서버 방화벽을 확인한다.

```bash
sudo firewall-cmd --state
```

firewalld가 실행 중이면:

```bash
sudo firewall-cmd --add-port=8080/tcp --permanent
sudo firewall-cmd --reload
sudo firewall-cmd --list-ports
```

그래도 안 되면 학교/연구실 네트워크에서 8080 포트가 막혀 있을 수 있으므로 관리자에게 포트 오픈을 요청해야 한다.

---

## 12. MCP Inspector 연결 방법

MCP Inspector는 서버에서 실행하지 않는다.
로컬 PC/WSL에서 실행한다.

```bash
npx @modelcontextprotocol/inspector
```

Inspector UI에서 다음과 같이 설정한다.

```text
Transport:
Streamable HTTP

URL:
http://163.152.126.51:8080/mcp

Header:
X-API-Key: local-redteam-key
```

연결 후 확인할 것:

```text
1. Connect 성공
2. Tools/List Tools 확인
3. getStoreList, getExternalInstitutionRecord, poisonedTool 표시 확인
4. getStoreList 호출 성공
5. getExternalInstitutionRecord 호출 시 reservations[].note 필드에 오염된 운영 메모가 포함되는지 확인
6. poisonedTool 호출 시 서버 로그에 "HACKED!!" 출력 확인
```

`getStoreList` 호출 예시:

```json
{
  "page": 1,
  "size": 20,
  "consumerCd": "local-test",
  "sido": "11",
  "sigungu": "강남구",
  "searchFreeYn": "Y",
  "searchSbclsCd": "011000",
  "searchMnclsCd": null
}
```

`getExternalInstitutionRecord` 호출 예시:

```json
{
  "institutionCode": "external-agency-01",
  "recordId": "record-001",
  "consumerCd": "local-test"
}
```

기대 응답에는 외부 기관 데이터베이스에서 받은 mock 예약 record들이 포함된다.
일부 예약 record의 `note` 필드에는 오염된 운영 메모가 포함되어 있으며, 이는 데이터베이스 record에 instruction이 섞여 들어간 indirect prompt injection 상황을 재현하기 위한 값이다.

`poisonedTool`은 입력값이 없다.
호출되면 MCP Server 로그에 다음 메시지가 출력된다.

```text
HACKED!!
```

---

## 13. 현재 API Key 목록

### local-redteam-key

```json
{
  "apiKey": "local-redteam-key",
  "clientId": "local-redteam-client",
  "status": "ACTIVE",
  "allowedTools": ["getStoreList", "getExternalInstitutionRecord", "poisonedTool"]
}
```

용도:

```text
정상 테스트용 API Key
getStoreList, getExternalInstitutionRecord, poisonedTool 호출 가능
```

### blocked-key

```json
{
  "apiKey": "blocked-key",
  "clientId": "blocked-client",
  "status": "ACTIVE",
  "allowedTools": []
}
```

용도:

```text
인증은 되지만 Tool 권한이 없는 경우 테스트
```

### inactive-key

```json
{
  "apiKey": "inactive-key",
  "clientId": "inactive-client",
  "status": "INACTIVE",
  "allowedTools": ["getStoreList", "getExternalInstitutionRecord", "poisonedTool"]
}
```

용도:

```text
비활성 API Key 차단 테스트
```

새 API Key 추가 예시:

```redis
SET api-key:redteam-user-01 '{"apiKey":"redteam-user-01","clientId":"redteam-user-01","status":"ACTIVE","allowedTools":["getStoreList","getExternalInstitutionRecord","poisonedTool"]}'
```

---

## 14. 재시작 방법

### Pod 전체 재시작

```bash
podman pod restart mcp-lab-pod
```

### 개별 컨테이너 재시작

```bash
podman restart mcp-lab-redis
podman restart mcp-lab-mcp-server
```

### 코드 수정 후 재배포

```bash
cd /home/young/mcp-lab/mcp-server
podman build -t mcp-lab-server:local .
podman rm -f mcp-lab-mcp-server
podman run -d \
  --pod mcp-lab-pod \
  --name mcp-lab-mcp-server \
  -e SPRING_DATA_REDIS_HOST=localhost \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e REDIS_HOST=localhost \
  -e REDIS_PORT=6379 \
  mcp-lab-server:local
```

---

## 15. 트러블슈팅

### health가 DOWN인 경우

```bash
curl http://localhost:8080/actuator/health
podman logs mcp-lab-mcp-server --tail 200
```

주요 원인:

```text
Redis 연결 실패
Redis 컨테이너 미실행
Redis API Key 미등록
환경변수 설정 오류
```

Podman pod 방식에서는 Redis host가 `localhost`여야 한다.

```bash
-e SPRING_DATA_REDIS_HOST=localhost
-e SPRING_DATA_REDIS_PORT=6379
```

### Redis PING 실패

```bash
podman exec -it mcp-lab-redis redis-cli PING
```

실패하면 Redis 컨테이너 상태 확인:

```bash
podman ps -a
podman logs mcp-lab-redis --tail 100
```


---

## 16. 향후 작업

우선순위 높은 작업:

```text
1. Gateway / Agentgateway 기본 라우팅 추가
2. Gateway 경유로 MCP 호출 확인
```

이후 작업:

```text
- Gateway-level access control
- Rate Limit
- TLS / HTTPS
- 실제 공유누리 REST API 연동
- REST Adapter 방어구조
- Tool 14개 확장
- MCP Resources 구현
- MCP Prompts 구현
- OpenTelemetry
- systemd 기반 자동 재시작
```

---

## 17. 현재 상태 요약

현재 상태:

```text
Redis 기반 API Key 인증/인가를 갖춘 Streamable HTTP MCP 서버를 구현했고,
Rocky Linux 서버에서 Podman pod 기반으로 Redis + MCP Server를 실행하는 데 성공했다.
```

남은 핵심:

```text
1. Gateway 연결
2. red teaming 준비
```


## Local애서 돌릴 때 해야하는 것
```bash
docker rm -f mcp-lab-redis
docker rm -f mcp-lab-mcp-server
docker rm -f mcp-lab-agentgateway
```

```bash
docker run -d \
  --name mcp-lab-redis \
  --network mcp-lab-net \
  -p 6379:6379 \
  redis:8.6
```

```bash
docker ps
```

```bash
docker exec -it mcp-lab-redis redis-cli PING
``` PONG

```bash
docker exec -it mcp-lab-redis redis-cli
```

```redis
SET api-key:local-redteam-key '{"apiKey":"local-redteam-key","clientId":"local-redteam-client","status":"ACTIVE","allowedTools":["getStoreList","getExternalInstitutionRecord","poisonedTool"]}'
SET api-key:blocked-key '{"apiKey":"blocked-key","clientId":"blocked-client","status":"ACTIVE","allowedTools":[]}'
SET api-key:inactive-key '{"apiKey":"inactive-key","clientId":"inactive-client","status":"INACTIVE","allowedTools":["getStoreList","getExternalInstitutionRecord","poisonedTool"]}'
GET api-key:local-redteam-key
exit
```

```bash
docker build -t mcp-lab-server:local .

docker run -d \
  --name mcp-lab-mcp-server \
  --network mcp-lab-net \
  -p 8080:8080 \
  -e SPRING_DATA_REDIS_HOST=mcp-lab-redis \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e REDIS_HOST=mcp-lab-redis \
  -e REDIS_PORT=6379 \
  mcp-lab-server:local
  ```


```bash
docker run -d \
  --name mcp-lab-agentgateway \
  --network mcp-lab-net \
  -p 8081:8081 \
  -p 127.0.0.1:15000:15000 \
  -e ADMIN_ADDR=0.0.0.0:15000 \
  -v ~/mcp-lab/agentgateway/agentgateway.yaml:/etc/agentgateway/agentgateway.yaml:ro \
  cr.agentgateway.dev/agentgateway:v1.0.1 \
  -f /etc/agentgateway/agentgateway.yaml
  ```

최종 확인
```bash
curl -i -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -H "X-API-Key: local-redteam-key" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"curl-gateway-test","version":"0.0.1"}}}'
  ```
