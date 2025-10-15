import { FiTarget, FiAlertCircle, FiFeather } from "react-icons/fi";

const INTEL_STATS = [
  { label: "Reports Today", value: "18", trend: "+6 vs. yesterday" },
  { label: "Action Items", value: "7 open", trend: "3 urgent" },
  { label: "Scout Coverage", value: "92%", trend: "Assign 2 scouts" },
  { label: "Priority Level", value: "High", trend: "Polar threat detected" },
];

const INTEL_FEEDS = [
  {
    title: "Polar Threat Movement",
    note: "Enemy rally spotted near sector 7. Estimate hit in 3h.",
  },
  {
    title: "Resource Cache Discovery",
    note: "Level 10 steel depot located at coordinates X:382 Y:214.",
  },
  {
    title: "Alliance Request",
    note: "Sophia needs recon on neighboring base for mobilization prep.",
  },
];

const RESPONSE_PLAYBOOK = [
  { title: "Scout Assignments", detail: "Redirect spare march to update thermal scans." },
  { title: "Alert Command", detail: "Notify defense team and prep counter rallies." },
  { title: "Share Findings", detail: "Post key intel in alliance channel with timers." },
];

const IntelPage = () => (
  <div className="view active" id="intelView">
    <div className="header">
      <h1>
        <FiTarget aria-hidden="true" className="header-icon" size={24} />
        <span>Intel Dashboard</span>
      </h1>
      <p className="header-subtitle">
        Centralize scout reports, flag urgent threats, and coordinate alliance responses.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>Intel Summary</h2>
        <div className="stat-grid">
          {INTEL_STATS.map((stat) => (
            <div className="stat-card" key={stat.label}>
              <h3>{stat.label}</h3>
              <span className="stat-value">{stat.value}</span>
              <span className="stat-trend">{stat.trend}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Recent Reports</h2>
        <div className="timeline">
          {INTEL_FEEDS.map((feed) => (
            <div className="timeline-item" key={feed.title}>
              <strong>{feed.title}</strong>
              <span>{feed.note}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Response Plan</h2>
        <div className="list-grid">
          {RESPONSE_PLAYBOOK.map((item) => (
            <div className="list-card" key={item.title}>
              <strong>{item.title}</strong>
              <span>{item.detail}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Tools & Communication</h2>
        <div className="page-grid">
          <div className="page-panel">
            <h3>
              <FiAlertCircle aria-hidden="true" /> Alert Templates
            </h3>
            <p>Use quick macros to broadcast intel updates to the appropriate squads.</p>
          </div>
          <div className="page-panel">
            <h3>
              <FiFeather aria-hidden="true" /> Report Archive
            </h3>
            <p>Review historical reports to identify enemy patterns and refine scouting routes.</p>
          </div>
        </div>
      </section>
    </div>
  </div>
);

export default IntelPage;
