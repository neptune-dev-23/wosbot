package cl.camodev.wosbot.city.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class CityEventsLayoutController extends AbstractProfileController {

	@FXML
	private CheckBox checkBoxCrystalLabFC, checkBoxExplorationChest, checkBoxHeroRecruitment, checkBoxNomadicMerchant, checkBoxNomadicMerchantVip, checkBoxWarAcademyShards, checkBoxDailyVipRewards;

	@FXML
	private TextField textfieldExplorationOffset;

	@FXML
	private void initialize() {
		checkBoxMappings.put(checkBoxCrystalLabFC, EnumConfigurationKey.BOOL_CRYSTAL_LAB_FC);
		checkBoxMappings.put(checkBoxExplorationChest, EnumConfigurationKey.BOOL_EXPLORATION_CHEST);
		checkBoxMappings.put(checkBoxHeroRecruitment, EnumConfigurationKey.BOOL_HERO_RECRUITMENT);
		checkBoxMappings.put(checkBoxNomadicMerchant, EnumConfigurationKey.BOOL_NOMADIC_MERCHANT);
		checkBoxMappings.put(checkBoxNomadicMerchantVip, EnumConfigurationKey.BOOL_NOMADIC_MERCHANT_VIP_POINTS);
		checkBoxMappings.put(checkBoxWarAcademyShards, EnumConfigurationKey.BOOL_WAR_ACADEMY_SHARDS);
		checkBoxMappings.put(checkBoxDailyVipRewards, EnumConfigurationKey.BOOL_VIP_POINTS);

		textFieldMappings.put(textfieldExplorationOffset, EnumConfigurationKey.INT_EXPLORATION_CHEST_OFFSET);

		initializeChangeEvents();
	}
}
