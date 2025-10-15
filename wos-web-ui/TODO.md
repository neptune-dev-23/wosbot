## TODO

- [ ] Normalize status ordering so executing profiles surface ahead of idle ones in both Tasks and Profiles views by tweaking `PROFILE_STATUS_ORDER` and related sorting helpers.
- [ ] Update countdown rendering to show `"Executing"` for in-progress entries, `"Ready"` at zero seconds, and keep the formatted timestamp available for tooltips.
- [ ] Introduce a shared ticking clock (1 Hz) that only updates countdowns that are currently visible (summary cards and expanded sections) to avoid unnecessary renders.
- [ ] Ensure the “focus” navigation from Profiles → Tasks expands the target profile, collapses the rest, leaves the profile filter on “All”, and scrolls the section into view.
- [ ] Restore the sidebar hamburger button: keep it pinned to the left edge, visible in both collapsed and expanded states, and adjust CSS so it no longer hides under the clip-mask.
- [ ] Add lightweight slide/fade transitions for profile and task reordering so resorting feels smooth.
- [x] Review `AGENTS.md` for current frontend task guidelines.
- [x] Add task rescheduling control to the Tasks view.
  - [x] Add a clock-icon button to each task item without altering layout dimensions.
  - [x] Show a reschedule modal with a datetime picker defaulting to the current time when the button is clicked.
  - [x] Persist the scheduled time through existing task update wiring or a dedicated action.


To resume codex session: codex resume 0199df90-fe8f-70b3-8c37-9de048828cfd
