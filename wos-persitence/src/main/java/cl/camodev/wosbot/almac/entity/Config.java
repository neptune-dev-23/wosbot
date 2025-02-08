package cl.camodev.wosbot.almac.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "config")
public class Config {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private Long id;

	@Column(name = "nombre_configuracion", nullable = false)
	private String nombreConfiguracion;

	@Column(name = "valor", nullable = false)
	private String valor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profile_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Profile profile;

	// Getters y Setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNombreConfiguracion() {
		return nombreConfiguracion;
	}

	public void setNombreConfiguracion(String nombreConfiguracion) {
		this.nombreConfiguracion = nombreConfiguracion;
	}

	public String getValor() {
		return valor;
	}

	public void setValor(String valor) {
		this.valor = valor;
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}
}
