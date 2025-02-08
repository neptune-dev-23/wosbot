package cl.camodev.wosbot.profile.view;

import cl.camodev.wosbot.profile.controller.ProfileManagerActionController;
import cl.camodev.wosbot.profile.model.ProfileAux;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.util.Callback;

public class ProfileManagerLayoutController {

	private ProfileManagerActionController profileManagerActionController;

	private ObservableList<ProfileAux> profiles;

	@FXML
	private TableView<ProfileAux> tableviewLogMessages;

	@FXML
	private TableColumn<ProfileAux, Void> columnDelete;
	@FXML
	private TableColumn<ProfileAux, Long> columnEmulatorNumber;
	@FXML
	private TableColumn<ProfileAux, Boolean> columnEnabled;
	@FXML
	private TableColumn<ProfileAux, String> columnProfileName;
	@FXML
	private TableColumn<ProfileAux, String> columnStatus;

	@FXML
	private void initialize() {
		initializeController();
		initializeTableView();
		loadProfiles();

	}

	private void initializeController() {
		profileManagerActionController = new ProfileManagerActionController();
	}

	private void initializeTableView() {
		profiles = FXCollections.observableArrayList();

		columnProfileName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
		columnEmulatorNumber.setCellValueFactory(cellData -> cellData.getValue().emulatorNumberProperty().asObject());
		columnStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

		// Configurar la columna "Delete" con un botón
		columnDelete.setCellFactory(new Callback<TableColumn<ProfileAux, Void>, TableCell<ProfileAux, Void>>() {
			@Override
			public TableCell<ProfileAux, Void> call(TableColumn<ProfileAux, Void> param) {
				return new TableCell<ProfileAux, Void>() {
					private final Button btnDelete = new Button("Delete");

					{
						// Configurar la acción del botón.
						btnDelete.setOnAction((ActionEvent event) -> {
							// Verificar si la tabla tiene al menos un registro
							if (getTableView().getItems().size() <= 1) {
								Alert alert = new Alert(Alert.AlertType.WARNING);
								alert.setTitle("WARNING");
								alert.setHeaderText(null);
								alert.setContentText("U MUST HAVE AT LEAST ONE PROFILE");
								alert.showAndWait();
								return; 
							}

							// Obtener el registro actual a partir del índice de la fila.
							ProfileAux currentProfile = getTableView().getItems().get(getIndex());
							System.out.println("Eliminando perfil con ID: " + currentProfile.getId());
							
		                    // Llamar al método deleteProfile y almacenar el resultado.
		                    boolean deletionResult = profileManagerActionController.deleteProfile(currentProfile.getId());

		                    if (deletionResult) {
		                        // Mostrar mensaje de éxito.
		                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
		                        alert.setTitle("Eliminación exitosa");
		                        alert.setHeaderText(null);
		                        alert.setContentText("El perfil se ha eliminado correctamente.");
		                        alert.showAndWait();

		                    } else {
		                        // Mostrar mensaje de error.
		                        Alert alert = new Alert(Alert.AlertType.ERROR);
		                        alert.setTitle("Error en eliminación");
		                        alert.setHeaderText(null);
		                        alert.setContentText("Hubo un error al eliminar el perfil.");
		                        alert.showAndWait();
		                    }
						});
					}

					@Override
					protected void updateItem(Void item, boolean empty) {
						super.updateItem(item, empty);
						if (empty) {
							setGraphic(null);
						} else {
							setGraphic(btnDelete);
						}
					}
				};
			}
		});

		columnEnabled.setCellValueFactory(cellData -> cellData.getValue().enabledProperty());

		columnEnabled.setCellFactory(col -> new TableCell<ProfileAux, Boolean>() {

			private final ToggleButton toggleButton = new ToggleButton();

			{

				toggleButton.setOnAction(event -> {
					ProfileAux currentProfile = getTableView().getItems().get(getIndex());
					boolean newValue = toggleButton.isSelected();
					currentProfile.setEnabled(newValue);
				});
			}

			@Override
			protected void updateItem(Boolean item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
				} else {
					toggleButton.setSelected(item);
					setGraphic(toggleButton);
				}
			}
		});

		tableviewLogMessages.setItems(profiles);

	}

	private void loadProfiles() {
		profileManagerActionController.loadProfiles(dtoProfiles -> {
			Platform.runLater(() -> {
				profiles.clear();
				dtoProfiles.forEach(prof -> {
					ProfileAux aux = new ProfileAux();
					aux.setId(prof.getId());
					aux.setName(prof.getName());
					aux.setEmulatorNumber(prof.getEmulatorNumber());
					aux.setEnabled(prof.getEnabled());
					aux.setEnabled(prof.getEnabled());
					profiles.add(aux);
				});
			});
		});

	}

}
