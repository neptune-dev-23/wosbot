package cl.camodev.wosbot.experts.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

public class ExpertsLayoutController extends AbstractProfileController {

    @FXML
    private CheckBox claimIntelCheckBox;

    @FXML
    private CheckBox claimLoyaltyTagCheckBox;

    @FXML
    private CheckBox claimTroopsCheckBox;

    @FXML
    private VBox troopOptionsVBox;

    @FXML
    private ComboBox<String> troopTypeComboBox;


    @FXML
    public void initialize() {
        troopTypeComboBox.getItems().addAll("Infantry", "Lancer", "Marksman");

        checkBoxMappings.put(claimIntelCheckBox, EnumConfigurationKey.EXPERT_AGNES_INTEL_BOOL);
        checkBoxMappings.put(claimLoyaltyTagCheckBox, EnumConfigurationKey.EXPERT_ROMULUS_TAG_BOOL);
        checkBoxMappings.put(claimTroopsCheckBox, EnumConfigurationKey.EXPERT_ROMULUS_TROOPS_BOOL);

        comboBoxMappings.put(troopTypeComboBox, EnumConfigurationKey.EXPERT_ROMULUS_TROOPS_TYPE_STRING);

        claimTroopsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            troopOptionsVBox.setVisible(newValue);
        });

        initializeChangeEvents();
    }
}
