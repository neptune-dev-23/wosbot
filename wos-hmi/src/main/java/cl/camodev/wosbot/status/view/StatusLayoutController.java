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
        // Set up the task name column
        colTaskName.setCellValueFactory(cellData -> cellData.getValue().taskNameProperty());
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
                    setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
                }
            }
        });

        // Set table data
        tableProfiles.setItems(taskData);
        
        // Create profile columns dynamically
        createProfileColumns();
        
        // Set up row styling
        tableProfiles.setRowFactory(tv -> {
            javafx.scene.control.TableRow<TaskStatusRow> row = new javafx.scene.control.TableRow<>();
            row.setStyle("-fx-background-color: #2a2a3d;");
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
                column.setPrefWidth(80.0);
                
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
                            String style = "-fx-font-size: 12px; -fx-font-weight: bold; ";
                            
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
            "Heroes", "Merchant", "Academy", "Crystal", "VIP", "PetAdv",
            "Explore", "Tech", "Essence", "PetTreas", "Chests", "Training",
            "Gather", "Bank", "Beast", "Speed", "Mail", "Online",
            "Intel", "Autojoin", "Help"
        };

        // Add pet skills as a combined row
        TaskStatusRow petSkillsRow = new TaskStatusRow("PetSkill");
        for (DTOProfiles profile : profiles) {
            Map<Integer, DTODailyTaskStatus> taskStatuses = dailyTaskRepository.findDailyTasksStatusByProfile(profile.getId());
            String combinedStatus = formatCombinedPetSkillsStatus(taskStatuses);
            petSkillsRow.setStatusForProfile(profile.getId(), combinedStatus);
        }
        taskData.add(petSkillsRow);

        // Create rows for each task
        for (int i = 0; i < tasks.length; i++) {
            TaskStatusRow row = new TaskStatusRow(taskNames[i]);
            
            for (DTOProfiles profile : profiles) {
                Map<Integer, DTODailyTaskStatus> taskStatuses = dailyTaskRepository.findDailyTasksStatusByProfile(profile.getId());
                DTODailyTaskStatus taskStatus = taskStatuses.get(tasks[i].getId());
                String formattedStatus = formatTaskStatus(taskStatus);
                row.setStatusForProfile(profile.getId(), formattedStatus);
            }
            
            taskData.add(row);
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
