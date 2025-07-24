package cl.camodev.wosbot.profile.view;

import java.util.function.UnaryOperator;

import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.profile.controller.ProfileManagerActionController;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.IntegerStringConverter;

public class NewProfileLayoutController {

	private ProfileManagerActionController profileManagerActionController;

	@FXML
	private Button buttonSaveProfile;

	@FXML
	private TextField textfieldEmulatorNumber;

	@FXML
	private TextField textfieldProfileName;

	@FXML
	private CheckBox checkboxEnabled;

	@FXML
	private Slider sliderPriority;

	@FXML
	private Label labelPriorityValue;

	public NewProfileLayoutController(ProfileManagerActionController profileManagerActionController) {
		this.profileManagerActionController = profileManagerActionController;
	}

	@FXML
	private void initialize() {

		UnaryOperator<TextFormatter.Change> filter = change -> {
			String newText = change.getControlNewText();
			if (newText.matches("\\d*")) {
				return change;
			}
			return null;
		};

		TextFormatter<Integer> formatter = new TextFormatter<>(new IntegerStringConverter(), 0, filter);
		textfieldEmulatorNumber.setTextFormatter(formatter);

		labelPriorityValue.setText(String.valueOf((int) sliderPriority.getValue()));

		sliderPriority.valueProperty().addListener((observable, oldValue, newValue) -> {
		int priorityValue = newValue.intValue();
		labelPriorityValue.setText(String.valueOf(priorityValue));
		});


		ChangeListener<String> fieldListener = (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
			buttonSaveProfile.setDisable(!validateFields());
		};

		textfieldEmulatorNumber.textProperty().addListener(fieldListener);
		textfieldProfileName.textProperty().addListener(fieldListener);


		buttonSaveProfile.setDisable(true);
	}

	@FXML
	private void handleSaveProfileButton(ActionEvent event) {
		long priority = (long) sliderPriority.getValue();
		boolean enabled = checkboxEnabled.isSelected();
		profileManagerActionController.addProfile(new DTOProfiles(-1L, textfieldProfileName.getText(), textfieldEmulatorNumber.getText(), enabled, priority));
		profileManagerActionController.closeNewProfileDialog();
	}

	private boolean validateFields() {
		String emulatorText = textfieldEmulatorNumber.getText();
		String profileText = textfieldProfileName.getText();

		if (emulatorText.isEmpty() || profileText.isEmpty()) {
			return false;
		}

		try {
			int emulatorNumber = Integer.parseInt(emulatorText);
			return emulatorNumber >= 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
