package cl.camodev.wosbot.training.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

public class TrainingLayoutController extends AbstractProfileController {

	@FXML
	private CheckBox checkBoxTrainingTroops, checkBoxUseResources;

	@FXML
	private void initialize() {
		checkBoxMappings.put(checkBoxTrainingTroops, EnumConfigurationKey.BOOL_TRAINING_TROOPS);
		checkBoxMappings.put(checkBoxUseResources, EnumConfigurationKey.BOOL_TRAINING_RESOURCES);

		initializeChangeEvents();
		checkBoxUseResources.setDisable(true);
	}
}
