package cl.camodev.wosbot.launcher.view;

import cl.camodev.wosbot.serv.impl.ServScheduler;

public class LauncherActionController {

	public void startBot() {
		ServScheduler.getServices().startBot();
	}

	public void stopBot() {
		ServScheduler.getServices().stopBot();
	}

}
