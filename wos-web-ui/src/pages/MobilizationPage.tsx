import { FiSend, FiUsers, FiFlag } from "react-icons/fi";

const MOBILIZATION_STATS = [
  { label: "Enlisted", value: "38 / 40", trend: "Recruiting closes in 8h" },
  { label: "Objectives Complete", value: "62%", trend: "+12% today" },
  { label: "Ticket Pool", value: "18", trend: "Allocate before reset" },
  { label: "Boost Status", value: "All active", trend: "Next refresh 12h" },
];

const TEAM_ASSIGNMENTS = [
  {
    name: "Logistics",
    leads: "Morgan / Lee",
    focus: "Resource transport and buff rotation.",
  },
  {
    name: "Recon",
    leads: "Ravi / Chen",
    focus: "Intel coverage for Polar front.",
  },
  {
    name: "Strike Teams",
    leads: "Sam / Harper",
    focus: "Rally coordination and war preparations.",
  },
];

const TICKET_PLAYBOOK = [
  { title: "Daily Sweep", detail: "Submit routine objectives before prime time." },
  { title: "High Value", detail: "Reserve epic tickets for alliance rally tasks." },
  { title: "Overflow plan", detail: "Convert surplus into support for new recruits." },
];

const MobilizationPage = () => (
  <div className="view active" id="mobilizationView">
    <div className="header">
      <h1>
        <FiSend aria-hidden="true" className="header-icon" size={24} />
        <span>Mobilization Planner</span>
      </h1>
      <p className="header-subtitle">
        Keep alliance mobilization on track with enlistment stats, ticket usage, and team assignments.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>Mobilization Health</h2>
        <div className="stat-grid">
          {MOBILIZATION_STATS.map((stat) => (
            <div className="stat-card" key={stat.label}>
              <h3>{stat.label}</h3>
              <span className="stat-value">{stat.value}</span>
              <span className="stat-trend">{stat.trend}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Team Assignments</h2>
        <div className="page-grid">
          {TEAM_ASSIGNMENTS.map((team) => (
            <div className="page-panel" key={team.name}>
              <h3>{team.name}</h3>
              <p>Leads: {team.leads}</p>
              <p>{team.focus}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Ticket Strategy</h2>
        <div className="list-grid">
          {TICKET_PLAYBOOK.map((item) => (
            <div className="list-card" key={item.title}>
              <strong>{item.title}</strong>
              <span>{item.detail}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Coordination Notes</h2>
        <div className="page-grid">
          <div className="page-panel">
            <h3>
              <FiUsers aria-hidden="true" /> Member Support
            </h3>
            <p>Spot teammates who need help completing high-value tasks or using boosts.</p>
          </div>
          <div className="page-panel">
            <h3>
              <FiFlag aria-hidden="true" /> War Alignment
            </h3>
            <p>Ensure mobilization objectives feed directly into the next war timeline.</p>
          </div>
        </div>
      </section>
    </div>
  </div>
);

export default MobilizationPage;
