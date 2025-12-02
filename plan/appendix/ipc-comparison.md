# IPC Options Detailed Comparison

## Overview

This document provides a detailed technical comparison of inter-process communication (IPC) mechanisms for the WosBot UI/Backend separation.

---

## Option 1: WebSocket (JSON) ✅ RECOMMENDED

### Description
Full-duplex communication over TCP using WebSocket protocol with JSON-encoded messages.

### Technical Details
- **Protocol:** WebSocket (RFC 6455) over TCP
- **Serialization:** JSON (Jackson library)
- **Libraries:** Jetty WebSocket (server), Java-WebSocket (client)
- **Port:** Configurable (default: 8765)

### Pros
✅ **Bidirectional:** Full-duplex, either side can initiate messages
✅ **Cross-platform:** Works on Windows, Linux, Mac
✅ **Debuggable:** JSON is human-readable, use browser dev tools
✅ **Mature ecosystem:** Many libraries, tools, documentation
✅ **Future-proof:** Can support web UI, remote access, mobile apps
✅ **Multiple clients:** Broadcast events to all connected UIs
✅ **Standardized:** Widely adopted, RFC-defined
✅ **Reconnection:** Built-in connection management
✅ **Firewall-friendly:** Uses standard HTTP ports, upgrades to WS

### Cons
❌ **Network overhead:** TCP stack, framing, JSON serialization
❌ **Port management:** Must ensure port is available
❌ **Latency:** ~1-5ms overhead vs direct call (negligible for UI)

### Performance
- **Latency:** 1-5ms per message
- **Throughput:** 10,000+ messages/second (more than sufficient)
- **Overhead:** ~500 bytes/message for typical command

### Implementation Complexity
- **Server:** Medium (Jetty WebSocket - ~300 lines)
- **Client:** Low (Java-WebSocket library - ~200 lines)
- **Protocol:** Low (JSON schemas)

### Example Code
```java
// Client
WebSocketClient client = new WebSocketClient(new URI("ws://localhost:8765/bot"));
client.send("{\"command\":\"startBot\"}");

// Server
@OnWebSocketMessage
public void onMessage(Session session, String message) {
    // Handle command
}
```

### Best For
- Applications requiring flexibility
- Future web UI or remote access
- Multiple client support
- Easy debugging

---

## Option 2: Named Pipes

### Description
OS-native IPC using named pipes (Windows) or FIFOs (Linux).

### Technical Details
- **Protocol:** Custom framing required
- **Platform:** Windows (Named Pipes), Linux (FIFO)
- **Serialization:** Custom or JSON
- **Libraries:** Java NIO FileChannel, JNA for native APIs

### Pros
✅ **Low latency:** No network stack overhead
✅ **OS-native:** Built into operating system
✅ **No ports:** No port conflicts possible

### Cons
❌ **Platform-specific:** Different APIs for Windows/Linux
❌ **No built-in framing:** Must implement message boundaries
❌ **Limited tooling:** Harder to debug than WebSocket
❌ **Single client:** Typically one reader, one writer
❌ **No remote access:** Cannot connect from another machine
❌ **No reconnection:** Manual reconnection logic required
❌ **Blocks future:** Cannot easily add web UI

### Performance
- **Latency:** <1ms (faster than WebSocket)
- **Throughput:** Very high (memory-to-memory)

### Implementation Complexity
- **Server:** High (platform-specific, framing protocol)
- **Client:** High (same as server)
- **Protocol:** High (custom message framing)

### Example Code (Windows)
```java
// Very platform-specific
RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\wosbot", "rw");
FileChannel channel = pipe.getChannel();
// Must implement message framing manually
```

### Best For
- High-performance local IPC
- Single platform (Windows-only or Linux-only)
- When network stack is unacceptable

---

## Option 3: gRPC

### Description
Modern RPC framework using Protocol Buffers and HTTP/2.

### Technical Details
- **Protocol:** gRPC (HTTP/2)
- **Serialization:** Protocol Buffers (binary)
- **Libraries:** gRPC Java
- **Port:** Configurable

### Pros
✅ **Efficient:** Binary serialization, HTTP/2 multiplexing
✅ **Type-safe:** .proto contracts enforced
✅ **Bidirectional streaming:** Supports push from server
✅ **Code generation:** Automatic client/server stubs

### Cons
❌ **Complex:** Steeper learning curve
❌ **Build complexity:** Requires protoc compiler, codegen
❌ **Overkill:** More complexity than needed for this use case
❌ **Debugging:** Binary format harder to inspect
❌ **HTTP/2 dependency:** Requires HTTP/2 support

### Performance
- **Latency:** 2-10ms (similar to WebSocket)
- **Throughput:** Very high (binary protocol)
- **Overhead:** Lower than JSON (binary)

### Implementation Complexity
- **Server:** Medium-High (define .proto, implement service)
- **Client:** Medium-High (generated code)
- **Protocol:** Medium (.proto definitions)

