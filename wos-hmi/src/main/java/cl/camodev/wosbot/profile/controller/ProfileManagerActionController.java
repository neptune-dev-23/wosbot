package cl.camodev.wosbot.profile.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.launcher.view.ILauncherConstants;
import cl.camodev.wosbot.ot.DTOConfig;
import cl.camodev.wosbot.ot.DTOProfileStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.profile.model.IProfileModel;
import cl.camodev.wosbot.profile.model.ProfileAux;
import cl.camodev.wosbot.profile.model.impl.ProfileCallback;
import cl.camodev.wosbot.profile.model.impl.ProfileModel;
import cl.camodev.wosbot.profile.view.NewProfileLayoutController;
import cl.camodev.wosbot.profile.view.ProfileManagerLayoutController;
import cl.camodev.wosbot.serv.IProfileStatusChangeListener;
import cl.camodev.wosbot.serv.impl.ServLogs;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ProfileManagerActionController implements IProfileStatusChangeListener {

	private ProfileManagerLayoutController profileManagerLayoutController;

	private Stage newProfileStage;

	private IProfileModel iModel;

	public ProfileManagerActionController(ProfileManagerLayoutController profileManagerLayoutController) {
		this.profileManagerLayoutController = profileManagerLayoutController;
		this.iModel = new ProfileModel();
		this.iModel.addProfileStatusChangeListerner(this);

	}

	public void loadProfiles(ProfileCallback callback) {
		CompletableFuture.supplyAsync(() -> {
			List<DTOProfiles> profiles = iModel.getProfiles();
			return profiles;
		}).thenAccept(profiles -> {

			if (callback != null) {
				callback.onProfilesLoaded(profiles);
			}

		}).exceptionally(ex -> {
			ex.printStackTrace();
			return null;
		});
	}

	public boolean deleteProfile(DTOProfiles profile) {
		return iModel.deleteProfile(profile);
	}

	public boolean addProfile(DTOProfiles profile) {
		return iModel.addProfile(profile);
	}

	public boolean saveProfile(ProfileAux currentProfile) {

		DTOProfiles dtoprofile = new DTOProfiles(currentProfile.getId(), currentProfile.getName(), currentProfile.getEmulatorNumber(), currentProfile.isEnabled());
		currentProfile.getConfigs().forEach(cfgAux -> {
			DTOConfig dtoConfig = new DTOConfig(currentProfile.getId(), cfgAux.getName(), cfgAux.getValue());
			dtoprofile.getConfigs().add(dtoConfig);
		});
		return iModel.saveProfile(dtoprofile);
	}

	public boolean bulkUpdateProfiles(ProfileAux templateProfile) {
		if (templateProfile == null) {
			return false;
		}

		DTOProfiles dtoTemplateProfile = new DTOProfiles(
			templateProfile.getId(), 
			templateProfile.getName(), 
			templateProfile.getEmulatorNumber(), 
			templateProfile.isEnabled()
		);
		
		templateProfile.getConfigs().forEach(cfgAux -> {
			DTOConfig dtoConfig = new DTOConfig(templateProfile.getId(), cfgAux.getName(), cfgAux.getValue());
			dtoTemplateProfile.getConfigs().add(dtoConfig);
		});
		
		return iModel.bulkUpdateProfiles(dtoTemplateProfile);
	}

	@Override
	public void onProfileStatusChange(DTOProfileStatus status) {
		if (status != null) {
			profileManagerLayoutController.handleProfileStatusChange(status);

		}

	}

	public void showNewProfileDialog() {
		try {
			FXMLLoader loader = new FXMLLoader(NewProfileLayoutController.class.getResource("NewProfileLayout.fxml"));
			NewProfileLayoutController controller = new NewProfileLayoutController(this);
			loader.setController(controller);

			Parent root = loader.load();
			Scene scene = new Scene(root);
			scene.getStylesheets().add(ILauncherConstants.getCssPath());

			newProfileStage = new Stage();
			newProfileStage.setTitle("New Profile");
			newProfileStage.setScene(scene);
			newProfileStage.initModality(Modality.APPLICATION_MODAL);
			newProfileStage.setResizable(false);

			newProfileStage.setOnCloseRequest(event -> closeNewProfileDialog());

			newProfileStage.showAndWait();
		} catch (IOException e) {
			e.printStackTrace();
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, "Profile Manager", "-", "Error loading FXML " + e.getMessage());
		}
	}

	public void closeNewProfileDialog() {
		if (newProfileStage != null) {
			newProfileStage.close();
			newProfileStage = null;
		}
		profileManagerLayoutController.loadProfiles();
	}

}
