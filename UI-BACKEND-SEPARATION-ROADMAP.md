# WosBot UI/Backend Separation Roadmap

## Investigation Summary

This document provides a comprehensive analysis and implementation roadmap for detaching the JavaFX UI (wos-hmi) from the backend services (wos-serv).

**Date:** December 2, 2025
**Project:** WosBot v1.5.4
**Objective:** Separate UI and backend into independent processes with IPC communication

---

## Current Architecture Analysis

### Module Structure

- **wos-hmi**: JavaFX UI layer with controllers and views
- **wos-serv**: Backend services (bot orchestration, scheduling, task management)
- **wos-ot**: Shared DTOs (Data Transfer Objects)
- **wos-persistence**: Database layer with Hibernate/SQLite
- **wos-utiles**: Utilities (image processing, OCR, ADB interaction)

### Critical Coupling Points

#### 1. Direct Singleton Calls (HIGH COUPLING)

```java
// LauncherActionController.java:28
ServScheduler.getServices().startBot();
ServConfig.getServices().getGlobalConfig();
StaminaService.getServices().getCurrentStamina();
```

UI controllers directly invoke backend singletons across **40+ call sites** in 14 files.

#### 2. Listener Pattern (MEDIUM COUPLING)

```java
// LauncherActionController.java:19-20
ServScheduler.getServices().registryBotStateListener(this);
ServScheduler.getServices().registryQueueStateListener(this);
```

Backend pushes state updates via interfaces:
- `IBotStateListener`
- `IQueueStateListener`
- `IStaminaChangeListener`
- `IProfileDataChangeListener`

#### 3. Shared DTOs (LOW COUPLING)

Objects like `DTOBotState`, `DTOQueueState`, `ProfileAux` are passed directly between layers but are serializable.

#### 4. Platform.runLater() Threading (MEDIUM COUPLING)

Backend callbacks execute `Platform.runLater()` to update JavaFX UI thread.

---

## IPC Options Evaluation

### Option 1: WebSocket (JSON/Binary) - ✅ RECOMMENDED

