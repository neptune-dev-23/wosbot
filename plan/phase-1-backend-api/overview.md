# Phase 1: Backend API Layer - Overview

## Goal
Create a WebSocket-based API layer that wraps existing backend services without modifying the UI. This phase establishes the foundation for UI/backend separation while maintaining backward compatibility.

## Duration
**3-5 days**

## Objectives
1. ✅ Create new `wos-api` module with clean structure
2. ✅ Implement a Spring Boot-based WebSocket server
3. ✅ Build service facade (`BotApiService`) that wraps all backend calls
4. ✅ Design JSON-based command/event protocol
5. ✅ Convert listener callbacks to WebSocket event broadcasts
6. ✅ Test API independently (no UI changes yet)

## Success Criteria
- [ ] WebSocket server starts on port 8765
- [ ] Can send `startBot` command via wscat/Postman and backend executes it
- [ ] Backend emits `botStateChanged` events to connected clients
- [ ] All 40+ backend service calls have API equivalents
- [ ] Existing UI still works unchanged (backward compatibility)

## Test Verification

### Run All Tests
```bash
# Single command to verify all Phase 1 success criteria
mvn -pl wos-api clean test
```

**Per-Step Success Criteria:** Each step document (1.1-1.5) contains detailed success criteria with automated test commands.

---

## Key Deliverables
- `wos-api` module with working WebSocket server
- `BotApiService` facade class
- Protocol documentation (Command/Event/Response schemas)
- Integration tests for API endpoints
- API testing guide (using wscat or Postman)

## Dependencies
- Existing `wos-serv` module (wraps these services)
- Existing `wos-ot` module (reuses DTOs)
- Jackson library (already in project)
- Spring Boot WebSocket (`spring-boot-starter-websocket`) (new dependency)

## Build Scope
All Maven commands for Phase 1 should target the `wos-api` module (for example, `mvn -pl wos-api clean test`). Building the entire reactor will likely fail because the other modules’ artifacts are not distributed yet, so keep compilation and testing confined to `wos-api` unless another module is explicitly required.

## Risks
| Risk | Mitigation |
|------|-----------|
| DTO serialization issues | Test all DTOs early, add `@JsonIgnore` where needed |
| Listener conversion breaks callbacks | Keep existing listeners working, add WebSocket broadcast in parallel |
| Port conflicts | Make port configurable, try fallback ports |

## Sub-Steps
1. [1.1 Create API Module Structure](1.1-create-api-module.md) - Set up Maven module and packages
2. [1.2 Implement WebSocket Server](1.2-websocket-server.md) - Build the server with Spring Boot WebSocket handlers
3. [1.3 Build Service Facade](1.3-service-facade.md) - Wrap all backend service calls
4. [1.4 Design Protocol](1.4-protocol-design.md) - Define Command/Event/Response formats
5. [1.5 Convert Listeners to Event Emitters](1.5-listener-conversion.md) - Broadcast events via WebSocket
6. [Testing](testing.md) - Validate API works independently

## Timeline
```
Day 1: Steps 1.1 + 1.2 (Module setup + WebSocket server)
Day 2: Step 1.3 (Service facade - half of methods)
Day 3: Step 1.3 (Service facade - complete) + 1.4 (Protocol)
Day 4: Step 1.5 (Listener conversion)
Day 5: Testing + fixes
```

## Next Phase
Once Phase 1 is complete and tested, proceed to [Phase 2: UI Client Implementation](../phase-2-ui-client/overview.md) to refactor the UI to use this new API.

---

[← Back to Main Plan](../README.md) | [Next: 1.1 Create API Module →](1.1-create-api-module.md)
