package cl.camodev.wosbot.alliance.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class AllianceLayoutController extends AbstractProfileController {

	@FXML
	private CheckBox checkBoxAutojoin, checkBoxChests, checkBoxTechContribution;

	@FXML
	private TextField textfieldAutojoinQueues, textfieldChestOffset, textfieldTechOffset;

	@FXML
	private void initialize() {
		checkBoxMappings.put(checkBoxAutojoin, EnumConfigurationKey.BOOL_ALLIANCE_AUTOJOIN);
		checkBoxMappings.put(checkBoxChests, EnumConfigurationKey.BOOL_ALLIANCE_CHESTS);
		checkBoxMappings.put(checkBoxTechContribution, EnumConfigurationKey.BOOL_ALLIANCE_TECH);

		textFieldMappings.put(textfieldAutojoinQueues, EnumConfigurationKey.INT_ALLIANCE_AUTOJOIN_QUEUES);
		textFieldMappings.put(textfieldChestOffset, EnumConfigurationKey.INT_ALLIANCE_CHESTS_OFFSET);
		textFieldMappings.put(textfieldTechOffset, EnumConfigurationKey.INT_ALLIANCE_TECH_OFFSET);

		initializeChangeEvents();
		checkBoxAutojoin.setDisable(true);
		textfieldAutojoinQueues.setDisable(true);
	}
}
