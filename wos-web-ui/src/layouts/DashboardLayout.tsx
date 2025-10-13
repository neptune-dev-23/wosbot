import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { FaRobot } from "react-icons/fa";
import { FiCheckSquare, FiClipboard, FiPause, FiPlay, FiStopCircle, FiUsers } from "react-icons/fi";

import type { BotState } from "../types/api";

type BotStatus = "running" | "paused" | "stopped" | "unknown";

const resolveBotStatus = (botState: BotState): BotStatus => {
  if (!botState) {
    return "unknown";
  }
  if (!botState.running) {
    return "stopped";
  }
  if (botState.paused) {
    return "paused";
  }
  return "running";
};

const DashboardLayout = () => {
  const [isNavOpen, setIsNavOpen] = useState(false);
  const [botStatus, setBotStatus] = useState<BotStatus>("unknown");
  const [botActionPending, setBotActionPending] = useState(false);
  const [versionLabel, setVersionLabel] = useState("Whiteout Survival Bot");

  const navRef = useRef<HTMLElement | null>(null);
  const hamburgerRef = useRef<HTMLDivElement | null>(null);

  const location = useLocation();

  useEffect(() => {
    if (!isNavOpen) {
      return;
    }

    const handleClick = (event: MouseEvent) => {
      const target = event.target as Node;
      const navContains = navRef.current?.contains(target) ?? false;
      const buttonContains = hamburgerRef.current?.contains(target) ?? false;
      if (!navContains && !buttonContains) {
        setIsNavOpen(false);
      }
    };

    document.addEventListener("click", handleClick);
    return () => document.removeEventListener("click", handleClick);
  }, [isNavOpen]);

  useEffect(() => {
    if (typeof window !== "undefined" && window.innerWidth <= 768) {
      setIsNavOpen(false);
    }
  }, [location.pathname]);

  useEffect(() => {
    let isCancelled = false;
    let retryTimer: ReturnType<typeof setTimeout> | null = null;
    let source: EventSource | null = null;

    const connect = () => {
      if (isCancelled) {
        return;
      }

      const eventSource = new EventSource("/api/bot/state/stream");
      source = eventSource;

      eventSource.addEventListener("botState", (event) => {
        if (isCancelled) {
          return;
        }
        try {
          const parsed = JSON.parse((event as MessageEvent).data) as BotState;
          setBotStatus(resolveBotStatus(parsed));
        } catch (error) {
          console.error("Failed to parse bot state event", error);
        }
      });

      eventSource.onerror = () => {
        if (isCancelled) {
          return;
        }
        eventSource.close();
        setBotStatus("unknown");
        if (retryTimer) {
          clearTimeout(retryTimer);
        }
        retryTimer = setTimeout(connect, 3000);
      };
    };

    connect();

    return () => {
      isCancelled = true;
      if (retryTimer) {
        clearTimeout(retryTimer);
      }
      if (source) {
        source.close();
      }
    };
  }, []);

  useEffect(() => {
    const loadVersion = async () => {
      try {
        const response = await fetch("/api/version");
        if (!response.ok) {
          return;
        }
        const data = (await response.json()) as { version?: string };
        if (data.version) {
          setVersionLabel(`Whiteout Survival Bot v${data.version}`);
        }
      } catch (error) {
        console.warn("Failed to load version", error);
      }
    };

    loadVersion().catch(() => undefined);
  }, []);

  const handleBotCommand = useCallback(
    async (endpoint: string, actionDescription: string) => {
      try {
        setBotActionPending(true);
        const response = await fetch(endpoint, { method: "POST" });
        if (!response.ok) {
          throw new Error(`Failed to ${actionDescription}: ${response.status}`);
        }
        const data = (await response.json()) as { success?: boolean; error?: string };
        if (data.success === false) {
          throw new Error(data.error ?? `Failed to ${actionDescription}`);
        }
      } catch (error) {
        console.error(error);
        const message = error instanceof Error ? error.message : `Failed to ${actionDescription}`;
        window.alert(message);
      } finally {
        setBotActionPending(false);
      }
    },
    []
  );

  const botStatusTextMap: Record<BotStatus, string> = useMemo(
    () => ({
      running: "Bot Status: Running",
      paused: "Bot Status: Paused",
      stopped: "Bot Status: Stopped",
      unknown: "Bot Status: Unknown",
    }),
    []
  );

  const handleToggleNav = () => {
    setIsNavOpen((previous) => !previous);
  };

  const handleNavItemClick = () => {
    if (typeof window !== "undefined" && window.innerWidth <= 768) {
      setIsNavOpen(false);
    }
  };

  const navItemClass = ({ isActive }: { isActive: boolean }) => `nav-item${isActive ? " active" : ""}`;

  return (
    <>
      <div className="hamburger-menu" id="hamburgerMenu">
        <div
          className={`hamburger-icon ${isNavOpen ? "active" : ""}`}
          id="hamburgerIcon"
          onClick={(event) => {
            event.stopPropagation();
            handleToggleNav();
          }}
          ref={hamburgerRef}
          role="button"
          tabIndex={0}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              handleToggleNav();
            }
          }}
        >
          <span />
          <span />
          <span />
        </div>
      </div>

      <nav className={`sidenav ${isNavOpen ? "open" : ""}`} id="sideNav" ref={navRef}>
        <div className="sidenav-header">
          <h2>
            <Link className="sidenav-brand" to="/" onClick={handleNavItemClick}>
              <FaRobot aria-hidden="true" className="header-icon" size={22} />
              <span className="sidenav-title-text">WosBot</span>
            </Link>
          </h2>
        </div>
        <ul className="sidenav-menu">
          <li>
            <NavLink to="/logs" className={navItemClass} onClick={handleNavItemClick}>
              <span className="nav-icon" aria-hidden="true">
                <FiClipboard size={20} />
              </span>
              <span className="nav-text">Logs</span>
            </NavLink>
          </li>
          <li>
            <NavLink to="/profiles" className={navItemClass} onClick={handleNavItemClick}>
              <span className="nav-icon" aria-hidden="true">
                <FiUsers size={20} />
              </span>
              <span className="nav-text">Profiles</span>
            </NavLink>
          </li>
          <li>
            <NavLink to="/tasks" className={navItemClass} onClick={handleNavItemClick}>
              <span className="nav-icon" aria-hidden="true">
                <FiCheckSquare size={20} />
              </span>
              <span className="nav-text">Task Manager</span>
            </NavLink>
          </li>
        </ul>
      </nav>

      <div className={`main-content ${isNavOpen ? "shifted" : ""}`} id="mainContent">
        <Outlet />
      </div>

      <div className={`bottom-bar ${isNavOpen ? "shifted" : ""}`} id="bottomBar">
        <div className="bottom-bar-left">
          <div className="bot-status">
            <span className={`bot-status-indicator ${botStatus}`} id="botStatusIndicator" />
            <span className="bot-status-text" id="botStatusText">
              {botStatusTextMap[botStatus]}
            </span>
          </div>
        </div>
        <div className="bottom-bar-center">
          <span className="app-version">{versionLabel}</span>
        </div>
        <div className="bottom-bar-right">
          {botStatus === "running" && (
            <>
              <button
                className="bottom-btn bottom-btn-pause"
                id="btnPause"
                onClick={() => handleBotCommand("/api/bot/pause", "pause bot")}
                disabled={botActionPending}
                type="button"
              >
                <span className="btn-icon" aria-hidden="true">
                  <FiPause size={16} />
                </span>
                <span className="btn-text">Pause</span>
              </button>
              <button
                className="bottom-btn bottom-btn-stop"
                id="btnStop"
                onClick={() => handleBotCommand("/api/bot/stop", "stop bot")}
                disabled={botActionPending}
                type="button"
              >
                <span className="btn-icon" aria-hidden="true">
                  <FiStopCircle size={16} />
                </span>
                <span className="btn-text">Stop Bot</span>
              </button>
            </>
          )}
          {botStatus === "paused" && (
            <>
              <button
                className="bottom-btn bottom-btn-resume"
                id="btnResume"
                onClick={() => handleBotCommand("/api/bot/resume", "resume bot")}
                disabled={botActionPending}
                type="button"
              >
                <span className="btn-icon" aria-hidden="true">
                  <FiPlay size={16} />
                </span>
                <span className="btn-text">Resume</span>
              </button>
              <button
                className="bottom-btn bottom-btn-stop"
                id="btnStopPaused"
                onClick={() => handleBotCommand("/api/bot/stop", "stop bot")}
                disabled={botActionPending}
                type="button"
              >
                <span className="btn-icon" aria-hidden="true">
                  <FiStopCircle size={16} />
                </span>
                <span className="btn-text">Stop Bot</span>
              </button>
            </>
          )}
          {(botStatus === "stopped" || botStatus === "unknown") && (
            <button
              className="bottom-btn bottom-btn-start"
              id="btnStart"
              onClick={() => handleBotCommand("/api/bot/start", "start bot")}
              disabled={botActionPending}
              type="button"
            >
              <span className="btn-icon" aria-hidden="true">
                <FiPlay size={16} />
              </span>
              <span className="btn-text">Start Bot</span>
            </button>
          )}
        </div>
      </div>
    </>
  );
};

export default DashboardLayout;
