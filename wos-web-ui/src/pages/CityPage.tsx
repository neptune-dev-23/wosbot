import { FiMap, FiTool, FiTrendingUp } from "react-icons/fi";

const CITY_STATS = [
  { label: "City Level", value: "35", trend: "Next upgrade in 18h" },
  { label: "Construction Queue", value: "2 / 2", trend: "Upgrade HQ & Hospital" },
  { label: "Power Rating", value: "9.2M", trend: "+120k today" },
  { label: "Idle Workers", value: "3", trend: "Assign to resource upgrades" },
];

const CITY_PROJECTS = [
  {
    title: "Headquarters",
    status: "Level 36 unlocks laboratory expansion",
    actions: ["Secure steel shipment", "Sync with alliance buffs"],
  },
  {
    title: "Wall Reinforcement",
    status: "Level 34 → 35 queued (4h)",
    actions: ["Ensure guard rotation", "Commit trap materials"],
  },
  {
    title: "Barracks Modernization",
    status: "Prioritize tier-10 infantry training speed",
    actions: ["Allocate timers", "Run training buffs"],
  },
];

const CITY_ECONOMY = [
  { resource: "Food", production: "1.8M / hr", status: "Stable" },
  { resource: "Wood", production: "1.6M / hr", status: "Needs +10%" },
  { resource: "Steel", production: "950k / hr", status: "Low, plan import" },
  { resource: "Gas", production: "640k / hr", status: "Stable" },
];

const CityPage = () => (
  <div className="view active" id="cityView">
    <div className="header">
      <h1>
        <FiMap aria-hidden="true" className="header-icon" size={24} />
        <span>City Management</span>
      </h1>
      <p className="header-subtitle">
        Oversee infrastructure, production, and power growth to keep the city combat ready.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>City Overview</h2>
        <div className="stat-grid">
          {CITY_STATS.map((stat) => (
            <div className="stat-card" key={stat.label}>
              <h3>{stat.label}</h3>
              <span className="stat-value">{stat.value}</span>
              <span className="stat-trend">{stat.trend}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Capital Projects</h2>
        <div className="page-grid">
          {CITY_PROJECTS.map((project) => (
            <div className="page-panel" key={project.title}>
              <h3>{project.title}</h3>
              <p>{project.status}</p>
              <ul>
                {project.actions.map((action) => (
                  <li key={action}>{action}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Economy Snapshot</h2>
        <div className="list-grid">
          {CITY_ECONOMY.map((entry) => (
            <div className="list-card" key={entry.resource}>
              <strong>{entry.resource}</strong>
              <span>{entry.production}</span>
              <span>{entry.status}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Optimization Notes</h2>
        <div className="page-grid">
          <div className="page-panel">
            <h3>
              <FiTrendingUp aria-hidden="true" /> Growth Path
            </h3>
            <p>Coordinate research speed-ups and align next milestones with alliance initiatives.</p>
          </div>
          <div className="page-panel">
            <h3>
              <FiTool aria-hidden="true" /> Maintenance Tasks
            </h3>
            <p>Collect idle builder timers, rotate VIP buffs, and finalize garrison presets.</p>
          </div>
        </div>
      </section>
    </div>
  </div>
);

export default CityPage;
