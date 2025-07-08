package cl.camodev.wosbot.serv.task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ex.HomeNotFoundException;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.impl.InitializeTask;

public abstract class DelayedTask implements Runnable {

	protected volatile boolean recurring = true;
	protected LocalDateTime scheduledTime;
	protected String taskName;
	protected DTOProfiles profile;
	protected String EMULATOR_NUMBER;
	protected TpDailyTaskEnum tpTask;
	protected int homeAttemps = 0;

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
			homeAttemps = 0;
			throw new HomeNotFoundException("Game is not running");
		}

		if (isGameHomeFound()) {
			execute();
		} else {
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
			if (++homeAttemps >= 10) {
				homeAttemps = 0;
				throw new HomeNotFoundException("Home not found after 10 attempts");
			}
		}
	}

	private boolean isGameHomeFound() {
		EmulatorManager emulator = EmulatorManager.getInstance();
		return emulator.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90).isFound() || emulator.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90).isFound();
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
		// 1) Mismo tipo de tarea y mismo perfil
		if (tpTask != that.tpTask)
			return false;
		if (!Objects.equals(profile.getId(), that.profile.getId()))
			return false;

		// 2) Sólo comparamos distinctKey si alguno lo define (no-null)
		Object keyThis = this.getDistinctKey();
		Object keyThat = that.getDistinctKey();
		if (keyThis != null || keyThat != null) {
			return Objects.equals(keyThis, keyThat);
		}

		// 3) Si ambos keys son null, da por igual y decimos que son iguales
		return true;
	}

	@Override
	public int hashCode() {
		// Incluyo getDistinctKey() sólo si no es null, para mantener coherencia con equals
		Object key = getDistinctKey();
		if (key != null) {
			return Objects.hash(getClass(), tpTask, profile.getId(), key);
		} else {
			return Objects.hash(getClass(), tpTask, profile.getId());
		}
	}
}
