package cl.camodev.wosbot.profile.model.impl;

import java.util.List;

import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.profile.model.IProfileModel;
import cl.camodev.wosbot.serv.IServProfile;
import cl.camodev.wosbot.serv.impl.ServProfiles;

public class ProfileModel implements IProfileModel {

	private IServProfile servProfile;

	public ProfileModel() {
		servProfile = ServProfiles.getServices();
	}

	@Override
	public List<DTOProfiles> getProfiles() {
		return ServProfiles.getServices().getProfiles();
	}

	@Override
	public boolean addProfile(DTOProfiles profile) {
		return servProfile.addProfile(profile);
	}

}
