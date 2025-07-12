package cl.camodev.wosbot.main;

import cl.camodev.wosbot.launcher.view.ILauncherConstants;
import cl.camodev.wosbot.launcher.view.LauncherLayoutController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.prefs.Preferences;

public class FXApp extends Application {
	private static final String KEY_X      = "windowX";
	private static final String KEY_Y      = "windowY";
	private static final String KEY_W      = "windowWidth";
	private static final String KEY_H      = "windowHeight";
	private static final double DEFAULT_W  = 900;
	private static final double DEFAULT_H  = 500;

	private Preferences prefs;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws IOException {
		// Inicializar Preferences
		prefs = Preferences.userRoot().node(FXApp.class.getName());

		// Cargar FXML y controlador
		FXMLLoader fxmlLoader = new FXMLLoader(
				LauncherLayoutController.class.getResource("LauncherLayout.fxml")
		);
		LauncherLayoutController controller = new LauncherLayoutController(stage);
		fxmlLoader.setController(controller);
		Parent root = fxmlLoader.load();

		// Leer tamaño desde prefs (o usar valores por defecto)
		double width  = prefs.getDouble(KEY_W, DEFAULT_W);
		double height = prefs.getDouble(KEY_H, DEFAULT_H);

		// Crear escena con tamaño leído
		Scene scene = new Scene(root, width, height);
		scene.getStylesheets().add(ILauncherConstants.getCssPath());
		stage.setScene(scene);

		// Restaurar posición si existe
		double x = prefs.getDouble(KEY_X, Double.NaN);
		double y = prefs.getDouble(KEY_Y, Double.NaN);
		if (!Double.isNaN(x) && !Double.isNaN(y)) {
			stage.setX(x);
			stage.setY(y);
		}

		stage.setTitle("Launcher");

		// Antes de cerrar, guardar posición y tamaño
		stage.setOnCloseRequest(event -> {
			prefs.putDouble(KEY_X,      stage.getX());
			prefs.putDouble(KEY_Y,      stage.getY());
			prefs.putDouble(KEY_W,      stage.getWidth());
			prefs.putDouble(KEY_H,      stage.getHeight());
			System.exit(0);
		});

		stage.show();
	}
}
