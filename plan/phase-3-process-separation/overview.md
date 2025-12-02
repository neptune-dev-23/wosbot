# Phase 3: Process Separation - Overview

## Goal
Split the monolithic application into two independent processes: backend (wos-backend-app) and UI (wos-hmi).

## Duration
**2-3 days**

## Objectives
1. ✅ Create wos-backend-app module with BackendMain
2. ✅ Update wos-hmi to be pure UI (remove backend dependencies)
3. ✅ Configure Maven to build two separate JARs
4. ✅ Implement connection logic in UI
5. ✅ Test backend and UI run as separate processes

## Success Criteria
- [ ] Can start backend independently: `java -jar wos-backend-app.jar`
- [ ] Can start UI independently: `java -jar wos-hmi.jar`
- [ ] UI connects to backend on startup
- [ ] UI shows error if backend is not running
- [ ] Both processes run concurrently
- [ ] Backend continues running if UI closes

## Key Deliverables
- wos-backend-app module with BackendMain class
- Updated pom.xml files (dependency separation)
- Two executable JARs
- Connection logic in FXApp.java
- README for running split processes

## Dependencies
- Completed Phase 2 (UI must use WebSocket client)
- Maven configured correctly
- No lingering wos-serv imports in wos-hmi

## Risks
| Risk | Mitigation |
|------|-----------|
| Build configuration errors | Test Maven build early, verify JARs are executable |
| UI can't find backend | Implement retry logic, show clear error messages |
| Database/resource conflicts | Ensure backend owns all resources |

## Sub-Steps
1. [3.1 Split Maven Modules](3.1-split-modules.md)
2. [3.2 Create Backend Executable](3.2-backend-executable.md)
3. [3.3 Update UI Connection Logic](3.3-ui-connection.md)
4. [Testing](testing.md)

## Timeline
```
Day 1: Steps 3.1 + 3.2 (Module split + backend executable)
Day 2: Step 3.3 (UI connection logic)
Day 3: Testing + fixes
```

## Next Phase
Once Phase 3 is complete, proceed to [Phase 4: Deployment & Polish](../phase-4-deployment/overview.md).

---

[← Phase 2](../phase-2-ui-client/overview.md) | [Back to Main Plan](../README.md) | [Next: 3.1 Split Modules →](3.1-split-modules.md)
