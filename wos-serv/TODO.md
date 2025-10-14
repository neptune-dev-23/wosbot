# Backend Task List

## 1. Diagnose and Fix `/api/profiles` Performance
- **Task:** The `/api/profiles` endpoint is taking ~2.8s. Investigate why it's so slow.
- **Sub-tasks:**
    - Review `ServProfiles.getProfiles()` method for any other slow, per-profile operations besides the now-cached `isRunning` check.
    - The user suspects something is still checking emulator status unnecessarily. Double-check the call hierarchy.

## 2. Display Next Task Time for Idle (Not Queued) Profiles
- **Task:** For profiles in the `idle-not-queued` state, display the time for their next scheduled task instead of "Not in queue".
- **Sub-tasks:**
    - In `utils/tasks.ts`, modify the `getProfileSummaryMeta` function.
    - In the logic block for the `idle-not-queued` status, add the same logic that other idle states use to find the `upcomingTask` and set the `nextExecutionLabel` and `nextExecutionSort` accordingly.
