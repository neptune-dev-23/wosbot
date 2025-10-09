package cl.camodev.utiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class that helps determine the current status of the
 * "Bear Trap" event window — a recurring time window anchored at a reference time.
 *
 * Semantics (anchor model):
 * - Start  = referenceAnchorUtc - n minutes
 * - End    = referenceAnchorUtc + 30 minutes
 * - Length = 30 + n minutes
 * - Repeats every 'intervalDays' days (default: 2)
 *
 * This class can calculate whether the current time is:
 * BEFORE the active window, INSIDE the active window (end inclusive), or AFTER it,
 * and provide timing details such as how long until the next cycle.
 */
public final class BearTrapHelper {

    /** Fixed portion of the window (in minutes). */
    private static final int DEFAULT_FIXED_WINDOW_MINUTES = 30;

    /** Interval (in days) between each Bear Trap cycle. */
    private static final int DEFAULT_INTERVAL_DAYS = 2;

    /** Prevent instantiation. */
    private BearTrapHelper() {}

    /**
     * Possible states relative to a Bear Trap window.
     */
    public enum WindowState {
        BEFORE,   // Before the current cycle's window
        INSIDE,   // Inside the execution window (end inclusive)
        AFTER     // After the current cycle's window
    }

    /**
     * Result of a window calculation with timing details and current state.
     */
    public static final class WindowResult {
        private final WindowState state;
        private final Instant currentWindowStart;
        private final Instant currentWindowEnd;
        private final Instant nextWindowStart;
        private final long minutesUntilNextWindow;
        private final int currentWindowDurationMinutes;

        private WindowResult(
                WindowState state,
                Instant currentWindowStart,
                Instant currentWindowEnd,
                Instant nextWindowStart,
                long minutesUntilNextWindow,
                int currentWindowDurationMinutes
        ) {
            this.state = state;
            this.currentWindowStart = currentWindowStart;
            this.currentWindowEnd = currentWindowEnd;
            this.nextWindowStart = nextWindowStart;
            this.minutesUntilNextWindow = minutesUntilNextWindow;
            this.currentWindowDurationMinutes = currentWindowDurationMinutes;
        }

        public WindowState getState() { return state; }
        public Instant getCurrentWindowStart() { return currentWindowStart; }
        public Instant getCurrentWindowEnd() { return currentWindowEnd; }
        public Instant getNextWindowStart() { return nextWindowStart; }
        public long getMinutesUntilNextWindow() { return minutesUntilNextWindow; }
        public int getCurrentWindowDurationMinutes() { return currentWindowDurationMinutes; }

        @Override
        public String toString() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'UTC'")
                    .withZone(ZoneOffset.UTC);

