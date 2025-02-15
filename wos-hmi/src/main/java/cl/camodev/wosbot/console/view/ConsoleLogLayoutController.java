package cl.camodev.wosbot.console.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import cl.camodev.wosbot.console.controller.ConsoleLogActionController;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.model.LogMessageAux;
import cl.camodev.wosbot.ot.DTOLogMessage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ConsoleLogLayoutController {

	private ConsoleLogActionController actionController;

	@FXML
	private Button buttonClearLogs;

	@FXML
	private CheckBox checkboxDebug;

	@FXML
	private TableView<LogMessageAux> tableviewLogMessages;

	@FXML
	private TableColumn<LogMessageAux, String> columnMessage;

	@FXML
	private TableColumn<LogMessageAux, String> columnTimeStamp;

	@FXML
	private TableColumn<LogMessageAux, String> columnProfile;

	@FXML
	private TableColumn<LogMessageAux, String> columnTask;

	@FXML
	private TableColumn<LogMessageAux, String> columnLevel;

	private ObservableList<LogMessageAux> logMessages;

	private ScrollBar verticalScrollBar;

//	private boolean autoScrollEnabled = true;

	@FXML
	private void initialize() {
		actionController = new ConsoleLogActionController(this);
		logMessages = FXCollections.observableArrayList();
		columnTimeStamp.setCellValueFactory(cellData -> cellData.getValue().timeStampProperty());
		columnMessage.setCellValueFactory(cellData -> cellData.getValue().messageProperty());
		columnLevel.setCellValueFactory(cellData -> cellData.getValue().severityProperty());
		columnTask.setCellValueFactory(cellData -> cellData.getValue().taskProperty());
		columnProfile.setCellValueFactory(cellData -> cellData.getValue().profileProperty());
		tableviewLogMessages.setItems(logMessages);

		// Botón para agregar datos (simula nuevos mensajes)
		tableviewLogMessages.setPlaceholder(new Label("NO LOGS"));

	}

	@FXML
	void handleButtonClearLogs(ActionEvent event) {

		logMessages.clear();
	}

	public void appendMessage(DTOLogMessage dtoMessage) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		String formattedDate = LocalDateTime.now().format(formatter);

		if (!checkboxDebug.isSelected() && dtoMessage.getSeverity() == EnumTpMessageSeverity.DEBUG) {
			return;
		}

		logMessages.add(0, new LogMessageAux(formattedDate, dtoMessage.getSeverity().toString(), dtoMessage.getMessage(), dtoMessage.getTask(), dtoMessage.getProfile()));

		if (logMessages.size() > 500) {
			logMessages.remove(logMessages.size() - 1);
		}

	}

}
