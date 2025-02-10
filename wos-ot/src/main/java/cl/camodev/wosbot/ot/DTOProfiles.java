
package cl.camodev.wosbot.ot;

import java.util.List;

public class DTOProfiles {
	private Long id;
	private String name;
	private Long emulatorNumber;
	private Boolean enabled;
	private List<DTOConfig> configs;

	/**
	 * Constructor de la clase DTOProfiles.
	 *
	 * @param id             El identificador único del perfil.
	 * @param name           El nombre del perfil.
	 * @param emulatorNumber El número del emulador asociado al perfil.
	 * @param enabled        Indica si el perfil está habilitado o no.
	 */
	public DTOProfiles(Long id, String name, Long emulatorNumber, Boolean enabled) {
		this.id = id;
		this.name = name;
		this.emulatorNumber = emulatorNumber;
		this.enabled = enabled;
	}

	// Getters y Setters

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Long getEmulatorNumber() {
		return emulatorNumber;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public List<DTOConfig> getConfigs() {
		return configs;
	}

	public void setConfigs(List<DTOConfig> configs) {
		this.configs = configs;
	}
}
