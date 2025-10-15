import { FiPackage, FiCompass, FiTrendingUp } from "react-icons/fi";

const GATHER_STATS = [
  { label: "Active Marches", value: "5 / 6", trend: "Full in 2m" },
  { label: "Hourly Yield", value: "4.3M", trend: "+8% buff active" },
  { label: "Target Nodes", value: "T10 gas", trend: "Cluster 4" },
  { label: "Recall Alerts", value: "2 due", trend: "Send replacements" },
];

const GATHER_TEAMS = [
  {
    name: "March 1",
    composition: "Infantry heavy",
    objective: "Steel node (T10)",
  },
  {
    name: "March 2",
    composition: "Balanced",
    objective: "Gas node (T10)",
  },
  {
    name: "March 3",
    composition: "Rider focus",
    objective: "Food node (T9)",
  },
  {
    name: "March 4",
    composition: "Ranged assist",
    objective: "Wood node (T9)",
  },
];

const GATHER_ACTIONS = [
  { title: "Refresh pathing", note: "Update preset locations for next cycle." },
  { title: "Alliance request", note: "Flag contested nodes for rally support." },
  { title: "Buff tracker", note: "Confirm gather boost expires in 45m." },
];

const GatherPage = () => (
  <div className="view active" id="gatherView">
    <div className="header">
      <h1>
        <FiPackage aria-hidden="true" className="header-icon" size={24} />
        <span>Gather Planner</span>
      </h1>
      <p className="header-subtitle">
        Monitor march assignments, buff windows, and resource targets for efficient gathering.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>Gather Overview</h2>
        <div className="stat-grid">
          {GATHER_STATS.map((stat) => (
            <div className="stat-card" key={stat.label}>
              <h3>{stat.label}</h3>
              <span className="stat-value">{stat.value}</span>
              <span className="stat-trend">{stat.trend}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>March Assignments</h2>
        <div className="page-grid">
          {GATHER_TEAMS.map((team) => (
            <div className="page-panel" key={team.name}>
              <h3>{team.name}</h3>
              <p>{team.composition}</p>
              <p>{team.objective}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Actions</h2>
        <div className="list-grid">
          {GATHER_ACTIONS.map((action) => (
            <div className="list-card" key={action.title}>
              <strong>{action.title}</strong>
              <span>{action.note}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Optimizations</h2>
        <div className="page-grid">
          <div className="page-panel">
            <h3>
              <FiCompass aria-hidden="true" /> Route Planning
            </h3>
            <p>Check alliance path markers and reposition to fresh spawn clusters.</p>
          </div>
          <div className="page-panel">
            <h3>
              <FiTrendingUp aria-hidden="true" /> Yield Forecast
            </h3>
            <p>Compare hourly output with event goals and cue extra speed-ups if needed.</p>
          </div>
        </div>
      </section>
    </div>
  </div>
);

export default GatherPage;
