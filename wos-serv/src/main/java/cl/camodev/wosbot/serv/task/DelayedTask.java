package cl.camodev.wosbot.serv.task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ex.HomeNotFoundException;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.impl.InitializeTask;

public abstract class DelayedTask implements Runnable, Delayed, Comparable<Delayed> {

	protected volatile boolean recurring = true;
	protected LocalDateTime lastExecutionTime;
	protected LocalDateTime scheduledTime;
	protected String taskName;
	protected DTOProfiles profile;
	protected String EMULATOR_NUMBER;
	protected TpDailyTaskEnum tpTask;

	protected EmulatorManager emuManager = EmulatorManager.getInstance();
	protected ServScheduler servScheduler = ServScheduler.getServices();
	protected ServLogs servLogs = ServLogs.getServices();

	protected Object getDistinctKey() {
		return null;
	}

	public DelayedTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
		this.profile = profile;
		this.taskName = tpTask.getName();
		this.scheduledTime = LocalDateTime.now();
		this.EMULATOR_NUMBER = profile.getEmulatorNumber().toString();
		this.tpTask = tpTask;
	}

	@Override
	public void run() {

		if (this instanceof InitializeTask) {
			execute();
			return;
		}

		if (!EmulatorManager.getInstance().isPackageRunning(EMULATOR_NUMBER, EmulatorManager.WHITEOUT_PACKAGE)) {
			throw new HomeNotFoundException("Game is not running");
		}

		if (isGameHomeFound()) {
			execute();
			return;
		}

		for (int attempt = 1; attempt <= 10; attempt++) {
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
			sleepTask(100);
			if (isGameHomeFound()) {
				execute();
				return;
			}
		}

		throw new HomeNotFoundException("Home not found after 10 attempts");
	}

	private boolean isGameHomeFound() {
		EmulatorManager emulator = EmulatorManager.getInstance();
		return emulator
				.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90)
				.isFound()
				|| emulator.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720,
						1280, 90).isFound();
	}

	protected abstract void execute();

	public boolean isRecurring() {
		return recurring;
	}

	public void setLastExecutionTime(LocalDateTime lastExecutionTime) {
		this.lastExecutionTime = lastExecutionTime;
	}

	public LocalDateTime getLastExecutionTime() {
		return lastExecutionTime;
	}

	public Integer getTpDailyTaskId() {
		return tpTask.getId();
	}

	public TpDailyTaskEnum getTpTask() {
		return tpTask;
	}

	public void setRecurring(boolean recurring) {
		this.recurring = recurring;
	}

	public void reschedule(LocalDateTime rescheduledTime) {
		Duration difference = Duration.between(LocalDateTime.now(), rescheduledTime);
		scheduledTime = LocalDateTime.now().plus(difference);
	}

	protected void sleepTask(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
	}

	public String getTaskName() {
		return taskName;
	}

	@Override
	public long getDelay(TimeUnit unit) {
		long diff = scheduledTime.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
		return unit.convert(diff, TimeUnit.SECONDS);
	}

	@Override
	public int compareTo(Delayed o) {
		if (this == o) return 0;

		boolean thisInit  = this instanceof InitializeTask;
		boolean otherInit = o instanceof InitializeTask;
		if (thisInit && !otherInit) return -1;
		if (!thisInit && otherInit) return  1;


		long diff = this.getDelay(TimeUnit.NANOSECONDS)
				- o.getDelay(TimeUnit.NANOSECONDS);
		return Long.compare(diff, 0);
	}

	public LocalDateTime getScheduled() {
		return scheduledTime;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DelayedTask))
			return false;
		if (getClass() != o.getClass())
			return false;

		DelayedTask that = (DelayedTask) o;

		if (tpTask != that.tpTask)
			return false;
		if (!Objects.equals(profile.getId(), that.profile.getId()))
			return false;

		Object keyThis = this.getDistinctKey();
		Object keyThat = that.getDistinctKey();
		if (keyThis != null || keyThat != null) {
			return Objects.equals(keyThis, keyThat);
		}

		return true;
	}

	@Override
	public int hashCode() {
		Object key = getDistinctKey();
		if (key != null) {
			return Objects.hash(getClass(), tpTask, profile.getId(), key);
		} else {
			return Objects.hash(getClass(), tpTask, profile.getId());
		}
	}

	public boolean provideDailyMissionProgress() {
		return false;
	}

	public boolean provideTriumphProgress() {
		return false;
	}

	public void logInfo(String message) {
		servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), message);
	}

	public void logWarning(String message) {
		servLogs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), message);
	}

	public void logError(String message) {
		servLogs.appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(), message);
	}

	public void logDebug(String message) {
		servLogs.appendLog(EnumTpMessageSeverity.DEBUG, taskName, profile.getName(), message);
	}
}
