package cl.camodev.wosbot.serv.task.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;
import net.sourceforge.tess4j.TesseractException;

public class TundraTruckEventTask extends DelayedTask {

	private boolean useGems = profile.getConfig(EnumConfigurationKey.TUNDRA_TRUCK_USE_GEMS_BOOL, Boolean.class);
	private boolean truckSSR = profile.getConfig(EnumConfigurationKey.TUNDRA_TRUCK_SSR_BOOL, Boolean.class);

	public TundraTruckEventTask(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	@Override
	protected void execute() {
		int attempt = 0;

		while (attempt < 5) {
			// Check if we are on the home screen
			DTOImageSearchResult homeResult = emuManager.searchTemplate(EMULATOR_NUMBER,
					EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 90);
			DTOImageSearchResult worldResult = emuManager.searchTemplate(EMULATOR_NUMBER,
					EnumTemplates.GAME_HOME_WORLD.getTemplate(), 90);

			if (homeResult.isFound() || worldResult.isFound()) {
				if (navigateToTundraEvent()) {
					handleTundraEvent();
					return;
				}
				return;
			} else {
				// If home screen is not found, log warning and go back
				logWarning("Home not found");
				EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
				sleepTask(2000);
			}
			attempt++;
		}

		// If menu is not found after 5 attempts, cancel the task
		if (attempt >= 5) {
			logWarning("Menu not found, removing task from scheduler");
			this.setRecurring(false);
		}

	}

	/**
	 * Navigates to the tundra truck event section in the game
	 *
	 * @return true if navigation was successful, false otherwise
	 */
	private boolean navigateToTundraEvent() {

		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(),
				"Executing Tundra Truck Event");

		// Search for the events button
		DTOImageSearchResult eventsResult = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.HOME_EVENTS_BUTTON.getTemplate(), 90);
		if (!eventsResult.isFound()) {
			logWarning("Events button not found");
			return false;
		}

		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, eventsResult.getPoint(), eventsResult.getPoint());
		sleepTask(2000);
		// Close any windows that may be open
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(529, 27), new DTOPoint(635, 63), 5, 300);

		// Search for the tundra truck within events
		DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.TUNDRA_TRUCK_TAB.getTemplate(), 90);

		if (result.isFound()) {
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, result.getPoint(), result.getPoint());
			sleepTask(1000);
			logInfo("Successfully navigated to tundra truck event");

			// Check if the event has ended
			if (eventHasEnded()) {
				return false;
			}

			return true;
		}

		// Swipe completely to the left
		logInfo("Tundra truck event not immediately visible, swiping left to locate");
		for (int i = 0; i < 3; i++) {
			emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(80, 120), new DTOPoint(578, 130));
			sleepTask(200);
		}

		int attempts = 0;
		while (attempts < 3) {
			result = emuManager.searchTemplate(EMULATOR_NUMBER,
					EnumTemplates.TUNDRA_TRUCK_TAB.getTemplate(), 90);

			if (result.isFound()) {
				emuManager.tapAtRandomPoint(EMULATOR_NUMBER, result.getPoint(), result.getPoint());
				sleepTask(1000);
				logInfo("Successfully navigated to tundra truck event");

				// Check if the event has ended
				if (eventHasEnded()) {
					return false;
				}
				return true;
			}

			logInfo("Tundra truck event not found, swiping right and retrying");
			emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(630, 143), new DTOPoint(500, 128));
			sleepTask(200);
			attempts++;
		}

		logWarning("Tundra truck event not found after multiple attempts, aborting task");
		this.setRecurring(false);
		return false;
	}

	private boolean eventHasEnded() {
		DTOImageSearchResult endedResult = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.TUNDRA_TRUCK_ENDED.getTemplate(), 90);
		if (endedResult.isFound()) {
			logInfo("Tundra Truck event has ended, removing the task.");
			this.setRecurring(false);
			return true;
		}
		return false;
	}

	private void handleTundraEvent() {
		clickMyTrucksTab();
		collectArrivedTrucks();

		if (!checkAvailableTrucks()) {
			return; // No trucks remaining for today, scheduled for game reset
		}

		attemptSendTrucks();
	}

	/**
	 * Extract next training completion time and schedule the task accordingly
	 */
	private void scheduleNextTruckForBothSides() {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(),
				"Extracting next training schedule time for both trucks");

		Optional<LocalDateTime> leftTime = extractNextTime(0);
		Optional<LocalDateTime> rightTime = extractNextTime(1);

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime nextSchedule = now.plusHours(1); // fallback

		if (leftTime.isPresent() && rightTime.isPresent()) {
			nextSchedule = leftTime.get().isAfter(rightTime.get()) ? leftTime.get() : rightTime.get();
			logInfo("Both times extracted. Next scheduled for: " + nextSchedule);
		} else if (leftTime.isPresent()) {
			nextSchedule = leftTime.get();
			logInfo("Only left time extracted. Next scheduled for: " + nextSchedule);
		} else if (rightTime.isPresent()) {
			nextSchedule = rightTime.get();
			logInfo("Only right time extracted. Next scheduled for: " + nextSchedule);
		} else {
			logInfo("Could not extract any truck time, rescheduling in 1 hour as fallback");
		}

		reschedule(nextSchedule);
	}

	private void closeWindow() {
		sleepTask(300);
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER,
				new DTOPoint(300, 1150), new DTOPoint(450, 1200), 2, 300);
	}

	private void handleGemRefresh(DTOImageSearchResult popupGems) {
		logInfo("Gem refresh pop-up detected");
		if (useGems) {
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(200, 704), new DTOPoint(220, 722));
			sleepTask(500);
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, popupGems.getPoint(), popupGems.getPoint());
		} else {
			logInfo("Gem refresh requested but useGems is false, cancelling refresh.");
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(626, 438), new DTOPoint(643, 454));
			closeWindow();
		}
	}

	private void refreshTrucks() {
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(588, 405), new DTOPoint(622, 436));
		sleepTask(1000);

		DTOImageSearchResult popup = emuManager.searchTemplate(
				EMULATOR_NUMBER, EnumTemplates.TUNDRA_TRUCK_REFRESH.getTemplate(), 90);
		DTOImageSearchResult popupGems = emuManager.searchTemplate(
				EMULATOR_NUMBER, EnumTemplates.TUNDRA_TRUCK_REFRESH_GEMS.getTemplate(), 98);

		if (popup.isFound()) {
			logInfo("Valuable cargo refresh pop-up detected");
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(200, 704), new DTOPoint(220, 722));
			sleepTask(500);
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, popup.getPoint(), popup.getPoint());
		} else if (popupGems.isFound()) {
			handleGemRefresh(popupGems);
		}
	}

	private boolean findSSRTruck() {
		DTOImageSearchResult truckRaritySSR = emuManager.searchTemplate(
				EMULATOR_NUMBER, EnumTemplates.TUNDRA_TRUCK_YELLOW.getTemplate(), 90);

		while (!truckRaritySSR.isFound()) {
			logInfo("SSR Truck not found, refreshing trucks");
			refreshTrucks();
			truckRaritySSR = emuManager.searchTemplate(
					EMULATOR_NUMBER, EnumTemplates.TUNDRA_TRUCK_YELLOW.getTemplate(), 90);
		}
		return truckRaritySSR.isFound();
	}

	private boolean truckAlreadyDeparted(int side) {
		DTOImageSearchResult departedTruck = emuManager.searchTemplate(
				EMULATOR_NUMBER, EnumTemplates.TUNDRA_TRUCK_DEPARTED.getTemplate(), 90);

		if (departedTruck.isFound()) {
			logInfo("Truck already departed on side " + (side == 0 ? "left" : "right") + ", skipping send.");
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(617, 770), new DTOPoint(650, 795));
			closeWindow();
			return true;
		}
		return false;
	}

	private boolean trySendTruck(int side, int xStart, int xEnd, int yStart, int yEnd) {
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER,
				new DTOPoint(xStart, yStart), new DTOPoint(xEnd, yEnd));
		sleepTask(300);

		if (truckAlreadyDeparted(side)) {
			return false;
		}

		DTOImageSearchResult sendTruckResult = emuManager.searchTemplate(
				EMULATOR_NUMBER, EnumTemplates.TUNDRA_TRUCK_ESCORT.getTemplate(), 90);
		sleepTask(500);

		if (sendTruckResult.isFound()) {
			if (!truckSSR || findSSRTruck()) {
				logInfo((truckSSR ? "SSR Truck found" : "Sending any truck") + ", proceeding to send");
				emuManager.tapAtRandomPoint(EMULATOR_NUMBER, sendTruckResult.getPoint(), sendTruckResult.getPoint());
				sleepTask(1000);
				return true;
			} else {
				logInfo("SSR Truck not found and user does not want to use gems or max attempts reached.");
			}
		} else {
			logInfo("No truck available to send on side " + (side == 0 ? "left" : "right"));
		}
		return false;
	}

	private void attemptSendTrucks() {
		int[] xStart = { 205, 450 };
		int[] xEnd = { 265, 515 };
		int yStart = 643, yEnd = 790;

		boolean found = false;

		for (int side = 0; side < 2; side++) {
			if (trySendTruck(side, xStart[side], xEnd[side], yStart, yEnd)) {
				found = true;
			}
		}

		if (found) {
			scheduleNextTruckForBothSides();
		}
	}

	private boolean checkAvailableTrucks() {
		try {
			String text = emuManager.ocrRegionText(EMULATOR_NUMBER,
					new DTOPoint(477, 1151), new DTOPoint(527, 1179));
			logInfo("Remaining trucks OCR result: " + text);

			if (text != null && text.trim().matches("0\\s*/\\s*\\d+")) {
				logInfo("No trucks available to send (" + text.trim() + "). Task will be rescheduled.");
				reschedule(UtilTime.getGameReset());
				return false;
			}
		} catch (IOException | TesseractException e) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, taskName,
					profile.getName(), "OCR error: " + e.getMessage());
		} catch (Exception e) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, taskName,
					profile.getName(), "Unexpected OCR error: " + e.getMessage());
		}
		return true;
	}

	private void collectArrivedTrucks() {
		int attempts = 0;
		while (true) {
			DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER,
					EnumTemplates.TUNDRA_TRUCK_ARRIVED.getTemplate(), 90);

			logInfo("Searching for arrived trucks, attempt " + (attempts + 1));

			if (result.isFound()) {
				logInfo("Arrived truck found, collecting rewards");
				emuManager.tapAtRandomPoint(EMULATOR_NUMBER, result.getPoint(), result.getPoint());
				sleepTask(1000);

				closeWindow();
			} else if (++attempts >= 3) {
				logInfo("No arrived trucks found after multiple attempts, moving on.");
				break;
			}
		}
		sleepTask(1000);
	}

	private void clickMyTrucksTab() {
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(120, 250), new DTOPoint(280, 270));
		sleepTask(1000);
	}

	/**
	 * Extract the next training completion time from the UI
	 * 
	 * @return Optional containing the next training time, or empty if extraction
	 *         failed
	 */
	private Optional<LocalDateTime> extractNextTime(int side) {
		try {
			// OCR region containing truck completion time
			String text;
			if (side == 0) {
				text = EmulatorManager.getInstance().ocrRegionText(EMULATOR_NUMBER,
						new DTOPoint(185, 852), new DTOPoint(287, 875));
			} else {
				text = EmulatorManager.getInstance().ocrRegionText(EMULATOR_NUMBER,
						new DTOPoint(432, 852), new DTOPoint(535, 875));
			}
			logInfo("OCR extracted text for side " + side + ": " + text);

			LocalDateTime nextTime = addTimeToLocalDateTime(LocalDateTime.now(), text);
			logInfo("Successfully extracted truck remaining time: " + nextTime);
			return Optional.of(nextTime);

		} catch (IOException | TesseractException e) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(),
					"OCR error while extracting truck remaining time: " + e.getMessage());
			return Optional.empty();
		} catch (Exception e) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(),
					"Unexpected error extracting truck remaining time: " + e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * Parse time string and add it to the current LocalDateTime
	 * 
	 * @param baseTime   Base time to add to (unused but kept for method signature
	 *                   compatibility)
	 * @param timeString Time string in format "[n]d HH:mm:ss"
	 * @return LocalDateTime with the parsed time added
	 */
	public static LocalDateTime addTimeToLocalDateTime(LocalDateTime baseTime, String timeString) {
		Pattern pattern = Pattern.compile("(?i).*?(?:(\\d+)\\s*d\\s*)?(\\d{1,2}:\\d{2}:\\d{2}).*", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(timeString.trim());

		if (!matcher.matches()) {
			throw new IllegalArgumentException(
					"Time string does not match expected format [n]d HH:mm:ss: " + timeString);
		}

		String daysStr = matcher.group(1); // Optional days component
		String timeStr = matcher.group(2); // Required time component

		int daysToAdd = (daysStr != null) ? Integer.parseInt(daysStr) : 0;

		// Parse time component
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm:ss");
		LocalTime timePart = LocalTime.parse(timeStr, timeFormatter);

		return baseTime
				.plusDays(daysToAdd)
				.plusHours(timePart.getHour())
				.plusMinutes(timePart.getMinute())
				.plusSeconds(timePart.getSecond());
	}
}
