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
