import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { FaRobot } from "react-icons/fa";
import {
  FiAlertTriangle,
  FiAward,
  FiBarChart2,
  FiCheckSquare,
  FiClipboard,
  FiCloudSnow,
  FiCpu,
  FiPause,
  FiHeart,
  FiMap,
  FiPackage,
  FiPlay,
  FiSend,
  FiShield,
  FiShoppingBag,
  FiStopCircle,
  FiTarget,
  FiUsers,
  FiBookOpen,
  FiCalendar,
  FiTrendingUp,
} from "react-icons/fi";
import type { IconType } from "react-icons";

import type { BotState } from "../types/api";
import { subscribeToMessage } from "../services/wsClient";

type BotStatus = "running" | "paused" | "stopped" | "unknown" | "starting";

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
  const [pendingAction, setPendingAction] = useState<"start" | "stop" | "pause" | "resume" | null>(null);
  const [versionLabel, setVersionLabel] = useState("Whiteout Survival Bot");
  const lastStableStatusRef = useRef<BotStatus>("unknown");

  const navRef = useRef<HTMLElement | null>(null);
  const hamburgerRef = useRef<HTMLButtonElement | null>(null);

  const location = useLocation();

  const navItems = useMemo(
    () =>
      [
        { path: "/logs", label: "Logs", icon: FiClipboard },
        { path: "/profiles", label: "Profiles", icon: FiUsers },
        { path: "/tasks", label: "Task Manager", icon: FiCheckSquare },
        { path: "/task-stats", label: "Task Stats", icon: FiTrendingUp },
        { path: "/alliance", label: "Alliance", icon: FiShield },
        { path: "/city", label: "City", icon: FiMap },
        { path: "/events", label: "Events", icon: FiCalendar },
        { path: "/gather", label: "Gather", icon: FiPackage },
        { path: "/intel", label: "Intel", icon: FiTarget },
        { path: "/mobilization", label: "Mobilization", icon: FiSend },
        { path: "/pets", label: "Pets", icon: FiHeart },
        { path: "/shop", label: "Shop", icon: FiShoppingBag },
        { path: "/training", label: "Training", icon: FiBarChart2 },
        { path: "/polar-terror", label: "Polar Terror", icon: FiCloudSnow },
        { path: "/bear-trap", label: "Bear Trap", icon: FiAlertTriangle },
        { path: "/chief-order", label: "Chief Order", icon: FiAward },
        { path: "/experts", label: "Experts", icon: FiBookOpen },
        { path: "/emulator", label: "Emulators", icon: FiCpu },
      ] satisfies Array<{ path: string; label: string; icon: IconType }>,
    []
  );

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
    const applyState = (state: BotState) => {
      const resolved = resolveBotStatus(state);
      setBotStatus(resolved);
      lastStableStatusRef.current = resolved;
      setPendingAction(null);
      setBotActionPending(false);
    };
    const unsubscribeSnapshot = subscribeToMessage<BotState>("botState.snapshot", applyState);
    const unsubscribeUpdate = subscribeToMessage<BotState>("botState.update", applyState);
    const unsubscribeDisconnected = subscribeToMessage("system.disconnected", () => {
      setBotStatus("unknown");
      lastStableStatusRef.current = "unknown";
    });

    return () => {
      unsubscribeSnapshot();
      unsubscribeUpdate();
      unsubscribeDisconnected();
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
          setVersionLabel(`WosBot v${data.version}`);
        }
      } catch (error) {
        console.warn("Failed to load version", error);
      }
    };

    loadVersion().catch(() => undefined);
  }, []);

  useEffect(() => {
    const loadInitialBotStatus = async () => {
      try {
        const response = await fetch("/api/bot/status");
        if (!response.ok) {
          return;
        }
        const data = (await response.json()) as {
          status?: string;
          running?: boolean;
          paused?: boolean;
        };

        if (typeof data.running === "boolean") {
          const resolved = resolveBotStatus({ running: data.running, paused: data.paused ?? false });
          setBotStatus(resolved);
          lastStableStatusRef.current = resolved;
          return;
        }

        if (typeof data.status === "string") {
          const normalized = data.status.toLowerCase();
          if (normalized === "paused") {
            setBotStatus("paused");
            lastStableStatusRef.current = "paused";
          } else if (normalized === "running") {
            setBotStatus("running");
            lastStableStatusRef.current = "running";
          } else if (normalized === "stopped") {
            setBotStatus("stopped");
            lastStableStatusRef.current = "stopped";
          }
        }
      } catch (error) {
        console.warn("Failed to load bot status", error);
      }
    };

    loadInitialBotStatus().catch(() => undefined);
  }, []);

  const handleBotCommand = useCallback(
    async (
      endpoint: string,
      actionDescription: string,
      actionKey: "start" | "stop" | "pause" | "resume",
    ) => {
      const previousStatus = botStatus;
      try {
        setBotActionPending(true);
        setPendingAction(actionKey);
        if (actionKey === "start") {
          lastStableStatusRef.current = botStatus !== "starting" ? botStatus : lastStableStatusRef.current;
          setBotStatus("starting");
        }
        const response = await fetch(endpoint, { method: "POST" });
        if (!response.ok) {
          throw new Error(`Failed to ${actionDescription}: ${response.status}`);
        }
        const data = (await response.json()) as { success?: boolean; error?: string };
        if (data.success === false) {
          throw new Error(data.error ?? `Failed to ${actionDescription}`);
        }

        switch (actionKey) {
          case "stop":
            setBotStatus("stopped");
            lastStableStatusRef.current = "stopped";
            break;
          case "pause":
            setBotStatus("paused");
            lastStableStatusRef.current = "paused";
            break;
          case "resume":
            setBotStatus("running");
            lastStableStatusRef.current = "running";
            break;
          case "start":
            setBotStatus("running");
            lastStableStatusRef.current = "running";
            break;
          default:
            break;
        }
        setPendingAction(null);
        setBotActionPending(false);
      } catch (error) {
        console.error(error);
        const message = error instanceof Error ? error.message : `Failed to ${actionDescription}`;
        window.alert(message);
        if (actionKey === "start") {
          setBotStatus(lastStableStatusRef.current);
        } else {
          setBotStatus(previousStatus);
          lastStableStatusRef.current = previousStatus;
        }
        setPendingAction(null);
        setBotActionPending(false);
      } finally {
        if (actionKey !== "start") {
          setBotActionPending(false);
          setPendingAction(null);
        }
      }
    },
    [botStatus]
  );

  const botStatusTextMap: Record<BotStatus, string> = useMemo(
    () => ({
      running: "Bot Status: Running",
      paused: "Bot Status: Paused",
      stopped: "Bot Status: Stopped",
      starting: "Bot Status: Starting...",
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
      <button
        type="button"
        className={`sidenav-toggle ${isNavOpen ? "active" : ""}`}
        id="sidenavToggle"
        onClick={(event) => {
          event.stopPropagation();
          handleToggleNav();
        }}
        ref={hamburgerRef}
        aria-expanded={isNavOpen}
        aria-label={isNavOpen ? "Collapse sidebar" : "Expand sidebar"}
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
      </button>
      <nav className={`sidenav ${isNavOpen ? "open" : ""}`} id="sideNav" ref={navRef}>
        <div className="sidenav-content">
          <div className="sidenav-header">
            <h2>
              <Link className="sidenav-brand" to="/" onClick={handleNavItemClick}>
                <FaRobot aria-hidden="true" className="header-icon" size={22} />
                <span className="sidenav-title-text">WosBot</span>
              </Link>
            </h2>
          </div>
          <ul className="sidenav-menu">
            {navItems.map((item) => (
              <li key={item.path}>
                <NavLink
                  to={item.path}
                  className={navItemClass}
                  onClick={handleNavItemClick}
                  title={item.label}
                >
                  <span className="nav-icon" aria-hidden="true">
                    <item.icon size={20} />
                  </span>
                  <span className="nav-text">{item.label}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </div>
      </nav>

      <div className={`main-content ${isNavOpen ? "shifted" : ""}`} id="mainContent">
        <Outlet />
      </div>

      <div className="bottom-bar" id="bottomBar">
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
                onClick={() => handleBotCommand("/api/bot/pause", "pause bot", "pause")}
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
                onClick={() => handleBotCommand("/api/bot/stop", "stop bot", "stop")}
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
                onClick={() => handleBotCommand("/api/bot/resume", "resume bot", "resume")}
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
                onClick={() => handleBotCommand("/api/bot/stop", "stop bot", "stop")}
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
          {(botStatus === "stopped" || botStatus === "unknown" || botStatus === "starting") && (
            <button
              className={`bottom-btn bottom-btn-start${botStatus === "starting" || pendingAction === "start" ? " pending-state" : ""}`}
              id="btnStart"
              onClick={() => handleBotCommand("/api/bot/start", "start bot", "start")}
              disabled={botActionPending}
              type="button"
            >
              <span className="btn-icon" aria-hidden="true">
                <FiPlay size={16} />
              </span>
              <span className="btn-text">{botStatus === "starting" ? "Starting..." : "Start Bot"}</span>
            </button>
          )}
        </div>
      </div>
    </>
  );
};

export default DashboardLayout;
