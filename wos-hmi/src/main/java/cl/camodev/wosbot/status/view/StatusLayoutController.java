package cl.camodev.wosbot.status.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private TableView<ProfileTaskRow> tableProfiles;
    
    @FXML
    private TableColumn<ProfileTaskRow, String> colProfileName;
    @FXML
    private TableColumn<ProfileTaskRow, String> colEmulator;
    @FXML
    private TableColumn<ProfileTaskRow, String> colIntel;
    @FXML
    private TableColumn<ProfileTaskRow, String> colGatherRes;
    @FXML
    private TableColumn<ProfileTaskRow, String> colAllianceHelp;
    @FXML
    private TableColumn<ProfileTaskRow, String> colAllianceChests;
    @FXML
    private TableColumn<ProfileTaskRow, String> colCrystalLab;
    @FXML
    private TableColumn<ProfileTaskRow, String> colHeroRecruit;
    @FXML
    private TableColumn<ProfileTaskRow, String> colLifeEssence;
    @FXML
    private TableColumn<ProfileTaskRow, String> colMailRewards;
    @FXML
    private TableColumn<ProfileTaskRow, String> colAllianceAutojoin;
    @FXML
    private TableColumn<ProfileTaskRow, String> colAllianceTech;
    @FXML
    private TableColumn<ProfileTaskRow, String> colBank;
    @FXML
    private TableColumn<ProfileTaskRow, String> colBeastSlay;
    @FXML
    private TableColumn<ProfileTaskRow, String> colExploration;
    @FXML
    private TableColumn<ProfileTaskRow, String> colGatherSpeed;
    @FXML
    private TableColumn<ProfileTaskRow, String> colNomadicMerchant;
    @FXML
    private TableColumn<ProfileTaskRow, String> colOnlineRewards;
    @FXML
    private TableColumn<ProfileTaskRow, String> colPetAdventure;
    @FXML
    private TableColumn<ProfileTaskRow, String> colPetTreasure;
    @FXML
    private TableColumn<ProfileTaskRow, String> colPetSkills;
    @FXML
    private TableColumn<ProfileTaskRow, String> colTrainingTroops;
    @FXML
    private TableColumn<ProfileTaskRow, String> colVip;
    @FXML
    private TableColumn<ProfileTaskRow, String> colWarAcademy;

    private IDailyTaskRepository dailyTaskRepository = DailyTaskRepository.getRepository();
    private Timeline updateTimeline;
    private ObservableList<ProfileTaskRow> profileData = FXCollections.observableArrayList();
    
    @FXML
    private void initialize() {
        setupTable();
        setupAutoRefresh();
        loadTaskStatuses();
    }

    private void setupTable() {
        // Set up table columns
        colEmulator.setCellValueFactory(cellData -> cellData.getValue().emulatorProperty());
        colProfileName.setCellValueFactory(cellData -> cellData.getValue().profileNameProperty());
        colHeroRecruit.setCellValueFactory(cellData -> cellData.getValue().heroRecruitProperty());
        colNomadicMerchant.setCellValueFactory(cellData -> cellData.getValue().nomadicMerchantProperty());
        colWarAcademy.setCellValueFactory(cellData -> cellData.getValue().warAcademyProperty());
        colCrystalLab.setCellValueFactory(cellData -> cellData.getValue().crystalLabProperty());
        colVip.setCellValueFactory(cellData -> cellData.getValue().vipProperty());
        colPetAdventure.setCellValueFactory(cellData -> cellData.getValue().petAdventureProperty());
        colExploration.setCellValueFactory(cellData -> cellData.getValue().explorationProperty());
        colAllianceTech.setCellValueFactory(cellData -> cellData.getValue().allianceTechProperty());
        colLifeEssence.setCellValueFactory(cellData -> cellData.getValue().lifeEssenceProperty());
        colPetTreasure.setCellValueFactory(cellData -> cellData.getValue().petTreasureProperty());
        colAllianceChests.setCellValueFactory(cellData -> cellData.getValue().allianceChestsProperty());
        colTrainingTroops.setCellValueFactory(cellData -> cellData.getValue().trainingTroopsProperty());
        colGatherRes.setCellValueFactory(cellData -> cellData.getValue().gatherResProperty());
        colBank.setCellValueFactory(cellData -> cellData.getValue().bankProperty());
        colBeastSlay.setCellValueFactory(cellData -> cellData.getValue().beastSlayProperty());
        colPetSkills.setCellValueFactory(cellData -> cellData.getValue().petSkillsProperty());
        colGatherSpeed.setCellValueFactory(cellData -> cellData.getValue().gatherSpeedProperty());
        colMailRewards.setCellValueFactory(cellData -> cellData.getValue().mailRewardsProperty());
        colOnlineRewards.setCellValueFactory(cellData -> cellData.getValue().onlineRewardsProperty());
        colIntel.setCellValueFactory(cellData -> cellData.getValue().intelProperty());
        colAllianceAutojoin.setCellValueFactory(cellData -> cellData.getValue().allianceAutojoinProperty());
        colAllianceHelp.setCellValueFactory(cellData -> cellData.getValue().allianceHelpProperty());

        // Set up custom cell factories for styling
        setupProfileColumns();
        setupTaskColumns();

        // Set table data
        tableProfiles.setItems(profileData);
        
        // Style the table rows to show active/inactive profiles
        tableProfiles.setRowFactory(tv -> {
            javafx.scene.control.TableRow<ProfileTaskRow> row = new javafx.scene.control.TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    if (newItem.getStatus().equals("ACTIVE")) {
                        row.setStyle("-fx-background-color: #2a2a3d; -fx-text-fill: white;");
                    } else {
                        row.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: #888; -fx-opacity: 0.6;");
                    }
                }
            });
            return row;
        });
    }

    private void setupProfileColumns() {
        // Emulator column styling (first column now)
        colEmulator.setCellFactory(column -> new TableCell<ProfileTaskRow, String>() {
            @Override
            protected void updateItem(String emulator, boolean empty) {
                super.updateItem(emulator, empty);
                if (empty || emulator == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(emulator);
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                }
            }
        });

        // Profile name column styling
        colProfileName.setCellFactory(column -> new TableCell<ProfileTaskRow, String>() {
            @Override
            protected void updateItem(String profileName, boolean empty) {
                super.updateItem(profileName, empty);
                if (empty || profileName == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(profileName);
                    setAlignment(Pos.CENTER_LEFT);
                    if (profileName.contains("(INACTIVE)")) {
                        setStyle("-fx-text-fill: #888; -fx-font-size: 12px; -fx-font-weight: normal;");
                    } else {
                        setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupTaskColumns() {
        TableColumn<ProfileTaskRow, String>[] taskColumns = new TableColumn[]{
            colHeroRecruit, colNomadicMerchant, colWarAcademy, colCrystalLab,
            colVip, colPetAdventure, colExploration, colAllianceTech,
            colLifeEssence, colPetTreasure, colAllianceChests, colTrainingTroops,
            colGatherRes, colBank, colBeastSlay, colPetSkills,
            colGatherSpeed, colMailRewards, colOnlineRewards, colIntel,
            colAllianceAutojoin, colAllianceHelp
        };

        for (TableColumn<ProfileTaskRow, String> column : taskColumns) {
            column.setCellFactory(col -> new TableCell<ProfileTaskRow, String>() {
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
                        
                        if (taskInfo.equals("Ready")) {
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
                                    if (timeValue <= 30) {
                                        style += "-fx-text-fill: #4CAF50;"; // Green - very soon (≤30m)
                                    } else if (timeValue <= 120) {
                                        style += "-fx-text-fill: #ffc107;"; // Orange - soon (≤2h)
                                    } else {
                                        style += "-fx-text-fill: #ff5722;"; // Red - far (>2h in minutes)
                                    }
                                } else if (taskInfo.contains("h")) {
                                    // Hours
                                    if (timeValue <= 2) {
                                        style += "-fx-text-fill: #ffc107;"; // Orange - soon (≤2h)
                                    } else if (timeValue <= 6) {
                                        style += "-fx-text-fill: #ff9800;"; // Red-orange - moderate (≤6h)
                                    } else {
                                        style += "-fx-text-fill: #ff5722;"; // Red - far (>6h)
                                    }
                                } else if (taskInfo.contains("d")) {
                                    // Days
                                    style += "-fx-text-fill: #ff5722;"; // Red - very far (days)
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
        }
    }

    private void setupAutoRefresh() {
        // Update every 5 seconds
        updateTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            Platform.runLater(this::loadTaskStatuses);
        }));
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
        updateTimeline.play();
    }

    private void loadTaskStatuses() {
        Platform.runLater(() -> {
            profileData.clear();
            
            List<DTOProfiles> profiles = ServProfiles.getServices().getProfiles();
            if (profiles == null || profiles.isEmpty()) {
                return;
            }

            int activeProfiles = 0;
            for (DTOProfiles profile : profiles) {
                if (profile.getEnabled()) {
                    activeProfiles++;
                }
                
                ProfileTaskRow row = createProfileTaskRow(profile);
                profileData.add(row);
            }
            
            updateTaskOverview(profiles);
        });
    }

    private ProfileTaskRow createProfileTaskRow(DTOProfiles profile) {
        Map<Integer, DTODailyTaskStatus> taskStatuses = dailyTaskRepository.findDailyTasksStatusByProfile(profile.getId());
        
        ProfileTaskRow row = new ProfileTaskRow();
        row.setProfileName(profile.getName() + (profile.getEnabled() ? "" : " (INACTIVE)"));
        row.setEmulator("#" + profile.getEmulatorNumber());
        row.setStatus(profile.getEnabled() ? "ACTIVE" : "INACTIVE"); // Keep for row styling
        
        // Set task statuses
        row.setIntel(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.INTEL.getId())));
        row.setGatherRes(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.GATHER_RESOURCES.getId())));
        row.setAllianceHelp(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.ALLIANCE_HELP.getId())));
        row.setAllianceChests(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.ALLIANCE_CHESTS.getId())));
        row.setCrystalLab(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.CRYSTAL_LABORATORY.getId())));
        row.setHeroRecruit(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.HERO_RECRUITMENT.getId())));
        row.setLifeEssence(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.LIFE_ESSENCE.getId())));
        row.setMailRewards(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.MAIL_REWARDS.getId())));
        
        // Additional task columns
        row.setAllianceAutojoin(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.ALLIANCE_AUTOJOIN.getId())));
        row.setAllianceTech(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.ALLIANCE_TECH.getId())));
        row.setBank(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.BANK.getId())));
        row.setBeastSlay(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.BEAST_SLAY.getId())));
        row.setExploration(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.EXPLORATION_CHEST.getId())));
        row.setGatherSpeed(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.GATHER_SPEED.getId())));
        row.setNomadicMerchant(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.NOMADIC_MERCHANT.getId())));
        row.setOnlineRewards(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.DAILY_TASKS.getId())));
        row.setPetAdventure(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.PET_ADVENTURE.getId())));
        row.setPetTreasure(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.ALLIANCE_PET_TREASURE.getId())));
        row.setPetSkills(formatCombinedPetSkillsStatus(taskStatuses));
        row.setTrainingTroops(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.TRAINING_TROOPS.getId())));
        row.setVip(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.VIP_POINTS.getId())));
        row.setWarAcademy(formatTaskStatus(taskStatuses.get(TpDailyTaskEnum.WAR_ACADEMY_SHARDS.getId())));
        
        return row;
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
    public static class ProfileTaskRow {
        private final SimpleStringProperty profileName = new SimpleStringProperty();
        private final SimpleStringProperty emulator = new SimpleStringProperty();
        private final SimpleStringProperty status = new SimpleStringProperty();
        private final SimpleStringProperty intel = new SimpleStringProperty();
        private final SimpleStringProperty gatherRes = new SimpleStringProperty();
        private final SimpleStringProperty allianceHelp = new SimpleStringProperty();
        private final SimpleStringProperty allianceChests = new SimpleStringProperty();
        private final SimpleStringProperty crystalLab = new SimpleStringProperty();
        private final SimpleStringProperty heroRecruit = new SimpleStringProperty();
        private final SimpleStringProperty lifeEssence = new SimpleStringProperty();
        private final SimpleStringProperty mailRewards = new SimpleStringProperty();
        private final SimpleStringProperty allianceAutojoin = new SimpleStringProperty();
        private final SimpleStringProperty allianceTech = new SimpleStringProperty();
        private final SimpleStringProperty bank = new SimpleStringProperty();
        private final SimpleStringProperty beastSlay = new SimpleStringProperty();
        private final SimpleStringProperty exploration = new SimpleStringProperty();
        private final SimpleStringProperty gatherSpeed = new SimpleStringProperty();
        private final SimpleStringProperty nomadicMerchant = new SimpleStringProperty();
        private final SimpleStringProperty onlineRewards = new SimpleStringProperty();
        private final SimpleStringProperty petAdventure = new SimpleStringProperty();
        private final SimpleStringProperty petTreasure = new SimpleStringProperty();
        private final SimpleStringProperty petSkills = new SimpleStringProperty();
        private final SimpleStringProperty trainingTroops = new SimpleStringProperty();
        private final SimpleStringProperty vip = new SimpleStringProperty();
        private final SimpleStringProperty warAcademy = new SimpleStringProperty();

        // Getters and setters
        public String getProfileName() { return profileName.get(); }
        public void setProfileName(String value) { profileName.set(value); }
        public SimpleStringProperty profileNameProperty() { return profileName; }

        public String getEmulator() { return emulator.get(); }
        public void setEmulator(String value) { emulator.set(value); }
        public SimpleStringProperty emulatorProperty() { return emulator; }

        public String getStatus() { return status.get(); }
        public void setStatus(String value) { status.set(value); }
        public SimpleStringProperty statusProperty() { return status; }

        public String getIntel() { return intel.get(); }
        public void setIntel(String value) { intel.set(value); }
        public SimpleStringProperty intelProperty() { return intel; }

        public String getGatherRes() { return gatherRes.get(); }
        public void setGatherRes(String value) { gatherRes.set(value); }
        public SimpleStringProperty gatherResProperty() { return gatherRes; }

        public String getAllianceHelp() { return allianceHelp.get(); }
        public void setAllianceHelp(String value) { allianceHelp.set(value); }
        public SimpleStringProperty allianceHelpProperty() { return allianceHelp; }

        public String getAllianceChests() { return allianceChests.get(); }
        public void setAllianceChests(String value) { allianceChests.set(value); }
        public SimpleStringProperty allianceChestsProperty() { return allianceChests; }

        public String getCrystalLab() { return crystalLab.get(); }
        public void setCrystalLab(String value) { crystalLab.set(value); }
        public SimpleStringProperty crystalLabProperty() { return crystalLab; }

        public String getHeroRecruit() { return heroRecruit.get(); }
        public void setHeroRecruit(String value) { heroRecruit.set(value); }
        public SimpleStringProperty heroRecruitProperty() { return heroRecruit; }

        public String getLifeEssence() { return lifeEssence.get(); }
        public void setLifeEssence(String value) { lifeEssence.set(value); }
        public SimpleStringProperty lifeEssenceProperty() { return lifeEssence; }

        public String getMailRewards() { return mailRewards.get(); }
        public void setMailRewards(String value) { mailRewards.set(value); }
        public SimpleStringProperty mailRewardsProperty() { return mailRewards; }

        public String getAllianceAutojoin() { return allianceAutojoin.get(); }
        public void setAllianceAutojoin(String value) { allianceAutojoin.set(value); }
        public SimpleStringProperty allianceAutojoinProperty() { return allianceAutojoin; }

        public String getAllianceTech() { return allianceTech.get(); }
        public void setAllianceTech(String value) { allianceTech.set(value); }
        public SimpleStringProperty allianceTechProperty() { return allianceTech; }

        public String getBank() { return bank.get(); }
        public void setBank(String value) { bank.set(value); }
        public SimpleStringProperty bankProperty() { return bank; }

        public String getBeastSlay() { return beastSlay.get(); }
        public void setBeastSlay(String value) { beastSlay.set(value); }
        public SimpleStringProperty beastSlayProperty() { return beastSlay; }

        public String getExploration() { return exploration.get(); }
        public void setExploration(String value) { exploration.set(value); }
        public SimpleStringProperty explorationProperty() { return exploration; }

        public String getGatherSpeed() { return gatherSpeed.get(); }
        public void setGatherSpeed(String value) { gatherSpeed.set(value); }
        public SimpleStringProperty gatherSpeedProperty() { return gatherSpeed; }

        public String getNomadicMerchant() { return nomadicMerchant.get(); }
        public void setNomadicMerchant(String value) { nomadicMerchant.set(value); }
        public SimpleStringProperty nomadicMerchantProperty() { return nomadicMerchant; }

        public String getOnlineRewards() { return onlineRewards.get(); }
        public void setOnlineRewards(String value) { onlineRewards.set(value); }
        public SimpleStringProperty onlineRewardsProperty() { return onlineRewards; }

        public String getPetAdventure() { return petAdventure.get(); }
        public void setPetAdventure(String value) { petAdventure.set(value); }
        public SimpleStringProperty petAdventureProperty() { return petAdventure; }

        public String getPetTreasure() { return petTreasure.get(); }
        public void setPetTreasure(String value) { petTreasure.set(value); }
        public SimpleStringProperty petTreasureProperty() { return petTreasure; }

        public String getPetSkills() { return petSkills.get(); }
        public void setPetSkills(String value) { petSkills.set(value); }
        public SimpleStringProperty petSkillsProperty() { return petSkills; }

        public String getTrainingTroops() { return trainingTroops.get(); }
        public void setTrainingTroops(String value) { trainingTroops.set(value); }
        public SimpleStringProperty trainingTroopsProperty() { return trainingTroops; }

        public String getVip() { return vip.get(); }
        public void setVip(String value) { vip.set(value); }
        public SimpleStringProperty vipProperty() { return vip; }

        public String getWarAcademy() { return warAcademy.get(); }
        public void setWarAcademy(String value) { warAcademy.set(value); }
        public SimpleStringProperty warAcademyProperty() { return warAcademy; }
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
