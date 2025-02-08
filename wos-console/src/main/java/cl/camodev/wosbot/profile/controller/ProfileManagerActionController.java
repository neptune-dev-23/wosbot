package cl.camodev.wosbot.profile.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.profile.model.IProfileModel;
import cl.camodev.wosbot.profile.model.impl.ProfileCallback;
import cl.camodev.wosbot.profile.model.impl.ProfileModel;

public class ProfileManagerActionController {

	private IProfileModel iModel;

	public ProfileManagerActionController() {
		iModel = new ProfileModel();
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

}
