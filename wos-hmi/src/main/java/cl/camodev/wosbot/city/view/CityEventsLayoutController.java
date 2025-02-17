package cl.camodev.wosbot.city.view;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.profile.model.IProfileChangeObserver;
import cl.camodev.wosbot.profile.model.IProfileLoadListener;
import cl.camodev.wosbot.profile.model.IProfileObserverInjectable;
import cl.camodev.wosbot.profile.model.ProfileAux;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class CityEventsLayoutController implements IProfileLoadListener, IProfileObserverInjectable {

	@FXML
	private CheckBox checkBoxCrystalLabFC;

	@FXML
	private CheckBox checkBoxExplorationChest;

	@FXML
	private CheckBox checkBoxHeroRecruitment;

	@FXML
	private CheckBox checkBoxNomadicMerchant;

	@FXML
	private CheckBox checkBoxNomadicMerchantVip;

	@FXML
	private CheckBox checkBoxWarAcademyShards;

	@FXML
	private CheckBox checkBoxDailyVipRewards;

	@FXML
	private TextField textfieldExplorationOffset;

	private IProfileChangeObserver profileObserver;

	private boolean isLoadingProfile = false; // Bandera para controlar eventos durante la carga

	@FXML
	private void initialize() {
		initializeChangeEvents();

	}

	private void initializeChangeEvents() {

		setupTextFieldUpdateOnFocusOrEnter(textfieldExplorationOffset, EnumConfigurationKey.INT_EXPLORATION_CHEST_OFFSET);

		setupCheckBoxListener(checkBoxCrystalLabFC, EnumConfigurationKey.BOOL_CRYSTAL_LAB_FC);
		setupCheckBoxListener(checkBoxExplorationChest, EnumConfigurationKey.BOOL_EXPLORATION_CHEST);
		setupCheckBoxListener(checkBoxHeroRecruitment, EnumConfigurationKey.BOOL_HERO_RECRUITMENT);
		setupCheckBoxListener(checkBoxNomadicMerchant, EnumConfigurationKey.BOOL_NOMADIC_MERCHANT);
		setupCheckBoxListener(checkBoxNomadicMerchantVip, EnumConfigurationKey.BOOL_NOMADIC_MERCHANT_VIP_POINTS);
		setupCheckBoxListener(checkBoxWarAcademyShards, EnumConfigurationKey.BOOL_WAR_ACADEMY_SHARDS);
		setupCheckBoxListener(checkBoxDailyVipRewards, EnumConfigurationKey.BOOL_VIP_POINTS);
	}

	@Override
	public void onProfileLoad(ProfileAux profile) {
		isLoadingProfile = true; // Deshabilita temporalmente los eventos

		try {
			setCheckBoxValue(checkBoxCrystalLabFC, profile, EnumConfigurationKey.BOOL_CRYSTAL_LAB_FC);
			setCheckBoxValue(checkBoxExplorationChest, profile, EnumConfigurationKey.BOOL_EXPLORATION_CHEST);
			setCheckBoxValue(checkBoxHeroRecruitment, profile, EnumConfigurationKey.BOOL_HERO_RECRUITMENT);
			setCheckBoxValue(checkBoxNomadicMerchant, profile, EnumConfigurationKey.BOOL_NOMADIC_MERCHANT);
			setCheckBoxValue(checkBoxNomadicMerchantVip, profile, EnumConfigurationKey.BOOL_NOMADIC_MERCHANT_VIP_POINTS);
			setCheckBoxValue(checkBoxWarAcademyShards, profile, EnumConfigurationKey.BOOL_WAR_ACADEMY_SHARDS);
			setCheckBoxValue(checkBoxDailyVipRewards, profile, EnumConfigurationKey.BOOL_VIP_POINTS);

			setTextFieldValue(textfieldExplorationOffset, profile, EnumConfigurationKey.INT_EXPLORATION_CHEST_OFFSET);

		} finally {
			isLoadingProfile = false; // Reactiva los eventos después de cargar el perfil
		}
	}

	@Override
	public void setProfileObserver(IProfileChangeObserver observer) {
		this.profileObserver = observer;

	}

	private void setupCheckBoxListener(CheckBox checkBox, EnumConfigurationKey configKey) {
		checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (!isLoadingProfile) {
				profileObserver.notifyProfileChange(configKey, newVal);
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

	private void setCheckBoxValue(CheckBox checkBox, ProfileAux profile, EnumConfigurationKey key) {
		Boolean value = profile.getConfig(key, Boolean.class);
		checkBox.setSelected(value != null && value);
	}

	private void setTextFieldValue(TextField textField, ProfileAux profile, EnumConfigurationKey key) {
		Integer value = profile.getConfig(key, Integer.class);
		textField.setText(value != null ? String.valueOf(value) : key.getDefaultValue());
	}

}
