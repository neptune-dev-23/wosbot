package cl.camodev.wosbot.taskmanager.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTODailyTaskStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.ot.DTOTaskState;
import cl.camodev.wosbot.profile.model.IProfileModel;
import cl.camodev.wosbot.profile.model.impl.ProfileCallback;
import cl.camodev.wosbot.profile.model.impl.ProfileModel;
import cl.camodev.wosbot.serv.impl.ServProfiles;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.taskmanager.ITaskStatusChangeListener;
import cl.camodev.wosbot.taskmanager.model.ITaskStatusModel;
import cl.camodev.wosbot.taskmanager.model.impl.TaskCallback;
import cl.camodev.wosbot.taskmanager.model.impl.TaskStatusModel;
import cl.camodev.wosbot.taskmanager.model.TaskManagerAux;
import cl.camodev.wosbot.taskmanager.view.TaskManagerLayoutController;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class TaskManagerActionController implements ITaskStatusChangeListener {

	private TaskManagerLayoutController taskManagerLayoutController;

	private IProfileModel profileModel;

	private ITaskStatusModel taskStatusModel;

	public TaskManagerActionController(TaskManagerLayoutController taskManagerLayoutController) {
		this.taskManagerLayoutController = taskManagerLayoutController;
		this.profileModel = new ProfileModel();
		this.taskStatusModel = new TaskStatusModel();
		this.taskStatusModel.addTaskStatusChangeListener(this);
	}

	public void loadProfiles(ProfileCallback callback) {
		CompletableFuture.supplyAsync(() -> {
			List<DTOProfiles> profiles = profileModel.getProfiles();
			return profiles;
		}).thenAccept(profiles -> {

			if (callback != null) {
				callback.onProfilesLoaded(profiles);
			}

		}).exceptionally(ex -> {
			ex.printStackTrace();
			return null;
		});
	}

	public void loadDailyTaskStatus(Long profileId, TaskCallback callback) {
		CompletableFuture.supplyAsync(() -> {
			List<DTODailyTaskStatus> taskStates = taskStatusModel.getDailyTaskStatusList(profileId);
			return taskStates;
		}).thenAccept(taskStates -> {

			if (callback != null) {
				callback.onTasksLoaded(taskStates);
			}

		}).exceptionally(ex -> {
			ex.printStackTrace();
			return null;
		});
	}

	@Override
	public void onTaskStatusChange(Long profileId, int taskNameId, DTOTaskState taskState) {
		if (taskManagerLayoutController != null) {
			taskManagerLayoutController.updateTaskStatus(profileId, taskNameId, taskState);
		}
	}

	/**
	 * Validates if the queue is active for a profile
	 */
	public boolean isQueueActive(Long profileId) {
		return ServScheduler.getServices().getQueueManager().getQueue(profileId) != null;
	}

	/**
	 * Validates if a task can be removed
	 */
	public boolean canRemoveTask(TaskManagerAux task) {
		return task.scheduledProperty().get() && !task.executingProperty().get();
	}

	/**
	 * Handles the execute now action
	 */
	public void executeTaskNow(TaskManagerAux task) {
		DTOProfiles profile = findProfileById(task.getProfileId());
		if (profile == null) {
			System.err.println("Profile not found: " + task.getProfileId());
			return;
		}

		ServScheduler scheduler = ServScheduler.getServices();
		scheduler.updateDailyTaskStatus(profile, task.getTaskEnum(), LocalDateTime.now());
		scheduler.getQueueManager().getQueue(profile.getId()).executeTaskNow(task.getTaskEnum());
	}

	/**
	 * Handles the remove task action with confirmation dialog
	 */
	public void removeTask(TaskManagerAux task, Runnable onSuccess) {
		DTOProfiles profile = findProfileById(task.getProfileId());
		if (profile == null) {
			System.err.println("Profile not found: " + task.getProfileId());
			return;
		}

		// Show confirmation dialog
		Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
		confirmDialog.setTitle("Remove Task");
		confirmDialog.setHeaderText("Remove task from scheduler");
		confirmDialog.setContentText("Are you sure you want to remove '" + task.getTaskEnum().getName() +
			"' from the scheduler for profile '" + profile.getName() + "'?");

		Optional<ButtonType> result = confirmDialog.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			ServScheduler scheduler = ServScheduler.getServices();
			scheduler.removeTaskFromScheduler(profile.getId(), task.getTaskEnum());

			if (onSuccess != null) {
				onSuccess.run();
			}
		}
	}

	/**
	 * Helper method to find profile by ID
	 */
	public DTOProfiles findProfileById(Long profileId) {
		List<DTOProfiles> allProfiles = ServProfiles.getServices().getProfiles();
		return allProfiles.stream()
			.filter(p -> p.getId().equals(profileId))
			.findFirst()
			.orElse(null);
	}
}
