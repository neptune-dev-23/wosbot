package cl.camodev.wosbot.gather.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

public class GatherLayoutController extends AbstractProfileController {

	@FXML
	private CheckBox checkBoxGatherCoal, checkBoxGatherIron, 
	checkBoxGatherMeat, checkBoxGatherWood, 
	checkBoxGatherSpeedBoost,checkBoxRemoveHeros;

	@FXML
	private ComboBox<Integer> comboBoxActiveMarchQueue, comboBoxLevelCoal, 
	comboBoxLevelIron, comboBoxLevelMeat, 
	comboBoxLevelWood;

	@FXML
	private void initialize() {
		checkBoxMappings.put(checkBoxGatherCoal, EnumConfigurationKey.GATHER_COAL_BOOL);
		checkBoxMappings.put(checkBoxGatherIron, EnumConfigurationKey.GATHER_IRON_BOOL);
		checkBoxMappings.put(checkBoxGatherMeat, EnumConfigurationKey.GATHER_MEAT_BOOL);
		checkBoxMappings.put(checkBoxGatherWood, EnumConfigurationKey.GATHER_WOOD_BOOL);
		checkBoxMappings.put(checkBoxGatherSpeedBoost, EnumConfigurationKey.GATHER_SPEED_BOOL);
        checkBoxMappings.put(checkBoxRemoveHeros, EnumConfigurationKey.GATHER_REMOVE_HEROS_BOOL);

		comboBoxLevelCoal.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8);
		comboBoxLevelIron.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8);
		comboBoxLevelMeat.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8);
		comboBoxLevelWood.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8);
		comboBoxMappings.put(comboBoxLevelCoal, EnumConfigurationKey.GATHER_COAL_LEVEL_INT);
		comboBoxMappings.put(comboBoxLevelIron, EnumConfigurationKey.GATHER_IRON_LEVEL_INT);
		comboBoxMappings.put(comboBoxLevelMeat, EnumConfigurationKey.GATHER_MEAT_LEVEL_INT);
		comboBoxMappings.put(comboBoxLevelWood, EnumConfigurationKey.GATHER_WOOD_LEVEL_INT);

		// Initialize ComboBox with values 1-6
		comboBoxActiveMarchQueue.getItems().addAll(1, 2, 3, 4, 5, 6);
		comboBoxMappings.put(comboBoxActiveMarchQueue, EnumConfigurationKey.GATHER_ACTIVE_MARCH_QUEUE_INT);

		initializeChangeEvents();
	}

}