            StringBuilder sb = new StringBuilder();
            sb.append("State: ").append(state).append("\n");
            sb.append("Current window duration: ").append(currentWindowDurationMinutes).append(" minutes\n");
            sb.append("Current window: ").append(fmt.format(currentWindowStart))
                    .append(" - ").append(fmt.format(currentWindowEnd)).append("\n");
            sb.append("Next window: ").append(fmt.format(nextWindowStart)).append("\n");
            sb.append("Minutes until next window: ").append(minutesUntilNextWindow);
            return sb.toString();
        }
    }

    /**
     * Calculates the Bear Trap window state for an anchor time.
     * Anchor semantics:
     * - Start = anchor - variableMinutes
     * - End   = anchor + fixedWindowMinutes (default 30)
     *
     * @param referenceAnchorUtc Anchor instant in UTC.
     * @param variableMinutes    Extra minutes n (must be >= 0).
     * @return WindowResult containing current state and timing info.
     */
    public static WindowResult calculateWindow(Instant referenceAnchorUtc, int variableMinutes) {
        return calculateWindow(
                referenceAnchorUtc,
                variableMinutes,
                DEFAULT_FIXED_WINDOW_MINUTES,
                DEFAULT_INTERVAL_DAYS,
                Clock.systemUTC()
        );
    }

    /**
     * Calculates the Bear Trap window state using configurable parameters and a testable Clock.
     *
     * Anchor semantics:
     * - Start = anchor - variableMinutes
     * - End   = anchor + fixedWindowMinutes
     * - Length = fixedWindowMinutes + variableMinutes
     *
     * End is treated as inclusive (i.e., now == end → INSIDE).
     *
     * @param referenceAnchorUtc    Anchor instant in UTC.
     * @param variableMinutes       Extra minutes n (must be >= 0).
     * @param fixedWindowMinutes    Fixed minutes (default 30) (must be > 0).
     * @param intervalDays          Cycle interval in days (must be > 0).
     * @param clock                 Time source.
     * @return WindowResult with computed timing and current state.
     * @throws IllegalArgumentException on invalid parameters.
     */
    public static WindowResult calculateWindow(
            Instant referenceAnchorUtc,
            int variableMinutes,
            int fixedWindowMinutes,
            int intervalDays,
            Clock clock
    ) {
        if (fixedWindowMinutes <= 0) {
            throw new IllegalArgumentException("Fixed window minutes must be greater than 0");
        }
        if (variableMinutes < 0) {
            throw new IllegalArgumentException("Variable minutes (n) must be >= 0");
        }
        if (intervalDays <= 0) {
            throw new IllegalArgumentException("Interval in days must be greater than 0");
        }

        final Instant now = Instant.now(clock);

        // Anchor semantics
        final Instant referenceStart = referenceAnchorUtc.minus(variableMinutes, ChronoUnit.MINUTES);
        final int windowDuration = fixedWindowMinutes + variableMinutes;

        final long intervalMinutes = intervalDays * 24L * 60L;
        final long minutesSinceRefStart = referenceStart.until(now, ChronoUnit.MINUTES);

        // Before the first anchored window
        if (minutesSinceRefStart < 0) {
            final long minutesUntilNext = now.until(referenceStart, ChronoUnit.MINUTES);
            return new WindowResult(
                    WindowState.BEFORE,
                    referenceStart,
                    referenceStart.plus(windowDuration, ChronoUnit.MINUTES),
                    referenceStart,
                    minutesUntilNext,
                    windowDuration
            );
        }

        // Current cycle based on the anchored start
        final long cycles = minutesSinceRefStart / intervalMinutes;
        final Instant currentStart = referenceStart.plus(cycles * intervalMinutes, ChronoUnit.MINUTES);
        final Instant currentEnd   = currentStart.plus(windowDuration, ChronoUnit.MINUTES);
        final Instant nextStart    = currentStart.plus(intervalMinutes, ChronoUnit.MINUTES);

        if (now.isBefore(currentStart)) {
            final long minutesUntilNext = now.until(currentStart, ChronoUnit.MINUTES);
            return new WindowResult(
                    WindowState.BEFORE,
                    currentStart,
                    currentEnd,
                    currentStart,
                    minutesUntilNext,
                    windowDuration
            );
        } else if (!now.isAfter(currentEnd)) { // inclusive end
            final long minutesUntilNext = now.until(nextStart, ChronoUnit.MINUTES);
            return new WindowResult(
                    WindowState.INSIDE,
                    currentStart,
                    currentEnd,
                    nextStart,
                    minutesUntilNext,
                    windowDuration
            );
        } else {
            final long minutesUntilNext = now.until(nextStart, ChronoUnit.MINUTES);
            return new WindowResult(
                    WindowState.AFTER,
                    currentStart,
                    currentEnd,
                    nextStart,
                    minutesUntilNext,
                    windowDuration
            );
        }
    }

    /**
     * Checks whether the Bear Trap event should execute now.
     * Returns true only if the current time is within the active window
     * (end inclusive).
     *
     * @param referenceAnchorUtc Anchor instant in UTC.
     * @param variableMinutes    Extra minutes n (>= 0).
     * @return true if inside the active window; false otherwise.
     */
    public static boolean shouldRun(Instant referenceAnchorUtc, int variableMinutes) {
        return calculateWindow(referenceAnchorUtc, variableMinutes).getState() == WindowState.INSIDE;
    }

    /**
     * Simple demo.
     */
    public static void main(String[] args) {
        // Example: anchor 2025-10-06 21:30 UTC, n = 5 → start 21:25, end 22:00
        ZonedDateTime anchor = ZonedDateTime.of(2025, 10, 6, 21, 30, 0, 0, ZoneOffset.UTC);
        Instant anchorUtc = anchor.toInstant();

        int n = 5;

        System.out.println("=== Bear Trap Window (Anchor Model) ===");
        System.out.println("Anchor (UTC): " + anchor.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'UTC'")));
        System.out.println("Window = [" + (30 + n) + " min]  Start = anchor - " + n + "m, End = anchor + 30m\n");

        WindowResult r = calculateWindow(anchorUtc, n);
        System.out.println(r);
        System.out.println("\nShould run now? " + shouldRun(anchorUtc, n));
    }
}
