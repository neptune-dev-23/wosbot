import { FiCalendar, FiClock, FiStar } from "react-icons/fi";

const EVENT_SCHEDULE = [
  { name: "Alliance Mobilization", phase: "Scoring", remaining: "1d 4h" },
  { name: "Bear Trap", phase: "Preparation", remaining: "18h" },
  { name: "Polar Terror", phase: "Recruitment", remaining: "6h" },
  { name: "Capital Clash", phase: "Lock-in", remaining: "3d" },
];

const EVENT_TASKS = [
  {
    title: "Daily Objectives",
    hints: ["Complete 12 dailies", "Run hero trials", "Submit intel reports"],
  },
  {
    title: "Alliance Initiatives",
    hints: ["Secure rally spots", "Assign support rotations", "Track donation goals"],
  },
  {
    title: "Power Push",
    hints: ["Queue T10 training", "Sync timers for boosts", "Share speed-ups"],
  },
];

const REWARD_TRACKS = [
  { stage: "Milestone 1", reward: "Universal speed-ups +2h" },
  { stage: "Milestone 2", reward: "Epic hero fragments" },
  { stage: "Milestone 3", reward: "Alliance honor +2,000" },
  { stage: "Completion", reward: "Legendary chest selection" },
];

const EventsPage = () => (
  <div className="view active" id="eventsView">
    <div className="header">
      <h1>
        <FiCalendar aria-hidden="true" className="header-icon" size={24} />
        <span>Event Planning</span>
      </h1>
      <p className="header-subtitle">
        Track active events, milestones, and team objectives so the alliance never misses rewards.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>Live Event Timeline</h2>
        <div className="timeline">
          {EVENT_SCHEDULE.map((event) => (
            <div className="timeline-item" key={event.name}>
              <strong>{event.name}</strong>
              <span>Phase: {event.phase}</span>
              <span>Time remaining: {event.remaining}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Focus Tasks</h2>
        <div className="page-grid">
          {EVENT_TASKS.map((task) => (
            <div className="page-panel" key={task.title}>
              <h3>{task.title}</h3>
              <ul>
                {task.hints.map((hint) => (
                  <li key={hint}>{hint}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Reward Track</h2>
        <div className="list-grid">
          {REWARD_TRACKS.map((track) => (
            <div className="list-card" key={track.stage}>
              <strong>{track.stage}</strong>
              <span>{track.reward}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Daily Checklist</h2>
        <div className="page-grid">
          <div className="page-panel">
            <h3>
              <FiClock aria-hidden="true" /> Reset Routine
            </h3>
            <p>Claim free packs, refresh store offers, and trigger hero training cycles.</p>
          </div>
          <div className="page-panel">
            <h3>
              <FiStar aria-hidden="true" /> Bonus Windows
            </h3>
            <p>Log peak times for double rewards and share timers with the alliance.</p>
          </div>
        </div>
      </section>
    </div>
  </div>
);

export default EventsPage;
