# Phase 1 Testing

## Objective
Validate that the WebSocket API layer works correctly before moving to Phase 2 (UI refactoring).

## Duration
**4-6 hours**

---

## Testing Approach

Phase 1 testing focuses on **API-only** validation. The UI has not been modified yet, so we test using:
- wscat (WebSocket command-line client)
- Postman (WebSocket support)
- Custom test scripts
- Unit tests

---

## Setup

> **Module scope:** All Maven commands in this plan should target the `wos-api` module. Run them from inside `wos-api` or use `mvn -pl wos-api ...` from the root so Maven does not attempt to build unrelated modules whose artifacts are missing.

### Install Testing Tools

**wscat (WebSocket CLI):**
```bash
npm install -g wscat
```

**Test server is running:**
```bash
cd wos-api
mvn clean compile spring-boot:run -Dspring-boot.run.arguments=--server.port=8765
```

---

## Test Suite

### 1. Connection Tests

#### 1.1 Server Starts Successfully
```bash
mvn -pl wos-api spring-boot:run -Dspring-boot.run.arguments=--server.port=8765
```

**Expected output:**
```
INFO  BotWebSocketServer - ✅ WebSocket server started successfully
INFO  BotWebSocketServer -    WebSocket endpoint: ws://localhost:8765/bot
INFO  BotWebSocketServer -    Health check: http://localhost:8765/health
```

**Status:** [ ]

---

#### 1.2 Health Check Endpoint
```bash
curl http://localhost:8765/health
```

**Expected:** `{"status":"UP"}`

**Status:** [ ]

---

#### 1.3 WebSocket Connection
```bash
wscat -c ws://localhost:8765/bot
```

**Expected:**
```
Connected (press CTRL+C to quit)
```

Server logs should show:
```
INFO  WebSocketHandler - WebSocket connection opened: /127.0.0.1:xxxxx
INFO  WebSocketSessionManager - Client connected. Total clients: 1
```

**Status:** [ ]

---

### 2. Command Tests

For each test, connect via wscat and send the command. Verify response matches expected format.

#### 2.1 getStatus Command
```json
{"id":"test-001","command":"getStatus","params":{}}
```

**Expected response:**
```json
{
  "id":"test-001",
  "success":true,
  "result":{
    "apiUp":true,
    "botRunning":false,
    "profileCount":2,
    "timestamp":1701531234567
  }
}
```

**Status:** [ ]

---

#### 2.2 getProfiles Command
```json
{"id":"test-002","command":"getProfiles","params":{}}
```

**Expected:** Array of profiles
```json
{
  "id":"test-002",
  "success":true,
  "result":[
    {"id":1,"name":"Profile 1","enabled":true},
    {"id":2,"name":"Profile 2","enabled":false}
  ]
}
```

**Status:** [ ]

---

#### 2.3 getGlobalConfig Command
```json
{"id":"test-003","command":"getGlobalConfig","params":{}}
```

**Expected:** Config map
```json
{
  "id":"test-003",
  "success":true,
  "result":{
    "CURRENT_EMULATOR_STRING":"LDPLAYER",
    "LDPLAYER_PATH_STRING":"C:\\LDPlayer\\..."
  }
}
```

**Status:** [ ]

---

#### 2.4 startBot Command
```json
{"id":"test-004","command":"startBot","params":{}}
```

**Expected response:**
```json
{
  "id":"test-004",
  "success":true,
  "result":null
}
```

**Also expect to receive event:**
```json
{
  "event":"botStateChanged",
  "data":{"running":true,"paused":false},
  "timestamp":1701531234567
}
```

**Status:** [ ]

---

#### 2.5 stopBot Command
```json
{"id":"test-005","command":"stopBot","params":{}}
```

**Expected response:**
```json
{
  "id":"test-005",
  "success":true,
  "result":null
}
```

**Also expect event:**
```json
{
  "event":"botStateChanged",
  "data":{"running":false,"paused":null},
  "timestamp":1701531234567
}
```

**Status:** [ ]

---

#### 2.6 pauseQueue Command (with params)
```json
{"id":"test-006","command":"pauseQueue","params":{"profileId":1}}
```

**Expected response:**
```json
{
  "id":"test-006",
  "success":true,
  "result":null
}
```

