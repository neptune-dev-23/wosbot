package cl.camodev.wosbot.launcher.view;

import cl.camodev.wosbot.ot.DTOBotState;
import cl.camodev.wosbot.serv.IBotStateListener;
import cl.camodev.wosbot.serv.impl.ServScheduler;

public class LauncherActionController implements IBotStateListener {

	private LauncherLayoutController layoutController;

	public LauncherActionController(LauncherLayoutController launcherLayoutController) {
		this.layoutController = launcherLayoutController;
		ServScheduler.getServices().registryBotStateListener(this);
	}

	public void startBot() {
		ServScheduler.getServices().startBot();
	}

	public void stopBot() {
		ServScheduler.getServices().stopBot();
	}
	public void pauseBot() {
		ServScheduler.getServices().pauseBot();
	}

	public void resumeBot() {
		ServScheduler.getServices().resumeBot();
	}

	public void captureScreenshots() {

	}

	@Override
	public void onBotStateChange(DTOBotState botState) {
		layoutController.onBotStateChange(botState);

	}

}
