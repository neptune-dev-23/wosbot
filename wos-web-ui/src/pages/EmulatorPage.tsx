import { FiCpu, FiWifi, FiPower } from "react-icons/fi";

const EMULATOR_STATS = [
  { label: "Online Instances", value: "6 / 8", trend: "2 scheduled for restart" },
  { label: "Average CPU", value: "62%", trend: "Stable" },
  { label: "Average RAM", value: "7.4 GB", trend: "Monitor during events" },
  { label: "ADB Connections", value: "6 active", trend: "Latency nominal" },
];

const EMULATOR_GRID = [
  { name: "EMU-01", profile: "Aurora", status: "Online" },
  { name: "EMU-02", profile: "Blaze", status: "Online" },
  { name: "EMU-03", profile: "Echo", status: "Restart queued" },
  { name: "EMU-04", profile: "Koda", status: "Online" },
  { name: "EMU-05", profile: "Sable", status: "Maintenance" },
  { name: "EMU-06", profile: "Nova", status: "Online" },
];

const EmulatorPage = () => (
  <div className="view active" id="emulatorView">
    <div className="header">
      <h1>
        <FiCpu aria-hidden="true" className="header-icon" size={24} />
        <span>Emulator Control</span>
      </h1>
      <p className="header-subtitle">
        Monitor emulator health, connectivity, and maintenance queues across your device farm.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>Fleet Overview</h2>
        <div className="stat-grid">
          {EMULATOR_STATS.map((stat) => (
            <div className="stat-card" key={stat.label}>
              <h3>{stat.label}</h3>
              <span className="stat-value">{stat.value}</span>
              <span className="stat-trend">{stat.trend}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Instance Status</h2>
        <div className="page-grid">
          {EMULATOR_GRID.map((emu) => (
            <div className="page-panel" key={emu.name}>
              <h3>{emu.name}</h3>
              <p>Assigned Profile: {emu.profile}</p>
              <p>Status: {emu.status}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Maintenance Notes</h2>
        <div className="page-grid">
          <div className="page-panel">
            <h3>
              <FiWifi aria-hidden="true" /> Connectivity
            </h3>
            <p>Verify network routing and restart ADB bridge if latency spikes occur during events.</p>
          </div>
          <div className="page-panel">
            <h3>
              <FiPower aria-hidden="true" /> Restart Plan
            </h3>
            <p>Schedule restarts during off-peak hours to keep instances fresh without impacting rallies.</p>
          </div>
        </div>
      </section>
    </div>
  </div>
);

export default EmulatorPage;
