package cl.camodev.wosbot.emulator.view;

import java.io.File;
import java.util.HashMap;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.emulator.EmulatorType;
import cl.camodev.wosbot.serv.impl.ServConfig;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;

public class EmuConfigLayoutController {

	@FXML
	private TextField muMuPathField;

	@FXML
	private TextField ldPlayerPathField;

	@FXML
	private RadioButton muMuRadio;

	@FXML
	private RadioButton ldPlayerRadio;

	private final ToggleGroup emulatorToggleGroup = new ToggleGroup();

	private final FileChooser fileChooser = new FileChooser();

	public void initialize() {
		muMuRadio.setToggleGroup(emulatorToggleGroup);
		ldPlayerRadio.setToggleGroup(emulatorToggleGroup);

		HashMap<String, String> globalConfig = ServConfig.getServices().getGlobalConfig();
		String muMuPath = globalConfig.get(EnumConfigurationKey.MUMU_PATH_STRING.name());
		String ldPlayerPath = globalConfig.get(EnumConfigurationKey.LDPLAYER_PATH_STRING.name());
		String currentEmulator = globalConfig.get(EnumConfigurationKey.CURRENT_EMULATOR_STRING.name());

		if (EmulatorType.MUMU.name().equals(currentEmulator)) {
			muMuRadio.setSelected(true);
		} else {
			ldPlayerRadio.setSelected(true);
		}

		muMuPathField.setText(muMuPath);
		ldPlayerPathField.setText(ldPlayerPath);
	}

	@FXML
	private void handleSelectMuMuPath() {
		File selectedFile = openFileChooser("Select MuMuManager.exe");
		if (selectedFile != null && selectedFile.getName().equals("MuMuManager.exe")) {
			muMuPathField.setText(selectedFile.getParent());
		} else {
			showError("Invalid file. Please select MuMuManager.exe");
		}
	}

	@FXML
	private void handleSelectLDPlayerPath() {
		File selectedFile = openFileChooser("Select ldconsole.exe");
		if (selectedFile != null && selectedFile.getName().equals("ldconsole.exe")) {
			ldPlayerPathField.setText(selectedFile.getParent());
		} else {
			showError("Invalid file. Please select ldconsole.exe");
		}
	}

	@FXML
	private void handleSaveConfiguration() {
		String muMuPath = muMuPathField.getText();
		String ldPlayerPath = ldPlayerPathField.getText();
		String activeEmulator;

		if (muMuPath.isEmpty() && ldPlayerPath.isEmpty()) {
			showError("Please select at least one valid emulator path before saving.");
			return;
		}

		if (muMuRadio.isSelected() && muMuPath.isEmpty()) {
			showError("MuMuPlayer is selected, but its path is empty. Please provide a valid path.");
			return;
		}

		if (!muMuRadio.isSelected() && ldPlayerPath.isEmpty()) {
			showError("LDPlayer is selected, but its path is empty. Please provide a valid path.");
			return;
		}

		if (muMuRadio.isSelected()) {
			activeEmulator = EmulatorType.MUMU.name();
			ServScheduler.getServices().saveEmulatorPath(EnumConfigurationKey.MUMU_PATH_STRING, muMuPath);
		} else {
			activeEmulator = EmulatorType.LDPLAYER.name();
			ServScheduler.getServices().saveEmulatorPath(EnumConfigurationKey.LDPLAYER_PATH_STRING, ldPlayerPath);
		}

		ServScheduler.getServices().saveEmulatorPath(EnumConfigurationKey.CURRENT_EMULATOR_STRING, activeEmulator);

		showInfo("Configuration saved successfully!");

	}

	private File openFileChooser(String title) {
		fileChooser.setTitle(title);
		fileChooser.getExtensionFilters().clear();
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Executable Files", "*.exe"));
		return fileChooser.showOpenDialog(null);
	}

	private void showError(String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	private void showInfo(String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Success");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
}