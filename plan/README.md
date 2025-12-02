# WosBot UI/Backend Separation - Implementation Plan

## Table of Contents

### Overview
- [Main Roadmap](../UI-BACKEND-SEPARATION-ROADMAP.md) - Complete analysis and summary

### Phase 1: Backend API Layer (3-5 days)
**Goal:** Create WebSocket API layer without modifying UI

- [Phase 1 Overview](phase-1-backend-api/overview.md)
- [1.1 Create API Module Structure](phase-1-backend-api/1.1-create-api-module.md)
- [1.2 Implement WebSocket Server](phase-1-backend-api/1.2-websocket-server.md)
- [1.3 Build Service Facade](phase-1-backend-api/1.3-service-facade.md)
- [1.4 Design Protocol](phase-1-backend-api/1.4-protocol-design.md)
- [1.5 Convert Listeners to Event Emitters](phase-1-backend-api/1.5-listener-conversion.md)
- [Phase 1 Testing](phase-1-backend-api/testing.md)

### Phase 2: UI Client Implementation (5-7 days)
**Goal:** Refactor UI to use WebSocket client instead of direct service calls

- [Phase 2 Overview](phase-2-ui-client/overview.md)
- [2.1 Create WebSocket Client Layer](phase-2-ui-client/2.1-websocket-client.md)
- [2.2 Refactor Controllers](phase-2-ui-client/2.2-refactor-controllers.md)
- [2.3 Error Handling & Reconnection](phase-2-ui-client/2.3-error-handling.md)
- [Phase 2 Testing](phase-2-ui-client/testing.md)

### Phase 3: Process Separation (2-3 days)
**Goal:** Split into independent backend and UI executables

- [Phase 3 Overview](phase-3-process-separation/overview.md)
- [3.1 Split Maven Modules](phase-3-process-separation/3.1-split-modules.md)
- [3.2 Create Backend Executable](phase-3-process-separation/3.2-backend-executable.md)
- [3.3 Update UI Connection Logic](phase-3-process-separation/3.3-ui-connection.md)
- [Phase 3 Testing](phase-3-process-separation/testing.md)

### Phase 4: Deployment & Polish (3-5 days)
**Goal:** Production-ready deployment with scripts and monitoring

- [Phase 4 Overview](phase-4-deployment/overview.md)
- [4.1 Create Launch Scripts](phase-4-deployment/4.1-launch-scripts.md)
- [4.2 Process Management](phase-4-deployment/4.2-process-management.md)
- [4.3 Monitoring & Health Checks](phase-4-deployment/4.3-monitoring.md)
- [Phase 4 Testing](phase-4-deployment/testing.md)

### Appendices
- [IPC Options Comparison](appendix/ipc-comparison.md)
- [Risks & Mitigation Strategies](appendix/risks-mitigation.md)
- [Dependencies & Libraries](appendix/dependencies.md)
- [Future Enhancements](appendix/future-enhancements.md)
- [Testing Checklist](appendix/testing-checklist.md)

---

## Quick Start Guide

### Prerequisites
- JDK 21+ (project uses virtual threads)
- Maven 3.8+
- Existing wosbot codebase

### Phase Execution Order
1. **Phase 1** - Build API layer (can be tested independently with Postman/wscat)
2. **Phase 2** - Refactor UI incrementally (start with LauncherActionController)
3. **Phase 3** - Split processes (requires Phases 1-2 complete)
4. **Phase 4** - Deployment polish (final step)

### Key Milestones
- ✅ **Milestone 1:** WebSocket server responds to test commands
- ✅ **Milestone 2:** One controller (Launcher) works via API
- ✅ **Milestone 3:** All controllers refactored, UI no longer depends on wos-serv
- ✅ **Milestone 4:** Backend and UI run as separate processes
- ✅ **Milestone 5:** Production deployment with scripts and monitoring

---

## Document Navigation Tips

- Each phase folder contains an `overview.md` with phase goals and context
- Numbered files (e.g., `1.1-`, `1.2-`) represent sequential sub-steps
- `testing.md` in each phase describes validation steps
- Appendix contains reference material and analysis

---

**Total Estimated Effort:** 13-20 days (1 developer)

**Last Updated:** December 2, 2025
