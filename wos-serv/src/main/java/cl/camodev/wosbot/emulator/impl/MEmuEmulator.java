package cl.camodev.wosbot.emulator.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import cl.camodev.wosbot.emulator.Emulator;

public class MEmuEmulator extends Emulator {

	@Override
	protected byte[] captureScreenshot(String emulatorNumber, String command) {
		// TODO Auto-generated method stub
		return super.captureScreenshot(emulatorNumber, command);
	}

	public MEmuEmulator(String consolePath) {
		super(consolePath);
	}

	@Override
	protected String[] buildAdbCommand(String emulatorNumber, String command) {
		return new String[] { consolePath + File.separator + "memuc", "-i", emulatorNumber, "adb", command };
	}

	@Override
	public void launchEmulator(String emulatorNumber) {
		String[] command = { consolePath + File.separator + "memuc", "start", "-i", emulatorNumber };
		executeCommand(command);
		System.out.println("🚀 MEmu lanzado en el índice " + emulatorNumber);
	}

	@Override
	public void closeEmulator(String emulatorNumber) {
		String[] command = { consolePath + File.separator + "memuc", "stop", "-i", emulatorNumber };
		executeCommand(command);
		System.out.println("🛑 MEmu cerrado en el índice " + emulatorNumber);
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
