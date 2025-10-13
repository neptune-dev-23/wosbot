import { Link } from "react-router-dom";

const LandingPage = () => {
  return (
    <div className="landing-page">
      <div className="landing-card">
        <h1 className="landing-title">Whiteout Survival Bot</h1>
        <p className="landing-subtitle">Monitor logs, manage profiles, and orchestrate tasks from your browser.</p>
        <Link className="landing-button" to="/logs">
          Enter Dashboard
        </Link>
      </div>
    </div>
  );
};

export default LandingPage;
