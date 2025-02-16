package cl.camodev.wosbot.serv.task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;

public abstract class DelayedTask implements Runnable {

	private volatile boolean recurring = true;
	protected LocalDateTime scheduledTime;
	protected final String taskName;
	protected final Integer idTpDailyTask;

	public DelayedTask(TpDailyTaskEnum tpTask, LocalDateTime scheduledTime) {
		this.scheduledTime = scheduledTime;
		this.taskName = tpTask.getName();
		this.idTpDailyTask = tpTask.getId();
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
		return idTpDailyTask;
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
