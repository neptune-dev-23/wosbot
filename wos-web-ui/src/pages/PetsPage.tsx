import { FiHeart, FiActivity, FiGift } from "react-icons/fi";

const PET_STATS = [
  { label: "Active Pets", value: "8", trend: "2 ready to evolve" },
  { label: "Adventure Slots", value: "3 / 4", trend: "Unlock in 12h" },
  { label: "Treat Stock", value: "120", trend: "Crafting stable" },
  { label: "Skill Books", value: "45", trend: "Focus on agility" },
];

const PET_ROSTER = [
  { name: "Aurora", role: "Support", focus: "Healing aura" },
  { name: "Blaze", role: "DPS", focus: "Flame burst" },
  { name: "Echo", role: "Utility", focus: "Stun chance" },
  { name: "Koda", role: "Tank", focus: "Damage reduction" },
];

const PET_TASKS = [
  "Queue adventure runs with optimal pairings.",
  "Feed pets before PvP window to maximize stats.",
  "Assign skill training focusing on rally composition.",
];

const PetsPage = () => (
  <div className="view active" id="petsView">
    <div className="header">
      <h1>
        <FiHeart aria-hidden="true" className="header-icon" size={24} />
        <span>Pets Management</span>
      </h1>
      <p className="header-subtitle">
        Track pet adventures, training, and evolutions to keep your squads combat-ready.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>Pet Overview</h2>
        <div className="stat-grid">
          {PET_STATS.map((stat) => (
            <div className="stat-card" key={stat.label}>
              <h3>{stat.label}</h3>
              <span className="stat-value">{stat.value}</span>
              <span className="stat-trend">{stat.trend}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Roster Highlights</h2>
        <div className="page-grid">
          {PET_ROSTER.map((pet) => (
            <div className="page-panel" key={pet.name}>
              <h3>{pet.name}</h3>
              <p>Role: {pet.role}</p>
              <p>Focus: {pet.focus}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Action Items</h2>
        <div className="list-grid">
          {PET_TASKS.map((task) => (
            <div className="list-card" key={task}>
              <strong>Task</strong>
              <span>{task}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Boosts & Rewards</h2>
        <div className="page-grid">
          <div className="page-panel">
            <h3>
              <FiActivity aria-hidden="true" /> Training Boosts
            </h3>
            <p>Schedule training speed buffs with alliance timers to accelerate evolutions.</p>
          </div>
          <div className="page-panel">
            <h3>
              <FiGift aria-hidden="true" /> Reward Tracker
            </h3>
            <p>Log upcoming pet events and ensure bonus rewards are claimed before reset.</p>
          </div>
        </div>
      </section>
    </div>
  </div>
);

export default PetsPage;
