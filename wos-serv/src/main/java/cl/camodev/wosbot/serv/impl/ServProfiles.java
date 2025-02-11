package cl.camodev.wosbot.serv.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cl.camodev.wosbot.almac.entity.Config;
import cl.camodev.wosbot.almac.entity.Profile;
import cl.camodev.wosbot.almac.repo.IProfileRepository;
import cl.camodev.wosbot.almac.repo.ProfileRepository;
import cl.camodev.wosbot.ot.DTOProfileStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.IProfileStatusChangeListener;
import cl.camodev.wosbot.serv.IServProfile;

public class ServProfiles implements IServProfile {

	private static ServProfiles instance;

	private IProfileRepository iProfileRepository;

	private List<IProfileStatusChangeListener> listeners;

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
		try {
			if (profile == null) {
				return false;
			}

			Profile newProfile = new Profile();
			newProfile.setName(profile.getName());
			newProfile.setEmulatorNumber(profile.getEmulatorNumber());
			newProfile.setEnabled(profile.getEnabled());

			boolean result = iProfileRepository.addProfile(newProfile);

			return result;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean saveProfile(DTOProfiles profileDTO) {
		try {
			if (profileDTO == null || profileDTO.getId() == null) {
				return false;
			}

			// Obtener el perfil existente
			Profile existingProfile = iProfileRepository.getProfileById(profileDTO.getId());

			if (existingProfile == null) {
				return false;
			}

			// Actualizar datos del perfil
			existingProfile.setName(profileDTO.getName());
			existingProfile.setEmulatorNumber(profileDTO.getEmulatorNumber());
			existingProfile.setEnabled(profileDTO.getEnabled());

			// Obtener y eliminar todas las configuraciones existentes del perfil
			List<Config> existingConfigs = iProfileRepository.getProfileConfigs(existingProfile.getId());
			iProfileRepository.deleteConfigs(existingConfigs);

			List<Config> newConfigs = profileDTO.getConfigs().stream().map(dtoConfig -> new Config(existingProfile, dtoConfig.getNombreConfiguracion(), dtoConfig.getValor())).collect(Collectors.toList());

			iProfileRepository.saveConfigs(newConfigs);

			return iProfileRepository.saveProfile(existingProfile);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean deleteProfile(DTOProfiles profile) {
		// TODO Auto-generated method stub
		return false;
	}

	public void notifyProfileStatusChange(DTOProfileStatus statusDto) {
		if (listeners != null) {
			listeners.forEach(listener -> {
				listener.onProfileStatusChange(statusDto);
			});
		}
	}

	@Override
	public void addProfileStatusChangeListerner(IProfileStatusChangeListener listener) {
		if (listeners == null) {
			listeners = new ArrayList<IProfileStatusChangeListener>();
		}
		listeners.add(listener);
	}
}
