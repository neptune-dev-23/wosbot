package cl.camodev.wosbot.console.enumerable;

public enum TpDailyTaskEnum {
	// Enumeración de tareas diarias
	//@formatter:off
	HERO_RECRUITMENT(1, "Hero Recruitment"), 
	NOMADIC_MERCHANT(2, "Nomadic Merchant"), 
	WAR_ACADEMY_SHARDS(3, "War Academy Shards"), 
	CRYSTAL_LABORATORY(4, "Crystal Laboratory"),
	VIP_POINTS(5, "VIP Points"),
	PET_ADVENTURE(6, "Pet Adventure"),
	EXPLORATION_CHEST(7, "Exploration Chest"),

	LIFE_ESSENCE(9, "Life Essence"),
	
	
	GATHER_RESOURCES(13, "Gathering"),
	BANK(14, "Bank"),
	BEAST_SLAY(15, "Beast Slay"),

	GATHER_SPEED(20, "Gather Speed Boost"),
	
	MAIL_REWARDS(30, "Mail Rewards"),
	DAILY_TASKS(31, "Daily Tasks"),
	STOREHOUSE_CHEST(32, "Storehouse Chest"),
	INTEL(33, "Intel"),
	STOREHOUSE_STAMINA(34, "Storehouse Stamina"),
	
	
	ALLIANCE_AUTOJOIN(40, "Alliance Autojoin"),
	ALLIANCE_HELP(41, "Alliance Help"),
	ALLIANCE_TECH(42, "Alliance Tech"),
	ALLIANCE_PET_TREASURE(43, "Alliance Pet Treasure"),
	ALLIANCE_CHESTS(44, "Alliance Chests"),
	
	PET_SKILL_STAMINA(51, "Pet Skill Stamina"),
	PET_SKILL_FOOD(52, "Pet Skill Food"),
	PET_SKILL_TREASURE(53, "Pet Skill Treasure"),
	PET_SKILL_GATHERING(54, "Pet Skill Gathering"),
	
	TRAINING_INFANTRY(61, "Training Infantry"),
	TRAINING_LANCER(62, "Training Lancer"),
	TRAINING_MARKSMAN(63, "Training Marksman"),
	
	CITY_UPGRADE_FURNACE(70, "City Upgrade Furnace"),
	CITY_UPGRADE_OTHERS(71, "City Upgrade Others"),
	

	
	
	INITIALIZE(100, "Initialize");
	//@formatter:on

	private final int id;
	private final String name;

	TpDailyTaskEnum(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public static TpDailyTaskEnum fromId(int id) {
		for (TpDailyTaskEnum task : values()) {
			if (task.id == id) {
				return task;
			}
		}
		throw new IllegalArgumentException("No existe un TpDailyTaskEnum con id " + id);
	}
}
