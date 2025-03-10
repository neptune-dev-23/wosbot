package cl.camodev.wosbot.emulator;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import cl.camodev.wosbot.ot.DTOPoint;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public abstract class Emulator {
	protected String consolePath;

	public Emulator(String consolePath) {
		this.consolePath = consolePath;
	}

	// 🔹 Método abstracto para que cada emulador defina su estructura de comando ADB
	protected abstract String[] buildAdbCommand(String emulatorNumber, String command);

	// 🔹 Método para lanzar el emulador
	public abstract void launchEmulator(String emulatorNumber);

	// 🔹 Método para cerrar el emulador
	public abstract void closeEmulator(String emulatorNumber);

	// 🔹 Método abstracto para verificar si el emulador está en ejecución
	public abstract boolean isRunning(String emulatorNumber);

	// 🔹 Ejecuta un comando ADB sin salida
	protected void executeAdbCommand(String emulatorNumber, String command) {
		int maxRetries = 10;
		int retryDelay = 3000;
		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				String[] fullCommand = buildAdbCommand(emulatorNumber, command);

				ProcessBuilder pb = new ProcessBuilder(fullCommand);
				pb.directory(new File(consolePath).getParentFile());

				Process process = pb.start();
				int exitCode = process.waitFor();

				if (exitCode == 0) {
					System.out.println("✅ Comando ejecutado con éxito: " + command);
					return;
				} else {
					System.err.println("❌ Error ejecutando el comando, código de salida: " + exitCode);
					restartAdb();
					Thread.sleep(retryDelay);
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// 🔹 Ejecuta un comando ADB con salida de texto
	protected String executeAdbCommandWithOutput(String emulatorNumber, String command) {
		int maxRetries = 10;
		int retryDelay = 3000;

		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				String[] fullCommand = buildAdbCommand(emulatorNumber, command);

				ProcessBuilder pb = new ProcessBuilder(fullCommand);
				pb.directory(new File(consolePath)); // Establecemos el directorio donde está adb.exe
				pb.redirectErrorStream(true); // Redirigir errores a la salida estándar

				Process process = pb.start();

				// Capturar salida en un hilo separado para evitar bloqueos
				StringBuilder output = new StringBuilder();
				Thread outputReader = new Thread(() -> {
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
						String line;
						while ((line = reader.readLine()) != null) {
							output.append(line).append("\n");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
				outputReader.start();

				int exitCode = process.waitFor();
				outputReader.join(); // Asegurar que hemos leído toda la salida antes de continuar

				String result = output.toString().trim();

				// 📌 Verificar si ADB está en estado offline y reiniciarlo
				if (result.contains("device offline") || result.isEmpty()) {
					System.err.println("⚠ Dispositivo en estado OFFLINE o sin respuesta. Reiniciando ADB interno de MuMu...");
					restartAdb();
					Thread.sleep(2000); // Esperar para que el servidor ADB se recupere
					continue; // Reintentar el comando
				}

				if (exitCode == 0 && !result.isEmpty()) {
					return result;
				} else {
					System.out.println("🔄 Intento " + attempt + " - No se obtuvo una salida válida, reintentando...");
					Thread.sleep(retryDelay);
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.err.println("❌ No se obtuvo una salida válida después de " + maxRetries + " intentos.");
		return null;
	}

	protected byte[] captureScreenshot(String emulatorNumber, String command) {
		int maxRetries = 10;
		int retryDelay = 3000;

		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				String[] fullCommand = buildAdbCommand(emulatorNumber, command);

				ProcessBuilder pb = new ProcessBuilder(fullCommand);
				pb.directory(new File(consolePath)); // Establecemos el directorio donde está adb.exe
				pb.redirectErrorStream(true); // Redirigir errores a la salida estándar

				Process process = pb.start();

				// Capturar salida en un hilo separado para evitar bloqueos

				AtomicReference<byte[]> outputBytesRef = new AtomicReference<>();
				Thread outputReader = new Thread(() -> {
					try (InputStream inputStream = process.getInputStream(); ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

						byte[] buffer = new byte[4096];
						int bytesRead;
						boolean pngStartFound = false;

						while ((bytesRead = inputStream.read(buffer)) != -1) {
							String output = new String(buffer, 0, bytesRead);
//							System.out.print(output);
							if (!pngStartFound) {
								// Buscar la firma PNG en los datos leídos
								int pngIndex = findPNGHeader(buffer, bytesRead);
								if (pngIndex != -1) {
									pngStartFound = true;
									// Guardar solo los datos a partir del inicio del PNG
									byteArrayOutputStream.write(buffer, pngIndex, bytesRead - pngIndex);
								}
							} else {
								// Si ya encontramos el PNG, seguir guardando todos los bytes
								byteArrayOutputStream.write(buffer, 0, bytesRead);
							}
						}

						// Guardar los bytes en outputBytesRef
						outputBytesRef.set(byteArrayOutputStream.toByteArray());

					} catch (IOException e) {
						e.printStackTrace();
					}
				});

				outputReader.start();

				int exitCode = process.waitFor();
				outputReader.join();

				byte[] outputBytes = outputBytesRef.get();
				if (exitCode == 0 && (outputBytes != null)) {
					return outputBytes;
				} else {
					System.out.println("🔄 Intento " + attempt + " - No se obtuvo una salida válida, reintentando...");
					restartAdb();
					Thread.sleep(retryDelay);
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.err.println("❌ No se obtuvo una salida válida después de " + maxRetries + " intentos.");
		return null;
	}

	private int findPNGHeader(byte[] buffer, int length) {
		byte[] pngSignature = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

		for (int i = 0; i <= length - pngSignature.length; i++) {
			boolean match = true;
			for (int j = 0; j < pngSignature.length; j++) {
				if (buffer[i + j] != pngSignature[j]) {
					match = false;
					break;
				}
			}
			if (match) {
				return i; // Retorna la posición donde empieza el PNG
			}
		}
		return -1; // No encontrado
	}

	public void restartAdb() {

		try {
			System.out.println("🔄 Reiniciando ADB del emulador...");
			String adbPath = consolePath + File.separator + "adb.exe";
			// Ejecutar "adb kill-server"
			ProcessBuilder killServer = new ProcessBuilder(adbPath, "kill-server");
			killServer.redirectErrorStream(true);
			Process killProcess = killServer.start();
			killProcess.waitFor();

			// Ejecutar "adb start-server"
			ProcessBuilder startServer = new ProcessBuilder(adbPath, "start-server");
			startServer.redirectErrorStream(true);
			Process startProcess = startServer.start();
			startProcess.waitFor();

			System.out.println("✅ ADB reiniciado con éxito.");
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			System.err.println("❌ Error al reiniciar el ADB interno de MuMu.");
		}

	}

	// 🔹 Captura de pantalla y devuelve un `ByteArrayInputStream`
	public byte[] captureScreenshot(String emulatorNumber) {
		String command = "exec-out screencap -p";
		byte[] imageBytes = captureScreenshot(emulatorNumber, command);

		if (imageBytes != null) {
			try {
				return imageBytes;
			} catch (IllegalArgumentException e) {
				System.err.println("❌ Error al decodificar la imagen.");
			}
		}
		return null;
	}

	public String ocrRegionText(String emulatorNumber, DTOPoint p1, DTOPoint p2) throws IOException, TesseractException {
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(captureScreenshot(emulatorNumber)));
		if (image == null)
			throw new IOException("No se pudo capturar la imagen.");

		int x = (int) Math.min(p1.getX(), p2.getX());
		int y = (int) Math.min(p1.getY(), p2.getY());
		int width = (int) Math.abs(p1.getX() - p2.getX());
		int height = (int) Math.abs(p1.getY() - p2.getY());

		BufferedImage subImage = image.getSubimage(x, y, width, height);
		Tesseract tesseract = new Tesseract();
		tesseract.setDatapath("tessdata");
		tesseract.setLanguage("eng");

		return tesseract.doOCR(subImage);
	}

	// 🔹 Tap aleatorio dentro de un área definida por dos puntos
	public boolean tapAtRandomPoint(String emulatorNumber, DTOPoint point1, DTOPoint point2) {
		if (point1 == null || point2 == null) {
			System.err.println("Alguno de los DTOPoint es null.");
			return false;
		}

		// Determinar los límites mínimo y máximo para X e Y
		int minX = (int) Math.round(Math.min(point1.getX(), point2.getX()));
		int maxX = (int) Math.round(Math.max(point1.getX(), point2.getX()));
		int minY = (int) Math.round(Math.min(point1.getY(), point2.getY()));
		int maxY = (int) Math.round(Math.max(point1.getY(), point2.getY()));

		// Generar coordenadas aleatorias dentro de los límites
		Random random = new Random();
		int randomX = minX + random.nextInt(maxX - minX + 1);
		int randomY = minY + random.nextInt(maxY - minY + 1);

		// Ejecutar comando ADB
		String command = String.format("shell input tap %d %d", randomX, randomY);
		executeAdbCommand(emulatorNumber, command);

		System.out.println("✅ Tap aleatorio en (" + randomX + ", " + randomY + ")");
		return true;
	}

	// 🔹 Tap aleatorio con múltiples repeticiones y delay
	public boolean tapAtRandomPoint(String emulatorNumber, DTOPoint point1, DTOPoint point2, int tapCount, int delayMs) {
		if (point1 == null || point2 == null) {
			System.err.println("Alguno de los DTOPoint es null.");
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

			String command = String.format("shell input tap %d %d", randomX, randomY);
			executeAdbCommand(emulatorNumber, command);

			System.out.println("✅ Tap aleatorio en (" + randomX + ", " + randomY + ")");

			// Esperar antes de la siguiente repetición
			if (i < tapCount - 1) {
				try {
					Thread.sleep(delayMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return false;
				}
			}
		}

		return true;
	}

	public void swipe(String emulatorNumber, DTOPoint point, DTOPoint point2) {
		String command = String.format("shell input swipe %d %d %d %d", point.getX(), point.getY(), point2.getX(), point2.getY());
		executeAdbCommand(emulatorNumber, command);
	}

	public void pressBackButton(String emulatorNumber) {
		String command = "shell input keyevent KEYCODE_BACK";
		executeAdbCommand(emulatorNumber, command);
		System.out.println("🔙 Botón de retroceso presionado en el emulador " + emulatorNumber);
	}

	// 🔹 Método para verificar si una app está instalada
	public boolean isAppInstalled(String emulatorNumber, String packageName) {
		String command = String.format("shell pm list packages | grep %s", packageName);
		String output = executeAdbCommandWithOutput(emulatorNumber, command);
		return output != null && !output.isEmpty();
	}

	public void launchApp(String emulatorNumber, String packageName) {
		String command = "shell monkey -p " + packageName + " -c android.intent.category.LAUNCHER 1";
		executeAdbCommand(emulatorNumber, command);
		System.out.println("📱 Aplicación " + packageName + " iniciada en el emulador " + emulatorNumber);
	}
}
