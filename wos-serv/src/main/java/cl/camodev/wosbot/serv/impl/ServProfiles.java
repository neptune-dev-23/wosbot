package cl.camodev.wosbot.serv.impl;

import java.util.List;

import cl.camodev.wosbot.almac.repo.IProfileRepository;
import cl.camodev.wosbot.almac.repo.ProfileRepository;
import cl.camodev.wosbot.ot.DTOProfiles;

public class ServProfiles {

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

	public List<DTOProfiles> getProfiles() {
		return iProfileRepository.getProfiles();
	}

}