### Example Code
```protobuf
// bot.proto
service BotService {
  rpc StartBot(StartBotRequest) returns (StartBotResponse);
}

// Generated code
BotServiceGrpc.BotServiceBlockingStub stub = ...;
stub.startBot(StartBotRequest.newBuilder().build());
```

### Best For
- High-performance microservices
- Strong typing requirements
- Large-scale distributed systems

---

## Option 4: REST API + Server-Sent Events (SSE)

### Description
HTTP REST API for commands, SSE for server→client events.

### Technical Details
- **Protocol:** HTTP (REST), SSE (text/event-stream)
- **Serialization:** JSON
- **Libraries:** Spring Boot REST, JAX-RS
- **Port:** Configurable (default: 8080)

### Pros
✅ **Simple REST API:** Easy to understand
✅ **Already has Spring Boot:** Reuse existing dependency
✅ **Browser-compatible:** Works in web browsers
✅ **Cacheable:** HTTP caching for read operations

### Cons
❌ **Unidirectional SSE:** Server→Client only, need separate channel for commands
❌ **No request/response correlation:** SSE events are independent
❌ **Overhead:** HTTP headers on every request
❌ **Polling needed:** Or use SSE + separate HTTP calls
❌ **Less efficient:** Than WebSocket for high-frequency updates

### Performance
- **Latency:** 5-20ms (HTTP overhead)
- **Throughput:** Lower than WebSocket

### Implementation Complexity
- **Server:** Low (Spring Boot REST controllers)
- **Client:** Low (HttpClient + SSE client)
- **Protocol:** Low (standard REST)

### Example Code
```java
// REST API
@PostMapping("/bot/start")
public void startBot() { ... }

// SSE
@GetMapping(value = "/events", produces = "text/event-stream")
public SseEmitter events() { ... }
```

### Best For
- Simple stateless operations
- When WebSocket is not available
- HTTP-only environments

---

## Decision Matrix

| Criteria | WebSocket | Named Pipes | gRPC | REST+SSE |
|----------|-----------|-------------|------|----------|
| **Latency** | 🟡 Good (1-5ms) | 🟢 Excellent (<1ms) | 🟡 Good (2-10ms) | 🔴 Fair (5-20ms) |
| **Cross-platform** | 🟢 Yes | 🔴 No | 🟢 Yes | 🟢 Yes |
| **Bidirectional** | 🟢 Yes | 🟢 Yes | 🟢 Yes | 🟡 SSE one-way |
| **Multiple clients** | 🟢 Yes | 🔴 No | 🟢 Yes | 🟢 Yes |
| **Debuggability** | 🟢 Excellent | 🔴 Poor | 🟡 Fair | 🟢 Good |
| **Implementation** | 🟢 Medium | 🔴 High | 🟡 Medium-High | 🟢 Low |
| **Future extensibility** | 🟢 Excellent | 🔴 Poor | 🟢 Good | 🟡 Fair |
| **Tooling** | 🟢 Excellent | 🔴 Poor | 🟢 Good | 🟢 Excellent |
| **Learning curve** | 🟢 Low | 🟡 Medium | 🔴 High | 🟢 Low |
| **Overhead** | 🟡 Low | 🟢 None | 🟢 Very Low | 🔴 Medium |

**Legend:** 🟢 Excellent | 🟡 Good | 🔴 Poor

---

## Recommendation: WebSocket

### Why WebSocket is the Best Choice

1. **Best balance:** Good performance, low complexity, high flexibility
2. **Future-proof:** Enables web UI, remote access, monitoring tools
3. **Debuggable:** JSON format, browser dev tools, wscat
4. **Standard:** Well-documented, mature ecosystem
5. **Cross-platform:** Works on Windows and Linux
6. **Already in use:** Project has Spring Boot, adding Jetty is minimal

### When to Consider Alternatives

- **Named Pipes:** Only if Windows-only, ultra-low latency required (<1ms), no future web UI
- **gRPC:** Only if building microservices, need strong typing, team has gRPC experience
- **REST+SSE:** Only if WebSocket is blocked by firewall/proxy

### Cost-Benefit Analysis

| Aspect | Cost | Benefit |
|--------|------|---------|
| Implementation | ~5 days | Clean separation, testable API |
| Performance | 1-5ms latency | Acceptable for UI responsiveness |
| Complexity | Medium | Offset by debugging ease |
| Extensibility | None | Huge (web UI, remote access, etc.) |

**Net Result:** High benefit, reasonable cost

---

## Alternative: Binary WebSocket

If JSON serialization becomes a bottleneck (unlikely), can upgrade to binary WebSocket:

- Use Protocol Buffers or MessagePack instead of JSON
- Same WebSocket infrastructure
- ~50% smaller messages
- Requires more complex serialization logic

**When to consider:** Only if profiling shows JSON is >10% of latency (very unlikely).

---

[← Back to Main Plan](../README.md)
