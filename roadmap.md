Current Architecture Analysis

Module Structure

- wos-hmi: JavaFX UI layer with controllers and views
- wos-serv: Backend services (bot orchestration, scheduling, task management)
- wos-ot: Shared DTOs (Data Transfer Objects)
- wos-persistence: Database layer with Hibernate/SQLite
- wos-utiles: Utilities (image processing, OCR, ADB interaction)

Critical Coupling Points

1. Direct Singleton Calls (HIGH COUPLING)

// LauncherActionController.java:28
ServScheduler.getServices().startBot();
ServConfig.getServices().getGlobalConfig();
StaminaService.getServices().getCurrentStamina();
UI controllers directly invoke backend singletons across 40+ call sites.

2. Listener Pattern (MEDIUM COUPLING)

// LauncherActionController.java:19-20
ServScheduler.getServices().registryBotStateListener(this);
ServScheduler.getServices().registryQueueStateListener(this);
Backend pushes state updates via interfaces like IBotStateListener, IQueueStateListener, IStaminaChangeListener.

3. Shared DTOs (LOW COUPLING)

Objects like DTOBotState, DTOQueueState, ProfileAux are passed directly between layers but are serializable.

4. Platform.runLater() Threading (MEDIUM COUPLING)

Backend callbacks execute Platform.runLater() to update JavaFX UI thread.

---
IPC Options Evaluation

Option 1: WebSocket (JSON/Binary) - RECOMMENDED

