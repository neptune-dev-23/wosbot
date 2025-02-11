package cl.camodev.wosbot.emulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cl.camodev.utiles.ImageSearchUtil;
import cl.camodev.utiles.UtilOCR;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
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

	public void captureScrenshotViaADB(String emulatorNumber) {
		// Obtener la IP del dispositivo usando el método getAdbIp
		String adbIp = getAdbIp(emulatorNumber);
		if (adbIp == null) {
			System.err.println("No se pudo obtener la IP para el emulador: " + emulatorNumber);
			return;
		}

		// Construir el comando para capturar el screenshot
		ProcessBuilder pb = new ProcessBuilder(ADB_PATH, "-s", adbIp, "exec-out", "screencap", "-p");
		pb.redirectErrorStream(true);

		try {
			Process process = pb.start();

			// Definir el directorio y el nombre del archivo (por ejemplo, "temp/0.png")
			Path tempDir = Paths.get("temp"); // Esto crea una ruta relativa llamada "temp"
			if (!Files.exists(tempDir)) {
				Files.createDirectories(tempDir);
			}
			Path filePath = tempDir.resolve(emulatorNumber + ".png");

			// Leer la salida del comando (imagen en formato PNG) y guardarla en el archivo indicado
			try (InputStream is = process.getInputStream()) {
				Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
			}

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println("Screenshot guardado en " + filePath.toAbsolutePath());
			} else {
				System.err.println("Error al ejecutar el comando, exit code: " + exitCode);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public DTOImageSearchResult searchTemplate(String emulatorNumber, String templatePath, int x, int y, int width, int height, double threshold) {
		captureScrenshotViaADB(emulatorNumber);
		DTOImageSearchResult result = ImageSearchUtil.buscarTemplate("temp/" + emulatorNumber + ".png", templatePath, x, y, width, height, threshold);
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

			// Definir el directorio y el nombre del archivo (por ejemplo, "temp/0.png")
			Path tempDir = Paths.get("temp"); // Esto crea una ruta relativa llamada "temp"
			if (!Files.exists(tempDir)) {
				Files.createDirectories(tempDir);
			}
			Path filePath = tempDir.resolve(emulatorNumber + ".png");

			// Leer la salida del comando (imagen en formato PNG) y guardarla en el archivo indicado
			try (InputStream is = process.getInputStream()) {
				Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
			}

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println("Screenshot guardado en " + filePath.toAbsolutePath());
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

	public String ocrRegionText(String emulatorNumber, DTOPoint dtoPoint, DTOPoint dtoPoint2) {
		captureScrenshotViaADB(emulatorNumber);
		try {
			Path tempDir = Paths.get("temp"); // Esto crea una ruta relativa llamada "temp"
			if (!Files.exists(tempDir)) {
				Files.createDirectories(tempDir);
			}
			Path filePath = tempDir.resolve(emulatorNumber + ".png");

			return UtilOCR.ocrFromRegion(filePath.toAbsolutePath().toString(), dtoPoint, dtoPoint2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TesseractException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
