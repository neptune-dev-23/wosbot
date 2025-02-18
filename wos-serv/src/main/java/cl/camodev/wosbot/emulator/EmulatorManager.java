package cl.camodev.wosbot.emulator;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import cl.camodev.utiles.ImageSearchUtil;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class EmulatorManager {

	private static EmulatorManager instance;

	private static final String MUMU_PATH = "\"C:\\Program Files\\Netease\\MuMuPlayerGlobal-12.0\\shell\\MuMuManager.exe\"";
	private static final String ADB_PATH = "C:/Program Files/Netease/MuMuPlayerGlobal-12.0/shell/adb";
	private static final String WHITEOUT_PACKAGE = "com.gof.global";

	private EmulatorManager() {
		// Constructor privado para evitar instanciación
	}

	public static EmulatorManager getInstance() {
		if (instance == null) {
			instance = new EmulatorManager();
		}
		return instance;
	}

	public void restartAdbServer() {
		try {
			// Comando para matar el servidor ADB
			ProcessBuilder killServer = new ProcessBuilder(ADB_PATH, "kill-server");
			killServer.redirectErrorStream(true);
			Process processKill = killServer.start();
			processKill.waitFor();

			// Comando para iniciar el servidor ADB
			ProcessBuilder startServer = new ProcessBuilder(ADB_PATH, "start-server");
			startServer.redirectErrorStream(true);
			Process processStart = startServer.start();
			processStart.waitFor();

			System.out.println("ADB server restarted successfully.");
		} catch (IOException | InterruptedException e) {
			System.err.println("Error restarting ADB server:");
			e.printStackTrace();
		}
	}

	public ByteArrayInputStream captureScreenshotViaADB(String emulatorNumber) {
		// Obtener la IP del dispositivo usando el método getAdbIp
		String adbIp = getAdbIp(emulatorNumber);
		if (adbIp == null) {
			System.err.println("No se pudo obtener la IP para el emulador: " + emulatorNumber);
			return null;
		}

		// Construir el comando para capturar el screenshot
		ProcessBuilder pb = new ProcessBuilder(ADB_PATH, "-s", adbIp, "exec-out", "screencap", "-p");
		pb.redirectErrorStream(true);

		try {
			Process process = pb.start();

			// Leer la salida del comando (imagen en formato PNG o mensaje de error)
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (InputStream is = process.getInputStream()) {
				byte[] buffer = new byte[8192];
				int bytesRead;
				while ((bytesRead = is.read(buffer)) != -1) {
					baos.write(buffer, 0, bytesRead);
				}
			}

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println("Captura de pantalla obtenida exitosamente.");
				return new ByteArrayInputStream(baos.toByteArray());
			} else {
				// Imprimir la salida que pudo contener mensajes de error
				System.err.println("Error al ejecutar el comando, exit code: " + exitCode);
				System.err.println("Salida del comando: " + baos.toString("UTF-8"));
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		return null;

	}

	public DTOImageSearchResult searchTemplate(String emulatorNumber, String templatePath, int x, int y, int width, int height, double threshold) {
//		captureScreenshotViaADB(emulatorNumber);
		DTOImageSearchResult result = ImageSearchUtil.buscarTemplate(captureScreenshotViaADB(emulatorNumber), templatePath, x, y, width, height, threshold);
		return result;

	}

	public void tapBackButton(String emulatorNumber) {
		// Obtener la IP del dispositivo usando el método getAdbIp shell input keyevent KEYCODE_BACK
		String adbIp = getAdbIp(emulatorNumber);
		if (adbIp == null) {
			System.err.println("No se pudo obtener la IP para el emulador: " + emulatorNumber);
			return;
		}

		// Construir el comando para capturar el screenshot
		ProcessBuilder pb = new ProcessBuilder(ADB_PATH, "-s", adbIp, "shell", "input", "keyevent", "KEYCODE_BACK");
		pb.redirectErrorStream(true);

		try {
			Process process = pb.start();

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println("Botón de retroceso presionado.");
			} else {
				System.err.println("Error al ejecutar el comando, exit code: " + exitCode);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Obtiene el estado del emulador.
	 * 
	 * @return Estado del emulador como un String.
	 */
	public boolean getPlayerState(String emulatorNumber) {
		String playerState = executeCommand(MUMU_PATH + " api -v " + emulatorNumber + " player_state");
		if (playerState.contains("state=start_finished")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Inicia el emulador si no está corriendo.
	 */
	public void launchPlayer(String emulatorNumber) {
		executeCommand(MUMU_PATH + " api -v " + emulatorNumber + " launch_player");
	}

	public void closePlayer(String emulatorNumber) {
		executeCommand(MUMU_PATH + " api -v " + emulatorNumber + " shutdown_player");
	}

	public boolean isWhiteoutSurvivalInstalled(String emulatorNumber) {
		String command = MUMU_PATH + " api -v " + emulatorNumber + " get_installed_apps";
		StringBuilder output = new StringBuilder();

		try {
			ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();

			// Leer la salida del comando
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
			process.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Analizar la salida y buscar el paquete "com.gof.global"
		return containsPackage(output.toString(), WHITEOUT_PACKAGE);
	}

	/**
	 * Verifica si un paquete específico está en la salida JSON.
	 * 
	 * @param output      Salida del comando.
	 * @param packageName Nombre del paquete a buscar.
	 * @return `true` si el paquete está presente, `false` en caso contrario.
	 */
	private boolean containsPackage(String output, String packageName) {
		Pattern pattern = Pattern.compile("\"packageName\"\\s*:\\s*\"" + packageName + "\"");
		Matcher matcher = pattern.matcher(output);
		return matcher.find();
	}

	/**
	 * Ejecuta un comando en la terminal y devuelve la salida como un String.
	 * 
	 * @param command Comando a ejecutar.
	 * @return Salida del comando.
	 */
	private String executeCommand(String command) {
		StringBuilder output = new StringBuilder();

		try {
			ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();

			// Leer la salida del comando
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}

			process.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return output.toString();
	}

	public void launchGame(String emulatorNumber) {
		String command = MUMU_PATH + " api -v " + emulatorNumber + " close_app " + WHITEOUT_PACKAGE;

		try {
			ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();

			// Leer la salida del comando
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line); // Mostrar salida en consola
			}

			process.waitFor();
			System.out.println("✅ Whiteout Survival ha sido iniciado en MuMu.");

		} catch (Exception e) {
			System.out.println("❌ Error al iniciar Whiteout Survival.");
			e.printStackTrace();
		}

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		command = MUMU_PATH + " api -v " + emulatorNumber + " launch_app " + WHITEOUT_PACKAGE;

		try {
			ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();

			// Leer la salida del comando
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line); // Mostrar salida en consola
			}

			process.waitFor();
			System.out.println("✅ Whiteout Survival ha sido iniciado en MuMu.");

		} catch (Exception e) {
			System.out.println("❌ Error al iniciar Whiteout Survival.");
			e.printStackTrace();
		}

	}

	public void closeGame(String emulatorNumber) {
		String command = MUMU_PATH + " api -v " + emulatorNumber + " close_app " + WHITEOUT_PACKAGE;

		try {
			ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();

			// Leer la salida del comando
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line); // Mostrar salida en consola
			}

			process.waitFor();
			System.out.println("✅ Whiteout Survival ha sido iniciado en MuMu.");

		} catch (Exception e) {
			System.out.println("❌ Error al iniciar Whiteout Survival.");
			e.printStackTrace();
		}
	}

	public String connectADB(String emulatorNumber) {
		StringBuilder output = new StringBuilder();

		try {
			// Se construye el comando: adb connect <deviceAddress>
			ProcessBuilder processBuilder = new ProcessBuilder(ADB_PATH, "connect", getAdbIp(emulatorNumber));
			// Combina la salida estándar y de error
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();

			// Se lee la salida del comando
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append(System.lineSeparator());
			}

			// Espera a que el proceso finalice
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				System.err.println("El comando adb connect finalizó con el código de error: " + exitCode);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		return output.toString();
	}

	public String getAdbIp(String emulatorNumber) {
		String command = MUMU_PATH + " adb -v " + emulatorNumber;

		try {
			ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains(":")) {
					process.waitFor();
					return line.trim();
				}
			}

			process.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null; // Retornar null si no se encontró la IP
	}

	/**
	 * Realiza un tap o clic en la posición especificada en el dispositivo/emulador vía ADB.
	 *
	 * @param point Objeto DTOPoint que contiene las coordenadas (x,y) del tap.
	 * @return true si el comando se ejecutó correctamente, false en caso de error.
	 */
	public boolean tapAtPoint(String emulatorNumber, DTOPoint point) {
		if (point == null) {
			System.err.println("El DTOPoint es null.");
			return false;
		}

		// Obtener la IP del dispositivo usando el método getAdbIp shell input keyevent KEYCODE_BACK
		String adbIp = getAdbIp(emulatorNumber);
		if (adbIp == null) {
			System.err.println("No se pudo obtener la IP para el emulador: " + emulatorNumber);
			return false;
		}

		// Convertir las coordenadas a enteros (o a String) para el comando ADB.
		String xStr = String.valueOf(Math.round(point.getX()));
		String yStr = String.valueOf(Math.round(point.getY()));

		// Comando: adb shell input tap <x> <y>
		ProcessBuilder pb = new ProcessBuilder(ADB_PATH, "-s", adbIp, "shell", "input", "tap", xStr, yStr);
		pb.redirectErrorStream(true);

		try {
			System.out.println("Ejecutando tap en (" + xStr + ", " + yStr + ")...");
			Process process = pb.start();

			// Leer la salida del comando (opcional, para depuración)
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
			}

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println("Tap ejecutado correctamente en (" + xStr + ", " + yStr + ").");
				return true;
			} else {
				System.err.println("Error al ejecutar el tap. Código de salida: " + exitCode);
				return false;
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean tapAtRandomPoint(String emulatorNumber, DTOPoint point1, DTOPoint point2) {
		if (point1 == null || point2 == null) {
			System.err.println("Alguno de los DTOPoint es null.");
			return false;
		}

		// Obtener la IP del dispositivo usando el método getAdbIp
		String adbIp = getAdbIp(emulatorNumber);
		if (adbIp == null) {
			System.err.println("No se pudo obtener la IP para el emulador: " + emulatorNumber);
			return false;
		}

		// Determinar los límites mínimo y máximo para X e Y.
		int minX = (int) Math.round(Math.min(point1.getX(), point2.getX()));
		int maxX = (int) Math.round(Math.max(point1.getX(), point2.getX()));
		int minY = (int) Math.round(Math.min(point1.getY(), point2.getY()));
		int maxY = (int) Math.round(Math.max(point1.getY(), point2.getY()));

		// Generar coordenadas aleatorias entre los límites
		Random random = new Random();
		int randomX = minX + random.nextInt(maxX - minX + 1);
		int randomY = minY + random.nextInt(maxY - minY + 1);

		// Convertir las coordenadas a String para el comando ADB.
		String xStr = String.valueOf(randomX);
		String yStr = String.valueOf(randomY);

		// Comando: adb -s <adbIp> shell input tap <x> <y>
		ProcessBuilder pb = new ProcessBuilder(ADB_PATH, "-s", adbIp, "shell", "input", "tap", xStr, yStr);
		pb.redirectErrorStream(true);

		try {
			System.out.println("Ejecutando tap aleatorio en (" + xStr + ", " + yStr + ")...");
			Process process = pb.start();

			// Leer la salida del comando (opcional, para depuración)
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
			}

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println("Tap ejecutado correctamente en (" + xStr + ", " + yStr + ").");
				return true;
			} else {
				System.err.println("Error al ejecutar el tap. Código de salida: " + exitCode);
				return false;
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean tapAtRandomPoint(String emulatorNumber, DTOPoint point1, DTOPoint point2, int tapCount, int delayMs) {
		if (point1 == null || point2 == null) {
			System.err.println("Alguno de los DTOPoint es null.");
			return false;
		}

		String adbIp = getAdbIp(emulatorNumber);
		if (adbIp == null) {
			System.err.println("No se pudo obtener la IP para el emulador: " + emulatorNumber);
			return false;
		}

		int minX = (int) Math.round(Math.min(point1.getX(), point2.getX()));
		int maxX = (int) Math.round(Math.max(point1.getX(), point2.getX()));
		int minY = (int) Math.round(Math.min(point1.getY(), point2.getY()));
		int maxY = (int) Math.round(Math.max(point1.getY(), point2.getY()));

		Random random = new Random();

		for (int i = 0; i < tapCount; i++) {
			int randomX = minX + random.nextInt(maxX - minX + 1);
			int randomY = minY + random.nextInt(maxY - minY + 1);

			String xStr = String.valueOf(randomX);
			String yStr = String.valueOf(randomY);

			ProcessBuilder pb = new ProcessBuilder(ADB_PATH, "-s", adbIp, "shell", "input", "tap", xStr, yStr);
			pb.redirectErrorStream(true);

			try {
				System.out.println("Ejecutando tap aleatorio en (" + xStr + ", " + yStr + ")...");
				Process process = pb.start();

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						System.out.println(line);
					}
				}

				int exitCode = process.waitFor();
				if (exitCode != 0) {
					System.err.println("Error al ejecutar el tap. Código de salida: " + exitCode);
					return false;
				}

				if (i < tapCount - 1) {
					Thread.sleep(delayMs);
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

	public String ocrRegionText(String emulatorNumbrer, DTOPoint p1, DTOPoint p2) throws IOException, TesseractException {
		// Cargar la imagen desde el InputStream
		BufferedImage image = ImageIO.read(captureScreenshotViaADB(emulatorNumbrer));
		if (image == null) {
			throw new IOException("No se pudo cargar la imagen desde el InputStream.");
		}

		// Calcular la región a extraer
		int x = (int) Math.min(p1.getX(), p2.getX());
		int y = (int) Math.min(p1.getY(), p2.getY());
		int width = (int) Math.abs(p1.getX() - p2.getX());
		int height = (int) Math.abs(p1.getY() - p2.getY());

		// Validar que la región no se salga de los límites de la imagen
		if (x + width > image.getWidth() || y + height > image.getHeight()) {
			throw new IllegalArgumentException("La región especificada se sale de los límites de la imagen.");
		}

		// Extraer la subimagen (región de interés)
		BufferedImage subImage = image.getSubimage(x, y, width, height);

		// Configurar Tesseract
		Tesseract tesseract = new Tesseract();
		tesseract.setDatapath("tessdata"); // Ruta a los datos de entrenamiento de Tesseract
		tesseract.setLanguage("eng"); // Cambia a "spa" si necesitas OCR en español

		// Ejecutar OCR sobre la subimagen y devolver el texto extraído
		return tesseract.doOCR(subImage);
	}

	public void ejecutarZoom(String emulatorNumber) {
		// Obtener la IP del dispositivo usando el método getAdbIp
		String adbIp = getAdbIp(emulatorNumber);
		if (adbIp == null) {
			System.err.println("No se pudo obtener la IP para el emulador: " + emulatorNumber);
			return;
		}

		String devicePrefix = "-s " + adbIp + " ";

		// @formatter:off
        String[] commands = {
                "shell sendevent /dev/input/event4 1 29 1",
                "shell sendevent /dev/input/event4 0 0 4294967295",
                "shell sendevent /dev/input/event4 1 330 1",
                "shell sendevent /dev/input/event4 3 47 10",
                "shell sendevent /dev/input/event4 3 57 10",
                "shell sendevent /dev/input/event4 3 53 367",
                "shell sendevent /dev/input/event4 3 54 450",
                "shell sendevent /dev/input/event4 3 47 11",
                "shell sendevent /dev/input/event4 3 57 11",
                "shell sendevent /dev/input/event4 3 53 367",
                "shell sendevent /dev/input/event4 3 54 899",
                "shell sendevent /dev/input/event4 0 0 4294967295",
                "shell sendevent /dev/input/event4 1 330 1",
                "shell sendevent /dev/input/event4 3 47 10",
                "shell sendevent /dev/input/event4 3 54 458",
                "shell sendevent /dev/input/event4 3 47 11",
                "shell sendevent /dev/input/event4 3 54 891",
                "shell sendevent /dev/input/event4 0 0 4294967295",
                "shell sendevent /dev/input/event4 1 330 1",
                "shell sendevent /dev/input/event4 3 47 10",
                "shell sendevent /dev/input/event4 3 54 466",
                "shell sendevent /dev/input/event4 3 47 11",
                "shell sendevent /dev/input/event4 3 54 883",
                "shell sendevent /dev/input/event4 0 0 4294967295",
                "shell sendevent /dev/input/event4 1 330 1",
                "shell sendevent /dev/input/event4 3 47 10",
                "shell sendevent /dev/input/event4 3 54 474",
                "shell sendevent /dev/input/event4 3 47 11",
                "shell sendevent /dev/input/event4 3 54 875",
                "shell sendevent /dev/input/event4 0 0 4294967295",
                "shell sendevent /dev/input/event4 1 330 0",
                "shell sendevent /dev/input/event4 3 47 10",
                "shell sendevent /dev/input/event4 3 57 50000",
                "shell sendevent /dev/input/event4 3 47 11",
                "shell sendevent /dev/input/event4 3 57 50000",
                "shell sendevent /dev/input/event4 0 0 4294967295",
                "shell sendevent /dev/input/event4 1 29 0",
                "shell sendevent /dev/input/event4 0 0 4294967295"
            };
        // @formatter:on

		for (int i = 0; i < 5; i++) {
			for (String command : commands) {
				executeCommand(ADB_PATH + " " + devicePrefix + command);
			}
		}
	}

	public void executeSwipe(String emulatorNumber, DTOPoint startPoint, DTOPoint endPoint) {
		// Obtener la IP del dispositivo usando el método getAdbIp
		String adbIp = getAdbIp(emulatorNumber);
		if (adbIp == null) {
			System.err.println("No se pudo obtener la IP para el emulador: " + emulatorNumber);
			return;
		}

		String devicePrefix = "-s " + adbIp + " ";

		// Simula un gesto de swipe desde el punto de inicio al punto de fin
		executeCommand(ADB_PATH + " " + devicePrefix + "shell input swipe " + startPoint.getX() + " " + startPoint.getY() + " " + endPoint.getX() + " " + endPoint.getY());

		System.out.println("Swipe ejecutado correctamente en el dispositivo: " + adbIp);
	}

}
