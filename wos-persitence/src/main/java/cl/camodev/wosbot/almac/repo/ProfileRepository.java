package cl.camodev.wosbot.almac.repo;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cl.camodev.wosbot.almac.entity.Config;
import cl.camodev.wosbot.almac.entity.Profile;
import cl.camodev.wosbot.almac.jpa.BotPersistence;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.ot.DTOConfig;
import cl.camodev.wosbot.ot.DTOProfiles;
import jakarta.persistence.Query;

public class ProfileRepository implements IProfileRepository {

	private static ProfileRepository instance;

	private BotPersistence persistence = BotPersistence.getInstance();

	private ProfileRepository() {
	}

	public static ProfileRepository getRepository() {
		if (instance == null) {
			instance = new ProfileRepository();
		}
		return instance;
	}

	@Override
	public List<DTOProfiles> getProfiles() {
		// Consulta para obtener los perfiles como DTOs
		String queryProfiles = "SELECT new cl.camodev.wosbot.ot.DTOProfiles(p.id, p.name, p.emulatorNumber, p.enabled) FROM Profile p";
		List<DTOProfiles> profiles = persistence.getQueryResults(persistence.createQuery(queryProfiles));

		// Si no existe ningún perfil, se crea uno por defecto
		if (profiles == null || profiles.isEmpty()) {
			// Se crea un perfil por defecto
			Profile defaultProfile = new Profile();
			defaultProfile.setName("Default");
			defaultProfile.setEmulatorNumber(0L);
			defaultProfile.setEnabled(true);
			persistence.createEntity(defaultProfile);

			for (EnumConfigurationKey key : EnumConfigurationKey.values()) {
				Config cfg = new Config();
				cfg.setProfile(defaultProfile);
				cfg.setNombreConfiguracion(key.toString());
				cfg.setValor(key.getDefaultValue());
				persistence.createEntity(cfg);
			}

			// Se vuelve a ejecutar la consulta para obtener el perfil recién creado
			profiles = persistence.getQueryResults(persistence.createQuery(queryProfiles));
		}

		// Se extraen los IDs de los perfiles obtenidos
		List<Long> profileIds = profiles.stream().map(DTOProfiles::getId).collect(Collectors.toList());

		// Consulta para obtener las configuraciones asociadas a los perfiles
		String queryConfigs = "SELECT new cl.camodev.wosbot.ot.DTOConfig(c.profile.id, c.nombreConfiguracion, c.valor) "
				+ "FROM Config c WHERE c.profile.id IN :profileIds";
		Query query = persistence.createQuery(queryConfigs);
		query.setParameter("profileIds", profileIds);
		List<DTOConfig> configs = persistence.getQueryResults(query);

		// Se agrupan las configuraciones por ID de perfil
		Map<Long, List<DTOConfig>> configMap = configs.stream().collect(Collectors.groupingBy(DTOConfig::getProfileId));

		// Se asigna la lista de configuraciones a cada perfil
		profiles.forEach(
				profile -> profile.setConfigs(configMap.getOrDefault(profile.getId(), Collections.emptyList())));

		return profiles;
	}

}
