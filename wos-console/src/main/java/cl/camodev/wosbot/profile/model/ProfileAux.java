package cl.camodev.wosbot.profile.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProfileAux {

	private LongProperty id;
	private StringProperty name;
	private LongProperty emulatorNumber;
	private BooleanProperty enabled;
	private StringProperty status;

	// Constructor vacío
	public ProfileAux() {
		this.id = new SimpleLongProperty();
		this.name = new SimpleStringProperty();
		this.emulatorNumber = new SimpleLongProperty();
		this.enabled = new SimpleBooleanProperty();
		this.status = new SimpleStringProperty();
	}

	// Constructor con parámetros
	public ProfileAux(long id, String name, long emulatorNumber, boolean enabled, String status) {
		this.id = new SimpleLongProperty(id);
		this.name = new SimpleStringProperty(name);
		this.emulatorNumber = new SimpleLongProperty(emulatorNumber);
		this.enabled = new SimpleBooleanProperty(enabled);
		this.status = new SimpleStringProperty(status);
	}

	// Métodos para la propiedad 'id'
	public long getId() {
		return id.get();
	}

	public void setId(long id) {
		this.id.set(id);
	}

	public LongProperty idProperty() {
		return id;
	}

	// Métodos para la propiedad 'name'
	public String getName() {
		return name.get();
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public StringProperty nameProperty() {
		return name;
	}

	// Métodos para la propiedad 'emulatorNumber'
	public long getEmulatorNumber() {
		return emulatorNumber.get();
	}

	public void setEmulatorNumber(long emulatorNumber) {
		this.emulatorNumber.set(emulatorNumber);
	}

	public LongProperty emulatorNumberProperty() {
		return emulatorNumber;
	}

	// Métodos para la propiedad 'enabled'
	public boolean isEnabled() {
		return enabled.get();
	}

	public void setEnabled(boolean enabled) {
		this.enabled.set(enabled);
	}

	public BooleanProperty enabledProperty() {
		return enabled;
	}

	// Métodos para la propiedad 'status'
	public String getStatus() {
		return status.get();
	}

	public void setStatus(String status) {
		this.status.set(status);
	}

	public StringProperty statusProperty() {
		return status;
	}
}