**Status:** [ ]

---

#### 2.7 getCurrentStamina Command
```json
{"id":"test-007","command":"getCurrentStamina","params":{"profileId":1}}
```

**Expected response:**
```json
{
  "id":"test-007",
  "success":true,
  "result":100
}
```

**Status:** [ ]

---

#### 2.8 Invalid Command (error handling)
```json
{"id":"test-008","command":"invalidCommand","params":{}}
```

**Expected error response:**
```json
{
  "id":"test-008",
  "success":false,
  "error":"Unknown command: invalidCommand"
}
```

**Status:** [ ]

---

#### 2.9 Missing Parameter (error handling)
```json
{"id":"test-009","command":"pauseQueue","params":{}}
```

**Expected error response:**
```json
{
  "id":"test-009",
  "success":false,
  "error":"Missing or invalid parameter: profileId"
}
```

**Status:** [ ]

---

### 3. Event Tests

#### 3.1 Receive Bot State Changed Event
1. Connect client via wscat
2. In another terminal, send `startBot` command
3. Verify client receives `botStateChanged` event

**Expected:**
```json
{
  "event":"botStateChanged",
  "data":{"running":true,"paused":false},
  "timestamp":1701531234567
}
```

**Status:** [ ]

---

#### 3.2 Receive Queue State Changed Event
1. Start bot
2. Send `pauseQueue` command
3. Verify client receives `queueStateChanged` event

**Status:** [ ]

---

#### 3.3 Receive Stamina Changed Event
1. Bot running
2. Stamina changes (trigger via bot action)
3. Verify client receives `staminaChanged` event

**Expected:**
```json
{
  "event":"staminaChanged",
  "data":{"profileId":1,"stamina":95},
  "timestamp":1701531234567
}
```

**Status:** [ ]

---

### 4. Multiple Client Tests

#### 4.1 Multiple Clients Receive Events
1. Connect two clients (two wscat windows)
2. Send `startBot` command from one client
3. Verify **both** clients receive `botStateChanged` event

**Status:** [ ]

---

#### 4.2 Client Disconnect Handling
1. Connect client
2. Disconnect abruptly (CTRL+C)
3. Verify server logs disconnection
4. Verify no errors when broadcasting events

**Expected logs:**
```
INFO  WebSocketSessionManager - Client disconnected. Total clients: 0
```

**Status:** [ ]

---

### 5. Performance Tests

#### 5.1 Response Time
Measure response time for simple command:
```bash
time echo '{"id":"perf-001","command":"getStatus","params":{}}' | wscat -c ws://localhost:8765/bot -x
```

**Expected:** < 100ms

**Status:** [ ]

---

#### 5.2 Concurrent Clients
1. Connect 10 clients simultaneously
2. Send commands from all clients
3. Verify all receive responses

**Script:** `test-concurrent.sh`
```bash
for i in {1..10}; do
  echo '{"id":"'$i'","command":"getStatus","params":{}}' | wscat -c ws://localhost:8765/bot -x &
done
wait
```

**Status:** [ ]

---

### 6. Unit Tests

Run existing unit tests:
```bash
mvn -pl wos-api test
```

**Expected:** All tests pass

Key test classes:
- `ProtocolTest` - Protocol serialization
- `MessageDispatcherTest` - Command dispatching
- `BotApiServiceTest` - Service facade methods
- `WebSocketSessionManagerTest` - Session management
- `WebSocketHandlerTest` - Connection lifecycle
- `EventBroadcasterTest` - Event broadcasting

**Status:** [ ]

---

## Comprehensive Automated Test Suite

> [!IMPORTANT]
> All automated tests must pass before Phase 1 is considered complete.

### Test Matrix

