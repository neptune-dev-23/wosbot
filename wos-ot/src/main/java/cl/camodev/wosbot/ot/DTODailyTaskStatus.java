package cl.camodev.wosbot.ot;

import java.time.LocalDateTime;

public class DTODailyTaskStatus {

	private LocalDateTime lastExecution;
	private Boolean finished;

	public DTODailyTaskStatus() {
	}

	public DTODailyTaskStatus(LocalDateTime lastExecution, Boolean finished) {
		this.lastExecution = lastExecution;
		this.finished = finished;
	}

	public LocalDateTime getLastExecution() {
		return lastExecution;
	}

	public void setLastExecution(LocalDateTime lastExecution) {
		this.lastExecution = lastExecution;
	}

	public Boolean getFinished() {
		return finished;
	}

	public void setFinished(Boolean finished) {
		this.finished = finished;
	}
}
