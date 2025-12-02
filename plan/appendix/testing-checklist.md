# Complete Testing Checklist

## Pre-Implementation Testing
- [ ] Document current functionality
- [ ] Create baseline performance metrics
- [ ] Set up test database/profiles

## Phase 1: Backend API Testing
- [ ] WebSocket server starts
- [ ] Health check endpoint responds
- [ ] All 15+ commands work
- [ ] Events are broadcast correctly
- [ ] Multiple clients supported
- [ ] Error handling works

## Phase 2: UI Client Testing
- [ ] BotApiClient connects
- [ ] All controllers refactored
- [ ] No wos-serv imports remain
- [ ] UI functionality unchanged
- [ ] Async operations work correctly

## Phase 3: Process Separation Testing
- [ ] Backend runs independently
- [ ] UI runs independently
- [ ] UI connects to backend
- [ ] Connection error handling
- [ ] Both JARs executable

## Phase 4: Deployment Testing
- [ ] Launch scripts work
- [ ] Stop scripts work cleanly
- [ ] Process management functions
- [ ] Health checks work
- [ ] Documentation accurate

## Feature Module Testing

### Core Functionality
- [ ] Bot start/stop
- [ ] Bot pause/resume (all queues)
- [ ] Bot pause/resume (specific queue)
- [ ] Profile selection
- [ ] Profile creation
- [ ] Profile editing
- [ ] Profile deletion
- [ ] Configuration changes

### 14 Feature Modules
- [ ] Task Manager - View tasks, schedule tasks
- [ ] City Upgrades - Enable/disable upgrades
- [ ] City Events - Configure event handling
- [ ] Polar Terror - Configure strategy
- [ ] Shop - Item purchase settings
- [ ] Gather - Resource gathering config
- [ ] Intel - Intel gathering config
- [ ] Alliance - Alliance activities
- [ ] Alliance Championship - Championship settings
- [ ] Alliance Shop - Shop purchase config
- [ ] Alliance Mobilization - Mobilization settings
- [ ] Bear Trap - Trap configuration
- [ ] Training - Training settings
- [ ] Pets - Pet management
- [ ] Events - Event participation
- [ ] Experts - Expert skill settings
- [ ] Chief Order - Chief order handling
- [ ] Config - Emulator configuration

### UI Components
- [ ] Logs console updates in real-time
- [ ] Stamina display updates
- [ ] Queue status updates
- [ ] Profile combo box updates
- [ ] Window title updates
- [ ] All buttons functional
- [ ] All text fields editable
- [ ] All checkboxes work
- [ ] All combo boxes work

### Error Scenarios
- [ ] Backend not running on UI start
- [ ] Backend crashes during operation
- [ ] Network timeout
- [ ] Invalid command sent
- [ ] Missing required parameter
- [ ] Database locked
- [ ] Emulator not found
- [ ] Profile not found

### Performance
- [ ] UI remains responsive
- [ ] No memory leaks (1 hour test)
- [ ] Log file size reasonable
- [ ] Database queries efficient
- [ ] WebSocket latency < 100ms

### Cross-Platform (if applicable)
- [ ] Works on Windows 10
- [ ] Works on Windows 11
- [ ] Works on Linux (if supported)

## Regression Testing
- [ ] All existing features work
- [ ] No new bugs introduced
- [ ] Performance not degraded
- [ ] Database migration works (if changed)
- [ ] Existing profiles load correctly

## User Acceptance Testing
- [ ] User can start application easily
- [ ] User understands connection status
- [ ] Error messages are clear
- [ ] Documentation is understandable
- [ ] No unexpected behavior

## Final Validation
- [ ] All tests documented
- [ ] All issues resolved or documented
- [ ] Code reviewed
- [ ] Documentation complete
- [ ] Ready for production

---

**Total Tests:** ~80+
**Recommended Test Cycle Time:** 2-3 days full regression
