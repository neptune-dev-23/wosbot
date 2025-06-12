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
	ALLIANCE_TECH(8, "Alliance Tech"),
	LIFE_ESSENCE(9, "Life Essence"),
	ALLIANCE_PET_TREASURE(10, "Alliance Pet Treasure"),
	ALLIANCE_CHESTS(11, "Alliance Chests"),
	TRAINING_TROOPS(12, "Training"),
	GATHER_RESOURCES(13, "Gathering"),
	BANK(14, "Bank"),
	BEAST_SLAY(15, "Beast Slay"),
	PET_SKILL_STAMINA(16, "Pet Skill Stamina"),
	PET_SKILL_FOOD(17, "Pet Skill Food"),
	PET_SKILL_TREASURE(18, "Pet Skill Treasure"),
	PET_SKILL_GATHERING(19, "Pet Skill Gathering"),
	
	GROWTH_SPEED(20, "Growth Speed"),
	
	
	MAIL_REWARDS(30, "Mail Rewards"),
	DAILY_TASKS(31, "Daily Tasks"),
	STOREHOUSE_CHEST(32, "Storehouse Chest"),
	INTEL(33, "Intel"),
	STOREHOUSE_STAMINA(34, "Storehouse Stamina"),
	
	ALLIANCE_AUTOJOIN(40, "Alliance Autojoin"),
	ALLIANCE_HELP(41, "Alliance Help"),
	
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
