package cl.camodev.wosbot.alliance.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class AllianceLayoutController extends AbstractProfileController {

	@FXML
	private CheckBox checkBoxAutojoin, checkBoxChests, checkBoxTechContribution, checkBoxHelpRequests;

	@FXML
	private TextField textfieldAutojoinQueues, textfieldChestOffset, textfieldTechOffset, textfieldHelpRequstOffset;

	@FXML
	private void initialize() {
		checkBoxMappings.put(checkBoxAutojoin, EnumConfigurationKey.ALLIANCE_AUTOJOIN_BOOL);
		checkBoxMappings.put(checkBoxChests, EnumConfigurationKey.ALLIANCE_CHESTS_BOOL);
		checkBoxMappings.put(checkBoxTechContribution, EnumConfigurationKey.ALLIANCE_TECH_BOOL);
		checkBoxMappings.put(checkBoxHelpRequests, EnumConfigurationKey.ALLIANCE_HELP_REQUESTS_BOOL);

		textFieldMappings.put(textfieldAutojoinQueues, EnumConfigurationKey.ALLIANCE_AUTOJOIN_QUEUES_INT);
		textFieldMappings.put(textfieldChestOffset, EnumConfigurationKey.ALLIANCE_CHESTS_OFFSET_INT);
		textFieldMappings.put(textfieldTechOffset, EnumConfigurationKey.ALLIANCE_TECH_OFFSET_INT);
		textFieldMappings.put(textfieldHelpRequstOffset, EnumConfigurationKey.ALLIANCE_HELP_REQUESTS_OFFSET_INT);

		initializeChangeEvents();
	}
}
