package cl.camodev.wosbot.city.view;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.profile.model.IProfileChangeObserver;
import cl.camodev.wosbot.profile.model.IProfileLoadListener;
import cl.camodev.wosbot.profile.model.IProfileObserverInjectable;
import cl.camodev.wosbot.profile.model.ProfileAux;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

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

	private IProfileChangeObserver profileObserver;

	private boolean isLoadingProfile = false; // Bandera para controlar eventos durante la carga

	@FXML
	private void initialize() {
		initializeChangeEvents();

	}

	private void initializeChangeEvents() {
		checkBoxCrystalLabFC.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (!isLoadingProfile) {
				profileObserver.notifyProfileChange(EnumConfigurationKey.BOOL_CRYSTAL_LAB_FC, newVal);
			}
		});

		checkBoxExplorationChest.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (!isLoadingProfile) {
				profileObserver.notifyProfileChange(EnumConfigurationKey.BOOL_EXPLORATION_CHEST, newVal);
			}
		});

		checkBoxHeroRecruitment.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (!isLoadingProfile) {
				profileObserver.notifyProfileChange(EnumConfigurationKey.BOOL_HERO_RECRUITMENT, newVal);
			}
		});

		checkBoxNomadicMerchant.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (!isLoadingProfile) {
				profileObserver.notifyProfileChange(EnumConfigurationKey.BOOL_NOMADIC_MERCHANT, newVal);
			}
		});

		checkBoxNomadicMerchantVip.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (!isLoadingProfile) {
				profileObserver.notifyProfileChange(EnumConfigurationKey.BOOL_NOMADIC_MERCHANT_VIP_POINTS, newVal);
			}
		});

		checkBoxWarAcademyShards.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (!isLoadingProfile) {
				profileObserver.notifyProfileChange(EnumConfigurationKey.BOOL_WAR_ACADEMY_SHARDS, newVal);
			}
		});
	}

	@Override
	public void onProfileLoad(ProfileAux profile) {
		isLoadingProfile = true; // Deshabilita temporalmente los eventos

		try {
			checkBoxCrystalLabFC.setSelected(profile.getConfig(EnumConfigurationKey.BOOL_CRYSTAL_LAB_FC, Boolean.class));
			checkBoxExplorationChest.setSelected(profile.getConfig(EnumConfigurationKey.BOOL_EXPLORATION_CHEST, Boolean.class));
			checkBoxHeroRecruitment.setSelected(profile.getConfig(EnumConfigurationKey.BOOL_HERO_RECRUITMENT, Boolean.class));
			checkBoxNomadicMerchant.setSelected(profile.getConfig(EnumConfigurationKey.BOOL_NOMADIC_MERCHANT, Boolean.class));
			checkBoxNomadicMerchantVip.setSelected(profile.getConfig(EnumConfigurationKey.BOOL_NOMADIC_MERCHANT_VIP_POINTS, Boolean.class));
			checkBoxWarAcademyShards.setSelected(profile.getConfig(EnumConfigurationKey.BOOL_WAR_ACADEMY_SHARDS, Boolean.class));
		} finally {
			isLoadingProfile = false; // Reactiva los eventos después de cargar el perfil
		}
	}

	@Override
	public void setProfileObserver(IProfileChangeObserver observer) {
		this.profileObserver = observer;

	}

}
