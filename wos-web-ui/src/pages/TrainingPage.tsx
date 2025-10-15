import { FiBarChart2, FiClock, FiCheck } from "react-icons/fi";

const TRAINING_STATS = [
  { label: "Queue Length", value: "4 / 5", trend: "Finish in 3h" },
  { label: "Daily Output", value: "46k troops", trend: "+12% boost" },
  { label: "Speed-ups Used", value: "18", trend: "Save 6 for clash" },
  { label: "Idle Barracks", value: "1", trend: "Assign queue now" },
];

const TRAINING_QUEUES = [
  { unit: "Infantry T10", remaining: "2h 15m", notes: "Use training buff at 30m mark" },
  { unit: "Rider T10", remaining: "3h 02m", notes: "Reserve speed-ups for event push" },
  { unit: "Marksman T9", remaining: "45m", notes: "Promote to T10 next" },
];

const TRAINING_CHECKLIST = [
  "Trigger training buff before major events start.",
  "Rotate heroes for barracks bonuses.",
  "Queue partial batches to sync with alliance timers.",
];

const TrainingPage = () => (
  <div className="view active" id="trainingView">
    <div className="header">
      <h1>
        <FiBarChart2 aria-hidden="true" className="header-icon" size={24} />
        <span>Training Monitor</span>
      </h1>
      <p className="header-subtitle">
        Keep troop production on schedule and ensure queues align with upcoming events.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>Training Snapshot</h2>
        <div className="stat-grid">
          {TRAINING_STATS.map((stat) => (
            <div className="stat-card" key={stat.label}>
              <h3>{stat.label}</h3>
              <span className="stat-value">{stat.value}</span>
              <span className="stat-trend">{stat.trend}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Active Queues</h2>
        <div className="timeline">
          {TRAINING_QUEUES.map((queue) => (
            <div className="timeline-item" key={queue.unit}>
              <strong>{queue.unit}</strong>
              <span>
                <FiClock aria-hidden="true" /> {queue.remaining}
              </span>
              <span>{queue.notes}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Checklist</h2>
        <div className="list-grid">
          {TRAINING_CHECKLIST.map((item) => (
            <div className="list-card" key={item}>
              <strong>
                <FiCheck aria-hidden="true" /> Step
              </strong>
              <span>{item}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  </div>
);

export default TrainingPage;
