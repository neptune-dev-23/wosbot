package cl.camodev.wosbot.emulator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cl.camodev.utiles.ImageSearchUtil;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;

public class EmulatorManager {

	private static EmulatorManager instance;

	private static final String MUMU_PATH = "\"C:\\Program Files\\Netease\\MuMuPlayerGlobal-12.0\\shell\\MuMuManager.exe\"";
	private static final String ADB_PATH = "C:/Program Files/Netease/MuMuPlayerGlobal-12.0/shell/adb";
	private static final String WHITEOUT_PACKAGE = "com.gof.global";

	private final Map<String, Process> deviceProcesses = new HashMap<>();
	private final Map<String, BufferedWriter> deviceWriters = new HashMap<>();

	private EmulatorManager() {
		// Constructor privado para evitar instanciación
	}

	public static EmulatorManager getInstance() {
		if (instance == null) {
			instance = new EmulatorManager();
		}
		return instance;
	}

	/**
	 * Inicializa la conexión ADB para el emulador especificado.
	 * <p>
	 * Este método obtiene la IP del ADB para el emulador indicado mediante el método {@link #getAdbIp(String)} y, a continuación, establece una
	 * conexión ADB en modo shell utilizando el ejecutable ADB. La conexión se almacena internamente para su posterior uso.
	 * </p>
	 *
	 * @param emulatorNumber Número o identificador del emulador.
	 */
	public void initializeAdbConnection(String emulatorNumber) {
		// Se obtiene la IP del dispositivo asociada al ADB
		String adbIp = getAdbIp(emulatorNumber);
		if (adbIp == null) {
			System.err.println("No se pudo obtener la IP ADB para el emulador " + emulatorNumber);
			return;
		}

		// Se construye el comando para abrir una sesión shell del ADB con el dispositivo
		ProcessBuilder processBuilder = new ProcessBuilder(ADB_PATH, "-s", adbIp, "shell");
		try {
			Process process = processBuilder.start();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
			// Se almacena el proceso y su BufferedWriter usando el emulatorNumber como clave
			deviceProcesses.put(emulatorNumber, process);
			deviceWriters.put(emulatorNumber, writer);
			System.out.println("Conexión ADB inicializada para el emulador " + emulatorNumber + " (IP: " + adbIp + ")");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error al inicializar la conexión ADB para el emulador " + emulatorNumber);
		}
	}

	/**
	 * Cierra todas las conexiones ADB abiertas.
	 * <p>
	 * Este método recorre internamente las conexiones abiertas (almacenadas en los mapas) y cierra los BufferedWriters, así como destruye los
	 * procesos asociados.
	 * </p>
	 */
	public void closeAllAdbConnections() {
		// Cerrar todos los BufferedWriters asociados
		for (Map.Entry<String, BufferedWriter> entry : deviceWriters.entrySet()) {
			try {
				entry.getValue().close();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Error al cerrar la conexión ADB para el emulador " + entry.getKey());
			}
		}
		// Destruir todos los procesos ADB
		for (Map.Entry<String, Process> entry : deviceProcesses.entrySet()) {
			entry.getValue().destroy();
		}
		// Limpiar los mapas
		deviceWriters.clear();
		deviceProcesses.clear();
		System.out.println("Todas las conexiones ADB han sido cerradas.");
	}

	public DTOImageSearchResult searchTemplate(String emulatorNumber, String templatePath, int x, int y, int width, int height) {

		return ImageSearchUtil.buscarTemplate(emulatorNumber, templatePath, x, y, width, height, 90);

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

	/**
	 * Captura la pantalla del emulador y guarda la imagen en el directorio de ejecución (user.dir) dentro de una carpeta "temp". Se crea el
	 * directorio en caso de no existir.
	 *
	 * @param emulatorNumber Número o identificador del emulador (puede usarse para personalizar la ruta si es necesario)
	 * @return true si la operación se completó correctamente, false en caso de error.
	 */
	public static boolean captureScreenshot(String emulatorNumber) {
		// Comando para capturar la pantalla en el emulador y guardarla en el
		// dispositivo
		ProcessBuilder pbCapture = new ProcessBuilder(MUMU_PATH, "adb", "-v", "0", "shell", "screencap", "-p", "/sdcard/foto.png");
		// Redirige errores al stream de salida para poder verlos si ocurren
		pbCapture.redirectErrorStream(true);

		try {
			System.out.println("Ejecutando comando de captura...");
			Process processCapture = pbCapture.start();

			// Leer la salida (si hubiera mensajes)
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(processCapture.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
			}

			int exitCodeCapture = processCapture.waitFor();
			if (exitCodeCapture != 0) {
				System.err.println("Error en la captura de pantalla. Código de salida: " + exitCodeCapture);
				return false;
			}

			// Obtener el directorio de ejecución actual y construir la ruta de salida
			String currentDir = System.getProperty("user.dir");
			String outputDirPath = currentDir + File.separator + "temp";
			File outputDir = new File(outputDirPath);
			if (!outputDir.exists()) {
				if (outputDir.mkdirs()) {
					System.out.println("Directorio creado: " + outputDirPath);
				} else {
					System.err.println("No se pudo crear el directorio: " + outputDirPath);
					return false;
				}
			}
			String outputFilePath = outputDirPath + File.separator + "foto.png";

			// Comando para extraer el archivo del dispositivo/emulador al host
			// adb pull /sdcard/foto.png <directorio_actual>/temp/foto.png
			ProcessBuilder pbPull = new ProcessBuilder(ADB_PATH, "pull", "/sdcard/foto.png", outputFilePath);
			pbPull.redirectErrorStream(true);

			System.out.println("Extrayendo el archivo de imagen...");
			Process processPull = pbPull.start();

			// Leer la salida del comando pull
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(processPull.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
			}

			int exitCodePull = processPull.waitFor();
			if (exitCodePull != 0) {
				System.err.println("Error al transferir la imagen. Código de salida: " + exitCodePull);
				return false;
			}

			System.out.println("Captura de pantalla guardada exitosamente en: " + outputFilePath);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static String getAdbIp(String emulatorNumber) {
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
	public static boolean tapAtPoint(DTOPoint point) {
		if (point == null) {
			System.err.println("El DTOPoint es null.");
			return false;
		}

		// Convertir las coordenadas a enteros (o a String) para el comando ADB.
		String xStr = String.valueOf(Math.round(point.getX()));
		String yStr = String.valueOf(Math.round(point.getY()));

		// Comando: adb shell input tap <x> <y>
		ProcessBuilder pb = new ProcessBuilder(ADB_PATH, "shell", "input", "tap", xStr, yStr);
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

}
