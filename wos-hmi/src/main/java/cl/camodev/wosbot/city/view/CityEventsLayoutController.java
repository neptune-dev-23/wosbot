package cl.camodev.wosbot.city.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class CityEventsLayoutController extends AbstractProfileController {

	@FXML
	private CheckBox checkBoxCrystalLabFC;

	@FXML
	private CheckBox checkBoxDailyVipRewards;

	@FXML
	private CheckBox checkBoxExplorationChest;

	@FXML
	private CheckBox checkBoxHeroRecruitment;

	@FXML
	private CheckBox checkBoxMailRewards;

	@FXML
	private CheckBox checkBoxNomadicMerchant;

	@FXML
	private CheckBox checkBoxNomadicMerchantVip;

	@FXML
	private CheckBox checkBoxStorehouseChest;

	@FXML
	private CheckBox checkBoxWarAcademyShards;

	@FXML
	private CheckBox checkBoxBuyMonthlyVip;

	@FXML
	private TextField textfieldExplorationOffset;

	@FXML
	private TextField textfieldMailOffset;

	@FXML
	private void initialize() {
		checkBoxMappings.put(checkBoxStorehouseChest, EnumConfigurationKey.STOREHOUSE_CHEST_BOOL);
		checkBoxMappings.put(checkBoxCrystalLabFC, EnumConfigurationKey.BOOL_CRYSTAL_LAB_FC);
		checkBoxMappings.put(checkBoxExplorationChest, EnumConfigurationKey.BOOL_EXPLORATION_CHEST);
		checkBoxMappings.put(checkBoxHeroRecruitment, EnumConfigurationKey.BOOL_HERO_RECRUITMENT);
		checkBoxMappings.put(checkBoxNomadicMerchant, EnumConfigurationKey.BOOL_NOMADIC_MERCHANT);
		checkBoxMappings.put(checkBoxNomadicMerchantVip, EnumConfigurationKey.BOOL_NOMADIC_MERCHANT_VIP_POINTS);
		checkBoxMappings.put(checkBoxWarAcademyShards, EnumConfigurationKey.BOOL_WAR_ACADEMY_SHARDS);
		checkBoxMappings.put(checkBoxDailyVipRewards, EnumConfigurationKey.BOOL_VIP_POINTS);
		checkBoxMappings.put(checkBoxMailRewards, EnumConfigurationKey.MAIL_REWARDS_BOOL);
		checkBoxMappings.put(checkBoxBuyMonthlyVip, EnumConfigurationKey.VIP_BUY_MONTHLY);

		textFieldMappings.put(textfieldExplorationOffset, EnumConfigurationKey.INT_EXPLORATION_CHEST_OFFSET);
		textFieldMappings.put(textfieldMailOffset, EnumConfigurationKey.MAIL_REWARDS_OFFSET_INT);

		initializeChangeEvents();
	}
}
