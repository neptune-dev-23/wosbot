package cl.camodev.wosbot.shop.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

public class ShopLayoutController extends AbstractProfileController {

	@FXML
	private CheckBox checkBoxNomadicMerchant;

	@FXML
	private CheckBox checkBoxNomadicMerchantVip;

	@FXML
	private void initialize() {

		checkBoxMappings.put(checkBoxNomadicMerchant, EnumConfigurationKey.BOOL_NOMADIC_MERCHANT);
		checkBoxMappings.put(checkBoxNomadicMerchantVip, EnumConfigurationKey.BOOL_NOMADIC_MERCHANT_VIP_POINTS);

		initializeChangeEvents();
	}

}
