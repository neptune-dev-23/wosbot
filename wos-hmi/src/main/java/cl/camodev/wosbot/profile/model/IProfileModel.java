package cl.camodev.wosbot.profile.model;

import java.util.List;

import cl.camodev.wosbot.ot.DTOProfiles;

public interface IProfileModel {

	public List<DTOProfiles> getProfiles();

	public boolean addProfile(DTOProfiles profile);

}
