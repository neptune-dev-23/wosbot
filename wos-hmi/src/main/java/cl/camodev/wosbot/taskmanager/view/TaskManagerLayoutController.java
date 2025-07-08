package cl.camodev.wosbot.taskmanager.view;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cl.camodev.wosbot.almac.repo.DailyTaskRepository;
import cl.camodev.wosbot.almac.repo.IDailyTaskRepository;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTODailyTaskStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.profile.model.IProfileLoadListener;
import cl.camodev.wosbot.profile.model.ProfileAux;
import cl.camodev.wosbot.serv.impl.ServProfiles;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.taskmanager.model.TaskManagerAux;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;

public class TaskManagerLayoutController implements IProfileLoadListener {

	@FXML
	private TabPane tabPaneProfiles;

	private IDailyTaskRepository dailyTaskRepository = DailyTaskRepository.getRepository();
	private Timeline updateTimeline;
	private HashMap<Long, TableView<TaskManagerAux>> tasks = new HashMap<>();

	@FXML
	private void initialize() {
		setupAutoRefresh();
		loadTaskStatuses();
	}

	private void setupProfileTabs() {
		// Additional TabPane setup can go here if needed
	}

	private void setupAutoRefresh() {

		updateTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
			Platform.runLater(this::loadTaskStatuses);
		}));
		updateTimeline.setCycleCount(Timeline.INDEFINITE);
		updateTimeline.play();
	}

	private void loadTaskStatuses() {
		if (tabPaneProfiles == null)
			return;

		SingleSelectionModel<Tab> selModel = tabPaneProfiles.getSelectionModel();
		Tab selectedTab = selModel.getSelectedItem();
		Long selectedProfileId = selectedTab != null ? (Long) selectedTab.getUserData() : null;

		List<DTOProfiles> profiles = ServProfiles.getServices().getProfiles();
		if (profiles == null || profiles.isEmpty()) {
			tabPaneProfiles.getTabs().clear();
			return;
		}

		Set<Long> enabledIds = profiles.stream().filter(DTOProfiles::getEnabled).map(DTOProfiles::getId).collect(Collectors.toSet());

		List<Tab> toRemove = tabPaneProfiles.getTabs().stream().filter(tab -> !enabledIds.contains((Long) tab.getUserData())).collect(Collectors.toList());
		toRemove.forEach(tab -> {
			tabPaneProfiles.getTabs().remove(tab);
			tasks.remove(tab.getUserData());
		});

		Set<Long> existingIds = tabPaneProfiles.getTabs().stream().map(tab -> (Long) tab.getUserData()).collect(Collectors.toSet());

		for (DTOProfiles profile : profiles) {
			if (!profile.getEnabled())
				continue;

			if (!existingIds.contains(profile.getId())) {
				// pestaña nueva
				Tab newTab = createProfileTab(profile);
				tabPaneProfiles.getTabs().add(newTab);
			} else {
				// pestaña existente: sólo refresco sus datos
				refreshProfileTab(profile);
			}
		}

		if (selectedProfileId != null && enabledIds.contains(selectedProfileId)) {
			tabPaneProfiles.getTabs().stream().filter(tab -> selectedProfileId.equals(tab.getUserData())).findFirst().ifPresent(selModel::select);
		} else if (!tabPaneProfiles.getTabs().isEmpty()) {
			selModel.select(0);
		}
	}

	private Tab createProfileTab(DTOProfiles profile) {
		Tab tab = new Tab(profile.getName());
		tab.setClosable(false);
		tab.setUserData(profile.getId());

		TableView<TaskManagerAux> table = createTaskTable();
		tasks.put(profile.getId(), table);

		Map<Integer, DTODailyTaskStatus> taskStatuses = dailyTaskRepository.findDailyTasksStatusByProfile(profile.getId());
		ServScheduler.getServices().getQueueManager().getQueue(profile.getId());
		List<TaskManagerAux> list = Arrays.stream(TpDailyTaskEnum.values()).map(task -> {
			DTODailyTaskStatus status = taskStatuses.get(task.getId());
			String last = formatLastExecution(status);
			String next = formatNextExecution(status);
			long minutes = Long.MAX_VALUE;
			boolean ready = false;
			if (status != null && status.getNextSchedule() != null) {
				long diff = ChronoUnit.MINUTES.between(LocalDateTime.now(), status.getNextSchedule());
				if (diff <= 0) {
					ready = true;
					minutes = 0;
				} else
					minutes = diff;
			}
			return new TaskManagerAux(task.getName(), last, next, task, profile.getId(), minutes, ready, false);
		}).sorted((a, b) -> {
			if (a.hasReadyTask() && !b.hasReadyTask())
				return -1;
			if (!a.hasReadyTask() && b.hasReadyTask())
				return 1;
			return Long.compare(a.getNearestMinutesUntilExecution(), b.getNearestMinutesUntilExecution());
		}).collect(Collectors.toList());

		table.getItems().setAll(list);
		tab.setContent(table);
		return tab;
	}

	private void refreshProfileTab(DTOProfiles profile) {
		TableView<TaskManagerAux> table = tasks.get(profile.getId());
		if (table == null)
			return;

		Map<Integer, DTODailyTaskStatus> statuses = dailyTaskRepository.findDailyTasksStatusByProfile(profile.getId());

		List<TaskManagerAux> updated = Arrays.stream(TpDailyTaskEnum.values()).map(task -> {
			DTODailyTaskStatus s = statuses.get(task.getId());
			String last = formatLastExecution(s);
			String next = formatNextExecution(s);
			long minutes = Long.MAX_VALUE;
			boolean ready = false;
			if (s != null && s.getNextSchedule() != null) {
				long diff = ChronoUnit.MINUTES.between(LocalDateTime.now(), s.getNextSchedule());
				if (diff <= 0) {
					ready = true;
					minutes = 0;
				} else
					minutes = diff;
			}
			return new TaskManagerAux(task.getName(), last, next, task, profile.getId(), minutes, ready, false);
		}).sorted((a, b) -> {
			if (a.hasReadyTask() && !b.hasReadyTask())
				return -1;
			if (!a.hasReadyTask() && b.hasReadyTask())
				return 1;
			return Long.compare(a.getNearestMinutesUntilExecution(), b.getNearestMinutesUntilExecution());
		}).collect(Collectors.toList());

		table.getItems().setAll(updated);
	}

	private TableView<TaskManagerAux> createTaskTable() {
		TableView<TaskManagerAux> table = new TableView<>();
		table.getStyleClass().add("table-view");

		// Task Name column
		TableColumn<TaskManagerAux, String> colTaskName = new TableColumn<>("Task Name");
		colTaskName.setCellValueFactory(cellData -> cellData.getValue().taskNameProperty());
		colTaskName.setPrefWidth(200);
		colTaskName.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setStyle("");
				} else {
					setText(item);
					setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
				}
			}
		});

		// Last Execution column
		TableColumn<TaskManagerAux, String> colLastExecution = new TableColumn<>("Last Execution");
		colLastExecution.setCellValueFactory(cellData -> cellData.getValue().lastExecutionProperty());
		colLastExecution.setPrefWidth(150);
		colLastExecution.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setStyle("");
				} else {
					setText(item);
					setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
				}
			}
		});

		// Next Execution column
		TableColumn<TaskManagerAux, String> colNextExecution = new TableColumn<>("Next Execution");
		colNextExecution.setCellValueFactory(cellData -> cellData.getValue().nextExecutionProperty());
		colNextExecution.setPrefWidth(150);
		colNextExecution.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setStyle("");
					return;
				}
				setText(item);

				String style = "-fx-font-size: 14px; ";
				if ("Ready".equals(item)) {
					style += "-fx-text-fill: #4CAF50; -fx-font-weight: bold;";
				} else if ("Never".equals(item) || "--".equals(item)) {
					style += "-fx-text-fill: #757575;";
				} else if (item.matches(".*[mhd].*")) {
					String timeStr = item.replaceAll("[^0-9]", "");
					int timeValue = timeStr.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(timeStr);
					if (item.contains("m")) {
						if (timeValue <= 15) {
							style += "-fx-text-fill: #4CAF50; -fx-font-weight: bold;";
						} else if (timeValue <= 60) {
							style += "-fx-text-fill: #ffc107;";
						} else {
							style += "-fx-text-fill: #ff9800;";
						}
					} else if (item.contains("h")) {
						style += (timeValue <= 2) ? "-fx-text-fill: #ffc107;" : "-fx-text-fill: #ff9800;";
					} else {
						style += "-fx-text-fill: #ff5722;";
					}
				} else {
					style += "-fx-text-fill: white;";
				}
				setStyle(style);
			}
		});

		TableColumn<TaskManagerAux, Void> colActions = new TableColumn<>("Actions");
		colActions.setPrefWidth(100);
		colActions.setCellFactory(column -> new TableCell<>() {
			private final Button btnExecute = new Button("Execute Now");
			{
				btnExecute.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12px; " + "-fx-padding: 6px 12px; -fx-border-radius: 3px; -fx-background-radius: 3px;");
				btnExecute.setOnAction(ev -> {
					TaskManagerAux item = getTableView().getItems().get(getIndex());
					List<DTOProfiles> allProfiles = ServProfiles.getServices().getProfiles();
					DTOProfiles profile = allProfiles.stream().filter(p -> p.getId().equals(item.getProfileId())).findFirst().orElse(null);

					if (profile == null) {
						System.err.println("Profile not found: " + item.getProfileId());
						return;
					}

					ServScheduler scheduler = ServScheduler.getServices();
					scheduler.updateDailyTaskStatus(profile, item.getTaskEnum(), LocalDateTime.now());
					scheduler.getQueueManager().getQueue(profile.getId()).executeTaskNow(item.getTaskEnum());

				});
			}

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(empty ? null : btnExecute);
			}
		});

		table.getColumns().addAll(colTaskName, colLastExecution, colNextExecution, colActions);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

		return table;
	}

	private String formatLastExecution(DTODailyTaskStatus taskStatus) {
		if (taskStatus == null || taskStatus.getLastExecution() == null) {
			return "Never";
		}

		long minutesAgo = ChronoUnit.MINUTES.between(taskStatus.getLastExecution(), LocalDateTime.now());
		return formatTimeAgo(minutesAgo);
	}

	private String formatNextExecution(DTODailyTaskStatus taskStatus) {
		if (taskStatus == null) {
			return "Never";
		}

		if (taskStatus.getNextSchedule() != null) {
			long minutesUntil = ChronoUnit.MINUTES.between(LocalDateTime.now(), taskStatus.getNextSchedule());
			if (minutesUntil <= 0) {
				return "Ready";
			} else {
				return formatTimeUntil(minutesUntil);
			}
		}

		return "--";
	}

	private String formatTaskStatus(DTODailyTaskStatus taskStatus) {
		if (taskStatus == null) {
			return "Never";
		}

		if (taskStatus.getNextSchedule() != null) {
			long minutesUntil = ChronoUnit.MINUTES.between(LocalDateTime.now(), taskStatus.getNextSchedule());
			if (minutesUntil <= 0) {
				return "Ready";
			} else {
				return formatTimeUntil(minutesUntil);
			}
		}

		if (taskStatus.getLastExecution() != null) {
			long minutesAgo = ChronoUnit.MINUTES.between(taskStatus.getLastExecution(), LocalDateTime.now());
			return formatTimeAgo(minutesAgo);
		}

		return "--";
	}

	private String formatTimeAgo(long minutes) {
		if (minutes < 1) {
			return "Just now";
		} else if (minutes < 60) {
			return minutes + "m ago";
		} else if (minutes < 1440) {
			long hours = minutes / 60;
			return hours + "h ago";
		} else {
			long days = minutes / 1440;
			return days + "d ago";
		}
	}

	private String formatTimeUntil(long minutes) {
		if (minutes < 60) {
			return minutes + "m";
		} else if (minutes < 1440) {
			long hours = minutes / 60;
			return hours + "h";
		} else {
			long days = minutes / 1440;
			return days + "d";
		}
	}

	@Override
	public void onProfileLoad(ProfileAux profile) {
		loadTaskStatuses();
	}

	public void cleanup() {
		if (updateTimeline != null) {
			updateTimeline.stop();
		}
	}

	// Inner classes for table data
	public static class TaskRowData {

	}

	private String formatCombinedPetSkillsStatus(Map<Integer, DTODailyTaskStatus> taskStatuses) {
		// Check all pet skill tasks and return the best status
		TpDailyTaskEnum[] petSkillTasks = { TpDailyTaskEnum.PET_SKILL_STAMINA, TpDailyTaskEnum.PET_SKILL_FOOD, TpDailyTaskEnum.PET_SKILL_TREASURE, TpDailyTaskEnum.PET_SKILL_GATHERING };

		String bestStatus = "Never";
		int bestPriority = 0; // 0=Never, 1=Future, 2=Ready

		for (TpDailyTaskEnum petSkillTask : petSkillTasks) {
			DTODailyTaskStatus taskStatus = taskStatuses.get(petSkillTask.getId());
			String status = formatTaskStatus(taskStatus);

			int priority = 0;
			if (status.equals("Ready")) {
				priority = 2;
			} else if (!status.equals("Never") && !status.equals("--")) {
				priority = 1;
			}

			if (priority > bestPriority) {
				bestPriority = priority;
				bestStatus = status;
			}
		}

		return bestStatus;
	}

}
