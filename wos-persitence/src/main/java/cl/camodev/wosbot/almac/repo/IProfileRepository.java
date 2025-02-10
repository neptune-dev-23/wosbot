package cl.camodev.wosbot.almac.repo;

import java.util.List;

import cl.camodev.wosbot.ot.DTOProfiles;

public interface IProfileRepository {

	public List<DTOProfiles> getProfiles();

	public boolean addProfile(DTOProfiles profile);

	public boolean saveProfile(DTOProfiles profile);

	public boolean deleteProfile(DTOProfiles profile);

}