| Test Class | Component | Coverage | Command |
|------------|-----------|----------|---------|
| `WebSocketSessionManagerTest` | Session Management | Session add/remove, broadcast | `mvn -pl wos-api test -Dtest=WebSocketSessionManagerTest` |
| `WebSocketHandlerTest` | Connection Lifecycle | Open, close, message handling | `mvn -pl wos-api test -Dtest=WebSocketHandlerTest` |
| `MessageDispatcherTest` | Command Routing | JSON parsing, command dispatch | `mvn -pl wos-api test -Dtest=MessageDispatcherTest` |
| `BotWebSocketServerIntegrationTest` | End-to-End | Health check, WebSocket echo | `mvn -pl wos-api test -Dtest=BotWebSocketServerIntegrationTest` |
| `ProtocolTest` | Protocol Classes | Serialization tests | `mvn -pl wos-api test -Dtest=ProtocolTest` |
| `BotApiServiceTest` | Service Facade | Service wrapper methods | `mvn -pl wos-api test -Dtest=BotApiServiceTest` |
| `EventBroadcasterTest` | Event Broadcasting | Listener registration, broadcast | `mvn -pl wos-api test -Dtest=EventBroadcasterTest` |

### Run Commands

```bash
# Run ALL tests (recommended before phase completion)
mvn -pl wos-api test

# Run by category
mvn -pl wos-api test -Dtest="*WebSocket*"            # WebSocket tests
mvn -pl wos-api test -Dtest="*Service*,*Dispatcher*"  # Service layer tests
mvn -pl wos-api test -Dtest="*Protocol*,*Event*"      # Protocol & event tests
mvn -pl wos-api test -Dtest="*IntegrationTest"        # Integration tests only
```

### Required Test Files

| Location | File | Status |
|----------|------|--------|
| `server/` | `WebSocketSessionManagerTest.java` | ✅ Exists |
| `server/` | `WebSocketHandlerTest.java` | ✅ Exists |
| `server/` | `MessageDispatcherTest.java` | ✅ Exists |
| `server/` | `BotWebSocketServerIntegrationTest.java` | ✅ Exists |
| `protocol/` | `ProtocolTest.java` | 📝 Create if missing |
| `service/` | `BotApiServiceTest.java` | 📝 Create if missing |
| `event/` | `EventBroadcasterTest.java` | 📝 Create if missing |

### Coverage Requirements

| Component | Target | Critical Methods |
|-----------|--------|------------------|
| `WebSocketSessionManager` | 80% | `addSession`, `broadcast`, `closeAll` |
| `MessageDispatcher` | 80% | `handleMessage`, `dispatchCommand` |
| `BotApiService` | 70% | All public methods |
| Protocol Classes | 90% | Serialization, factory methods |
| `EventBroadcaster` | 75% | Event handlers, `broadcastEvent` |

---

## Validation Checklist

### Functionality
- [ ] Server starts without errors
- [ ] Health check endpoint works
- [ ] Clients can connect via WebSocket
- [ ] All commands execute successfully
- [ ] Command responses have correct format
- [ ] Events are received by connected clients
- [ ] Multiple clients work simultaneously
- [ ] Error responses are properly formatted
- [ ] Invalid commands return errors
- [ ] Missing parameters return errors

### Performance
- [ ] Response time < 100ms for simple commands
- [ ] 10 concurrent clients handled without issues
- [ ] No memory leaks (test with long-running server)

### Logging
- [ ] Server logs connections/disconnections
- [ ] Command execution is logged
- [ ] Errors are logged with details
- [ ] No excessive debug logs in production mode

### Integration
- [ ] Backend services are called correctly
- [ ] DTOs serialize without errors
- [ ] Listeners are registered
- [ ] Events are broadcast correctly

---

## Known Issues Log

| Issue | Severity | Status | Notes |
|-------|----------|--------|-------|
| Example: DTO circular reference | MEDIUM | FIXED | Added @JsonIgnore |
|  |  |  |  |
|  |  |  |  |

---

## Phase 1 Completion Criteria

✅ **Phase 1 is complete when:**
- [ ] All functionality tests pass
- [ ] All unit tests pass
- [ ] Server runs stably for 1 hour
- [ ] Documentation is complete
- [ ] Known issues are documented or fixed

---

## Next Steps

Once Phase 1 testing is complete and all issues are resolved, proceed to:

**[Phase 2: UI Client Implementation](../phase-2-ui-client/overview.md)**

Phase 2 will create a WebSocket client in the UI and refactor controllers to use the API instead of direct service calls.

---

[← Back: 1.5 Listener Conversion](1.5-listener-conversion.md) | [Next Phase: UI Client →](../phase-2-ui-client/overview.md)
