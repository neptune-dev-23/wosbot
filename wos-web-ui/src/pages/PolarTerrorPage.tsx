import { FiCloudSnow, FiShield, FiCompass } from "react-icons/fi";

const POLAR_STATS = [
  { label: "Siege Progress", value: "Stage 3 / 5", trend: "Boss unlock in 4h" },
  { label: "Alliance Damage", value: "62.1M", trend: "+8% vs last run" },
  { label: "Defense Boosts", value: "All active", trend: "Next refresh 9h" },
  { label: "Threat Level", value: "High", trend: "Scout every 2h" },
];

const POLAR_PHASES = [
  {
    title: "Preparation",
    notes: ["Assign rally leaders", "Refill traps", "Coordinate healer rotations"],
  },
  {
    title: "Engagement",
    notes: ["Trigger boosts", "Monitor rally timers", "Balance troop types"],
  },
  {
    title: "Recovery",
    notes: ["Run hospital cycles", "Distribute aid", "Review scout reports"],
  },
];

const PolarTerrorPage = () => (
  <div className="view active" id="polarTerrorView">
    <div className="header">
      <h1>
        <FiCloudSnow aria-hidden="true" className="header-icon" size={24} />
        <span>Polar Terror Ops</span>
      </h1>
      <p className="header-subtitle">
        Manage rally plans, threat scouting, and recovery actions during Polar Terror events.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>Event Snapshot</h2>
        <div className="stat-grid">
          {POLAR_STATS.map((stat) => (
            <div className="stat-card" key={stat.label}>
              <h3>{stat.label}</h3>
              <span className="stat-value">{stat.value}</span>
              <span className="stat-trend">{stat.trend}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Phase Checklist</h2>
        <div className="page-grid">
          {POLAR_PHASES.map((phase) => (
            <div className="page-panel" key={phase.title}>
              <h3>{phase.title}</h3>
              <ul>
                {phase.notes.map((note) => (
                  <li key={note}>{note}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Coordination Notes</h2>
        <div className="page-grid">
          <div className="page-panel">
            <h3>
              <FiShield aria-hidden="true" /> Defensive Prep
            </h3>
            <p>Rotate shield coverage and ensure healing supplies are stocked ahead of rallies.</p>
          </div>
          <div className="page-panel">
            <h3>
              <FiCompass aria-hidden="true" /> Scout Network
            </h3>
            <p>Schedule sweep scouts to update threat intel and path reroutes for the alliance.</p>
          </div>
        </div>
      </section>
    </div>
  </div>
);

export default PolarTerrorPage;
