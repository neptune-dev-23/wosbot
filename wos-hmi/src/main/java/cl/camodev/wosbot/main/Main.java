package cl.camodev.wosbot.main;

public class Main {
	public static void main(String[] args) {
//		// Iniciar Spring Boot en un hilo separado para no bloquear JavaFX
//		Thread springThread = new Thread(() -> {
//			SpringApp.start();
//		});
//		springThread.setDaemon(true);
//		springThread.start();
//
//		// Iniciar JavaFX
		FXApp.main(args);

	}
}
