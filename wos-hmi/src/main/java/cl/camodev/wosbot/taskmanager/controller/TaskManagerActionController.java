package cl.camodev.wosbot.taskmanager.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import cl.camodev.wosbot.ot.DTODailyTaskStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.ot.DTOTaskState;
import cl.camodev.wosbot.profile.model.IProfileModel;
import cl.camodev.wosbot.profile.model.impl.ProfileCallback;
import cl.camodev.wosbot.profile.model.impl.ProfileModel;
import cl.camodev.wosbot.taskmanager.ITaskStatusChangeListener;
import cl.camodev.wosbot.taskmanager.model.ITaskStatusModel;
import cl.camodev.wosbot.taskmanager.model.impl.TaskCallback;
import cl.camodev.wosbot.taskmanager.model.impl.TaskStatusModel;
import cl.camodev.wosbot.taskmanager.view.TaskManagerLayoutController;

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

}
