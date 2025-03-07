package cl.camodev.wosbot.emulator;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;

public enum EmulatorType {
	//@formatter:off
	MUMU("MuMuPlayer", EnumConfigurationKey.MUMU_PATH_STRING, "C:\\Program Files\\Netease\\MuMuPlayerGlobal-12.0\\shell\\MuMuManager.exe"), 
	LDPLAYER("LDPlayer", EnumConfigurationKey.LDPLAYER_PATH_STRING, "C:\\LDPlayer\\LDPlayer9\\ldconsole.exe");
	//@formatter:on

	private final String displayName;
	private final EnumConfigurationKey configKey;
	private final String defaultPath;

	EmulatorType(String displayName, EnumConfigurationKey configKey, String defaultPath) {
		this.displayName = displayName;
		this.configKey = configKey;
		this.defaultPath = defaultPath;
	}

	public String getDisplayName() {
		return displayName;
	}

	public EnumConfigurationKey getConfigKey() {
		return configKey;
	}

	public String getDefaultPath() {
		return defaultPath;
	}
}
