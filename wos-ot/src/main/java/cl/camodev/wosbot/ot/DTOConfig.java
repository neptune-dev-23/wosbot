package cl.camodev.wosbot.ot;

public class DTOConfig {
	private Long profileId; // Para saber a qué perfil pertenece
	private String nombreConfiguracion;
	private String valor;

	public DTOConfig(Long profileId, String nombreConfiguracion, String valor) {
		this.profileId = profileId;
		this.nombreConfiguracion = nombreConfiguracion;
		this.valor = valor;
	}

	// Getters y Setters

	public Long getProfileId() {
		return profileId;
	}

	public String getNombreConfiguracion() {
		return nombreConfiguracion;
	}

	public String getValor() {
		return valor;
	}
}
