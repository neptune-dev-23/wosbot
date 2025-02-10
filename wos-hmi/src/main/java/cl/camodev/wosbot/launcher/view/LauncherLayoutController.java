package cl.camodev.wosbot.launcher.view;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.view.ConsoleLogLayoutController;
import cl.camodev.wosbot.pets.view.PetsLayoutController;
import cl.camodev.wosbot.profile.view.ProfileManagerLayoutController;
import cl.camodev.wosbot.shop.view.ShopLayoutController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class LauncherLayoutController {

	@FXML
	private VBox buttonsContainer;

	@FXML
	private AnchorPane mainContentPane;

	private LauncherActionController actionController;

	private ConsoleLogLayoutController consoleLogLayoutController;

	private Map<String, Object> moduleControllers = new HashMap<>();

	@FXML
	private void initialize() {

		initializeLogModule();
		initializeModules();

	}

	private void initializeModules() {
		//@formatter:off
		List<ModuleDefinition> modules = Arrays.asList(
				new ModuleDefinition("ProfileManagerLayout", "Profiles", ProfileManagerLayoutController::new), 
				new ModuleDefinition("ShopLayoutLayout", "Shop", ShopLayoutController::new), 
				new ModuleDefinition("PetsLayout", "Pets", PetsLayoutController::new));
		//@formatter:on

		for (ModuleDefinition module : modules) {
			consoleLogLayoutController.appendMessage(EnumTpMessageSeverity.INFO, "Loading " + module.getButtonTitle());
			Object controller = module.getControllerSupplier().get();
			moduleControllers.put(module.getButtonTitle(), controller);
			addButton(module.getFxmlName(), module.getButtonTitle(), controller);
		}

	}

	private void initializeLogModule() {
		actionController = new LauncherActionController();
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

	}

	@FXML
	void handleButtonStartStop(ActionEvent event) {
		actionController.startBot();
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

	/**
	 * Clase auxiliar para definir cada módulo. Contiene el nombre del FXML, el título del botón y una función para crear el controlador.
	 */
	private static class ModuleDefinition {
		private final String fxmlName;
		private final String buttonTitle;
		private final Supplier<Object> controllerSupplier;

		public ModuleDefinition(String fxmlName, String buttonTitle, Supplier<Object> controllerSupplier) {
			this.fxmlName = fxmlName;
			this.buttonTitle = buttonTitle;
			this.controllerSupplier = controllerSupplier;
		}

		public String getFxmlName() {
			return fxmlName;
		}

		public String getButtonTitle() {
			return buttonTitle;
		}

		public Supplier<Object> getControllerSupplier() {
			return controllerSupplier;
		}
	}

}
