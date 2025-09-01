package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class MysteryShopTask extends DelayedTask {

	public MysteryShopTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
		super(profile, tpTask);
	}

	@Override
	protected void execute() {
		int attempt = 0;

		while (attempt < 5) {
			if (navigateToShop()) {
				handleMysteryShopOperations();
				return;
			} else {
				logWarning("Navigate to shop failed, retrying...");
				sleepTask(2000);
			}
			attempt++;
		}

		// If navigation fails after 5 attempts, reschedule for 1 hour
		if (attempt >= 5) {
			logWarning("Shop navigation failed after multiple attempts, rescheduling task for 1 hour");
			LocalDateTime nextAttempt = LocalDateTime.now().plusHours(1);
			this.reschedule(nextAttempt);
		}
	}

	/**
	 * Navigates to the shop section in the game
	 *
	 * @return true if navigation was successful, false otherwise
	 */
	private boolean navigateToShop() {
		// STEP 1: Search for the bottom bar shop button
		DTOImageSearchResult shopButtonResult = emuManager.searchTemplate(
			EMULATOR_NUMBER,
			EnumTemplates.GAME_HOME_BOTTOM_BAR_SHOP_BUTTON.getTemplate(),
			 90
		);

		if (!shopButtonResult.isFound()) {
			logWarning("Shop button not found, rescheduling task for 1 hour");
			LocalDateTime nextAttempt = LocalDateTime.now().plusHours(1);
			this.reschedule(nextAttempt);
			return false;
		}

		// Tap on shop button
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, shopButtonResult.getPoint(), shopButtonResult.getPoint());
		sleepTask(1000);

		// STEP 2: Search for mystery shop within the shop menu
		DTOImageSearchResult mysteryShopResult = emuManager.searchTemplate(
			EMULATOR_NUMBER,
			EnumTemplates.SHOP_MYSTERY_BUTTON.getTemplate(),
			 90
		);

		if (!mysteryShopResult.isFound()) {
			logWarning("Mystery shop button not found, rescheduling task for 1 hour");
			emuManager.tapBackButton(EMULATOR_NUMBER);
			LocalDateTime nextAttempt = LocalDateTime.now().plusHours(1);
			this.reschedule(nextAttempt);
			return false;
		}

		// Tap on mystery shop
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, mysteryShopResult.getPoint(), mysteryShopResult.getPoint());
		sleepTask(1000);
		return true;
	}

	/**
	 * Handles all mystery shop operations: scroll, claim free rewards, use daily refresh
	 */
	private void handleMysteryShopOperations() {
		// STEP 3: Scroll down in specific area to reveal all items
		DTOPoint scrollStart = new DTOPoint(350, 1100);
		DTOPoint scrollEnd = new DTOPoint(350, 650);
		emuManager.executeSwipe(EMULATOR_NUMBER, scrollStart, scrollEnd);
		sleepTask(500);

		// STEP 4: Process free rewards and daily refresh in a loop
		boolean foundFreeRewards = true;
		int dailyRefreshUsedCount = 0;
		final int maxDailyRefreshes = 10; // configurable limit to avoid abusing refresh
		int maxIterations = 15; // Prevent infinite loops
		int iteration = 0;
		boolean totalClaimedAny = false;

		while ((foundFreeRewards || dailyRefreshUsedCount < maxDailyRefreshes) && iteration < maxIterations) {
			iteration++;

			// First, try to claim all free rewards
			foundFreeRewards = claimAllFreeRewards();
			totalClaimedAny = totalClaimedAny || foundFreeRewards;

			// If no free rewards found, try to use daily refresh one or more times (up to remaining limit)
			if (!foundFreeRewards && dailyRefreshUsedCount < maxDailyRefreshes) {
				// Try using daily refresh repeatedly until no more refresh is available or we reach the limit
				while (dailyRefreshUsedCount < maxDailyRefreshes) {
					boolean used = tryUseDailyRefresh();
					if (!used) break; // no refresh available now
					dailyRefreshUsedCount++;

					// After using a refresh, give UI a moment to update and scroll again to reveal new items
					sleepTask(1000);
					emuManager.executeSwipe(EMULATOR_NUMBER, scrollStart, scrollEnd);
					sleepTask(1000);

					// After refresh, attempt to claim rewards again
					foundFreeRewards = claimAllFreeRewards();
					totalClaimedAny = totalClaimedAny || foundFreeRewards;

					// If we found rewards after this refresh, break inner refresh loop and continue outer loop
					if (foundFreeRewards) break;
					// otherwise continue trying another refresh (if any left)
				}
			}
		}

		// Navigate back
		emuManager.tapBackButton(EMULATOR_NUMBER);
		sleepTask(1000);
		emuManager.tapBackButton(EMULATOR_NUMBER);

		// If no more actions possible, reschedule to game reset time
		if (!foundFreeRewards) {
			LocalDateTime nextReset = UtilTime.getGameReset();
			this.reschedule(nextReset);
			if (totalClaimedAny) {
				logInfo("Free rewards claimed");
			} else if (dailyRefreshUsedCount > 0) {
				logInfo("Daily refresh used but no rewards found");
			} else {
				logInfo("No free rewards or daily refresh available");
			}
		}
	}

	/**
	 * Claims all available free rewards
	 *
	 * @return true if at least one free reward was found and claimed, false otherwise
	 */
	private boolean claimAllFreeRewards() {
		boolean foundAnyReward = false;
		boolean foundRewardInThisIteration = true;
		int maxRewardAttempts = 5;
		int rewardAttempt = 0;

		// Keep looking for free rewards until none are found
		while (foundRewardInThisIteration && rewardAttempt < maxRewardAttempts) {
			rewardAttempt++;
			foundRewardInThisIteration = false;

			// Search for free reward button on screen (one at a time)
			DTOImageSearchResult freeRewardResult = emuManager.searchTemplate(
				EMULATOR_NUMBER,
				EnumTemplates.MYSTERY_SHOP_FREE_REWARD.getTemplate(),
				 90
			);

			// If found, claim the reward
			if (freeRewardResult.isFound()) {
				// Tap on the free reward
				emuManager.tapAtRandomPoint(EMULATOR_NUMBER, freeRewardResult.getPoint(), freeRewardResult.getPoint());
				sleepTask(400);

				// Confirm the claim (tap on confirm button or area)
				emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(360, 830));
				sleepTask(300);

				logInfo("Free reward claimed");
				foundAnyReward = true;
				foundRewardInThisIteration = true;

				// Wait a bit before searching for the next reward
				sleepTask(1000);
			}
		}

		return foundAnyReward;
	}

	/**
	 * Tries to use the daily refresh if available
	 *
	 * @return true if daily refresh was used, false otherwise
	 */
	private boolean tryUseDailyRefresh() {
		DTOImageSearchResult dailyRefreshResult = emuManager.searchTemplate(
			EMULATOR_NUMBER,
			EnumTemplates.MYSTERY_SHOP_DAILY_REFRESH.getTemplate(),
			 90
		);

		if (dailyRefreshResult.isFound()) {
			// Tap on daily refresh
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, dailyRefreshResult.getPoint(), dailyRefreshResult.getPoint());
			sleepTask(1000);

			logInfo("Daily refresh used successfully");
			return true;
		}

		return false;
	}
}
