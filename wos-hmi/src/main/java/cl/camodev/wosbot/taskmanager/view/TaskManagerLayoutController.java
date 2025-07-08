package cl.camodev.wosbot.taskmanager.view;

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
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

// Additional imports for task execution
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.TaskQueue;
import cl.camodev.wosbot.serv.task.TaskQueueManager;
import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;

public class TaskManagerLayoutController implements IProfileLoadListener {


    
    @FXML
    private TabPane tabPaneProfiles;

    private IDailyTaskRepository dailyTaskRepository = DailyTaskRepository.getRepository();
    private Timeline updateTimeline;
    private List<ProfileTaskTable> profileTables = new ArrayList<>();
    
    @FXML
    private void initialize() {
        setupAutoRefresh();
        loadTaskStatuses();
    }

    private void setupProfileTabs() {
        // Additional TabPane setup can go here if needed
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
            profileTables.clear();
            if (tabPaneProfiles != null) {
                tabPaneProfiles.getTabs().clear();
            }
            
            List<DTOProfiles> profiles = ServProfiles.getServices().getProfiles();
            if (profiles == null || profiles.isEmpty()) {
                return;
            }
            
            // Create profile tabs
            createProfileTabs(profiles);
        });
    }

    private void createProfileTabs(List<DTOProfiles> profiles) {
        // Define tasks in enum order
        TpDailyTaskEnum[] tasks = {
            TpDailyTaskEnum.HERO_RECRUITMENT, TpDailyTaskEnum.NOMADIC_MERCHANT, TpDailyTaskEnum.WAR_ACADEMY_SHARDS,
            TpDailyTaskEnum.CRYSTAL_LABORATORY, TpDailyTaskEnum.VIP_POINTS, TpDailyTaskEnum.PET_ADVENTURE,
            TpDailyTaskEnum.EXPLORATION_CHEST, TpDailyTaskEnum.ALLIANCE_TECH, TpDailyTaskEnum.LIFE_ESSENCE,
            TpDailyTaskEnum.ALLIANCE_PET_TREASURE, TpDailyTaskEnum.ALLIANCE_CHESTS, TpDailyTaskEnum.TRAINING_TROOPS,
            TpDailyTaskEnum.GATHER_RESOURCES, TpDailyTaskEnum.BANK, TpDailyTaskEnum.BEAST_SLAY,
            TpDailyTaskEnum.GATHER_SPEED, TpDailyTaskEnum.MAIL_REWARDS, TpDailyTaskEnum.DAILY_TASKS,
            TpDailyTaskEnum.INTEL, TpDailyTaskEnum.ALLIANCE_AUTOJOIN, TpDailyTaskEnum.ALLIANCE_HELP,
            TpDailyTaskEnum.PET_SKILL_STAMINA, TpDailyTaskEnum.PET_SKILL_FOOD,
            TpDailyTaskEnum.PET_SKILL_TREASURE, TpDailyTaskEnum.PET_SKILL_GATHERING
        };

        String[] taskNames = {
            "Hero Recruitment", "Nomadic Merchant", "War Academy Shards", "Crystal Laboratory", "VIP Points", "Pet Adventure",
            "Exploration Chest", "Alliance Tech", "Life Essence", "Alliance Pet Treasure", "Alliance Chests", "Training Troops",
            "Gather Resources", "Bank", "Beast Slay", "Gather Speed", "Mail Rewards", "Daily Tasks",
            "Intel", "Alliance Autojoin", "Alliance Help", "Pet Skill Stamina", "Pet Skill Food",
            "Pet Skill Treasure", "Pet Skill Gathering"
        };

        // Create tabs for each profile
        boolean isFirstTab = true;
        for (DTOProfiles profile : profiles) {
            if (!profile.getEnabled()) continue;
            
            // Create tab
            Tab tab = new Tab(profile.getName());
            tab.setClosable(false);
            // Tab styling will be handled by CSS classes from style.css
            
            // Create table for this profile
            TableView<TaskRowData> table = createTaskTable();
            ProfileTaskTable profileTable = new ProfileTaskTable(profile.getId(), table);
            profileTables.add(profileTable);
            
            // Get task statuses for this profile
            Map<Integer, DTODailyTaskStatus> taskStatuses = dailyTaskRepository.findDailyTasksStatusByProfile(profile.getId());
            
            // Create task rows and sort by execution time
            List<TaskRowData> taskRowDataList = new ArrayList<>();
            
            for (int i = 0; i < tasks.length; i++) {
                DTODailyTaskStatus taskStatus = taskStatuses.get(tasks[i].getId());
                String lastExecution = formatLastExecution(taskStatus);
                String nextExecution = formatNextExecution(taskStatus);
                
                long nearestMinutesUntilExecution = Long.MAX_VALUE;
                boolean hasReadyTask = false;
                
                if (taskStatus != null && taskStatus.getNextSchedule() != null) {
                    long minutesUntil = ChronoUnit.MINUTES.between(LocalDateTime.now(), taskStatus.getNextSchedule());
                    if (minutesUntil <= 0) {
                        hasReadyTask = true;
                        nearestMinutesUntilExecution = 0;
                    } else {
                        nearestMinutesUntilExecution = minutesUntil;
                    }
                }
                
                TaskRowData rowData = new TaskRowData(taskNames[i], lastExecution, nextExecution, 
                                                   tasks[i], profile.getId(), nearestMinutesUntilExecution, hasReadyTask);
                taskRowDataList.add(rowData);
            }
            
            // Sort by execution time (Ready tasks first, then by schedule time)
            taskRowDataList.sort((a, b) -> {
                if (a.hasReadyTask && !b.hasReadyTask) return -1;
                if (!a.hasReadyTask && b.hasReadyTask) return 1;
                return Long.compare(a.nearestMinutesUntilExecution, b.nearestMinutesUntilExecution);
            });
            
            // Add sorted data to table
            table.getItems().addAll(taskRowDataList);
            
            // Set tab content
            tab.setContent(table);
            
            // Add tab to TabPane
            tabPaneProfiles.getTabs().add(tab);
            
            // Select first tab by default
            if (isFirstTab) {
                tabPaneProfiles.getSelectionModel().select(tab);
                isFirstTab = false;
            }
        }
    }
    
    private TableView<TaskRowData> createTaskTable() {
        TableView<TaskRowData> table = new TableView<>();
        // Table styling will be handled by CSS classes from style.css
        table.getStyleClass().add("table-view");
        
        // Task Name column
        TableColumn<TaskRowData, String> colTaskName = new TableColumn<>("Task Name");
        colTaskName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().taskName));
        colTaskName.setPrefWidth(200);
        colTaskName.setCellFactory(column -> new TableCell<TaskRowData, String>() {
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
        TableColumn<TaskRowData, String> colLastExecution = new TableColumn<>("Last Execution");
        colLastExecution.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().lastExecution));
        colLastExecution.setPrefWidth(150);
        colLastExecution.setCellFactory(column -> new TableCell<TaskRowData, String>() {
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
        TableColumn<TaskRowData, String> colNextExecution = new TableColumn<>("Next Execution");
        colNextExecution.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().nextExecution));
        colNextExecution.setPrefWidth(150);
        colNextExecution.setCellFactory(column -> new TableCell<TaskRowData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String style = "-fx-font-size: 14px; ";
                    if (item.equals("Ready")) {
                        style += "-fx-text-fill: #4CAF50; -fx-font-weight: bold;";
                    } else if (item.contains("Never") || item.equals("--")) {
                        style += "-fx-text-fill: #757575;";
                    } else if (item.contains("m") || item.contains("h") || item.contains("d")) {
                        // Time-based coloring for upcoming tasks
                        String timeStr = item.replaceAll("[^0-9]", "");
                        if (!timeStr.isEmpty()) {
                            int timeValue = Integer.parseInt(timeStr);
                            if (item.contains("m")) {
                                // Minutes
                                if (timeValue <= 15) {
                                    style += "-fx-text-fill: #4CAF50; -fx-font-weight: bold;"; // Green - very soon (≤15m)
                                } else if (timeValue <= 60) {
                                    style += "-fx-text-fill: #ffc107;"; // Yellow - soon
                                } else {
                                    style += "-fx-text-fill: #ff9800;"; // Orange - far
                                }
                            } else if (item.contains("h")) {
                                // Hours
                                if (timeValue <= 2) {
                                    style += "-fx-text-fill: #ffc107;"; // Yellow - within 2 hours
                                } else {
                                    style += "-fx-text-fill: #ff9800;"; // Orange - far
                                }
                            } else {
                                style += "-fx-text-fill: #ff5722;"; // Red - days away
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
        
        // Actions column
        TableColumn<TaskRowData, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(100);
        colActions.setCellFactory(column -> new TableCell<TaskRowData, Void>() {
            private final Button btnExecute = new Button("Execute Now");
            
            {
                btnExecute.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6px 12px; -fx-border-radius: 3px; -fx-background-radius: 3px;");
                btnExecute.setOnAction(event -> {
                    TaskRowData rowData = getTableView().getItems().get(getIndex());
                    executeTask(rowData);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnExecute);
                }
            }
        });
        
        table.getColumns().addAll(colTaskName, colLastExecution, colNextExecution, colActions);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        return table;
    }
    
    private void executeTask(TaskRowData rowData) {
        try {
            // Get all profiles from ServProfiles
            List<DTOProfiles> allProfiles = ServProfiles.getServices().getProfiles();
            
            // Get the profile
            DTOProfiles profile = allProfiles.stream()
                .filter(p -> p.getId().equals(rowData.profileId))
                .findFirst()
                .orElse(null);
            
            if (profile == null) {
                System.err.println("Profile not found: " + rowData.profileId);
                return;
            }
            
            // Get the task enum for this task name
            TpDailyTaskEnum taskEnum = findTaskEnumByName(rowData.taskName);
            if (taskEnum == null) {
                System.err.println("Task enum not found for: " + rowData.taskName);
                return;
            }
            
            // Create the task instance
            DelayedTask task = createTaskInstance(profile, taskEnum);
            if (task == null) {
                System.err.println("Could not create task instance for: " + rowData.taskName);
                return;
            }
            
            // Update the task status in the database immediately to mark it as executed
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextSchedule = calculateNextExecution(taskEnum, profile, now);
            
            // Update the database with the new execution time and next schedule
            ServScheduler scheduler = ServScheduler.getServices();
            scheduler.updateDailyTaskStatus(profile, taskEnum, nextSchedule);
            
            // Get the task queue manager and add the task to run immediately
            TaskQueueManager queueManager = getQueueManager(scheduler);
            
            if (queueManager != null) {
                TaskQueue queue = queueManager.getQueue(profile.getName());
                if (queue != null) {
                    // Schedule the task to run immediately
                    task.reschedule(now);
                    queue.addTask(task);
                    System.out.println("Successfully queued task: " + rowData.taskName + " for profile: " + profile.getName());
                    System.out.println("Updated next execution to: " + nextSchedule);
                    
                    // Refresh the task status view after a short delay
                    Platform.runLater(() -> {
                        Timeline refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                            loadTaskStatuses();
                        }));
                        refreshTimeline.play();
                    });
                } else {
                    System.err.println("Task queue not found for profile: " + profile.getName());
                }
            } else {
                System.err.println("Could not access task queue manager");
            }
            
        } catch (Exception e) {
            System.err.println("Error executing task: " + e.getMessage());
            e.printStackTrace();
        }
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
        private final String taskName;
        private final String lastExecution;
        private final String nextExecution;
        private final TpDailyTaskEnum taskEnum;
        private final Long profileId;
        private final long nearestMinutesUntilExecution;
        private final boolean hasReadyTask;
        
        public TaskRowData(String taskName, String lastExecution, String nextExecution, 
                          TpDailyTaskEnum taskEnum, Long profileId, long nearestMinutesUntilExecution, boolean hasReadyTask) {
            this.taskName = taskName;
            this.lastExecution = lastExecution;
            this.nextExecution = nextExecution;
            this.taskEnum = taskEnum;
            this.profileId = profileId;
            this.nearestMinutesUntilExecution = nearestMinutesUntilExecution;
            this.hasReadyTask = hasReadyTask;
        }
        
        // Getters
        public String getTaskName() { return taskName; }
        public String getLastExecution() { return lastExecution; }
        public String getNextExecution() { return nextExecution; }
        public TpDailyTaskEnum getTaskEnum() { return taskEnum; }
        public Long getProfileId() { return profileId; }
        public long getNearestMinutesUntilExecution() { return nearestMinutesUntilExecution; }
        public boolean hasReadyTask() { return hasReadyTask; }
    }
    
    public static class ProfileTaskTable {
        private final Long profileId;
        private final TableView<TaskRowData> table;
        
        public ProfileTaskTable(Long profileId, TableView<TaskRowData> table) {
            this.profileId = profileId;
            this.table = table;
        }
        
        public Long getProfileId() { return profileId; }
        public TableView<TaskRowData> getTable() { return table; }
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
    
    private TpDailyTaskEnum findTaskEnumByName(String taskName) {
        // Map task display names to their corresponding enum values
        TpDailyTaskEnum[] tasks = {
            TpDailyTaskEnum.HERO_RECRUITMENT, TpDailyTaskEnum.NOMADIC_MERCHANT, TpDailyTaskEnum.WAR_ACADEMY_SHARDS,
            TpDailyTaskEnum.CRYSTAL_LABORATORY, TpDailyTaskEnum.VIP_POINTS, TpDailyTaskEnum.PET_ADVENTURE,
            TpDailyTaskEnum.EXPLORATION_CHEST, TpDailyTaskEnum.ALLIANCE_TECH, TpDailyTaskEnum.LIFE_ESSENCE,
            TpDailyTaskEnum.ALLIANCE_CHESTS, TpDailyTaskEnum.TRAINING_TROOPS, TpDailyTaskEnum.ALLIANCE_PET_TREASURE,
            TpDailyTaskEnum.PET_SKILL_FOOD, TpDailyTaskEnum.PET_SKILL_GATHERING, TpDailyTaskEnum.PET_SKILL_STAMINA,
            TpDailyTaskEnum.PET_SKILL_TREASURE, TpDailyTaskEnum.ALLIANCE_AUTOJOIN, TpDailyTaskEnum.STOREHOUSE_CHEST,
            TpDailyTaskEnum.STOREHOUSE_STAMINA, TpDailyTaskEnum.MAIL_REWARDS, TpDailyTaskEnum.GATHER_RESOURCES,
            TpDailyTaskEnum.GATHER_SPEED, TpDailyTaskEnum.INTEL, TpDailyTaskEnum.BANK,
            TpDailyTaskEnum.ALLIANCE_HELP, TpDailyTaskEnum.BEAST_SLAY
        };
        
        String[] taskNames = {
            "Hero Recruitment", "Nomadic Merchant", "War Academy Shards",
            "Crystal Laboratory", "VIP Points", "Pet Adventure",
            "Exploration Chest", "Alliance Tech", "Life Essence",
            "Alliance Chests", "Training Troops", "Alliance Pet Treasure",
            "Pet Skill Food", "Pet Skill Gathering", "Pet Skill Stamina",
            "Pet Skill Treasure", "Alliance Autojoin", "Storehouse Chest",
            "Storehouse Stamina", "Mail Rewards", "Gather Resources",
            "Gather Speed", "Intel", "Bank",
            "Alliance Help", "Beast Slay"
        };
        
        for (int i = 0; i < taskNames.length; i++) {
            if (taskNames[i].equals(taskName)) {
                return tasks[i];
            }
        }
        return null;
    }
    
    private DelayedTask createTaskInstance(DTOProfiles profile, TpDailyTaskEnum taskEnum) {
        // Create task instance based on the task enum
        switch (taskEnum) {
            case HERO_RECRUITMENT:
                return new cl.camodev.wosbot.serv.task.impl.HeroRecruitmentTask(profile, taskEnum);
            case NOMADIC_MERCHANT:
                return new cl.camodev.wosbot.serv.task.impl.NomadicMerchantTask(profile, taskEnum);
            case WAR_ACADEMY_SHARDS:
                return new cl.camodev.wosbot.serv.task.impl.WarAcademyTask(profile, taskEnum);
            case CRYSTAL_LABORATORY:
                return new cl.camodev.wosbot.serv.task.impl.CrystalLaboratoryTask(profile, taskEnum);
            case VIP_POINTS:
                return new cl.camodev.wosbot.serv.task.impl.VipTask(profile, taskEnum);
            case PET_ADVENTURE:
                return new cl.camodev.wosbot.serv.task.impl.PetAdventureChestTask(profile, taskEnum);
            case EXPLORATION_CHEST:
                return new cl.camodev.wosbot.serv.task.impl.ExplorationTask(profile, taskEnum);
            case ALLIANCE_TECH:
                return new cl.camodev.wosbot.serv.task.impl.AllianceTechTask(profile, taskEnum);
            case LIFE_ESSENCE:
                return new cl.camodev.wosbot.serv.task.impl.LifeEssenceTask(profile, taskEnum);
            case ALLIANCE_CHESTS:
                return new cl.camodev.wosbot.serv.task.impl.AllianceChestTask(profile, taskEnum);
            case TRAINING_TROOPS:
                // For training troops, we'll create an infantry task by default
                return new cl.camodev.wosbot.serv.task.impl.TrainingTroopsTask(profile, taskEnum, 
                    cl.camodev.wosbot.serv.task.impl.TrainingTroopsTask.TroopType.INFANTRY);
            case ALLIANCE_PET_TREASURE:
                return new cl.camodev.wosbot.serv.task.impl.PetAllianceTreasuresTask(profile, taskEnum);
            case PET_SKILL_FOOD:
                return new cl.camodev.wosbot.serv.task.impl.PetSkillsTask(profile, taskEnum,
                    cl.camodev.wosbot.serv.task.impl.PetSkillsTask.PetSkill.FOOD);
            case PET_SKILL_GATHERING:
                return new cl.camodev.wosbot.serv.task.impl.PetSkillsTask(profile, taskEnum,
                    cl.camodev.wosbot.serv.task.impl.PetSkillsTask.PetSkill.GATHERING);
            case PET_SKILL_STAMINA:
                return new cl.camodev.wosbot.serv.task.impl.PetSkillsTask(profile, taskEnum,
                    cl.camodev.wosbot.serv.task.impl.PetSkillsTask.PetSkill.STAMINA);
            case PET_SKILL_TREASURE:
                return new cl.camodev.wosbot.serv.task.impl.PetSkillsTask(profile, taskEnum,
                    cl.camodev.wosbot.serv.task.impl.PetSkillsTask.PetSkill.TREASURE);
            case ALLIANCE_AUTOJOIN:
                return new cl.camodev.wosbot.serv.task.impl.AllianceAutojoinTask(profile, taskEnum);
            case STOREHOUSE_CHEST:
                return new cl.camodev.wosbot.serv.task.impl.OnlineRewardTask(profile, taskEnum);
            case STOREHOUSE_STAMINA:
                return new cl.camodev.wosbot.serv.task.impl.DailyStaminaTask(profile, taskEnum);
            case MAIL_REWARDS:
                return new cl.camodev.wosbot.serv.task.impl.MailRewardsTask(profile, taskEnum);
            case GATHER_RESOURCES:
                // For gather resources, we'll create a meat gathering task by default
                return new cl.camodev.wosbot.serv.task.impl.GatherTask(profile, taskEnum,
                    cl.camodev.wosbot.serv.task.impl.GatherTask.GatherType.MEAT);
            case GATHER_SPEED:
                return new cl.camodev.wosbot.serv.task.impl.GatherSpeedTask(profile, taskEnum);
            case INTEL:
                return new cl.camodev.wosbot.serv.task.impl.IntelligenceTask(profile, taskEnum);
            case BANK:
                return new cl.camodev.wosbot.serv.task.impl.BankTask(profile, taskEnum);
            case ALLIANCE_HELP:
                return new cl.camodev.wosbot.serv.task.impl.AllianceHelpTask(profile, taskEnum);
            case BEAST_SLAY:
                return new cl.camodev.wosbot.serv.task.impl.BeastSlayTask(profile);
            default:
                return null;
        }
    }
    
    private TaskQueueManager getQueueManager(ServScheduler scheduler) {
        try {
            // Use reflection to access the private queueManager field
            java.lang.reflect.Field field = scheduler.getClass().getDeclaredField("queueManager");
            field.setAccessible(true);
            return (TaskQueueManager) field.get(scheduler);
        } catch (Exception e) {
            System.err.println("Failed to access task queue manager: " + e.getMessage());
            return null;
        }
    }
    
    private LocalDateTime calculateNextExecution(TpDailyTaskEnum taskEnum, DTOProfiles profile, LocalDateTime now) {
        // Calculate next execution time based on task type and profile configuration
        // Using the same logic as the actual task implementations
        switch (taskEnum) {
            case HERO_RECRUITMENT:
            case NOMADIC_MERCHANT:
            case WAR_ACADEMY_SHARDS:
            case CRYSTAL_LABORATORY:
            case VIP_POINTS:
                // Daily reset tasks - next execution at game reset
                return UtilTime.getGameReset();
                
            case ALLIANCE_TECH:
                // Alliance tech based on profile configuration offset
                int allianceTechOffset = profile.getConfig(EnumConfigurationKey.ALLIANCE_TECH_OFFSET_INT, Integer.class);
                return now.plusHours(allianceTechOffset);
                
            case LIFE_ESSENCE:
                // Life essence based on profile configuration offset
                int lifeEssenceOffset = profile.getConfig(EnumConfigurationKey.LIFE_ESSENCE_OFFSET_INT, Integer.class);
                return now.plusHours(lifeEssenceOffset);
                
            case ALLIANCE_CHESTS:
                // Alliance chests based on profile configuration offset
                int allianceChestsOffset = profile.getConfig(EnumConfigurationKey.ALLIANCE_CHESTS_OFFSET_INT, Integer.class);
                return now.plusHours(allianceChestsOffset);
                
            case PET_ADVENTURE:
                // Pet adventure typically resets daily
                return UtilTime.getGameReset();
                
            case EXPLORATION_CHEST:
                // Exploration chest typically every 8 hours
                return now.plusHours(8);
                
            case TRAINING_TROOPS:
                // Training troops - typically runs every few hours based on training time
                return now.plusHours(4);
                
            case ALLIANCE_PET_TREASURE:
                // Alliance pet treasure typically resets daily
                return UtilTime.getGameReset();
                
            case PET_SKILL_FOOD:
            case PET_SKILL_GATHERING:
            case PET_SKILL_STAMINA:
            case PET_SKILL_TREASURE:
                // Pet skills typically every 8 hours
                return now.plusHours(8);
                
            case ALLIANCE_AUTOJOIN:
                // Alliance autojoin typically every 7 hours
                return now.plusHours(7);
                
            case STOREHOUSE_CHEST:
            case STOREHOUSE_STAMINA:
                // Storehouse tasks - next reset (12h intervals)
                return UtilTime.getNextReset();
                
            case MAIL_REWARDS:
                // Mail rewards - daily reset
                return UtilTime.getGameReset();
                
            case GATHER_RESOURCES:
                // Gather resources typically every 5 minutes
                return now.plusMinutes(5);
                
            case GATHER_SPEED:
                // Gather speed boost typically every 8 hours
                return now.plusHours(8);
                
            case INTEL:
                // Intelligence typically every hour
                return now.plusHours(1);
                
            case BANK:
                // Bank typically every 8 hours
                return now.plusHours(8);
                
            case ALLIANCE_HELP:
                // Alliance help typically every 2 hours
                return now.plusHours(2);
                
            case BEAST_SLAY:
                // Beast slay depends on stamina regeneration, typically 2-4 hours
                return now.plusHours(3);
                
            default:
                // Default to 8 hours for unknown tasks
                return now.plusHours(8);
        }
    }
    
    private LocalDateTime getNextGameReset(LocalDateTime now) {
        // Use the existing UtilTime.getGameReset() method
        return UtilTime.getGameReset();
    }
    
    private int getProfileConfigInt(DTOProfiles profile, String configKey, int defaultValue) {
        try {
            // This method is no longer needed since we're using profile.getConfig() directly
            // Keeping it for backward compatibility
            if (profile.getGlobalsettings() != null && profile.getGlobalsettings().containsKey(configKey)) {
                String value = profile.getGlobalsettings().get(configKey);
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            System.err.println("Failed to get config " + configKey + ": " + e.getMessage());
        }
        return defaultValue;
    }
}
