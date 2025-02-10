package cl.camodev.wosbot.profile.model;

import cl.camodev.wosbot.ot.DTOProfiles;

public interface IProfileLoadListener {

	public void onProfilesLoaded(DTOProfiles profile);
}
