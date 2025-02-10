package cl.camodev.wosbot.console.enumerable;

public enum EnumTemplates {

	// @formatter:off
	GAME_HOME_FURNACE(""),
	GAME_HOME_WORLD(""),
	NOMADIC_MERCHANT_COAL("/templates/nomadicmerchant/coal.png"), 
	NOMADIC_MERCHANT_WOOD("/templates/nomadicmerchant/wood.png"), 
	NOMADIC_MERCHANT_MEAT("/templates/nomadicmerchant/meat.png"), 
	NOMADIC_MERCHANT_STONE("/templates/nomadicmerchant/stone.png");
	// @formatter:on

	private String template;

	private EnumTemplates(String template) {
		this.template = template;
	}

	public String getTemplate() {
		return template;
	}
}
