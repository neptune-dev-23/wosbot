package cl.camodev.wosbot.console.model;

import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import javafx.beans.property.SimpleStringProperty;

public class LogMessageAux {

	private SimpleStringProperty timeStamp;
	private SimpleStringProperty message;
	private SimpleStringProperty severity;

	public LogMessageAux(String timeStamp, EnumTpMessageSeverity severity, String message) {
		this.timeStamp = new SimpleStringProperty(timeStamp);
		this.message = new SimpleStringProperty(message);
		this.severity = new SimpleStringProperty(severity.toString());
	}

	public String getTimeStamp() {
		return timeStamp.get();
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp.set(timeStamp);
	}

	public String getMessage() {
		return message.get();
	}

	public void setMessage(String message) {
		this.message.set(message);
	}

	public SimpleStringProperty timeStampProperty() {
		return timeStamp;
	}

	public SimpleStringProperty messageProperty() {
		return message;
	}

	public SimpleStringProperty severityProperty() {
		return severity;
	}

}
