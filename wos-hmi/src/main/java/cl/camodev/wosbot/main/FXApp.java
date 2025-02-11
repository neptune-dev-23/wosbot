package cl.camodev.wosbot.main;

import java.io.IOException;

import cl.camodev.wosbot.launcher.view.ILauncherConstants;
import cl.camodev.wosbot.launcher.view.LauncherLayoutController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FXApp extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(LauncherLayoutController.class.getResource("LauncherLayout.fxml"));
		LauncherLayoutController controller = new LauncherLayoutController(stage);
		fxmlLoader.setController(controller);
		Parent root = fxmlLoader.load();
		Scene scene = new Scene(root, 900, 500);
		scene.getStylesheets().add(ILauncherConstants.getCssPath());
		stage.setScene(scene);
		stage.setTitle("Launcher");
		stage.setOnCloseRequest(event -> {
			System.exit(0);
		});
		stage.show();
	}

}