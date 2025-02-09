package cl.camodev.wosbot.console.enumerable;

public enum EnumConfigurationKey {

	BOOL_NOMADIC_MERCHANT("false"), //
	BOOL_NOMADIC_MERCHANT_VIP_POINTS("false");//

	private final String defaultValue;

	EnumConfigurationKey(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

}
