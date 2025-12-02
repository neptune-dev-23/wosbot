# Risks & Mitigation Strategies

## Technical Risks

### 1. Serialization Issues ⚠️ HIGH

**Risk:** DTOs fail to serialize due to circular references, lazy loading, or missing Jackson annotations.

**Impact:** Events/responses fail to send, UI shows errors

**Probability:** MEDIUM

**Mitigation:**
- Test all DTOs early in Phase 1
- Add `@JsonIgnore` to problematic fields
- Use `@JsonBackReference` / `@JsonManagedReference` for bidirectional relationships
- Configure Jackson to handle Hibernate proxies:
  ```java
  objectMapper.registerModule(new Hibernate5Module());
  ```
- Add DTO validation tests

**Fallback:** Create simple DTO wrappers without complex relationships

---

### 2. Async/Threading Complexity ⚠️ MEDIUM

**Risk:** Race conditions, deadlocks, or improper JavaFX thread usage

**Impact:** UI freezes, inconsistent state, crashes

**Probability:** LOW

**Mitigation:**
- Always use `Platform.runLater()` for UI updates
- Use `CompletableFuture` for async operations
- Add timeout to all API calls (30s default)
- Log all async errors
- Test concurrent operations thoroughly

**Fallback:** Simplify async logic, make some operations synchronous if needed

---

### 3. WebSocket Connection Loss ⚠️ HIGH

**Risk:** Network issues, backend crash, or process killed causes connection loss

**Impact:** UI becomes non-functional, users lose work

**Probability:** MEDIUM

**Mitigation:**
- Implement automatic reconnection (exponential backoff)
- Queue commands during disconnection
- Show clear connection status in UI
- Heartbeat/ping every 30s
- Graceful degradation (disable controls when disconnected)

**Fallback:** Manual reconnect button, restart application

---

### 4. Backend Crash ⚠️ HIGH

**Risk:** Backend process crashes, taking down the bot

**Impact:** Complete service outage

**Probability:** LOW-MEDIUM

**Mitigation:**
- Comprehensive error handling in backend
- Health monitoring endpoint
- Auto-restart backend on crash (watchdog process)
- Backend logs all errors
- UI detects backend down and offers restart

**Fallback:** Manual backend restart, investigate crash logs

---

### 5. Port Conflicts ⚠️ MEDIUM

**Risk:** Port 8765 already in use by another application

**Impact:** Backend fails to start

**Probability:** LOW

**Mitigation:**
- Make port configurable
- Try fallback ports (8765-8769)
- Show clear error message with port number
- Provide instructions to change port

**Fallback:** User manually changes port in config

---

### 6. Performance Degradation ⚠️ LOW

**Risk:** WebSocket overhead, serialization cost, or network latency slows down operations

**Impact:** User perceives slowness

**Probability:** LOW

**Mitigation:**
- Profile performance early
- Use binary protocol if JSON too slow
- Batch events if too frequent
- Optimize DTO serialization
- Measure and compare to baseline

**Fallback:** Accept slight overhead as trade-off for separation

---

## Implementation Risks

### 7. Breaking Existing Functionality ⚠️ HIGH

**Risk:** Refactoring introduces bugs, features stop working

**Impact:** Users cannot use application

**Probability:** HIGH (during development)

**Mitigation:**
- **Incremental refactoring** (one controller at a time)
- Test after each controller refactor
- Keep existing code working during Phase 1
- Comprehensive test suite before starting
- Rollback plan (git branches)

**Fallback:** Revert to working state, fix issues incrementally

---

### 8. Missed Backend Calls ⚠️ MEDIUM

**Risk:** Forget to wrap some backend service calls in API

**Impact:** Some UI features don't work after refactor

**Probability:** MEDIUM

**Mitigation:**
- Complete audit of all ServScheduler/ServConfig calls (done: 40+ found)
- Grep for all backend imports in wos-hmi
- Compilation will fail if imports removed too early
- Thorough testing of all 14 feature modules

**Fallback:** Add missing API methods as discovered

---

### 9. Maven Build Complexity ⚠️ MEDIUM

**Risk:** Multi-module build fails, dependency conflicts, circular dependencies

**Impact:** Cannot build project

**Probability:** LOW-MEDIUM

**Mitigation:**
- Test Maven build after each pom.xml change
- Use dependency:tree to check for conflicts
- Keep module dependencies unidirectional
- Document build process

**Fallback:** Simplify module structure if needed

---

### 10. Database/Resource Conflicts ⚠️ LOW

**Risk:** Both processes try to access database simultaneously

**Impact:** Database locks, corruption

**Probability:** LOW

**Mitigation:**
- Backend owns all database access
- UI only accesses backend via API (no direct DB access)
- SQLite WAL mode for concurrency
- Proper transaction handling

**Fallback:** Serialize database access

---

## Project Management Risks

### 11. Underestimated Effort ⚠️ MEDIUM

**Risk:** Implementation takes longer than 13-20 days estimate

**Impact:** Project delays

**Probability:** MEDIUM

**Mitigation:**
- Buffer time built into estimate
- Incremental approach allows stopping at any phase
- Prioritize core functionality over edge cases
- Daily progress tracking

**Fallback:** Extend timeline, reduce scope

---

### 12. Insufficient Testing ⚠️ HIGH

**Risk:** Not enough time for testing, bugs slip through

**Impact:** Production issues, user complaints

**Probability:** MEDIUM

**Mitigation:**
- Testing built into each phase
- Automated tests where possible
- Manual test checklist
- Don't skip testing to save time

**Fallback:** Extended testing phase post-development

---

## User Impact Risks

### 13. User Confusion ⚠️ MEDIUM

**Risk:** Users don't understand split processes, can't start application

**Impact:** Support burden, user frustration

**Probability:** MEDIUM

**Mitigation:**
- Clear launch scripts (one-click start)
- Good documentation with screenshots
- Clear error messages
- Auto-start backend from UI if missing

**Fallback:** Detailed troubleshooting guide

---

### 14. Data Loss ⚠️ LOW

**Risk:** Database migration fails, profiles lost

**Impact:** Users lose their configuration

**Probability:** LOW

**Mitigation:**
- No database schema changes planned
- Backup database before starting
- Test with copy of production database
- Versioned database schema

**Fallback:** Restore from backup

---

## Risk Summary

| Risk Level | Count | Mitigation Status |
|------------|-------|-------------------|
| HIGH       | 5     | All have mitigation plans |
| MEDIUM     | 7     | All have mitigation plans |
| LOW        | 2     | All have mitigation plans |

**Overall Project Risk:** MEDIUM

The highest risks are around:
1. Breaking existing functionality → Mitigated by incremental approach
2. Connection/backend crashes → Mitigated by reconnection logic
3. Serialization issues → Mitigated by early testing

All high-risk items have comprehensive mitigation strategies.

---

[← Back to Main Plan](../README.md)
