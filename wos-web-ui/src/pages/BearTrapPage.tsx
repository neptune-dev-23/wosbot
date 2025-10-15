import { FiAlertTriangle, FiTarget, FiLayers } from "react-icons/fi";

const BEAR_TRAP_STATS = [
  { label: "Phase", value: "Preparation", trend: "Battle phase in 18h" },
  { label: "Armory Stock", value: "85%", trend: "Restock traps" },
  { label: "Enlisted Teams", value: "12 / 14", trend: "Recruiting open" },
  { label: "Damage Goal", value: "4.2B", trend: "Projected +15%" },
];

const BEAR_TRAP_TEAMS = [
  {
    title: "Frontline",
    responsibilities: ["Rally coordination", "Damage rotations", "Emergency heals"],
  },
  {
    title: "Support",
    responsibilities: ["Supply drops", "Buff schedules", "Aid distribution"],
  },
  {
    title: "Recon",
    responsibilities: ["Monitor threat meter", "Update alliance alerts", "Scout enemy reinforcements"],
  },
];

const BearTrapPage = () => (
  <div className="view active" id="bearTrapView">
    <div className="header">
      <h1>
        <FiAlertTriangle aria-hidden="true" className="header-icon" size={24} />
        <span>Bear Trap Ops</span>
      </h1>
      <p className="header-subtitle">
        Organize the alliance for the Bear Trap event and keep supply lines steady.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>Event Overview</h2>
        <div className="stat-grid">
          {BEAR_TRAP_STATS.map((stat) => (
            <div className="stat-card" key={stat.label}>
              <h3>{stat.label}</h3>
              <span className="stat-value">{stat.value}</span>
              <span className="stat-trend">{stat.trend}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Team Roles</h2>
        <div className="page-grid">
          {BEAR_TRAP_TEAMS.map((team) => (
            <div className="page-panel" key={team.title}>
              <h3>{team.title}</h3>
              <ul>
                {team.responsibilities.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Key Actions</h2>
        <div className="page-grid">
          <div className="page-panel">
            <h3>
              <FiTarget aria-hidden="true" /> Rally Assignments
            </h3>
            <p>Confirm rally leaders, support timings, and backup rotations before the battle window.</p>
          </div>
          <div className="page-panel">
            <h3>
              <FiLayers aria-hidden="true" /> Supply Logistics
            </h3>
            <p>Finalize trap inventory, distribute boosters, and track aid requests in real time.</p>
          </div>
        </div>
      </section>
    </div>
  </div>
);

export default BearTrapPage;
