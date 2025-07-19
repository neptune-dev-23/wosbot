package cl.camodev.wosbot.emulator.impl;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.imageio.ImageIO;



import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.NullOutputReceiver;
import com.android.ddmlib.RawImage;
import com.android.ddmlib.TimeoutException;

import cl.camodev.wosbot.emulator.Emulator;
import cl.camodev.wosbot.ot.DTOPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MEmuEmulator extends Emulator {
	private static final Logger logger = LoggerFactory.getLogger(MEmuEmulator.class);
	private static final int MAX_RETRIES = 10;
	private static final int RETRY_DELAY_MS = 5000;
	private static final int INIT_LOOPS = 10;
	private static final int INIT_DELAY_MS = 500;

	private AndroidDebugBridge bridge = null;

	public MEmuEmulator(String consolePath) {
		super(consolePath);
		if (bridge == null) {
			AndroidDebugBridge.disconnectBridge(5000, TimeUnit.MILLISECONDS);
			AndroidDebugBridge.terminate();
			AndroidDebugBridge.init(false);
			bridge = AndroidDebugBridge.createBridge(consolePath + File.separator + "adb.exe", true, 5000,
					TimeUnit.MILLISECONDS);
		}

	}

	@Override
	public void restartAdb() {
		AndroidDebugBridge.disconnectBridge(5000, TimeUnit.MILLISECONDS);
		AndroidDebugBridge.terminate();
		// Inicializa ddmlib
		AndroidDebugBridge.init(false);
		// Fuerza un servidor limpio
		bridge = AndroidDebugBridge.createBridge(consolePath + File.separator + "adb.exe", true, 5000,
				TimeUnit.MILLISECONDS);
	}

	 private void waitForBridge() throws InterruptedException {
	        int loops = 0;
	        while ((bridge == null || !bridge.hasInitialDeviceList()) && loops < INIT_LOOPS) {
	            Thread.sleep(INIT_DELAY_MS);
	            loops++;
	        }
	    }

	    private IDevice findDevice(String emulatorNumber) throws InterruptedException {
	        waitForBridge();
	        String serial = "127.0.0.1:" + (21503 + Integer.parseInt(emulatorNumber) * 10);
	        for (IDevice d : bridge.getDevices()) {
	            if (serial.equals(d.getSerialNumber())) {
	                return d;
	            }
	        }
	        return null;
	    }

	    private <T> T withRetries(String emulatorNumber,
                              Function<IDevice, T> action,
                              String actionName) {
	        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
	            try {
	                IDevice device = findDevice(emulatorNumber);
	                if (device == null) {
	                    logger.error("❌ Device not found for " + actionName + ": " + emulatorNumber);
	                    restartAdb();
	                    Thread.sleep(RETRY_DELAY_MS);
	                    continue;
	                }
	                return action.apply(device);
	            } catch (Exception e) {
	                logger.warn("⚠️ Attempt " + attempt + " of " + actionName + " failed: " + e.getMessage(), e);
	                try {
	                    restartAdb();
	                    Thread.sleep(RETRY_DELAY_MS);
	                } catch (InterruptedException ie) {
	                    Thread.currentThread().interrupt();
	                }
	            }
	        }
	        logger.error("❌ All attempts for " + actionName + " failed on " + emulatorNumber);
	        throw new RuntimeException("❌ All attempts for " + actionName + " failed on " + emulatorNumber);
	    }

	    /**
	     * Captura la pantalla del emulador y devuelve un arreglo de bytes PNG.
	     */
	    public byte[] captureScreenshot(String emulatorNumber, String unusedCommand) {
	        return withRetries(emulatorNumber, device -> {
	            try {
	                RawImage raw = device.getScreenshot();
	                if (raw == null) {
	                    throw new RuntimeException("RawImage es null");
	                }
	                BufferedImage img = convertRawImageToBufferedImage(raw);
	                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
	                    ImageIO.write(img, "png", baos);
	                    return baos.toByteArray();
	                }
	            } catch (IOException ioe) {
	                throw new UncheckedIOException(ioe);
	            } catch (TimeoutException e) {
					e.printStackTrace();
				} catch (AdbCommandRejectedException e) {
					e.printStackTrace();
				}
	            return null;
	        }, "captureScreenshot");
	    }

	    /**
	     * Envía múltiple taps aleatorios dentro del rectángulo definido por p1/p2.
	     */
	    public boolean tapAtRandomPoint(String emulatorNumber,
                                    DTOPoint p1,
                                    DTOPoint p2,
                                    int tapCount,
                                    int delayMs) {
	        return withRetries(emulatorNumber, device -> {
	            Random rnd = new Random();
	            int minX = Math.min(p1.getX(), p2.getX()), maxX = Math.max(p1.getX(), p2.getX());
	            int minY = Math.min(p1.getY(), p2.getY()), maxY = Math.max(p1.getY(), p2.getY());
	            for (int i = 1; i <= tapCount; i++) {
	                int x = minX + rnd.nextInt(maxX - minX + 1);
	                int y = minY + rnd.nextInt(maxY - minY + 1);
	                try {
	                    device.executeShellCommand("input tap " + x + " " + y, new NullOutputReceiver());
	                    logger.info("✅ Tap " + i + "/" + tapCount + " sent to (" + x + "," + y + ")");
	                    if (i < tapCount) Thread.sleep(delayMs);
	                } catch (Exception ex) {
	                    throw new RuntimeException(ex);
	                }
	            }
	            return Boolean.TRUE;
	        }, "tapAtRandomPoint x" + tapCount);
	    }

	    /**
	     * Envía un solo tap aleatorio dentro del rectángulo definido por p1/p2.
	     */
	    public boolean tapAtRandomPoint(String emulatorNumber, DTOPoint p1, DTOPoint p2) {
	        return tapAtRandomPoint(emulatorNumber, p1, p2, 1, 0);
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

	@Override
	protected String[] buildAdbCommand(String emulatorNumber, String command) {
		return new String[] { consolePath + File.separator + "memuc", "-i", emulatorNumber, "adb", command };
	}

	@Override
	public void launchEmulator(String emulatorNumber) {
		String[] command = { consolePath + File.separator + "memuc", "start", "-i", emulatorNumber };
		executeCommand(command);
		logger.info("🚀 MEmu launched at index " + emulatorNumber);
	}

	@Override
	public void closeEmulator(String emulatorNumber) {
		String[] command = { consolePath + File.separator + "memuc", "stop", "-i", emulatorNumber };
		executeCommand(command);
		logger.info("🛑 MEmu closed at index " + emulatorNumber);
	}

	private void executeCommand(String[] command) {
		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(new File(consolePath).getParentFile());
			Process process = pb.start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			logger.error("Error executing command", e);
		}
	}

	@Override
	public boolean isRunning(String emulatorNumber) {
		try {
			String[] command = { consolePath + File.separator + "memuc", "isvmrunning", "-i", emulatorNumber };
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(new File(consolePath).getParentFile());

			Process process = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				if (!line.equals("Not Running")) {
					return true;
				}
			}

			process.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean isPackageRunning(String emulatorNumber, String packageName) {
		try {
			String com = "shell dumpsys activity activities | grep mResumedActivity";
			String[] command = { consolePath + File.separator + "memuc", "-i", emulatorNumber, "adb", com };
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
