package cl.camodev.wosbot.serv.impl;

import java.util.List;

import cl.camodev.wosbot.almac.repo.IProfileRepository;
import cl.camodev.wosbot.almac.repo.ProfileRepository;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.IServProfile;

public class ServProfiles implements IServProfile {

	private static ServProfiles instance;

	private IProfileRepository iProfileRepository;

	private ServProfiles() {
		iProfileRepository = ProfileRepository.getRepository();
	}

	public static ServProfiles getServices() {
		if (instance == null) {
			instance = new ServProfiles();
		}
		return instance;
	}

	@Override
	public List<DTOProfiles> getProfiles() {
		return iProfileRepository.getProfiles();
	}

	@Override
	public boolean addProfile(DTOProfiles profile) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean saveProfile(DTOProfiles profile) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteProfile(DTOProfiles profile) {
		// TODO Auto-generated method stub
		return false;
	}

}
