package cl.camodev.wosbot.console.enumerable;

public enum EnumTemplates {

	// @formatter:off
	GAME_HOME_FURNACE("/templates/city.png"),
	GAME_HOME_WORLD("/templates/world.png"),
	GAME_HOME_PETS("/templates/home/petsButton.png"),
	GAME_HOME_INTEL("/templates/home/intelButton.png"),
	
	GAME_HOME_SHORTCUTS_INFANTRY("/templates/shortcuts/infantry.png"),
	GAME_HOME_SHORTCUTS_LANCER("/templates/shortcuts/lancer.png"),
	GAME_HOME_SHORTCUTS_MARKSMAN("/templates/shortcuts/marksman.png"),
	GAME_HOME_SHORTCUTS_RESEARCH_CENTER("/templates/shortcuts/researchCenter.png"),
	GAME_HOME_CAMP_TRAIN("/templates/home/camp/train.png"),
	
	GAME_HOME_SHORTCUTS_MEAT("/templates/shortcuts/meat.png"),
	GAME_HOME_SHORTCUTS_WOOD("/templates/shortcuts/wood.png"),
	GAME_HOME_SHORTCUTS_COAL("/templates/shortcuts/coal.png"),
	GAME_HOME_SHORTCUTS_IRON("/templates/shortcuts/iron.png"),
	
	GAME_HOME_SHORTCUTS_FARM_MEAT("/templates/shortcuts/farmMeat.png"),
	GAME_HOME_SHORTCUTS_FARM_WOOD("/templates/shortcuts/farmWood.png"),
	GAME_HOME_SHORTCUTS_FARM_COAL("/templates/shortcuts/farmCoal.png"),
	GAME_HOME_SHORTCUTS_FARM_IRON("/templates/shortcuts/farmIron.png"),
	
	GAME_HOME_SHORTCUTS_FARM_TICK("/templates/shortcuts/farmTick.png"),
	GAME_HOME_SHORTCUTS_FARM_GATHER("/templates/shortcuts/farmGather.png"),
	
	RALLY_REMOVE_HERO_BUTTON("/templates/rally/removeHeroButton.png"),
	RALLY_GATHER_BUTTON("/templates/rally/gatherButton.png"),
	RALLY_GATHER_ALREADY_MARCHING("/templates/rally/gatherAlreadyMarching.png"),
	
	HOME_DEALS_BUTTON("/templates/home/dealsButton.png"),
	HOME_EVENTS_BUTTON("/templates/home/eventsButton.png"),
	
	VIP_UNLOCK_BUTTON("/templates/vip/unlockButton.png"),
	
	TRAINING_TRAIN_BUTTON("/templates/home/camp/training.png"),
	
	CRYSTAL_LAB_FC_BUTTON("/templates/crystallab/fcButton.png"),
	
	ALLIANCE_CHEST_BUTTON("/templates/alliance/chestButton.png"),
	ALLIANCE_TECH_BUTTON("/templates/alliance/techButton.png"),
	ALLIANCE_WAR_BUTTON("/templates/alliance/warButton.png"),
	
	ALLIANCE_CHEST_LOOT_CLAIM_BUTTON("/templates/alliance/lootClaimAllButton.png"),
	
	STOREHOUSE_CHEST("/templates/storehouse/chest.png"),
	STOREHOUSE_STAMINA("/templates/storehouse/stamina.png"),
	
	
	EVENTS_DEALS_BANK("/templates/events/deals/bank.png"),
	EVENTS_DEALS_BANK_INDEPOSIT("/templates/events/deals/bankInDeposit.png"),
	EVENTS_DEALS_BANK_DEPOSIT("/templates/events/deals/bankDeposit.png"),
	EVENTS_DEALS_BANK_WITHDRAW("/templates/events/deals/bankWithdraw.png"),
	
	INTEL_COMPLETED("/templates/intel/completed.png"),
	INTEL_VIEW("/templates/intel/beastView.png"),
	INTEL_ATTACK("/templates/intel/beastAttack.png"),
	INTEL_RESCUE("/templates/intel/survivorRescue.png"),
	INTEL_EXPLORE("/templates/intel/journeyExplore.png"),
	INTEL_ATTACK_CONFIRM("/templates/intel/beastAttackConfirm.png"),
	
	INTEL_FIRE_BEAST("/templates/intel/beastFire.png"),
	INTEL_BEAST_YELLOW("/templates/intel/beastYellow.png"),
	INTEL_BEAST_PURPLE("/templates/intel/beastPurple.png"),
	INTEL_BEAST_BLUE("/templates/intel/beastBlue.png"),
	INTEL_BEAST_GREEN("/templates/intel/beastGreen.png"),
	INTEL_BEAST_GREY("/templates/intel/beastGrey.png"),
	
	INTEL_SURVIVOR_YELLOW("/templates/intel/survivorYellow.png"),
	INTEL_SURVIVOR_PURPLE("/templates/intel/survivorPurple.png"),
	INTEL_SURVIVOR_BLUE("/templates/intel/survivorBlue.png"),
	INTEL_SURVIVOR_GREEN("/templates/intel/survivorGreen.png"),
	INTEL_SURVIVOR_GREY("/templates/intel/survivorGrey.png"),
	
	INTEL_JOURNEY_YELLOW("/templates/intel/journeyYellow.png"),
	INTEL_JOURNEY_PURPLE("/templates/intel/journeyPurple.png"),
	INTEL_JOURNEY_BLUE("/templates/intel/journeyBlue.png"),
	INTEL_JOURNEY_GREEN("/templates/intel/journeyGreen.png"),
	INTEL_JOURNEY_GREY("/templates/intel/journeyGrey.png"),
	
	INTEL_MASTER_BOUNTY("/templates/intel/masterBounty.png"),
	
	
	PETS_BEAST_CAGE("/templates/pets/beastCage.png"),
	PETS_BEAST_ALLIANCE_CLAIM("/templates/pets/claimButton.png"),
	PETS_INFO_SKILLS("/templates/pets/infoSkill.png"),
	PETS_SKILL_USE("/templates/pets/useSkill.png"),
	
	PETS_CHEST_COMPLETED("/templates/pets/chestCompleted.png"),
	PETS_CHEST_SELECT("/templates/pets/chestSelect.png"),
	PETS_CHEST_START("/templates/pets/chestStart.png"),
	PETS_CHEST_ATTEMPT("/templates/pets/chestAttempt.png"),
	PETS_CHEST_SHARE("/templates/pets/chestShare.png"),
	PETS_CHEST_RED("/templates/pets/chestRed.png"),
	PETS_CHEST_PURPLE("/templates/pets/chestPurple.png"),
	PETS_CHEST_BLUE("/templates/pets/chestBlue.png"),

	
	
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
