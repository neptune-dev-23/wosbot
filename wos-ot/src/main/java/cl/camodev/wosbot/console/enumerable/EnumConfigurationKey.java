package cl.camodev.wosbot.console.enumerable;

public enum EnumConfigurationKey {

	//@formatter:off
	BOOL_DEBUG("false", Boolean.class ), 
	BOOL_NOMADIC_MERCHANT("false", Boolean.class), 
	BOOL_NOMADIC_MERCHANT_VIP_POINTS("false", Boolean.class), 
	BOOL_WAR_ACADEMY_SHARDS("false", Boolean.class),
	BOOL_CRYSTAL_LAB_FC("false",Boolean.class),
	BOOL_EXPLORATION_CHEST("false",Boolean.class),
	BOOL_HERO_RECRUITMENT("false",Boolean.class);
	;
	//@formatter:on
	private final String defaultValue;
	private final Class<?> type;

	EnumConfigurationKey(String defaultValue, Class<?> type) {
		this.defaultValue = defaultValue;
		this.type = type;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public Class<?> getType() {
		return type;
	}

	/**
	 * Método que convierte un String al tipo definido en 'type'. Agrega conversiones según los tipos que necesites.
	 */
	@SuppressWarnings("unchecked")
	public <T> T castValue(String value) {
		if (type.equals(Boolean.class)) {
			return (T) Boolean.valueOf(value);
		} else if (type.equals(Integer.class)) {
			return (T) Integer.valueOf(value);
		} else if (type.equals(Double.class)) {
			return (T) Double.valueOf(value);
		}
		// Agrega otros if/else según los tipos soportados
		throw new UnsupportedOperationException("Tipo " + type.getSimpleName() + " no soportado");
	}
}
