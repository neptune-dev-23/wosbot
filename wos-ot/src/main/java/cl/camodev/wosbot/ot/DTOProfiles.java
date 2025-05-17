
package cl.camodev.wosbot.ot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;

public class DTOProfiles {
	private Long id;
	private String name;
	private String emulatorNumber;
	private Boolean enabled;
	private String status;
	private List<DTOConfig> configs = new ArrayList<DTOConfig>();
	private HashMap<String, String> globalsettings = new HashMap<String, String>();

	/**
	 * Constructor de la clase DTOProfiles.
	 *
	 * @param id             El identificador único del perfil.
	 * @param name           El nombre del perfil.
	 * @param emulatorNumber El número del emulador asociado al perfil.
	 * @param enabled        Indica si el perfil está habilitado o no.
	 */
	public DTOProfiles(Long id, String name, String emulatorNumber, Boolean enabled) {
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

	public String getEmulatorNumber() {
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

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEmulatorNumber(String emulatorNumber) {
		this.emulatorNumber = emulatorNumber;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public void setGlobalSettings(HashMap<String, String> globalsettings) {
		this.setGlobalsettings(globalsettings);

	}

	/**
	 * Obtiene el valor de una configuración específica utilizando EnumConfigurationKey. Es un método genérico que devuelve el tipo correcto
	 * basado en la clave.
	 */
	public <T> T getConfig(EnumConfigurationKey key, Class<T> clazz) {
		Optional<DTOConfig> configOptional = configs.stream().filter(config -> config.getNombreConfiguracion().equalsIgnoreCase(key.name())).findFirst();

		if (!configOptional.isPresent()) {

			DTOConfig defaultConfig = new DTOConfig(-1L, key.name(), key.getDefaultValue());
			configs.add(defaultConfig);
		}
		String valor = configOptional.map(DTOConfig::getValor).orElse(key.getDefaultValue());

		return key.castValue(valor);
	}

	public <T> void setConfig(EnumConfigurationKey key, T value) {
		String valorAAlmacenar = value.toString();
		Optional<DTOConfig> configOptional = configs.stream().filter(config -> config.getNombreConfiguracion().equalsIgnoreCase(key.name())).findFirst();

		if (configOptional.isPresent()) {
			configOptional.get().setValor(valorAAlmacenar);
		} else {
			DTOConfig nuevaConfig = new DTOConfig(getId(), key.name(), valorAAlmacenar);
			configs.add(nuevaConfig);
		}
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public HashMap<String, String> getGlobalsettings() {
		return globalsettings;
	}

	public void setGlobalsettings(HashMap<String, String> globalsettings) {
		this.globalsettings = globalsettings;
	}

}
