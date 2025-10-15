import { FiAward, FiBook, FiList } from "react-icons/fi";

const CHIEF_STATS = [
  { label: "Current Chapter", value: "10", trend: "Progress 72%" },
  { label: "Active Orders", value: "4", trend: "Focus: combat" },
  { label: "Claimable Rewards", value: "3", trend: "Collect before reset" },
  { label: "Order EXP", value: "24,300", trend: "Gain 1,700 to rank up" },
];

const ORDER_TASKS = [
  {
    title: "Combat Orders",
    steps: ["Defeat 10 elite enemies", "Win 5 arena matches", "Participate in 2 rallies"],
  },
  {
    title: "Development Orders",
    steps: ["Upgrade facility to Lv. 25", "Complete 6 research cycles", "Gather 3M steel"],
  },
  {
    title: "Alliance Orders",
    steps: ["Donate 15 times", "Assist allies 10 times", "Join alliance guard duty"],
  },
];

const ChiefOrderPage = () => (
  <div className="view active" id="chiefOrderView">
    <div className="header">
      <h1>
        <FiAward aria-hidden="true" className="header-icon" size={24} />
        <span>Chief Order Planner</span>
      </h1>
      <p className="header-subtitle">
        Track order progress and ensure daily and seasonal objectives stay on pace.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>Order Snapshot</h2>
        <div className="stat-grid">
          {CHIEF_STATS.map((stat) => (
            <div className="stat-card" key={stat.label}>
              <h3>{stat.label}</h3>
              <span className="stat-value">{stat.value}</span>
              <span className="stat-trend">{stat.trend}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Task Breakdown</h2>
        <div className="page-grid">
          {ORDER_TASKS.map((task) => (
            <div className="page-panel" key={task.title}>
              <h3>{task.title}</h3>
              <ul>
                {task.steps.map((step) => (
                  <li key={step}>{step}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Planning Notes</h2>
        <div className="page-grid">
          <div className="page-panel">
            <h3>
              <FiList aria-hidden="true" /> Priority Queue
            </h3>
            <p>Line up orders based on available buffs and alliance schedules.</p>
          </div>
          <div className="page-panel">
            <h3>
              <FiBook aria-hidden="true" /> Resource Log
            </h3>
            <p>Document cost per order to optimize future runs and minimize waste.</p>
          </div>
        </div>
      </section>
    </div>
  </div>
);

export default ChiefOrderPage;