Pros:
- ✅ Bidirectional, full-duplex communication
- ✅ Cross-platform (works on Windows, Linux, can support web UI later)
- ✅ Mature libraries (Java: Spring WebSocket/Netty, C#: SignalR/System.Net.WebSockets)
- ✅ Easy debugging (inspect traffic with browser tools)
- ✅ Native support for JSON serialization
- ✅ Reconnection handling built-in
- ✅ Can support multiple UI clients simultaneously

Cons:
- ❌ Slight overhead compared to pipes (negligible for this use case)
- ❌ Requires port management

Implementation:
- Backend: Embed Jetty/Netty WebSocket server (already has Spring Boot dependencies)
- Protocol: JSON-RPC 2.0 or custom command/event pattern
- DTOs: Reuse existing wos-ot DTOs with Jackson serialization

---
Option 2: Named Pipes

Pros:
- ✅ Low latency, no network stack
- ✅ OS-native IPC

Cons:
- ❌ Windows-specific (different APIs for Linux: FIFO)
- ❌ Requires custom framing/protocol for messages
- ❌ No built-in reconnection logic
- ❌ Harder to debug
- ❌ Blocks C# web UI or remote access

---
Option 3: gRPC

Pros:
- ✅ Efficient binary protocol (Protobuf)
- ✅ Strong typing with .proto contracts
- ✅ Bidirectional streaming

Cons:
- ❌ Steeper learning curve
- ❌ Requires code generation from .proto files
- ❌ Overkill for simple RPC needs
- ❌ HTTP/2 dependency

---
Option 4: REST API + Server-Sent Events (SSE)

Pros:
- ✅ Simple, stateless for commands
- ✅ SSE for server→client updates
- ✅ Already has Spring Boot dependency

Cons:
- ❌ SSE is unidirectional (client must poll or use separate channel for commands)
- ❌ Less efficient than WebSocket for real-time updates
- ❌ No built-in request/response correlation

---
Recommended Approach: WebSocket with JSON Protocol

Why: Best balance of simplicity, debuggability, cross-platform support, and future extensibility (web UI, remote access).

---
Detailed Implementation Roadmap

Phase 1: Backend API Layer (Foundation)

1.1 Create API Abstraction Layer

Location: New module wos-api or package cl.camodev.wosbot.api

Files to create:
```
wos-api/
├── dto/          # API-specific DTOs (may reuse wos-ot)
├── server/       # WebSocket server
│   ├── BotWebSocketServer.java
│   ├── WebSocketSessionManager.java
│   └── MessageDispatcher.java
├── protocol/     # Message protocol
│   ├── Command.java
│   ├── Event.java
│   └── Response.java
└── service/      # API service facade
    └── BotApiService.java
```
Key classes:

BotApiService.java - Facade over existing services
```java
public class BotApiService {
    public void startBot() { ServScheduler.getServices().startBot(); }
    public void stopBot() { ServScheduler.getServices().stopBot(); }
    public void pauseQueue(Long profileId) { /* ... */ }
    // ... wrap all 40+ backend calls
}
```

BotWebSocketServer.java - WebSocket endpoint
@ServerEndpoint("/bot")
public class BotWebSocketServer {
    private BotApiService apiService;

    @OnMessage
    public void onMessage(String message, Session session) {
        Command cmd = parseCommand(message);
        Response response = handleCommand(cmd);
        session.getBasicRemote().sendText(toJson(response));
    }
}

Protocol example:
// Command (UI → Backend)
{
"id": "uuid-1234",
"command": "startBot",
"params": {}
}

// Response (Backend → UI)
{
"id": "uuid-1234",
"success": true,
"result": null
}

// Event (Backend → UI, unsolicited)
{
"event": "botStateChanged",
"data": {
    "running": true,
    "paused": false
}
}

1.2 Convert Listeners to Event Emitters

Replace Platform.runLater() calls in listeners with WebSocket broadcasts:

Before:
@Override
public void onBotStateChange(DTOBotState botState) {
    Platform.runLater(() -> layoutController.onBotStateChange(botState));
}

After:
@Override
public void onBotStateChange(DTOBotState botState) {
    webSocketServer.broadcast(new Event("botStateChanged", botState));
}

---
Phase 2: UI Client Implementation

2.1 Create WebSocket Client Layer

Location: wos-hmi/src/main/java/cl/camodev/wosbot/client/

BotApiClient.java - Replaces direct service calls
public class BotApiClient {
    private WebSocketClient client;

    public CompletableFuture<Void> startBot() {
        return sendCommand("startBot", null);
    }

    public void onBotStateChanged(Consumer<DTOBotState> handler) {
        registerEventHandler("botStateChanged", handler);
    }
}

2.2 Refactor Controllers

Replace all ServScheduler.getServices() calls with apiClient.startBot().

Impact: ~14 controller files need modification (from grep results).

Example refactor:
```java
// LauncherActionController.java - BEFORE
public void startBot() {
    ServScheduler.getServices().startBot();
}
```
// LauncherActionController.java - AFTER
```java
public void startBot() {
    apiClient.startBot().thenRun(() ->
        Platform.runLater(() -> updateUI())
    );
}
```
---
Phase 3: Process Separation

3.1 Split Maven Modules

Before:
```
wosbot/
├── wos-hmi (depends on wos-serv, wos-ot, wos-utiles)
└── wos-serv
```
After:
```
wosbot/
├── wos-backend-app (executable JAR with main(), includes wos-api server)
│   └── depends on: wos-serv, wos-api, wos-ot, wos-persistence, wos-utiles
├── wos-hmi (executable JAR with JavaFX main())
│   └── depends on: wos-api (client only), wos-ot (DTOs)
└── wos-api (shared module)
    ├── client/ (WebSocket client for UI)
    ├── server/ (WebSocket server for backend)
    └── dto/    (shared DTOs)
```
3.2 Create Backend Executable

wos-backend-app/src/main/java/cl/camodev/wosbot/backend/BackendMain.java
```java
public class BackendMain {
    public static void main(String[] args) {
        // 1. Initialize Hibernate
        // 2. Start WebSocket server on port 8765
        BotWebSocketServer server = new BotWebSocketServer(8765);
        server.start();
        // 3. Keep process alive
    }
}
```

3.3 Update UI to Connect to Backend

FXApp.java - Add connection logic
```java
@Override
public void start(Stage stage) throws IOException {
    // Connect to backend before showing UI
    BotApiClient apiClient = new BotApiClient("ws://localhost:8765/bot");
    apiClient.connect().get(5, TimeUnit.SECONDS);

    // Pass apiClient to controllers
    LauncherLayoutController controller = new LauncherLayoutController(stage, apiClient);
    // ... rest of UI initialization
}
```
---
Phase 4: Deployment & Launch

4.1 Startup Script

start.bat (Windows)
```bat
@echo off
start "WosBot Backend" java -jar wos-backend-app-1.5.4.jar
timeout /t 2
start "WosBot UI" javaw -jar wos-hmi-1.5.4.jar
```
4.2 Process Management

- Backend writes PID to backend.pid
- UI checks for running backend on startup
- UI shows "Connecting..." dialog if backend unavailable
- Implement graceful shutdown (UI sends "shutdown" command)

---
Implementation Recommendations

Technology Stack

| Component        | Technology                               | Rationale                                                                           |
|------------------|------------------------------------------|-------------------------------------------------------------------------------------|
| WebSocket Server | Jetty WebSocket or Spring Boot WebSocket | Already has Spring Boot dependency in wos-hmi pom.xml                               |
| Protocol         | JSON-RPC 2.0 or custom JSON              | Simple, debuggable, reuses Jackson (already in deps)                                |
| Serialization    | Jackson (already in use)                 | No new dependencies                                                                 |
| Client           | Java-WebSocket library or OkHttp         | Lightweight, mature                                                                 |
| Threading        | Virtual threads (JDK 21+)                | Backend already uses Thread.ofVirtual() (line 563 in LauncherLayoutController.java) |

Port Configuration

- Default: ws://localhost:8765
- Configurable via config.properties or command-line arg
- UI fallback: Try ports 8765-8769 if default fails

Error Handling

- Connection loss: UI shows warning banner, queues commands, retries every 5s
- Timeout: Commands timeout after 30s, show error dialog
- Backend crash: UI detects via heartbeat, offers restart button

Security (if needed later)

- Add authentication token in WebSocket handshake
- Use WSS (TLS) for remote access
- For now: Bind to localhost only

---
Migration Strategy

Option A: Big Bang (Risky)

Implement all phases at once. High risk of breaking existing functionality.

Option B: Incremental (RECOMMENDED)

1. Week 1-2: Implement Phase 1 (API layer) alongside existing code
- Add WebSocket server
- Keep UI calling ServScheduler directly (no changes yet)
- Test API with Postman/wscat
2. Week 3: Phase 2 partial (refactor 1-2 controllers)
- Start with LauncherActionController
- Verify bot start/stop works via WebSocket
- Keep other controllers on direct calls
3. Week 4: Phase 2 complete (refactor remaining controllers)
- Convert all 14 controller files
- Remove wos-serv dependency from wos-hmi pom.xml
4. Week 5: Phase 3 (process separation)
- Split into separate JARs
- Test launch scripts
5. Week 6: Polish, error handling, documentation

---
Risks & Mitigation

| Risk                                       | Impact | Mitigation                                             |
|--------------------------------------------|--------|--------------------------------------------------------|
| Serialization issues (circular refs, etc.) | HIGH   | Use @JsonIgnore on problematic fields; test DTOs early |
| Race conditions in async calls             | MEDIUM | Use CompletableFuture with proper error handling       |
| Backend crash takes down everything        | HIGH   | Implement UI resilience (reconnect, queue commands)    |
| Performance degradation                    | LOW    | WebSocket overhead is <1ms; profile if needed          |
| Regression in existing features            | HIGH   | Write integration tests before refactoring             |

---
Testing Strategy

1. Unit tests: Mock BotApiClient in controller tests
2. Integration tests: Start backend + UI in same JVM, test via API
3. E2E tests: Launch separate processes, test full scenarios
4. Manual testing checklist:
- Start/stop bot
- Pause/resume queues
- Profile switching
- Log console updates
- All 14 feature modules load correctly

---
Future Enhancements (Post-Separation)

- Web UI: Reuse WebSocket API to build browser-based UI
- Remote management: Expose API over network with auth
- Multiple UI instances: Multiple clients monitoring same backend
- REST API: Add HTTP endpoints alongside WebSocket for scripting
- Health monitoring: Backend exposes /health endpoint for monitoring tools

---
Files Requiring Modification (Summary)

New Files (~15)

- wos-api module (server, client, protocol)
- BackendMain.java
- start.bat, start.sh

Modified Files (~20)

- 14 controller files (replace ServScheduler calls)
- 3 pom.xml files (dependency changes)
- FXApp.java, Main.java (connection logic)
- Launch scripts

Deleted Dependencies

- Remove wos-serv from wos-hmi/pom.xml

---
Estimated Effort

| Phase                  | Effort     | Complexity  |
|------------------------|------------|-------------|
| Phase 1: Backend API   | 3-5 days   | Medium      |
| Phase 2: UI Refactor   | 5-7 days   | Medium-High |
| Phase 3: Process Split | 2-3 days   | Low         |
| Phase 4: Polish/Test   | 3-5 days   | Medium      |
| Total                  | 13-20 days | -           |

---
Conclusion

The current architecture is tightly coupled via singleton calls and listener patterns. The recommended approach is:

1. Introduce WebSocket API layer (backward-compatible)
2. Incrementally refactor UI to use API client
3. Split into separate processes once refactor complete
4. Deploy with launch scripts that manage both processes

WebSocket is the optimal IPC mechanism due to its cross-platform support, debugging ease, and future extensibility for web UIs or remote access.

● I've completed the investigation. The roadmap above provides:

- Current state analysis: 40+ direct backend calls, listener-based updates, tight coupling via singletons
- IPC evaluation: WebSocket (recommended), Named Pipes, gRPC, and REST+SSE compared
- 4-phase implementation plan: API layer → UI refactor → process split → deployment
- Concrete code examples: Shows exactly how to refactor existing controllers
- Migration strategy: Incremental approach to minimize risk
- Effort estimate: 13-20 days total