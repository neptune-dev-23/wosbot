package cl.camodev.wosbot.console.enumerable;

public enum EnumTemplates {

	// @formatter:off
	GAME_HOME_FURNACE("/templates/city.png"),
	GAME_HOME_WORLD("/templates/world.png"),
	GAME_HOME_PETS("/templates/home/petsButton.png"),
	
	GAME_HOME_SHORTCUTS_INFANTRY("/templates/shortcuts/infantry.png"),
	GAME_HOME_SHORTCUTS_LANCER("/templates/shortcuts/lancer.png"),
	GAME_HOME_SHORTCUTS_MARKSMAN("/templates/shortcuts/marksman.png"),
	GAME_HOME_CAMP_TRAIN("/templates/home/camp/train.png"),
	
	HOME_DEALS_BUTTON("/templates/home/dealsButton.png"),
	HOME_EVENTS_BUTTON("/templates/home/eventsButton.png"),
	
	TRAINING_TRAIN_BUTTON("/templates/home/camp/training.png"),
	
	ALLIANCE_CHEST_BUTTON("/templates/alliance/chestButton.png"),
	ALLIANCE_TECH_BUTTON("/templates/alliance/techButton.png"),
	ALLIANCE_WAR_BUTTON("/templates/alliance/warButton.png"),
	
	ALLIANCE_CHEST_LOOT_CLAIM_BUTTON("/templates/alliance/lootClaimAllButton.png"),
	
	
	EVENTS_DEALS_BANK("/templates/events/deals/bank.png"),
	EVENTS_DEALS_BANK_INDEPOSIT("/templates/events/deals/bankInDeposit.png"),
	EVENTS_DEALS_BANK_DEPOSIT("/templates/events/deals/bankDeposit.png"),
	
	PETS_BEAST_CAGE("/templates/pets/beastCage.png"),
	PETS_BEAST_ALLIANCE_CLAIM("/templates/pets/claimButton.png"),
	PETS_INFO_SKILLS("/templates/pets/infoSkill.png"),
	PETS_SKILL_USE("/templates/pets/useSkill.png"),
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
