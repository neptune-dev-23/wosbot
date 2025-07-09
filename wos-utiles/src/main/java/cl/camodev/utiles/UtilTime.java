package cl.camodev.utiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class UtilTime {

	public static LocalDateTime getGameReset() {
		ZonedDateTime nowUtc = ZonedDateTime.now(ZoneId.of("UTC"));
		ZonedDateTime nextUtcMidnight = nowUtc.toLocalDate().plusDays(1).atStartOfDay(ZoneId.of("UTC"));
		ZonedDateTime localNextMidnight = nextUtcMidnight.withZoneSameInstant(ZoneId.systemDefault());
		return localNextMidnight.toLocalDateTime();
	}

	public static LocalDateTime getNextReset() {
		ZonedDateTime nowUtc = ZonedDateTime.now(ZoneId.of("UTC"));

		ZonedDateTime nextMidnightUtc = nowUtc.toLocalDate().plusDays(1).atStartOfDay(ZoneId.of("UTC"));
		ZonedDateTime nextNoonUtc = nowUtc.toLocalDate().atTime(12, 0).atZone(ZoneId.of("UTC"));

		if (nowUtc.isAfter(nextNoonUtc)) {
			nextNoonUtc = nextNoonUtc.plusDays(1);
		}

		ZonedDateTime nextResetUtc = nowUtc.until(nextMidnightUtc, ChronoUnit.SECONDS) < nowUtc.until(nextNoonUtc, ChronoUnit.SECONDS) ? nextMidnightUtc : nextNoonUtc;
		ZonedDateTime localNextReset = nextResetUtc.withZoneSameInstant(ZoneId.systemDefault());
		return localNextReset.toLocalDateTime();
	}

	public static String localDateTimeToDDHHMMSS(LocalDateTime dateTime) {
		LocalDateTime now = LocalDateTime.now();

		if (dateTime.isBefore(now)) {
			return "ASAP";
		}

		Duration duration = Duration.between(now, dateTime);

		long days = duration.toDays();
		long hours = duration.toHours() % 24;
		long minutes = duration.toMinutes() % 60;
		long seconds = duration.getSeconds() % 60;

		StringBuilder formattedString = new StringBuilder();
		if (days > 0) {
			formattedString.append(days).append(" days ");
		}
		formattedString.append(String.format("%02d:%02d:%02d", hours, minutes, seconds));

		return formattedString.toString();
	}

	public static String formatLastExecution(LocalDateTime execution) {
		if (execution == null) {
			return "Never";
		}
		long minutesAgo = ChronoUnit.MINUTES.between(execution, LocalDateTime.now());
		return formatTimeAgo(minutesAgo);
	}

	public static String formatNextExecution(LocalDateTime execution, LocalDateTime clock, boolean executing) {

		if (executing)
			return "Executing";

		if (execution == null)
			return "Never";

		long minutesUntil = ChronoUnit.MINUTES.between(clock, execution);
		if (minutesUntil <= 0) {
			return "Ready";
		}
		return formatTimeUntil(minutesUntil);
	}

	private static String formatTimeAgo(long minutes) {
		if (minutes < 1) {
			return "Just now";
		} else if (minutes < 60) {
			return minutes + "m ago";
		} else if (minutes < 1440) {
			long hours = minutes / 60;
			return hours + "h ago";
		} else {
			long days = minutes / 1440;
			return days + "d ago";
		}
	}

	private static String formatTimeUntil(long minutes) {
		if (minutes < 1) {
			return "Now";
		}
		long days = minutes / 1440;
		long hours = (minutes % 1440) / 60;
		long mins = minutes % 60;

		if (days > 0) {
			if (hours > 0) {
				return days + "d " + hours + "h";
			} else {
				return days + "d";
			}
		} else if (hours > 0) {
			if (mins > 0) {
				return hours + "h " + mins + "m";
			} else {
				return hours + "h";
			}
		} else {
			return mins + "m";
		}
	}

}
