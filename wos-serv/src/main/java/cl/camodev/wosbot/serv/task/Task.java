package cl.camodev.wosbot.serv.task;

import java.time.Duration;
import java.time.LocalDateTime;

public abstract class Task implements Runnable {
	private volatile boolean recurring = true;
	protected LocalDateTime scheduledTime;

	public Task(LocalDateTime scheduledTime) {
		this.scheduledTime = scheduledTime;
	}

	@Override
	public void run() {
		execute();
	}

	protected abstract void execute();

	public boolean isRecurring() {
		return recurring;
	}

	public void setRecurring(boolean recurring) {
		this.recurring = recurring;
	}

	public void reschedule(LocalDateTime rescheduledTime) {
		Duration difference = Duration.between(LocalDateTime.now(), rescheduledTime);
		scheduledTime = LocalDateTime.now().plus(difference);
	}
}
