package cl.camodev.wosbot.serv.task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOProfiles;

public abstract class DelayedTask implements Runnable {

	protected volatile boolean recurring = true;
	protected LocalDateTime scheduledTime;
	protected String taskName;
	protected DTOProfiles profile;
	protected String EMULATOR_NUMBER;
	protected TpDailyTaskEnum tpTask;

	public DelayedTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
		this.profile = profile;
		this.taskName = tpTask.getName();
		this.scheduledTime = LocalDateTime.now();
		this.EMULATOR_NUMBER = profile.getEmulatorNumber().toString();
		this.tpTask = tpTask;
	}

	@Override
	public void run() {
		execute();
	}

	protected abstract void execute();

	public boolean isRecurring() {
		return recurring;
	}

	public Integer getTpDailyTaskId() {
		return tpTask.getId();
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

	public long getDelay(TimeUnit unit) {
		long delayMillis = ChronoUnit.MILLIS.between(LocalDateTime.now(), scheduledTime);
		return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
	}
}