**Pros:**
- ✅ Bidirectional, full-duplex communication
- ✅ Cross-platform (Windows, Linux, can support web UI later)
- ✅ Mature libraries (Java: Spring WebSocket/Netty, C#: SignalR/System.Net.WebSockets)
- ✅ Easy debugging (inspect traffic with browser tools)
- ✅ Native support for JSON serialization
- ✅ Reconnection handling built-in
- ✅ Can support multiple UI clients simultaneously

**Cons:**
- ❌ Slight overhead compared to pipes (negligible for this use case)
- ❌ Requires port management

**Implementation:**
- Backend: Embed Jetty/Netty WebSocket server (already has Spring Boot dependencies)
- Protocol: JSON-RPC 2.0 or custom command/event pattern
- DTOs: Reuse existing `wos-ot` DTOs with Jackson serialization

---

### Option 2: Named Pipes

**Pros:**
- ✅ Low latency, no network stack
- ✅ OS-native IPC

**Cons:**
- ❌ **Windows-specific** (different APIs for Linux: FIFO)
- ❌ Requires custom framing/protocol for messages
- ❌ No built-in reconnection logic
- ❌ Harder to debug
- ❌ Blocks C# web UI or remote access

---

### Option 3: gRPC

**Pros:**
- ✅ Efficient binary protocol (Protobuf)
- ✅ Strong typing with `.proto` contracts
- ✅ Bidirectional streaming

**Cons:**
- ❌ Steeper learning curve
- ❌ Requires code generation from `.proto` files
- ❌ Overkill for simple RPC needs
- ❌ HTTP/2 dependency

---

### Option 4: REST API + Server-Sent Events (SSE)

**Pros:**
- ✅ Simple, stateless for commands
- ✅ SSE for server→client updates
- ✅ Already has Spring Boot dependency

**Cons:**
- ❌ SSE is unidirectional (client must poll or use separate channel for commands)
- ❌ Less efficient than WebSocket for real-time updates
- ❌ No built-in request/response correlation

---

## Recommended Approach: WebSocket with JSON Protocol

**Why:** Best balance of simplicity, debuggability, cross-platform support, and future extensibility (web UI, remote access).

---

## Detailed Implementation Roadmap

### Phase 1: Backend API Layer (Foundation)

#### 1.1 Create API Abstraction Layer

**Location:** New module `wos-api` or package `cl.camodev.wosbot.api`

**Directory Structure:**
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

**Key Classes:**

**`BotApiService.java`** - Facade over existing services
```java
public class BotApiService {
    private final ServScheduler scheduler = ServScheduler.getServices();
    private final ServConfig config = ServConfig.getServices();

    public void startBot() {
        scheduler.startBot();
    }

    public void stopBot() {
        scheduler.stopBot();
    }

    public void pauseQueue(Long profileId) {
        scheduler.pauseQueue(profileId);
    }

    public Map<String, String> getGlobalConfig() {
        return config.getGlobalConfig();
    }

    // ... wrap all 40+ backend calls
}
```

**`BotWebSocketServer.java`** - WebSocket endpoint
```java
@ServerEndpoint("/bot")
public class BotWebSocketServer {
    private final BotApiService apiService = new BotApiService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            Command cmd = objectMapper.readValue(message, Command.class);
            Response response = handleCommand(cmd);
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            sendError(session, e.getMessage());
        }
    }

    private Response handleCommand(Command cmd) {
        switch (cmd.getCommand()) {
            case "startBot":
                apiService.startBot();
                return Response.success(cmd.getId());
            case "stopBot":
                apiService.stopBot();
                return Response.success(cmd.getId());
            // ... handle all commands
            default:
                return Response.error(cmd.getId(), "Unknown command");
        }
    }
}
```

**Protocol Example:**

```json
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
```

#### 1.2 Convert Listeners to Event Emitters

Replace `Platform.runLater()` calls in listeners with WebSocket broadcasts.

**Before:**
```java
@Override
public void onBotStateChange(DTOBotState botState) {
    Platform.runLater(() -> layoutController.onBotStateChange(botState));
}
```

**After:**
```java
@Override
public void onBotStateChange(DTOBotState botState) {
    Event event = new Event("botStateChanged", botState);
    webSocketServer.broadcast(event);
}
```

---

### Phase 2: UI Client Implementation

#### 2.1 Create WebSocket Client Layer

**Location:** `wos-hmi/src/main/java/cl/camodev/wosbot/client/`

**`BotApiClient.java`** - Replaces direct service calls
```java
public class BotApiClient {
    private WebSocketClient client;
    private final Map<String, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<Event>>> eventHandlers = new ConcurrentHashMap<>();

    public BotApiClient(String serverUrl) {
        this.client = new WebSocketClient(new URI(serverUrl)) {
            @Override
            public void onMessage(String message) {
                handleMessage(message);
            }
        };
    }

    public CompletableFuture<Void> startBot() {
        return sendCommand("startBot", null);
    }

    public CompletableFuture<Void> stopBot() {
        return sendCommand("stopBot", null);
    }

    public void onBotStateChanged(Consumer<DTOBotState> handler) {
        registerEventHandler("botStateChanged", event -> {
            DTOBotState state = objectMapper.convertValue(event.getData(), DTOBotState.class);
            handler.accept(state);
        });
    }

    private CompletableFuture<Void> sendCommand(String command, Object params) {
        String id = UUID.randomUUID().toString();
        Command cmd = new Command(id, command, params);
        CompletableFuture<Response> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        client.send(objectMapper.writeValueAsString(cmd));
        return future.thenApply(r -> null);
    }

    private void handleMessage(String message) {
        JsonNode node = objectMapper.readTree(message);
        if (node.has("id")) {
            // Response to command
            Response response = objectMapper.treeToValue(node, Response.class);
            CompletableFuture<Response> future = pendingRequests.remove(response.getId());
            if (future != null) {
                future.complete(response);
            }
        } else if (node.has("event")) {
            // Event from server
            Event event = objectMapper.treeToValue(node, Event.class);
            List<Consumer<Event>> handlers = eventHandlers.get(event.getEvent());
            if (handlers != null) {
                handlers.forEach(h -> h.accept(event));
            }
        }
    }
}
```

#### 2.2 Refactor Controllers

Replace all `ServScheduler.getServices()` calls with `apiClient` calls.

**Impact:** ~14 controller files need modification.

**Example Refactor:**

**LauncherActionController.java - BEFORE:**
```java
public void startBot() {
    ServScheduler.getServices().startBot();
}

public void stopBot() {
    ServScheduler.getServices().stopBot();
}
```

**LauncherActionController.java - AFTER:**
```java
private final BotApiClient apiClient;

public LauncherActionController(LauncherLayoutController layoutController, BotApiClient apiClient) {
    this.layoutController = layoutController;
    this.apiClient = apiClient;

    // Subscribe to events instead of registering as listener
    apiClient.onBotStateChanged(state ->
        Platform.runLater(() -> layoutController.onBotStateChange(state))
    );
    apiClient.onQueueStateChanged(state ->
        Platform.runLater(() -> layoutController.onQueueStateChange(state))
    );
}

public void startBot() {
    apiClient.startBot()
        .thenRun(() -> Platform.runLater(() -> updateUI()))
        .exceptionally(e -> {
            Platform.runLater(() -> showError(e));
            return null;
        });
}

public void stopBot() {
    apiClient.stopBot()
        .thenRun(() -> Platform.runLater(() -> updateUI()))
        .exceptionally(e -> {
            Platform.runLater(() -> showError(e));
            return null;
        });
}
```

**Files to Modify:**
1. `LauncherActionController.java`
2. `LauncherLayoutController.java`
3. `ProfileManagerActionController.java`
4. `ProfileManagerLayoutController.java`
5. `TaskManagerActionController.java`
6. `TaskManagerLayoutController.java`
7. `ConsoleLogActionController.java`
8. `ConsoleLogLayoutController.java`
9. `EmuConfigLayoutController.java`
10. `EditProfileController.java`
11. `TaskStatusModel.java`
12. `ProfileModel.java`
13. `TaskGanttOverviewController.java`
14. Other feature controllers as needed

---

### Phase 3: Process Separation

#### 3.1 Split Maven Modules

**Current Structure:**
```
wosbot/
├── wos-hmi (depends on wos-serv, wos-ot, wos-utiles)
├── wos-serv
├── wos-ot
├── wos-persistence
└── wos-utiles
```

**New Structure:**
```
wosbot/
├── wos-backend-app (new executable JAR)
│   └── depends on: wos-serv, wos-api, wos-ot, wos-persistence, wos-utiles
├── wos-hmi (refactored executable JAR)
│   └── depends on: wos-api (client only), wos-ot (DTOs only)
├── wos-api (new shared module)
│   ├── client/     # WebSocket client for UI
│   ├── server/     # WebSocket server for backend
│   ├── protocol/   # Command/Event/Response classes
│   └── dto/        # Shared DTOs (may just reference wos-ot)
├── wos-serv
├── wos-ot
├── wos-persistence
└── wos-utiles
```

#### 3.2 Create Backend Executable

**`wos-backend-app/src/main/java/cl/camodev/wosbot/backend/BackendMain.java`**
```java
package cl.camodev.wosbot.backend;

import cl.camodev.wosbot.api.server.BotWebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendMain {
    private static final Logger logger = LoggerFactory.getLogger(BackendMain.class);

    public static void main(String[] args) {
        try {
            logger.info("Starting WosBot Backend...");

            // 1. Initialize Hibernate/Database
            initializeDatabase();

            // 2. Start WebSocket server
            int port = getPortFromArgs(args, 8765);
            BotWebSocketServer server = new BotWebSocketServer(port);
            server.start();

            logger.info("Backend started on port {}", port);
            logger.info("Waiting for UI connections...");

            // 3. Keep process alive
            Thread.currentThread().join();

        } catch (Exception e) {
            logger.error("Failed to start backend: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void initializeDatabase() {
        // Initialize Hibernate session factory
        // Already done in persistence layer
    }

    private static int getPortFromArgs(String[] args, int defaultPort) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                return Integer.parseInt(args[i + 1]);
            }
        }
        return defaultPort;
    }
}
```

**`wos-backend-app/pom.xml`**
```xml
<project>
    <artifactId>wos-backend-app</artifactId>
    <name>WosBot Backend Application</name>

    <dependencies>
        <dependency>
            <groupId>cl.camodev</groupId>
            <artifactId>wos-api</artifactId>
            <version>${revision}</version>
        </dependency>
        <dependency>
            <groupId>cl.camodev</groupId>
            <artifactId>wos-serv</artifactId>
            <version>${revision}</version>
        </dependency>
        <!-- Other dependencies -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>cl.camodev.wosbot.backend.BackendMain</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 3.3 Update UI to Connect to Backend

**`FXApp.java` - Add connection logic**
```java
@Override
public void start(Stage stage) throws IOException {
    try {
        // Connect to backend before showing UI
        BotApiClient apiClient = new BotApiClient("ws://localhost:8765/bot");
        apiClient.connect().get(5, TimeUnit.SECONDS);

        logger.info("Connected to backend");

        // Pass apiClient to controllers
        LauncherLayoutController controller = new LauncherLayoutController(stage, apiClient);
        fxmlLoader.setController(controller);

        // ... rest of UI initialization

    } catch (TimeoutException e) {
        showBackendConnectionError();
        System.exit(1);
    }
}

private void showBackendConnectionError() {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Backend Connection Failed");
    alert.setHeaderText("Cannot connect to WosBot backend");
    alert.setContentText("Please ensure the backend is running on port 8765.");
    alert.showAndWait();
}
```

**Update `wos-hmi/pom.xml` - Remove backend dependencies:**
```xml
<!-- REMOVE these dependencies -->
<!--
<dependency>
    <groupId>cl.camodev</groupId>
    <artifactId>wos-serv</artifactId>
</dependency>
<dependency>
    <groupId>cl.camodev</groupId>
    <artifactId>wos-utiles</artifactId>
</dependency>
-->

<!-- KEEP these dependencies -->
<dependency>
    <groupId>cl.camodev</groupId>
    <artifactId>wos-api</artifactId>
    <classifier>client</classifier> <!-- Only client code -->
</dependency>
<dependency>
    <groupId>cl.camodev</groupId>
    <artifactId>wos-ot</artifactId> <!-- DTOs only -->
</dependency>
```

---

### Phase 4: Deployment & Launch

#### 4.1 Startup Scripts

**`start.bat` (Windows)**
```batch
@echo off
echo Starting WosBot Backend...
start "WosBot Backend" java -jar wos-backend-app-1.5.4.jar

echo Waiting for backend to start...
timeout /t 3 /nobreak >nul

echo Starting WosBot UI...
start "WosBot UI" javaw -jar wos-hmi-1.5.4.jar

echo WosBot started. Close this window to keep both processes running.
pause
```

**`start.sh` (Linux/Mac)**
```bash
#!/bin/bash

echo "Starting WosBot Backend..."
java -jar wos-backend-app-1.5.4.jar &
BACKEND_PID=$!
echo $BACKEND_PID > backend.pid

sleep 3

echo "Starting WosBot UI..."
java -jar wos-hmi-1.5.4.jar &
UI_PID=$!
echo $UI_PID > ui.pid

echo "WosBot started."
echo "Backend PID: $BACKEND_PID"
echo "UI PID: $UI_PID"
```

**`stop.bat` (Windows)**
```batch
@echo off
echo Stopping WosBot...
taskkill /FI "WINDOWTITLE eq WosBot Backend*" /T /F
taskkill /FI "WINDOWTITLE eq WosBot UI*" /T /F
echo WosBot stopped.
```

**`stop.sh` (Linux/Mac)**
```bash
#!/bin/bash

if [ -f backend.pid ]; then
    kill $(cat backend.pid)
    rm backend.pid
fi

if [ -f ui.pid ]; then
    kill $(cat ui.pid)
    rm ui.pid
fi

echo "WosBot stopped."
```

#### 4.2 Process Management Features

**Backend:**
- Writes PID to `backend.pid` on startup
- Exposes health check endpoint: `GET /health`
- Graceful shutdown on `SIGTERM`
- Logs to `logs/backend.log`

**UI:**
- Checks for running backend on startup
- Shows "Connecting..." dialog if backend unavailable
- Implements reconnection logic (retry every 5s)
- Sends "shutdown" command to backend on exit (optional)

**Health Check Implementation:**
```java
// In BackendMain.java
server.addHealthCheckHandler(() -> {
    return Map.of(
        "status", "UP",
        "uptime", getUptimeSeconds(),
        "activeClients", server.getActiveClientCount()
    );
});
```

---

## Implementation Recommendations

### Technology Stack

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| **WebSocket Server** | Jetty WebSocket or Spring Boot WebSocket | Already has Spring Boot dependency |
| **Protocol** | JSON-RPC 2.0 or custom JSON | Simple, debuggable, reuses Jackson |
| **Serialization** | Jackson (already in use) | No new dependencies |
| **Client Library** | Java-WebSocket or OkHttp | Lightweight, mature |
| **Threading** | Virtual threads (JDK 21+) | Backend already uses `Thread.ofVirtual()` |

### WebSocket Library Dependencies

**Backend (wos-api/pom.xml):**
```xml
<dependency>
    <groupId>org.eclipse.jetty.websocket</groupId>
    <artifactId>websocket-jetty-server</artifactId>
    <version>11.0.15</version>
</dependency>
```

**UI Client (wos-api/pom.xml):**
```xml
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.4</version>
</dependency>
```

### Port Configuration

- **Default:** `ws://localhost:8765`
- **Configurable via:** `config.properties` or command-line arg `--port 8765`
- **UI Fallback:** Try ports 8765-8769 if default fails
- **Security:** Bind to `localhost` only (no remote access by default)

### Error Handling

| Scenario | Behavior |
|----------|----------|
| **Connection loss** | UI shows warning banner, queues commands, retries every 5s |
| **Command timeout** | Commands timeout after 30s, show error dialog |
| **Backend crash** | UI detects via heartbeat (ping/pong), offers restart button |
| **Serialization error** | Log error, return error response to client |
| **Port in use** | Backend tries next port (8766-8769), logs warning |

### Security Considerations (Future)

- **Authentication:** Add token in WebSocket handshake header
- **Encryption:** Use WSS (TLS) for remote access
- **Current:** Bind to `localhost` only (no external access)

---

## Migration Strategy

### Option A: Big Bang (NOT RECOMMENDED)
Implement all phases at once. **High risk** of breaking existing functionality.

### Option B: Incremental (RECOMMENDED)

| Week | Phase | Tasks | Validation |
|------|-------|-------|------------|
| **1-2** | Phase 1 | • Create `wos-api` module<br>• Implement WebSocket server<br>• Create `BotApiService` facade<br>• Keep UI unchanged | Test API with Postman/wscat |
| **3** | Phase 2 (partial) | • Create `BotApiClient`<br>• Refactor `LauncherActionController`<br>• Refactor `LauncherLayoutController` | Verify bot start/stop via WebSocket |
| **4** | Phase 2 (complete) | • Refactor remaining 12 controllers<br>• Remove `wos-serv` imports | Full feature regression test |
| **5** | Phase 3 | • Create `wos-backend-app` module<br>• Update `wos-hmi/pom.xml`<br>• Build separate JARs | Test launch scripts |
| **6** | Phase 4 | • Polish error handling<br>• Add reconnection logic<br>• Write documentation | E2E testing, manual QA |

---

## Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|-----------|
| **Serialization issues** (circular refs, lazy loading) | HIGH | MEDIUM | • Use `@JsonIgnore` on problematic fields<br>• Test all DTOs early<br>• Add DTO validation tests |
| **Race conditions** in async calls | MEDIUM | LOW | • Use `CompletableFuture` with proper error handling<br>• Add timeout to all commands<br>• Log all async errors |
| **Backend crash** takes down everything | HIGH | MEDIUM | • Implement UI resilience (reconnect, queue commands)<br>• Add backend health monitoring<br>• Automatic restart on crash |
| **Performance degradation** | LOW | LOW | • WebSocket overhead is <1ms<br>• Profile if needed<br>• Use binary protocol if required |
| **Regression** in existing features | HIGH | HIGH | **Write integration tests BEFORE refactoring**<br>• Test all 14 feature modules<br>• Manual QA checklist |

---

## Testing Strategy

### 1. Unit Tests
- Mock `BotApiClient` in controller tests
- Test command serialization/deserialization
- Test event handler registration

### 2. Integration Tests
- Start backend + UI in same JVM
- Test via API (bypass network)
- Verify all 40+ API calls work correctly

### 3. E2E Tests
- Launch separate processes
- Test full user scenarios
- Verify reconnection logic

### 4. Manual Testing Checklist

**Core Functionality:**
- [ ] Start/stop bot
- [ ] Pause/resume all queues
- [ ] Pause/resume specific queue
- [ ] Profile switching
- [ ] Profile creation/editing

**Feature Modules (14 total):**
- [ ] Task Manager
- [ ] City Upgrades
- [ ] City Events
- [ ] Polar Terror
- [ ] Shop
- [ ] Gather
- [ ] Intel
- [ ] Alliance
- [ ] Alliance Championship
- [ ] Alliance Shop
- [ ] Alliance Mobilization
- [ ] Bear Trap
- [ ] Training
- [ ] Pets
- [ ] Events
- [ ] Experts
- [ ] Chief Order
- [ ] Config

**Error Handling:**
- [ ] Backend not running on UI startup
- [ ] Backend crash during operation
- [ ] Network timeout
- [ ] Invalid command
- [ ] Serialization error

**Performance:**
- [ ] UI responsiveness (no blocking)
- [ ] Log console updates in real-time
- [ ] Stamina updates reflect immediately
- [ ] Queue state changes update UI

---

## Future Enhancements (Post-Separation)

### 1. Web UI
Reuse WebSocket API to build browser-based UI:
- React/Vue frontend
- Same WebSocket protocol
- Remote management via browser

### 2. Remote Management
Expose API over network with authentication:
- Add JWT authentication
- Use WSS (TLS) for encryption
- IP whitelist for security

### 3. Multiple UI Instances
Support multiple clients monitoring same backend:
- Each client gets own session
- Broadcast events to all clients
- Lock mechanism for concurrent commands

### 4. REST API
Add HTTP endpoints alongside WebSocket for scripting:
```
POST /api/bot/start
POST /api/bot/stop
GET /api/profiles
GET /api/status
```

### 5. Health Monitoring
Backend exposes metrics for monitoring tools:
```
GET /health
GET /metrics (Prometheus format)
```

### 6. Plugin System
Load feature modules dynamically:
- Hot-reload modules without restart
- Community-contributed modules
- Sandboxed execution

---

## Files Requiring Modification

### New Files (~20)

**wos-api module:**
- `src/main/java/cl/camodev/wosbot/api/server/BotWebSocketServer.java`
- `src/main/java/cl/camodev/wosbot/api/server/WebSocketSessionManager.java`
- `src/main/java/cl/camodev/wosbot/api/server/MessageDispatcher.java`
- `src/main/java/cl/camodev/wosbot/api/client/BotApiClient.java`
- `src/main/java/cl/camodev/wosbot/api/protocol/Command.java`
- `src/main/java/cl/camodev/wosbot/api/protocol/Event.java`
- `src/main/java/cl/camodev/wosbot/api/protocol/Response.java`
- `src/main/java/cl/camodev/wosbot/api/service/BotApiService.java`
- `pom.xml`

**wos-backend-app module:**
- `src/main/java/cl/camodev/wosbot/backend/BackendMain.java`
- `pom.xml`

**Scripts:**
- `start.bat`
- `start.sh`
- `stop.bat`
- `stop.sh`

**Tests:**
- `wos-api/src/test/java/cl/camodev/wosbot/api/ApiIntegrationTest.java`
- `wos-api/src/test/java/cl/camodev/wosbot/api/DtoSerializationTest.java`

### Modified Files (~20)

**Controllers (14 files):**
- `wos-hmi/src/main/java/cl/camodev/wosbot/launcher/view/LauncherActionController.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/launcher/view/LauncherLayoutController.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/profile/controller/ProfileManagerActionController.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/profile/view/ProfileManagerLayoutController.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/taskmanager/controller/TaskManagerActionController.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/taskmanager/view/TaskManagerLayoutController.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/console/controller/ConsoleLogActionController.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/console/view/ConsoleLogLayoutController.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/emulator/view/EmuConfigLayoutController.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/profile/view/EditProfileController.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/taskmanager/model/impl/TaskStatusModel.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/profile/model/impl/ProfileModel.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/taskmanager/view/TaskGanttOverviewController.java`
- + other feature controllers as needed

**Entry points:**
- `wos-hmi/src/main/java/cl/camodev/wosbot/main/FXApp.java`
- `wos-hmi/src/main/java/cl/camodev/wosbot/main/Main.java`

**Build files:**
- `wos-hmi/pom.xml` (remove backend dependencies)
- `pom.xml` (add new modules)

### Deleted Dependencies

From `wos-hmi/pom.xml`:
```xml
<!-- REMOVE these -->
<dependency>
    <groupId>cl.camodev</groupId>
    <artifactId>wos-serv</artifactId>
</dependency>
<dependency>
    <groupId>cl.camodev</groupId>
    <artifactId>wos-utiles</artifactId>
</dependency>
```

---

## Effort Estimation

| Phase | Tasks | Effort | Complexity |
|-------|-------|--------|-----------|
| **Phase 1: Backend API** | Create API module, WebSocket server, service facade | 3-5 days | Medium |
| **Phase 2: UI Refactor** | Create client, refactor 14 controllers, update entry point | 5-7 days | Medium-High |
| **Phase 3: Process Split** | Create backend module, update build, split JARs | 2-3 days | Low |
| **Phase 4: Polish & Test** | Error handling, reconnection, scripts, testing | 3-5 days | Medium |
| **Total** | | **13-20 days** | |

**Assumptions:**
- 1 developer working full-time
- Existing codebase is stable and well-understood
- No major architectural changes beyond separation
- Testing infrastructure exists

---

## Conclusion

The current wos-hmi architecture is **tightly coupled** to wos-serv via:
- 40+ direct singleton method calls
- Listener pattern with JavaFX threading
- Shared DTOs (low coupling)

**Recommended approach:**
1. **Introduce WebSocket API layer** (backward-compatible, no UI changes)
2. **Incrementally refactor UI** to use API client instead of direct calls
3. **Split into separate processes** once refactor is complete and tested
4. **Deploy with launch scripts** that manage both processes

**WebSocket is the optimal IPC mechanism** due to:
- Cross-platform support (Windows, Linux)
- Easy debugging (JSON protocol)
- Future extensibility (web UI, remote access, multiple clients)
- Mature libraries and tools

**Key success factors:**
- Write tests BEFORE refactoring
- Implement incrementally (week-by-week)
- Keep backward compatibility during Phase 1
- Thorough regression testing after Phase 2

---

## Next Steps

1. **Review this roadmap** with the team
2. **Prioritize phases** based on business needs
3. **Set up development branch** for API work
4. **Create Phase 1 tasks** in project management tool
5. **Begin implementation** of `wos-api` module

---

**Document Version:** 1.0
**Last Updated:** December 2, 2025
**Author:** Claude Code Analysis
