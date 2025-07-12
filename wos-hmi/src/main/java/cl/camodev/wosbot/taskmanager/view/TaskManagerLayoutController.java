package cl.camodev.wosbot.taskmanager.view;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTODailyTaskStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.ot.DTOTaskState;
import cl.camodev.wosbot.serv.impl.ServProfiles;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.taskmanager.controller.TaskManagerActionController;
import cl.camodev.wosbot.taskmanager.model.TaskManagerAux;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TaskManagerLayoutController {

	private final Image iconTrue = new Image(getClass().getResourceAsStream("/icons/indicators/green.png"));
	private final Image iconFalse = new Image(getClass().getResourceAsStream("/icons/indicators/red.png"));

	private TaskManagerActionController taskManagerActionController = new TaskManagerActionController(this);

	@FXML
	private TabPane tabPaneProfiles;

	private final ObjectProperty<LocalDateTime> globalClock = new SimpleObjectProperty<>(LocalDateTime.now());

	private final Map<Long, Tab> profileTabsMap = new HashMap<>();
	private final Map<Long, ObservableList<TaskManagerAux>> tasks = new HashMap<>();

	private static final Comparator<TaskManagerAux> TASK_AUX_COMPARATOR = (a, b) -> {
		if (a.isScheduled() && !b.isScheduled())
			return -1;
		if (!a.isScheduled() && b.isScheduled())
			return 1;
		if (a.isExecuting() && !b.isExecuting())
			return -1;
		if (!a.isExecuting() && b.isExecuting())
			return 1;
		if (a.hasReadyTask() && !b.hasReadyTask())
			return -1;
		if (!a.hasReadyTask() && b.hasReadyTask())
			return 1;
		return Long.compare(a.getNearestMinutesUntilExecution(), b.getNearestMinutesUntilExecution());
	};

	@FXML
	private void initialize() {
		loadProfiles();

		Timeline ticker = new Timeline(new KeyFrame(Duration.seconds(5), evt -> {
			globalClock.set(LocalDateTime.now());
		}));
		ticker.setCycleCount(Animation.INDEFINITE);
		ticker.play();

	}

	private void loadProfiles() {
		taskManagerActionController.loadProfiles(dtoProfiles -> {
			Platform.runLater(() -> {
				if (tabPaneProfiles == null)
					return;

				for (DTOProfiles profile : dtoProfiles) {

					Tab existing = profileTabsMap.get(profile.getId());
					if (existing == null) {
						// Nuevo perfil → crea tab
						Tab newTab = createProfileTab(profile);
						profileTabsMap.put(profile.getId(), newTab);
						tabPaneProfiles.getTabs().add(newTab);
					} else {
//						refreshProfileTab(profile);
					}
				}

				SingleSelectionModel<Tab> sel = tabPaneProfiles.getSelectionModel();
				if (!tabPaneProfiles.getTabs().isEmpty()) {
					sel.select(0);
				}
			});
		});

	}

	private Tab createProfileTab(DTOProfiles profile) {
		Tab tab = new Tab(profile.getName());
		tab.setClosable(false);
		tab.setUserData(profile.getId());

		// 1) Prepara la tabla y la lista observable vacía
		ObservableList<TaskManagerAux> dataList = FXCollections.observableArrayList();
		TableView<TaskManagerAux> table = createTaskTable();
		table.setItems(dataList);
		// Guarda la lista para futuras actualizaciones
		tasks.put(profile.getId(), dataList);
		tab.setContent(table);

		// 2) Llama al builder asíncrono y actualiza la tabla cuando esté listo
		buildTaskManagerList(profile, list -> {
			// Siempre desde JavaFX Application Thread
			dataList.setAll(list);
			FXCollections.sort(dataList, TASK_AUX_COMPARATOR);
		});

		return tab;
	}

//	private void refreshProfileTab(DTOProfiles profile) {
//		ObservableList<TaskManagerAux> dataList = tasks.get(profile.getId());
//		if (dataList == null)
//			return;
//
//		List<TaskManagerAux> updated = buildTaskManagerList(profile);
//		dataList.setAll(updated);
//	}

	/**
	 * Recarga el estado de las tareas y, cuando estén disponibles, construye la lista de TaskManagerAux y la entrega al consumidor.
	 */
	private void buildTaskManagerList(DTOProfiles profile, Consumer<List<TaskManagerAux>> onListReady) {
		// Ahora `statuses` es una List<DTODailyTaskStatus>
		taskManagerActionController.loadDailyTaskStatus(profile.getId(), (List<DTODailyTaskStatus> statuses) -> {
			List<TaskManagerAux> list = Arrays.stream(TpDailyTaskEnum.values()).map(task -> {
				// Busca el status cuyo ID coincida con el ID de la tarea
//				System.out.println(">>> statuses.size=" + statuses.size() + "  buscando id=" + task.getId());

				DTODailyTaskStatus s = statuses.stream().filter(st -> st.getIdTpDailyTask() == task.getId()) // o st.getTaskId()
						.findFirst().orElse(null);

				if (s == null) {
					return new TaskManagerAux(task.getName(), null, null, task, profile.getId(), Long.MAX_VALUE, false, false, false);
				}

				long diff = Long.MAX_VALUE;
				boolean ready = false;
				if (s.getNextSchedule() != null) {
					diff = ChronoUnit.MINUTES.between(LocalDateTime.now(), s.getNextSchedule());
					if (diff <= 0) {
						ready = true;
						diff = 0;
					}
				}

				boolean scheduled = Optional.ofNullable(ServScheduler.getServices().getQueueManager().getQueue(profile.getId())).map(q -> q.isTaskScheduled(task)).orElse(false);

				return new TaskManagerAux(task.getName(), s.getLastExecution(), s.getNextSchedule(), task, profile.getId(), diff, ready, scheduled, false);
			}).sorted((a, b) -> {
				if (a.isScheduled() && !b.isScheduled())
					return -1;
				if (!a.isScheduled() && b.isScheduled())
					return 1;
				if (a.hasReadyTask() && !b.hasReadyTask())
					return -1;
				if (!a.hasReadyTask() && b.hasReadyTask())
					return 1;
				return Long.compare(a.getNearestMinutesUntilExecution(), b.getNearestMinutesUntilExecution());
			}).collect(Collectors.toList());

			Platform.runLater(() -> onListReady.accept(list));
		});
	}

	private TableView<TaskManagerAux> createTaskTable() {
		TableView<TaskManagerAux> table = new TableView<>();
		table.getStyleClass().add("table-view");


		// Task Name column
		TableColumn<TaskManagerAux, String> colTaskName = new TableColumn<>("Task Name");
		colTaskName.setCellValueFactory(cellData -> cellData.getValue().taskNameProperty());
		colTaskName.setPrefWidth(200);
		colTaskName.setCellFactory(column -> new TableCell<TaskManagerAux, String>() {
			private final ImageView imageView = new ImageView();
			{
				// Ajusta tamaño del icono si es necesario
				imageView.setFitWidth(16);
				imageView.setFitHeight(16);
			}

			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					setStyle("");
				} else {
					setText(item);
					// Obtén el objeto de la fila actual
					TaskManagerAux task = getTableRow().getItem();
					if (task != null) {
						// Elige el icono según la propiedad booleana
						boolean flag = task.scheduledProperty().get();
						imageView.setImage(flag ? iconTrue : iconFalse);
						setGraphic(imageView);
						setContentDisplay(ContentDisplay.LEFT);
					} else {
						setGraphic(null);
					}
					setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
				}
			}
		});

		// Last Execution column
		TableColumn<TaskManagerAux, String> colLastExecution = new TableColumn<>("Last Execution");
		colLastExecution.setCellValueFactory(cellData -> {
			TaskManagerAux t = cellData.getValue();
			return Bindings.createStringBinding(() -> {
				return UtilTime.formatLastExecution(t.getLastExecution());
			}, t.nextExecutionProperty(), t.executingProperty(), globalClock);
		});
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
		colNextExecution.setPrefWidth(150);
		colNextExecution.setCellValueFactory(cellData -> {
			TaskManagerAux t = cellData.getValue();
			return Bindings.createStringBinding(() -> {
				return UtilTime.formatNextExecution(t.getNextExecution(), globalClock.get(), t.executingProperty().get());
			}, t.nextExecutionProperty(), t.executingProperty(), globalClock);
		});

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

	public void updateTaskStatus(Long profileId, int taskNameId, DTOTaskState taskState) {
		Platform.runLater(() -> {
			ObservableList<TaskManagerAux> dataList = tasks.get(profileId);
			if (dataList == null)
				return;
			Optional<TaskManagerAux> optionalTask = dataList.stream().filter(aux -> aux.getTaskEnum().getId() == taskNameId).findFirst();
			if (!optionalTask.isPresent())
				return;

			Tab t = profileTabsMap.get(profileId);
			boolean hasQueue = ServScheduler
					.getServices()
					.getQueueManager()
					.getQueue(profileId) != null;

			ImageView iv = new ImageView(hasQueue ? iconTrue : iconFalse);

			iv.setFitWidth(16);
			iv.setFitHeight(16);

			t.setGraphic(iv);
			TaskManagerAux taskAux = optionalTask.get();
			taskAux.setLastExecution(taskState.getLastExecutionTime());
			taskAux.setNextExecution(taskState.getNextExecutionTime());
			taskAux.setScheduled(taskState.isScheduled());
			taskAux.setExecuting(taskState.isExecuting());
			taskAux.setHasReadyTask(taskState.getNextExecutionTime() != null && ChronoUnit.MINUTES.between(LocalDateTime.now(), taskState.getNextExecutionTime()) <= 0);
			taskAux.setNearestMinutesUntilExecution(taskState.getNextExecutionTime() != null ? ChronoUnit.MINUTES.between(LocalDateTime.now(), taskState.getNextExecutionTime()) : Long.MAX_VALUE);

			FXCollections.sort(dataList, TASK_AUX_COMPARATOR);

			List<Tab> sortedTabs = profileTabsMap.entrySet().stream()
					.sorted((e1, e2) -> {

						boolean b1 = ServScheduler.getServices().getQueueManager().getQueue(e1.getKey()) != null;
						boolean b2 = ServScheduler.getServices().getQueueManager().getQueue(e2.getKey()) != null;
						if (b1 == b2) return 0;
						return b1 ? -1 : 1; // true primero
					})
					.map(Map.Entry::getValue)
					.collect(Collectors.toList());
			tabPaneProfiles.getTabs().setAll(sortedTabs);
		});

	}

}
