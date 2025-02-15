package cl.camodev.wosbot.ot;

import java.time.LocalDateTime;

public class DTOBotState {

	private Boolean running;
	private LocalDateTime actionTime;

	public Boolean getRunning() {
		return running;
	}

	public void setRunning(Boolean running) {
		this.running = running;
	}

	public LocalDateTime getActionTime() {
		return actionTime;
	}

	public void setActionTime(LocalDateTime actionTime) {
		this.actionTime = actionTime;
	}

}
