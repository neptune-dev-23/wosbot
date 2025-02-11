package cl.camodev.wosbot.profile.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import cl.camodev.wosbot.ot.DTOConfig;
import cl.camodev.wosbot.ot.DTOProfileStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.profile.model.IProfileModel;
import cl.camodev.wosbot.profile.model.ProfileAux;
import cl.camodev.wosbot.profile.model.impl.ProfileCallback;
import cl.camodev.wosbot.profile.model.impl.ProfileModel;
import cl.camodev.wosbot.profile.view.ProfileManagerLayoutController;
import cl.camodev.wosbot.serv.IProfileStatusChangeListener;

public class ProfileManagerActionController implements IProfileStatusChangeListener {

	private ProfileManagerLayoutController profileManagerLayoutController;

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

	public boolean deleteProfile(long id) {
		// TODO Auto-generated method stub
		return false;
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

	@Override
	public void onProfileStatusChange(DTOProfileStatus status) {
		if (status != null) {
			profileManagerLayoutController.handleProfileStatusChange(status);

		}

	}

}
