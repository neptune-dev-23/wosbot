package cl.camodev.wosbot.console.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import cl.camodev.wosbot.console.controller.ConsoleLogActionController;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.model.LogMessageAux;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
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

	private ObservableList<LogMessageAux> logMessages;

	private ScrollBar verticalScrollBar;
	private boolean autoScrollEnabled = true;

	@FXML
	private void initialize() {
		actionController = new ConsoleLogActionController(this);
		logMessages = FXCollections.observableArrayList();
		columnTimeStamp.setCellValueFactory(cellData -> cellData.getValue().timeStampProperty());
		columnMessage.setCellValueFactory(cellData -> cellData.getValue().messageProperty());
		tableviewLogMessages.setItems(logMessages);

		tableviewLogMessages.skinProperty().addListener((obs, oldSkin, newSkin) -> {
			if (newSkin != null) {
				verticalScrollBar = getVerticalScrollBar();
				if (verticalScrollBar != null) {
					verticalScrollBar.valueProperty().addListener((obs2, oldVal, newVal) -> {
						autoScrollEnabled = newVal.doubleValue() >= verticalScrollBar.getMax(); // Solo auto-scroll si
																								// estamos abajo
					});
				}
			}
		});

		// Botón para agregar datos (simula nuevos mensajes)
		tableviewLogMessages.setPlaceholder(new Label("No hay mensajes aún"));

	}

	@FXML
	void handleButtonClearLogs(ActionEvent event) {

		logMessages.clear();
	}

	public void appendMessage(EnumTpMessageSeverity severity, String message) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		String formattedDate = LocalDateTime.now().format(formatter);

		logMessages.add(new LogMessageAux(formattedDate, severity, message));

		if (logMessages.size() > 200) {
			logMessages.remove(0);
		}
		if (autoScrollEnabled && verticalScrollBar != null) {
            verticalScrollBar.setValue(verticalScrollBar.getMax());
        }
	}

	private ScrollBar getVerticalScrollBar() {
		for (Node node : tableviewLogMessages.lookupAll(".scroll-bar")) {
			if (node instanceof ScrollBar) {
				ScrollBar sb = (ScrollBar) node;
				if (sb.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
					return sb;
				}
			}
		}
		return null;
	}

}
