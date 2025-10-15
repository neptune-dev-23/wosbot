import { FiBookOpen, FiTool, FiSun } from "react-icons/fi";

const EXPERT_STATS = [
  { label: "Unlocked Experts", value: "12 / 15", trend: "Next recruit in 2d" },
  { label: "Research Speed", value: "140%", trend: "+20% boost active" },
  { label: "Assignments", value: "5 / 6", trend: "Rotate in 3h" },
  { label: "Skill Books", value: "74", trend: "Plan upgrade path" },
];

const EXPERT_ASSIGNMENTS = [
  {
    name: "Dr. Voss",
    role: "Research lead",
    focus: "Engineering tech and speed boosts.",
  },
  {
    name: "Lena Ward",
    role: "Field instructor",
    focus: "Rally efficiency and damage mitigation.",
  },
  {
    name: "Marcus Hale",
    role: "Logistics",
    focus: "Resource transport and hospital recovery.",
  },
];

const EXPERT_ACTIONS = [
  "Review expertise trees and allocate upcoming skill points.",
  "Reassign experts before major events to maximize uptime.",
  "Track equipment and memory shards needed for next breakthroughs.",
];

const ExpertsPage = () => (
  <div className="view active" id="expertsView">
    <div className="header">
      <h1>
        <FiBookOpen aria-hidden="true" className="header-icon" size={24} />
        <span>Experts Hub</span>
      </h1>
      <p className="header-subtitle">
        Manage expert assignments, upgrades, and equipment to amplify alliance performance.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>Expert Overview</h2>
        <div className="stat-grid">
          {EXPERT_STATS.map((stat) => (
            <div className="stat-card" key={stat.label}>
              <h3>{stat.label}</h3>
              <span className="stat-value">{stat.value}</span>
              <span className="stat-trend">{stat.trend}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Current Assignments</h2>
        <div className="page-grid">
          {EXPERT_ASSIGNMENTS.map((assignment) => (
            <div className="page-panel" key={assignment.name}>
              <h3>{assignment.name}</h3>
              <p>Role: {assignment.role}</p>
              <p>{assignment.focus}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Next Steps</h2>
        <div className="list-grid">
          {EXPERT_ACTIONS.map((action) => (
            <div className="list-card" key={action}>
              <strong>Action</strong>
              <span>{action}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Upgrade Planning</h2>
        <div className="page-grid">
          <div className="page-panel">
            <h3>
              <FiTool aria-hidden="true" /> Equipment Loadouts
            </h3>
            <p>Match gear sets to upcoming operations and flag missing pieces for crafting.</p>
          </div>
          <div className="page-panel">
            <h3>
              <FiSun aria-hidden="true" /> Breakthrough Targets
            </h3>
            <p>Identify which experts unlock the largest gains at the next skill tier.</p>
          </div>
        </div>
      </section>
    </div>
  </div>
);

export default ExpertsPage;
