package cl.camodev.wosbot.emulator.impl;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.RawImage;

import cl.camodev.wosbot.emulator.Emulator;

public class LDPlayerEmulator extends Emulator {

	private AndroidDebugBridge bridge = null;

	@Override
	protected byte[] captureScreenshot(String emulatorNumber, String command) {
		int maxRetries = 10;
		int retryDelay = 3000;

		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				// Inicializar ADB una sola vez
				if (bridge == null) {
					AndroidDebugBridge.init(false);
					bridge = AndroidDebugBridge.createBridge(consolePath + File.separator + "adb.exe", false, 5000, TimeUnit.MILLISECONDS);
				}

				// Esperar a que ADB cargue la lista de dispositivos
				int count = 0;
				while ((bridge == null || !bridge.hasInitialDeviceList()) && count < 10) {
					Thread.sleep(500);
					count++;
				}

				IDevice[] devices = bridge.getDevices();

				int targetPort = 5554 + (Integer.parseInt(emulatorNumber) * 2);
				String targetSerial = "emulator-" + targetPort;

				IDevice targetDevice = null;
				for (IDevice device : devices) {
					if (device.getSerialNumber().equals(targetSerial)) {
						targetDevice = device;
						break;
					}
				}

				if (targetDevice == null) {
					System.out.println("❌ No se encontró el dispositivo con serial: " + targetSerial);
					restartAdb();
					Thread.sleep(retryDelay);
					continue;
				}

				// Capturar pantalla
				RawImage rawImage = targetDevice.getScreenshot();

				if (rawImage == null) {
					System.out.println("⚠️ Captura fallida, intento " + attempt);
					restartAdb();
					Thread.sleep(retryDelay);
					continue;
				}

				BufferedImage image = convertRawImageToBufferedImage(rawImage);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "png", baos);
//				FileOutputStream fos = new FileOutputStream("screenshot.png");
//				fos.write(baos.toByteArray());
//				fos.close();
				return baos.toByteArray();

			} catch (Exception e) {
				e.printStackTrace();
				try {
					restartAdb();
					Thread.sleep(retryDelay);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		}

		System.err.println("❌ No se pudo obtener una captura después de " + maxRetries + " intentos.");
		return null;
	}

	private BufferedImage convertRawImageToBufferedImage(RawImage rawImage) {
		BufferedImage image = new BufferedImage(rawImage.width, rawImage.height, BufferedImage.TYPE_INT_ARGB);

		int index = 0;
		for (int y = 0; y < rawImage.height; y++) {
			for (int x = 0; x < rawImage.width; x++) {
				int offset = index * rawImage.bpp / 8;

				int r = getColorComponent(rawImage, offset, rawImage.red_offset);
				int g = getColorComponent(rawImage, offset, rawImage.green_offset);
				int b = getColorComponent(rawImage, offset, rawImage.blue_offset);
				int a = rawImage.alpha_offset != -1 ? getColorComponent(rawImage, offset, rawImage.alpha_offset) : 255;

				int argb = (a << 24) | (r << 16) | (g << 8) | b;

				image.setRGB(x, y, argb);
				index++;
			}
		}

		return image;
	}

	private int getColorComponent(RawImage rawImage, int baseOffset, int bitOffset) {
		if (bitOffset == -1)
			return 0;
		int byteOffset = bitOffset / 8;
		return rawImage.data[baseOffset + byteOffset] & 0xFF;
	}

	private int findPNGHeader(byte[] buffer, int length) {
		byte[] pngSignature = { (byte) 0x89, 'P', 'N', 'G' };

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

	public LDPlayerEmulator(String consolePath) {
		super(consolePath);
	}

	@Override
	protected String[] buildAdbCommand(String emulatorNumber, String command) {
		return new String[] { consolePath + File.separator + "ldconsole.exe", "adb", "--index", emulatorNumber, "--command", command };
	}

	@Override
	public void launchEmulator(String emulatorNumber) {
		String[] command = { consolePath + File.separator + "ldconsole.exe", "launch", "--index", emulatorNumber };
		executeCommand(command);
		System.out.println("🚀 LDPlayer lanzado en el índice " + emulatorNumber);

	}

	@Override
	public void closeEmulator(String emulatorNumber) {
		String[] command = { consolePath + File.separator + "ldconsole.exe", "quit", "--index", emulatorNumber };
		executeCommand(command);
		System.out.println("🛑 LDPlayer cerrado en el índice " + emulatorNumber);

	}

	private void executeCommand(String[] command) {
		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(new File(consolePath).getParentFile());
			Process process = pb.start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isRunning(String emulatorNumber) {
		try {
			String[] command = { consolePath + File.separator + "ldconsole.exe", "isrunning", "--index", emulatorNumber };
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(new File(consolePath).getParentFile());

			Process process = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = reader.readLine();

			process.waitFor();

			return line != null && line.trim().equalsIgnoreCase("running");
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean isPackageRunning(String emulatorNumber, String packageName) {
		try {
			String com = "shell dumpsys activity activities | grep mResumedActivity";
			String[] command = { consolePath + File.separator + "ldconsole.exe", "adb", "--index", emulatorNumber, "--command", com };
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(new File(consolePath).getParentFile());

			Process process = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				if (line.contains(packageName)) {
					return true;
				}
			}

			process.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}
}
