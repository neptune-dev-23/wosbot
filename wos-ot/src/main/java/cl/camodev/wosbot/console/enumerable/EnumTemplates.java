package cl.camodev.wosbot.console.enumerable;

public enum EnumTemplates {

	// @formatter:off
	GAME_HOME_FURNACE("/templates/city.png"),
	GAME_HOME_WORLD("/templates/world.png"),
	LIFE_ESSENCE_MENU("/templates/essence/threeMenu.png"),
	NOMADIC_MERCHANT_COAL("/templates/nomadicmerchant/coal.png"), 
	NOMADIC_MERCHANT_WOOD("/templates/nomadicmerchant/wood.png"), 
	NOMADIC_MERCHANT_MEAT("/templates/nomadicmerchant/meat.png"), 
	NOMADIC_MERCHANT_STONE("/templates/nomadicmerchant/stone.png"),
	NOMADIC_MERCHANT_VIP("/templates/nomadicmerchant/vip.png"),
	NOMADIC_MERCHANT_REFRESH("/templates/nomadicmerchant/refresh.png"),
	EXPLORATION_CLAIM("/templates/exploration/claim.png"),
	HERO_RECRUIT_CLAIM("/templates/herorecruitment/freebutton.png"),;
	// @formatter:on

	private String template;

	private EnumTemplates(String template) {
		this.template = template;
	}

	public String getTemplate() {
		return template;
	}
}
