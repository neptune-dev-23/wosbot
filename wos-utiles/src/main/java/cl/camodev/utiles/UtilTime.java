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
}
