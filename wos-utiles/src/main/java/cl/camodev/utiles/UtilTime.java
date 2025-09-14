package cl.camodev.utiles;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static LocalDateTime parseTime(String input) {
        Pattern pattern = Pattern.compile("(?i).*?(\\d+)[^\\d:]*?(\\d{1,2}:\\d{2}:\\d{2}).*", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input.trim());

        if (!matcher.matches()) {
            Pattern timeOnlyPattern = Pattern.compile("(?i).*?(\\d{1,2}:\\d{2}:\\d{2}).*", Pattern.DOTALL);
            Matcher timeOnlyMatcher = timeOnlyPattern.matcher(input.trim());

            if (timeOnlyMatcher.matches()) {
                String timeStr = timeOnlyMatcher.group(1);
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm:ss");
                LocalTime timePart = LocalTime.parse(timeStr, timeFormatter);

                return LocalDateTime.now()
                        .plusHours(timePart.getHour())
                        .plusMinutes(timePart.getMinute())
                        .plusSeconds(timePart.getSecond());
            }

            throw new IllegalArgumentException("Input does not match expected format. Input: " + input);
        }

        String daysStr = matcher.group(1);   // number before the time
        String timeStr = matcher.group(2);   // time HH:mm:ss

        int daysToAdd = Integer.parseInt(daysStr);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm:ss");
        LocalTime timePart = LocalTime.parse(timeStr, timeFormatter);


        return LocalDateTime.now()
                .plusDays(daysToAdd)
                .plusHours(timePart.getHour())
                .plusMinutes(timePart.getMinute())
                .plusSeconds(timePart.getSecond());
    }


}
