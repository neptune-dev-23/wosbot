package cl.camodev.wosbot.taskmanager.view;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import cl.camodev.wosbot.taskmanager.model.TaskManagerAux;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ScheduleTaskDialogController {

    @FXML
    private Label lblTaskName;

    @FXML
    private CheckBox cbImmediate;

    @FXML
    private TextField timeField;

    @FXML
    private CheckBox cbRecurring;

    @FXML
    private Label lblInfo;

    private TaskManagerAux task;
    private boolean confirmed = false;
    private LocalDateTime scheduledTime;
    private boolean immediate;
    private boolean recurring;

    public void initialize() {
        // Set up listeners
        cbImmediate.selectedProperty().addListener((obs, oldVal, newVal) -> {
            timeField.setDisable(newVal);
            if (newVal) {
                timeField.clear();
            }
        });
    }

    public void setTask(TaskManagerAux task) {
        this.task = task;

        // Set task name in header
        lblTaskName.setText("Schedule: " + task.getTaskEnum().getName());

        // Default settings: Always start with "Execute immediately" selected
        cbImmediate.setSelected(true);
        cbRecurring.setSelected(false);
        timeField.setDisable(true);
        timeField.clear();

        // Check if task is already scheduled and show info, but don't change defaults
        boolean isTaskScheduled = task.scheduledProperty().get();
        if (isTaskScheduled) {
            // Show info message but keep execute immediately as default
            lblInfo.setText("Note: Task is currently scheduled. You can modify the schedule or execute immediately.");
            lblInfo.setVisible(true);

            // Pre-fill time field for convenience (but keep it disabled since immediate is selected)
            if (task.getNextExecution() != null) {
                String currentTime = task.getNextExecution().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                timeField.setText(currentTime);
            }
        } else {
            lblInfo.setVisible(false);
        }
    }

    @FXML
    private void handleSchedule() {
        immediate = cbImmediate.isSelected();
        recurring = cbRecurring.isSelected();

        if (!immediate) {
            String timeInput = timeField.getText().trim();
            if (timeInput.isEmpty()) {
                showError("Please enter a valid time in HH:MM:SS format");
                return;
            }

            try {
                LocalTime time = LocalTime.parse(timeInput, DateTimeFormatter.ofPattern("HH:mm:ss"));
                scheduledTime = LocalDateTime.now().with(time);

                // If the time is in the past for today, schedule for tomorrow
                if (scheduledTime.isBefore(LocalDateTime.now())) {
                    scheduledTime = scheduledTime.plusDays(1);
                }
            } catch (DateTimeParseException e) {
                showError("Invalid time format. Please use HH:MM:SS format (e.g., 14:30:00)");
                return;
            }
        }

        confirmed = true;
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        closeDialog();
    }

    private void showError(String message) {
        lblInfo.setText("Error: " + message);
        lblInfo.setStyle("-fx-text-fill: #ff5722;");
        lblInfo.setVisible(true);
    }

    private void closeDialog() {
        Stage stage = (Stage) lblTaskName.getScene().getWindow();
        stage.close();
    }

    // Getters for the results
    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isImmediate() {
        return immediate;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }
}
