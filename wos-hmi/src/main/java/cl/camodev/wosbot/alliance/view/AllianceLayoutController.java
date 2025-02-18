package cl.camodev.wosbot.alliance.view;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.profile.model.IProfileChangeObserver;
import cl.camodev.wosbot.profile.model.IProfileLoadListener;
import cl.camodev.wosbot.profile.model.IProfileObserverInjectable;
import cl.camodev.wosbot.profile.model.ProfileAux;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class AllianceLayoutController implements IProfileLoadListener, IProfileObserverInjectable {

	@FXML
	private CheckBox checkBoxAutojoin;

	@FXML
	private CheckBox checkBoxChests;

	@FXML
	private CheckBox checkBoxTechContribution;

	@FXML
	private TextField textfieldAutojoinQueues;

	@FXML
	private TextField textfieldChestOffset;

	@FXML
	private TextField textfieldTechOffset;

	private IProfileChangeObserver profileObserver;

	private boolean isLoadingProfile = false;

	@FXML
	private void initialize() {
		initializeChangeEvents();
		checkBoxAutojoin.setDisable(true);
		checkBoxChests.setDisable(true);
		textfieldAutojoinQueues.setDisable(true);
		textfieldChestOffset.setDisable(true);

	}

	private void initializeChangeEvents() {
		setupCheckBoxListener(checkBoxAutojoin, EnumConfigurationKey.BOOL_ALLIANCE_AUTOJOIN);
		setupCheckBoxListener(checkBoxChests, EnumConfigurationKey.BOOL_ALLIANCE_CHESTS);
		setupCheckBoxListener(checkBoxTechContribution, EnumConfigurationKey.BOOL_ALLIANCE_TECH);
		setupTextFieldUpdateOnFocusOrEnter(textfieldAutojoinQueues, EnumConfigurationKey.INT_ALLIANCE_AUTOJOIN_QUEUES);
		setupTextFieldUpdateOnFocusOrEnter(textfieldChestOffset, EnumConfigurationKey.INT_ALLIANCE_CHESTS_OFFSET);
		setupTextFieldUpdateOnFocusOrEnter(textfieldTechOffset, EnumConfigurationKey.INT_ALLIANCE_TECH_OFFSET);
	}

	@Override
	public void setProfileObserver(IProfileChangeObserver observer) {
		this.profileObserver = observer;

	}

	private void setCheckBoxValue(CheckBox checkBox, ProfileAux profile, EnumConfigurationKey key) {
		Boolean value = profile.getConfig(key, Boolean.class);
		checkBox.setSelected(value != null && value);
	}

	private void setTextFieldValue(TextField textField, ProfileAux profile, EnumConfigurationKey key) {
		Integer value = profile.getConfig(key, Integer.class);
		textField.setText(value != null ? String.valueOf(value) : key.getDefaultValue());
	}

	@Override
	public void onProfileLoad(ProfileAux profile) {
		isLoadingProfile = true;

		try {
			setCheckBoxValue(checkBoxTechContribution, profile, EnumConfigurationKey.BOOL_ALLIANCE_TECH);
			setCheckBoxValue(checkBoxChests, profile, EnumConfigurationKey.BOOL_ALLIANCE_CHESTS);
			setCheckBoxValue(checkBoxAutojoin, profile, EnumConfigurationKey.BOOL_ALLIANCE_AUTOJOIN);

			setTextFieldValue(textfieldAutojoinQueues, profile, EnumConfigurationKey.INT_ALLIANCE_AUTOJOIN_QUEUES);
			setTextFieldValue(textfieldChestOffset, profile, EnumConfigurationKey.INT_ALLIANCE_CHESTS_OFFSET);
			setTextFieldValue(textfieldTechOffset, profile, EnumConfigurationKey.INT_ALLIANCE_TECH_OFFSET);
		} finally {
			isLoadingProfile = false;
		}
	}

	private void setupCheckBoxListener(CheckBox checkBox, EnumConfigurationKey configKey) {
		checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (!isLoadingProfile) {
				profileObserver.notifyProfileChange(configKey, newVal);
			}
		});
	}

	private void setupTextFieldUpdateOnFocusOrEnter(TextField textField, EnumConfigurationKey configKey) {
		textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
			if (!isNowFocused && !isLoadingProfile) {
				updateProfile(textField, configKey);
			}
		});

		textField.setOnAction(event -> {
			if (!isLoadingProfile) {
				updateProfile(textField, configKey);
			}
		});
	}

	private void updateProfile(TextField textField, EnumConfigurationKey configKey) {
		String newVal = textField.getText();
		if (isValidPositiveInteger(newVal)) {
			profileObserver.notifyProfileChange(configKey, Integer.valueOf(newVal));
		} else {
			textField.setText(configKey.getDefaultValue());
		}
	}

	private boolean isValidPositiveInteger(String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}
		try {
			int number = Integer.parseInt(value);
			return number >= 0 && number <= 24;
		} catch (NumberFormatException e) {
			return false;
		}
	}

}
