package cl.camodev.wosbot.launcher.view;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import cl.camodev.utiles.UtilCV;
import cl.camodev.wosbot.alliance.view.AllianceLayoutController;
import cl.camodev.wosbot.city.view.CityEventsLayoutController;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.view.ConsoleLogLayoutController;
import cl.camodev.wosbot.intel.view.IntelLayoutController;
import cl.camodev.wosbot.ot.DTOBotState;
import cl.camodev.wosbot.ot.DTOLogMessage;
import cl.camodev.wosbot.pets.view.PetsLayoutController;
import cl.camodev.wosbot.profile.model.IProfileChangeObserver;
import cl.camodev.wosbot.profile.model.IProfileLoadListener;
import cl.camodev.wosbot.profile.model.IProfileObserverInjectable;
import cl.camodev.wosbot.profile.model.ProfileAux;
import cl.camodev.wosbot.profile.view.ProfileManagerLayoutController;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.training.view.TrainingLayoutController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class LauncherLayoutController implements IProfileLoadListener {

	@FXML
	private VBox buttonsContainer;

	@FXML
	private Button buttonHideShow;

	@FXML
	private Button buttonPause;

	@FXML
	private Button buttonPhoto;

	@FXML
	private Button buttonStartStop;

	@FXML
	private AnchorPane mainContentPane;

	@FXML
	private Label labelRunTime;

	private Stage stage;

	private LauncherActionController actionController;

	private ConsoleLogLayoutController consoleLogLayoutController;

	private ProfileManagerLayoutController profileManagerLayoutController;

	private Map<String, Object> moduleControllers = new HashMap<>();

	private boolean estado = false;

	public LauncherLayoutController(Stage stage) {
		this.stage = stage;
	}

	@FXML
	private void initialize() {
		initializeDiscordBot();
		initializeLogModule();
		initializeProfileModule();
		initializeModules();
		initializeExternalLibraries();
		initializeEmulatorManager();

	}

	private void initializeEmulatorManager() {
		String defaultPath = "C:\\Program Files\\Netease\\MuMuPlayerGlobal-12.0\\shell\\MuMuManager.exe";
		File defaultFile = new File(defaultPath);

		if (defaultFile.exists()) {
			// Si el archivo existe en la ruta predeterminada, se usa esta ruta directamente
			ServScheduler.getServices().saveEmuManagerPath(defaultFile.getParent());
		} else {
			// Si no existe, se solicita al usuario que seleccione el archivo manualmente
			if (!ServScheduler.getServices().isEmuManagerReady()) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Select MuMuManager.exe");

				FileChooser.ExtensionFilter exeFilter = new FileChooser.ExtensionFilter("MuMuManager Executable", "*.exe");
				fileChooser.getExtensionFilters().add(exeFilter);

				fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

				File selectedFile = fileChooser.showOpenDialog(stage);

				if (selectedFile != null && selectedFile.getName().equals("MuMuManager.exe")) {
					String directoryPath = selectedFile.getParent(); // Obtener la carpeta contenedora
					ServScheduler.getServices().saveEmuManagerPath(directoryPath);
				} else {
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("ERROR");
					alert.setHeaderText(null);
					alert.setContentText("MuMuManager.exe not found, please select the correct file");
					alert.showAndWait();

					System.exit(0); // Cerrar la aplicación después de mostrar la alerta
				}
			}
		}
	}

	private void initializeDiscordBot() {
//		ServDiscord.getServices();

	}

	private void initializeProfileModule() {
		profileManagerLayoutController = new ProfileManagerLayoutController();
		addButton("ProfileManagerLayout", "Profiles", profileManagerLayoutController);

	}

	private void initializeExternalLibraries() {
		try {
			UtilCV.loadNativeLibrary("/native/opencv_java4110.dll");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			UtilCV.extractResourceFolder("/native/opencv", new File("tessdata"));
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	private void initializeModules() {
		//@formatter:off
		List<ModuleDefinition> modules = Arrays.asList(				
				new ModuleDefinition("CityEventsLayout", "City Events", CityEventsLayoutController::new),
				new ModuleDefinition("AllianceLayout", "Alliance", AllianceLayoutController::new),
				new ModuleDefinition("TrainingLayout", "Training", TrainingLayoutController::new),
				new ModuleDefinition("PetsLayout", "Pets", PetsLayoutController::new),
				new ModuleDefinition("IntelLayout", "Intel", IntelLayoutController::new)
				);
		//@formatter:on

		for (ModuleDefinition module : modules) {
			consoleLogLayoutController.appendMessage(new DTOLogMessage(EnumTpMessageSeverity.INFO, "Loading module: " + module.getButtonTitle(), "-", "-"));

			// Crear el controlador con la instancia de profileObserver
			Object controller = module.createController(profileManagerLayoutController);
			moduleControllers.put(module.getButtonTitle(), controller);
			addButton(module.getFxmlName(), module.getButtonTitle(), controller);

			if (controller instanceof IProfileLoadListener) {
				profileManagerLayoutController.addProfileLoadListener((IProfileLoadListener) controller);
			}
		}
		profileManagerLayoutController.addProfileLoadListener(this);
	}

	private void initializeLogModule() {
		actionController = new LauncherActionController(this);
		consoleLogLayoutController = new ConsoleLogLayoutController();
		addButton("ConsoleLogLayout", "Logs", consoleLogLayoutController).fire();

	}

	@FXML
	void handleButtonHideShow(ActionEvent event) {

	}

	@FXML
	void handleButtonPause(ActionEvent event) {

	}

	@FXML
	void handleButtonPhoto(ActionEvent event) {
//		EmulatorManager.getInstance().captureScrenshotViaADB("0");
	}

	@FXML
	public void handleButtonStartStop(ActionEvent event) {
		if (!estado) {
			actionController.startBot();
		} else {
			actionController.stopBot();
		}

	}

	private Button addButton(String fxmlName, String title, Object controller) {
		try {
			FXMLLoader loader = new FXMLLoader(controller.getClass().getResource(fxmlName + ".fxml"));
			loader.setController(controller);
			Parent root = loader.load();

			Button button = new Button(title);
			button.setMaxWidth(Double.MAX_VALUE);
			HBox.setHgrow(button, Priority.ALWAYS);

			// Asigna la clase personalizada para esquinas cuadradas a este botón
			button.getStyleClass().add("square-button");

			button.setOnAction(e -> {
				// Limpia el contenido actual y agrega el nuevo panel
				mainContentPane.getChildren().clear();
				AnchorPane.setTopAnchor(root, 0.0);
				AnchorPane.setBottomAnchor(root, 0.0);
				AnchorPane.setLeftAnchor(root, 0.0);
				AnchorPane.setRightAnchor(root, 0.0);
				mainContentPane.getChildren().add(root);

				// Remueve la clase de estilo "active" de todos los botones
				for (Node node : buttonsContainer.getChildren()) {
					if (node instanceof Button) {
						node.getStyleClass().remove("active");
					}
				}
				// Agrega la clase "active" al botón actual para resaltarlo
				button.getStyleClass().add("active");
			});

			buttonsContainer.getChildren().add(button);
			return button;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public <T> T getModuleController(String key, Class<T> type) {
		Object controller = moduleControllers.get(key);
		if (controller == null) {
			return null;
		}
		return type.cast(controller);
	}

	private static class ModuleDefinition {
		private final String fxmlName;
		private final String buttonTitle;
		private final Supplier<Object> controllerSupplier;

		public ModuleDefinition(String fxmlName, String buttonTitle, Supplier<Object> controllerSupplier) {
			this.fxmlName = fxmlName;
			this.buttonTitle = buttonTitle;
			this.controllerSupplier = controllerSupplier;
		}

		public Object createController(IProfileChangeObserver profileObserver) {
			Object controller = controllerSupplier.get();
			if (controller instanceof IProfileObserverInjectable) {
				((IProfileObserverInjectable) controller).setProfileObserver(profileObserver);
			}
			return controller;
		}

		public String getFxmlName() {
			return fxmlName;
		}

		public String getButtonTitle() {
			return buttonTitle;
		}

	}

	@Override
	public void onProfileLoad(ProfileAux profile) {
		stage.setTitle("Whiteout Survival Bot - " + profile.getName());
		buttonStartStop.setDisable(false);
		buttonPhoto.setDisable(false);

	}

	public void onBotStateChange(DTOBotState botState) {
		if (botState != null) {
			if (botState.getRunning()) {
				buttonStartStop.setText("Stop");
				buttonHideShow.setDisable(false);
				buttonPause.setDisable(false);
				buttonPhoto.setDisable(false);
				estado = true;

//				startTime = LocalDateTime.now();
//				startUpdatingExecutionTime();
			} else {
				buttonStartStop.setText("Start");
				buttonHideShow.setDisable(true);
				buttonPause.setDisable(true);
				buttonPhoto.setDisable(true);
				estado = false;

//				stopUpdatingExecutionTime();
			}
		}

	}

}
