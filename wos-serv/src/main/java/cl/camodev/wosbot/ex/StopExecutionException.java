package cl.camodev.wosbot.ex;

public class StopExecutionException extends RuntimeException {
	public StopExecutionException(String message) {
		super(message);
	}
}