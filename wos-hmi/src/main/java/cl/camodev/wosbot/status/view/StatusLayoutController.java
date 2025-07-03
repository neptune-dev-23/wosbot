package cl.camodev.wosbot.status.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cl.camodev.wosbot.almac.repo.DailyTaskRepository;
import cl.camodev.wosbot.almac.repo.IDailyTaskRepository;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTODailyTaskStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.profile.model.IProfileLoadListener;
import cl.camodev.wosbot.profile.model.ProfileAux;
import cl.camodev.wosbot.serv.impl.ServProfiles;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class StatusLayoutController implements IProfileLoadListener {

    @FXML
    private GridPane gridTaskOverview;
    
    @FXML
    private TableView<TaskStatusRow> tableProfiles;
    
    @FXML
    private TableColumn<TaskStatusRow, String> colTaskName;

    private IDailyTaskRepository dailyTaskRepository = DailyTaskRepository.getRepository();
    private Timeline updateTimeline;
    private ObservableList<TaskStatusRow> taskData = FXCollections.observableArrayList();
    private List<TableColumn<TaskStatusRow, String>> profileColumns = new ArrayList<>();
    
    @FXML
    private void initialize() {
        setupTable();
        setupAutoRefresh();
        loadTaskStatuses();
    }

    private void setupTable() {
        // Set up the task name column with auto-sizing
        colTaskName.setCellValueFactory(cellData -> cellData.getValue().taskNameProperty());
        colTaskName.setMinWidth(80.0);
        colTaskName.setPrefWidth(120.0);
        colTaskName.setMaxWidth(Double.MAX_VALUE);
        colTaskName.setCellFactory(column -> new TableCell<TaskStatusRow, String>() {
            @Override
            protected void updateItem(String taskName, boolean empty) {
                super.updateItem(taskName, empty);
                if (empty || taskName == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(taskName);
                    setAlignment(Pos.CENTER_LEFT);
                    setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4px 15px 4px 15px;");
                }
            }
        });

        // Set table data
        tableProfiles.setItems(taskData);
        
        // Enable constrained resize to fill 100% width
        tableProfiles.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Create profile columns dynamically
        createProfileColumns();
        
        // Set up row styling with reduced height and alternating colors
        tableProfiles.setRowFactory(tv -> {
            javafx.scene.control.TableRow<TaskStatusRow> row = new javafx.scene.control.TableRow<TaskStatusRow>() {
                @Override
                protected void updateItem(TaskStatusRow item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        if (getIndex() % 2 == 0) {
                            // Even rows - matches CSS .table-row-cell:even
                            setStyle("-fx-background-color: #2a2a3d; -fx-padding: 0;");
                        } else {
                            // Odd rows - matches CSS .table-row-cell:odd
                            setStyle("-fx-background-color: #3a3a4d; -fx-padding: 0;");
                        }
                    }
                }
            };
            return row;
        });
    }

    private void createProfileColumns() {
        // Clear existing profile columns
        tableProfiles.getColumns().removeAll(profileColumns);
        profileColumns.clear();
        
        // Get profiles and create columns
        List<DTOProfiles> profiles = ServProfiles.getServices().getProfiles();
        if (profiles != null) {
            for (DTOProfiles profile : profiles) {
                TableColumn<TaskStatusRow, String> column = new TableColumn<>(String.valueOf(profile.getEmulatorNumber()));
                // Make profile columns as small as possible while readable
                column.setMinWidth(45.0);
                column.setPrefWidth(50.0);
                column.setMaxWidth(60.0);
                
                // Set cell value factory to get the status for this profile
                final Long profileId = profile.getId();
                column.setCellValueFactory(cellData -> {
                    TaskStatusRow row = cellData.getValue();
                    return new SimpleStringProperty(row.getStatusForProfile(profileId));
                });
                
                // Set cell factory for styling
                final boolean isActive = profile.getEnabled();
                column.setCellFactory(col -> new TableCell<TaskStatusRow, String>() {
                    @Override
                    protected void updateItem(String taskInfo, boolean empty) {
                        super.updateItem(taskInfo, empty);
                        if (empty || taskInfo == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(taskInfo);
                            setAlignment(Pos.CENTER);
                            
                            // Enhanced color coding based on task status and timing
                            String style = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 1px 2px 1px 2px; ";
                            
                            if (!isActive) {
                                style += "-fx-text-fill: #888; -fx-opacity: 0.6;"; // Dim inactive profiles
                            } else if (taskInfo.equals("Ready")) {
                                style += "-fx-text-fill: #4CAF50;"; // Green - ready to execute
                            } else if (taskInfo.contains("Never") || taskInfo.equals("--")) {
                                style += "-fx-text-fill: #757575; -fx-font-weight: normal;"; // Gray - never executed
                            } else if (taskInfo.contains("ago")) {
                                style += "-fx-text-fill: #ffc107;"; // Yellow - executed in past
                            } else if (taskInfo.contains("m") || taskInfo.contains("h") || taskInfo.contains("d")) {
                                // Time-based coloring for upcoming tasks
                                String timeStr = taskInfo.replaceAll("[^0-9]", "");
                                if (!timeStr.isEmpty()) {
                                    int timeValue = Integer.parseInt(timeStr);
                                    if (taskInfo.contains("m")) {
                                        // Minutes
                                        if (timeValue <= 15) {
                                            style += "-fx-text-fill: #4CAF50;"; // Green - very soon (≤15m)
                                        } else {
                                            style += "-fx-text-fill: #ffc107;"; // Orange - far
                                        }
                                    } else {
                                        style += "-fx-text-fill: #ff9800;"; // Orange - far
                                    }
                                } else {
                                    style += "-fx-text-fill: white;"; // Default white
                                }
                            } else {
                                style += "-fx-text-fill: white;"; // Default white
                            }
                            
                            setStyle(style);
                        }
                    }
                });
                
                profileColumns.add(column);
                tableProfiles.getColumns().add(column);
            }
        }
    }

    private void setupAutoRefresh() {
        // Update every 30 seconds
        updateTimeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            Platform.runLater(this::loadTaskStatuses);
        }));
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
        updateTimeline.play();
    }

    private void loadTaskStatuses() {
        Platform.runLater(() -> {
            taskData.clear();
            
            List<DTOProfiles> profiles = ServProfiles.getServices().getProfiles();
            if (profiles == null || profiles.isEmpty()) {
                return;
            }

            // Recreate profile columns
            createProfileColumns();
            
            // Create task rows
            createTaskRows(profiles);
            
            updateTaskOverview(profiles);
        });
    }

    private void createTaskRows(List<DTOProfiles> profiles) {
        // Define tasks in enum order
        TpDailyTaskEnum[] tasks = {
            TpDailyTaskEnum.HERO_RECRUITMENT, TpDailyTaskEnum.NOMADIC_MERCHANT, TpDailyTaskEnum.WAR_ACADEMY_SHARDS,
            TpDailyTaskEnum.CRYSTAL_LABORATORY, TpDailyTaskEnum.VIP_POINTS, TpDailyTaskEnum.PET_ADVENTURE,
            TpDailyTaskEnum.EXPLORATION_CHEST, TpDailyTaskEnum.ALLIANCE_TECH, TpDailyTaskEnum.LIFE_ESSENCE,
            TpDailyTaskEnum.ALLIANCE_PET_TREASURE, TpDailyTaskEnum.ALLIANCE_CHESTS, TpDailyTaskEnum.TRAINING_TROOPS,
            TpDailyTaskEnum.GATHER_RESOURCES, TpDailyTaskEnum.BANK, TpDailyTaskEnum.BEAST_SLAY,
            TpDailyTaskEnum.GATHER_SPEED, TpDailyTaskEnum.MAIL_REWARDS, TpDailyTaskEnum.DAILY_TASKS,
            TpDailyTaskEnum.INTEL, TpDailyTaskEnum.ALLIANCE_AUTOJOIN, TpDailyTaskEnum.ALLIANCE_HELP
        };

        String[] taskNames = {
            "Hero Recruitment", "Nomadic Merchant", "War Academy Shards", "Crystal Laboratory", "VIP Points", "Pet Adventure",
            "Exploration Chest", "Alliance Tech", "Life Essence", "Alliance Pet Treasure", "Alliance Chests", "Training Troops",
            "Gather Resources", "Bank", "Beast Slay", "Gather Speed", "Mail Rewards", "Daily Tasks",
            "Intel", "Alliance Autojoin", "Alliance Help"
        };

        // Create task rows and sort by nearest execution time
        List<TaskRowData> taskRowDataList = new ArrayList<>();
        
        // Add pet skills as a combined row for sorting
        TaskStatusRow petSkillsRow = new TaskStatusRow("Pet Skills");
        long petSkillsNearestMinutes = Long.MAX_VALUE;
        boolean petSkillsHasReady = false;
        boolean petSkillsHasScheduled = false;
        
        for (DTOProfiles profile : profiles) {
            if (!profile.getEnabled()) continue; // Skip disabled profiles
            
            Map<Integer, DTODailyTaskStatus> taskStatuses = dailyTaskRepository.findDailyTasksStatusByProfile(profile.getId());
            String combinedStatus = formatCombinedPetSkillsStatus(taskStatuses);
            petSkillsRow.setStatusForProfile(profile.getId(), combinedStatus);
            
            // Calculate pet skills execution time for sorting
            TpDailyTaskEnum[] petSkillTasks = {
                TpDailyTaskEnum.PET_SKILL_STAMINA, TpDailyTaskEnum.PET_SKILL_FOOD,
                TpDailyTaskEnum.PET_SKILL_TREASURE, TpDailyTaskEnum.PET_SKILL_GATHERING
            };
            
            for (TpDailyTaskEnum petSkillTask : petSkillTasks) {
                DTODailyTaskStatus taskStatus = taskStatuses.get(petSkillTask.getId());
                if (taskStatus != null && taskStatus.getNextSchedule() != null) {
                    long minutesUntil = ChronoUnit.MINUTES.between(LocalDateTime.now(), taskStatus.getNextSchedule());
                    if (minutesUntil <= 0) {
                        petSkillsHasReady = true;
                        petSkillsNearestMinutes = 0;
                        break; // If any pet skill is ready, that's the best we can get
                    } else if (!petSkillsHasReady) {
                        petSkillsHasScheduled = true;
                        petSkillsNearestMinutes = Math.min(petSkillsNearestMinutes, minutesUntil);
                    }
                }
            }
        }
        
        if (!petSkillsHasReady && !petSkillsHasScheduled) {
            petSkillsNearestMinutes = Long.MAX_VALUE;
        }
        
        taskRowDataList.add(new TaskRowData(petSkillsRow, petSkillsNearestMinutes, petSkillsHasReady));
        
        // Add other tasks
        for (int i = 0; i < tasks.length; i++) {
            TaskStatusRow row = new TaskStatusRow(taskNames[i]);
            long nearestMinutesUntilExecution = Long.MAX_VALUE;
            boolean hasReadyTask = false;
            boolean hasScheduledTask = false;
            
            for (DTOProfiles profile : profiles) {
                if (!profile.getEnabled()) continue; // Skip disabled profiles
                
                Map<Integer, DTODailyTaskStatus> taskStatuses = dailyTaskRepository.findDailyTasksStatusByProfile(profile.getId());
                DTODailyTaskStatus taskStatus = taskStatuses.get(tasks[i].getId());
                String formattedStatus = formatTaskStatus(taskStatus);
                row.setStatusForProfile(profile.getId(), formattedStatus);
                
                // Calculate actual minutes until execution based on the task status
                if (taskStatus != null && taskStatus.getNextSchedule() != null) {
                    long minutesUntil = ChronoUnit.MINUTES.between(LocalDateTime.now(), taskStatus.getNextSchedule());
                    if (minutesUntil <= 0) {
                        hasReadyTask = true;
                        nearestMinutesUntilExecution = 0;
                    } else if (!hasReadyTask) {
                        hasScheduledTask = true;
                        nearestMinutesUntilExecution = Math.min(nearestMinutesUntilExecution, minutesUntil);
                    }
                }
            }
            
            // If no scheduled tasks found, set to max value for "Never" tasks
            if (!hasReadyTask && !hasScheduledTask) {
                nearestMinutesUntilExecution = Long.MAX_VALUE;
            }
            
            taskRowDataList.add(new TaskRowData(row, nearestMinutesUntilExecution, hasReadyTask));
        }
        
        // Sort by nearest execution time (Ready tasks first, then by schedule time)
        taskRowDataList.sort((a, b) -> {
            // Ready tasks always come first
            if (a.hasReadyTask && !b.hasReadyTask) return -1;
            if (!a.hasReadyTask && b.hasReadyTask) return 1;
            
            // If both are ready or both are not ready, sort by minutes until execution
            return Long.compare(a.nearestMinutesUntilExecution, b.nearestMinutesUntilExecution);
        });
        
        // Add sorted rows to table
        for (TaskRowData taskRowData : taskRowDataList) {
            taskData.add(taskRowData.row);
        }
    }
    
    // Helper class to hold task row data with execution time for sorting
    private static class TaskRowData {
        final TaskStatusRow row;
        final long nearestMinutesUntilExecution;
        final boolean hasReadyTask;
        
        TaskRowData(TaskStatusRow row, long nearestMinutesUntilExecution, boolean hasReadyTask) {
            this.row = row;
            this.nearestMinutesUntilExecution = nearestMinutesUntilExecution;
            this.hasReadyTask = hasReadyTask;
        }
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

    private void updateTaskOverview(List<DTOProfiles> profiles) {
        gridTaskOverview.getChildren().clear();
        
        // Count tasks by status
        int readyTasks = 0;
        int overdueTasks = 0;
        int scheduledTasks = 0;
        int neverExecutedTasks = 0;
        
        TpDailyTaskEnum[] importantTasks = {
            TpDailyTaskEnum.HERO_RECRUITMENT, TpDailyTaskEnum.NOMADIC_MERCHANT, TpDailyTaskEnum.WAR_ACADEMY_SHARDS,
            TpDailyTaskEnum.CRYSTAL_LABORATORY, TpDailyTaskEnum.VIP_POINTS, TpDailyTaskEnum.PET_ADVENTURE,
            TpDailyTaskEnum.EXPLORATION_CHEST, TpDailyTaskEnum.ALLIANCE_TECH, TpDailyTaskEnum.LIFE_ESSENCE,
            TpDailyTaskEnum.ALLIANCE_PET_TREASURE, TpDailyTaskEnum.ALLIANCE_CHESTS, TpDailyTaskEnum.TRAINING_TROOPS,
            TpDailyTaskEnum.GATHER_RESOURCES, TpDailyTaskEnum.BANK, TpDailyTaskEnum.BEAST_SLAY,
            TpDailyTaskEnum.PET_SKILL_STAMINA, TpDailyTaskEnum.PET_SKILL_FOOD,
            TpDailyTaskEnum.PET_SKILL_TREASURE, TpDailyTaskEnum.PET_SKILL_GATHERING,
            TpDailyTaskEnum.GATHER_SPEED, TpDailyTaskEnum.MAIL_REWARDS, TpDailyTaskEnum.DAILY_TASKS,
            TpDailyTaskEnum.INTEL, TpDailyTaskEnum.ALLIANCE_AUTOJOIN, TpDailyTaskEnum.ALLIANCE_HELP
        };
        
        for (DTOProfiles profile : profiles) {
            if (!profile.getEnabled()) continue;
            
            Map<Integer, DTODailyTaskStatus> taskStatuses = dailyTaskRepository.findDailyTasksStatusByProfile(profile.getId());
            
            for (TpDailyTaskEnum taskEnum : importantTasks) {
                DTODailyTaskStatus taskStatus = taskStatuses.get(taskEnum.getId());
                if (taskStatus == null) {
                    neverExecutedTasks++;
                } else if (taskStatus.getNextSchedule() != null) {
                    long minutesUntil = ChronoUnit.MINUTES.between(LocalDateTime.now(), taskStatus.getNextSchedule());
                    if (minutesUntil <= 0) {
                        readyTasks++;
                    } else {
                        scheduledTasks++;
                    }
                } else if (taskStatus.getLastExecution() != null) {
                    long minutesAgo = ChronoUnit.MINUTES.between(taskStatus.getLastExecution(), LocalDateTime.now());
                    if (minutesAgo > 120) { // Over 2 hours ago
                        overdueTasks++;
                    } else {
                        scheduledTasks++;
                    }
                } else {
                    neverExecutedTasks++;
                }
            }
        }
        
        // Create overview cards
        createOverviewCard("Ready", String.valueOf(readyTasks), "#4CAF50", 0, 0);
        createOverviewCard("Overdue", String.valueOf(overdueTasks), "#ff5722", 0, 1);
        createOverviewCard("Scheduled", String.valueOf(scheduledTasks), "#ffc107", 0, 2);
        createOverviewCard("Never Run", String.valueOf(neverExecutedTasks), "#757575", 0, 3);
    }

    private void createOverviewCard(String title, String count, String color, int row, int col) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: #2a2a3d; -fx-border-color: " + color + "; -fx-border-width: 2px; " +
                     "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 10px; -fx-min-width: 120px;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label countLabel = new Label(count);
        countLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        card.getChildren().addAll(titleLabel, spacer, countLabel);
        gridTaskOverview.add(card, col, row);
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

    // Inner class for table data
    public static class TaskStatusRow {
        private final SimpleStringProperty taskName = new SimpleStringProperty();
        private final Map<Long, String> profileStatuses = new HashMap<>();

        public TaskStatusRow(String taskName) {
            this.taskName.set(taskName);
        }

        public String getTaskName() { return taskName.get(); }
        public SimpleStringProperty taskNameProperty() { return taskName; }

        public void setStatusForProfile(Long profileId, String status) {
            profileStatuses.put(profileId, status);
        }

        public String getStatusForProfile(Long profileId) {
            return profileStatuses.getOrDefault(profileId, "--");
        }
    }
    
    private String formatCombinedPetSkillsStatus(Map<Integer, DTODailyTaskStatus> taskStatuses) {
        // Check all pet skill tasks and return the best status
        TpDailyTaskEnum[] petSkillTasks = {
            TpDailyTaskEnum.PET_SKILL_STAMINA,
            TpDailyTaskEnum.PET_SKILL_FOOD,
            TpDailyTaskEnum.PET_SKILL_TREASURE,
            TpDailyTaskEnum.PET_SKILL_GATHERING
        };
        
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
