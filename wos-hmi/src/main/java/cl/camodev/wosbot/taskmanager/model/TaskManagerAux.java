package cl.camodev.wosbot.taskmanager.model;

import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TaskManagerAux {

	private final StringProperty taskName;
	private final StringProperty lastExecution;
	private final StringProperty nextExecution;
	private final ObjectProperty<TpDailyTaskEnum> taskEnum;
	private final LongProperty profileId;
	private final LongProperty nearestMinutesUntilExecution;
	private final BooleanProperty hasReadyTask;
	private final BooleanProperty isScheduled;

	public TaskManagerAux(String taskName, String lastExecution, String nextExecution, TpDailyTaskEnum taskEnum, Long profileId, long nearestMinutesUntilExecution, boolean hasReadyTask, boolean isScheduled) {
		this.taskName = new SimpleStringProperty(taskName);
		this.lastExecution = new SimpleStringProperty(lastExecution);
		this.nextExecution = new SimpleStringProperty(nextExecution);
		this.taskEnum = new SimpleObjectProperty<>(taskEnum);
		this.profileId = new SimpleLongProperty(profileId);
		this.nearestMinutesUntilExecution = new SimpleLongProperty(nearestMinutesUntilExecution);
		this.hasReadyTask = new SimpleBooleanProperty(hasReadyTask);
		this.isScheduled = new SimpleBooleanProperty(isScheduled);
	}

	// Getters
	public String getTaskName() {
		return taskName.get();
	}

	public String getLastExecution() {
		return lastExecution.get();
	}

	public String getNextExecution() {
		return nextExecution.get();
	}

	public TpDailyTaskEnum getTaskEnum() {
		return taskEnum.get();
	}

	public long getProfileId() {
		return profileId.get();
	}

	public long getNearestMinutesUntilExecution() {
		return nearestMinutesUntilExecution.get();
	}

	public boolean hasReadyTask() {
		return hasReadyTask.get();
	}

	public boolean isScheduled() {
		return isScheduled.get();
	}

	// Property getters
	public StringProperty taskNameProperty() {
		return taskName;
	}

	public StringProperty lastExecutionProperty() {
		return lastExecution;
	}

	public StringProperty nextExecutionProperty() {
		return nextExecution;
	}

	public ObjectProperty<TpDailyTaskEnum> taskEnumProperty() {
		return taskEnum;
	}

	public LongProperty profileIdProperty() {
		return profileId;
	}

	public LongProperty nearestMinutesUntilExecutionProperty() {
		return nearestMinutesUntilExecution;
	}

	public BooleanProperty hasReadyTaskProperty() {
		return hasReadyTask;
	}

	public BooleanProperty isScheduledProperty() {
		return isScheduled;
	}

}
