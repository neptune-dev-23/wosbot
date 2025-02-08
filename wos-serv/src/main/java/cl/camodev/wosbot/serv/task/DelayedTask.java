package cl.camodev.wosbot.serv.task;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public abstract class DelayedTask extends Task {
	

	public DelayedTask(String taskName, LocalDateTime scheduledTime) {
		super(taskName, scheduledTime);
	}

	/**
	 * Devuelve el retraso restante en la unidad indicada.
	 */
	public long getDelay(TimeUnit unit) {
		long delayMillis = ChronoUnit.MILLIS.between(LocalDateTime.now(), scheduledTime);
		return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
	}


}