# Phase 2: UI Client Implementation - Overview

## Goal
Refactor the JavaFX UI to communicate with the backend via WebSocket API instead of direct service calls.

## Duration
**5-7 days**

## Objectives
1. ✅ Create WebSocket client library
2. ✅ Refactor LauncherActionController (pilot refactor)
3. ✅ Refactor remaining 13 controllers
4. ✅ Remove wos-serv dependency from wos-hmi
5. ✅ Implement error handling and reconnection logic
6. ✅ Test all UI features work via API

## Success Criteria
- [ ] All controllers use BotApiClient instead of ServScheduler/ServConfig
- [ ] No direct imports of wos-serv classes in wos-hmi
- [ ] UI works identically to before (no functionality regression)
- [ ] UI handles connection loss gracefully
- [ ] All manual tests pass (14 feature modules + core functionality)

## Key Deliverables
- `BotApiClient` class (WebSocket client)
- 14 refactored controller files
- Error handling and reconnection logic
- Updated pom.xml (removed wos-serv dependency)
- Integration tests

## Dependencies
- Completed Phase 1 (WebSocket API server must be working)
- Java-WebSocket library or OkHttp WebSocket
- Existing wos-hmi UI code

## Risks
| Risk | Mitigation |
|------|-----------|
| Breaking existing UI functionality | Refactor incrementally, test after each controller |
| Async complexity in JavaFX | Use CompletableFuture + Platform.runLater correctly |
| Connection loss during operation | Implement reconnection queue, show user feedback |

## Sub-Steps
1. [2.1 Create WebSocket Client Layer](2.1-websocket-client.md)
2. [2.2 Refactor Controllers](2.2-refactor-controllers.md)
3. [2.3 Error Handling & Reconnection](2.3-error-handling.md)
4. [Testing](testing.md)

## Timeline
```
Day 1: Step 2.1 (WebSocket client implementation)
Day 2: Step 2.2 (Refactor LauncherActionController - pilot)
Day 3: Step 2.2 (Refactor 5 more controllers)
Day 4: Step 2.2 (Refactor remaining controllers)
Day 5: Step 2.3 (Error handling)
Day 6-7: Testing + fixes
```

## Next Phase
Once Phase 2 is complete, proceed to [Phase 3: Process Separation](../phase-3-process-separation/overview.md).

---

[← Back to Main Plan](../README.md) | [Next: 2.1 WebSocket Client →](2.1-websocket-client.md)
