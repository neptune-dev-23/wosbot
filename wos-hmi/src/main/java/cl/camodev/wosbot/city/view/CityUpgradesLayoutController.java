package cl.camodev.wosbot.city.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

public class CityUpgradesLayoutController extends AbstractProfileController {

	@FXML
	private CheckBox checkBoxUpgradeFurnace;

	@FXML
	private void initialize() {

		checkBoxMappings.put(checkBoxUpgradeFurnace, EnumConfigurationKey.CITY_UPGRADE_FURNACE_BOOL);

		initializeChangeEvents();
	}

}
