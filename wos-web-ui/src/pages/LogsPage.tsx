import { useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { FiRefreshCw, FiSearch } from "react-icons/fi";

import type { LogMessage } from "../types/api";
import { formatTimestamp } from "../utils/format";
import { subscribeToMessage } from "../services/wsClient";


const MAX_LOGS = 1000;
const SEVERITY_LEVELS = ["INFO", "WARNING", "ERROR", "DEBUG"] as const;


const LogsPage = () => {
  const [logs, setLogs] = useState<LogMessage[]>([]);
  const [logConnectionStatus, setLogConnectionStatus] = useState<"connecting" | "connected" | "disconnected">(
    "connecting",
  );
  const [showDebug, setShowDebug] = useState(false);
  const [filters, setFilters] = useState({ search: "", profile: "", severity: "" });

  const [autoScroll, setAutoScroll] = useState(true);

  const logContainerRef = useRef<HTMLDivElement | null>(null);
  const previousScrollHeightRef = useRef(0);

  const availableLogProfiles = useMemo(() => {
    const unique = new Set<string>();
    logs.forEach((log) => {
      if (log.profile) {
        unique.add(log.profile);
      }
    });
    return Array.from(unique).sort((a, b) => a.localeCompare(b));
  }, [logs]);

  const filteredLogs = useMemo(() => {
    return logs.filter((log) => {
      if (!showDebug && log.severity === "DEBUG") {
        return false;
      }
      if (filters.profile && log.profile !== filters.profile) {
        return false;
      }
      if (filters.severity && log.severity !== filters.severity) {
        return false;
      }
      if (filters.search) {
        const haystack = `${log.profile ?? ""} ${log.task ?? ""} ${log.message ?? ""} ${log.severity ?? ""}`.toLowerCase();
        if (!haystack.includes(filters.search.toLowerCase())) {
          return false;
        }
      }
      return true;
    });
  }, [filters.profile, filters.search, filters.severity, logs, showDebug]);

  const orderedLogs = useMemo(() => filteredLogs.slice().reverse(), [filteredLogs]);



  const logStatusText =
    logConnectionStatus === "connected"
      ? "Connected"
      : logConnectionStatus === "connecting"
      ? "Connecting..."
      : "Disconnected - Reconnecting...";



  useLayoutEffect(() => {
    const container = logContainerRef.current;
    if (!container) {
      return;
    }

    // If auto-scroll is off, we want to maintain the scroll position.
    // This happens when new logs are added at the top.
    const scrollDelta = container.scrollHeight - previousScrollHeightRef.current;

    // Only adjust scroll if new content has been added at the top.
    if (!autoScroll && scrollDelta > 0) {
      container.scrollTop += scrollDelta;
    }

    // Always update the scroll height for the next render.
    previousScrollHeightRef.current = container.scrollHeight;
  }, [autoScroll, orderedLogs]);

  useEffect(() => {
    setLogConnectionStatus("connecting");

    const handleSnapshot = (snapshot: LogMessage[]) => {
      setLogConnectionStatus("connected");
      setLogs(() => {
        if (!Array.isArray(snapshot)) {
          return [];
        }
        return snapshot.slice(-MAX_LOGS);
      });
      setAutoScroll(true);
    };

    const handleAppend = (entry: LogMessage) => {
      setLogConnectionStatus("connected");
      setLogs((previous) => {
        const next = [...previous, entry];
        if (next.length > MAX_LOGS) {
          next.shift();
        }
        return next;
      });
    };

    const unsubscribeSnapshot = subscribeToMessage<LogMessage[]>("logs.snapshot", handleSnapshot);
    const unsubscribeAppend = subscribeToMessage<LogMessage>("logs.append", handleAppend);
    const unsubscribeConnected = subscribeToMessage("system.connected", () => setLogConnectionStatus("connecting"));
    const unsubscribeDisconnected = subscribeToMessage("system.disconnected", () =>
      setLogConnectionStatus("disconnected"),
    );

    return () => {
      unsubscribeSnapshot();
      unsubscribeAppend();
      unsubscribeConnected();
      unsubscribeDisconnected();
    };
  }, []);

  const handleClearLogs = () => {
    setLogs([]);
    setAutoScroll(true);
  };

  const handleRefreshLogs = () => {
    logContainerRef.current?.scrollTo({ top: 0, behavior: "auto" });
  };



  return (
    <div className="view active" id="logsView">
      <div className="logs-header">
        <div className="logs-search-bar">
          <span className="search-icon" aria-hidden="true">
            <FiSearch size={18} />
          </span>
          <input
            type="text"
            placeholder="Search logs..."
            className="search-input"
            value={filters.search}
            onChange={(event) => {
              setFilters((previous) => ({ ...previous, search: event.target.value }));
            }}
          />
        </div>
        <div className="logs-controls">
          <select
            className="filter-dropdown"
            value={filters.profile}
            onChange={(event) => {
              setFilters((previous) => ({ ...previous, profile: event.target.value }));
            }}
          >
            <option value="">All profiles</option>
            {availableLogProfiles.map((profile) => (
              <option key={profile} value={profile}>
                {profile}
              </option>
            ))}
          </select>
          <select
            className="filter-dropdown"
            value={filters.severity}
            onChange={(event) => {
              setFilters((previous) => ({ ...previous, severity: event.target.value }));
            }}
          >
            <option value="">All levels</option>
            {SEVERITY_LEVELS.map((level) => (
              <option key={level} value={level}>
                {level}
              </option>
            ))}
          </select>
          <button aria-label="Refresh logs" className="refresh-btn" onClick={handleRefreshLogs} type="button">
            <FiRefreshCw aria-hidden="true" className="refresh-icon" size={18} />
          </button>
        </div>
      </div>

      <div className="logs-table-container" id="logContainer" ref={logContainerRef}>
        <table className="logs-table">
          <thead>
            <tr>
              <th>TIMESTAMP</th>
              <th>LEVEL</th>
              <th>PROFILE</th>
              <th>TASK</th>
              <th>MESSAGE</th>
            </tr>
          </thead>
          <tbody>
            {orderedLogs.length === 0 ? (
              <tr className="no-logs-row">
                <td colSpan={5}>{logs.length === 0 ? "Waiting for logs..." : "No logs match the current filters."}</td>
              </tr>
            ) : (
              orderedLogs.map((log, index) => (
                <tr key={`${log.timestamp ?? "ts"}-${index}`}>
                  <td>{formatTimestamp(log.timestamp)}</td>
                  <td>
                    <span className={`log-level ${log.severity}`}>{log.severity}</span>
                  </td>
                  <td>{log.profile ?? ""}</td>
                  <td>{log.task ?? ""}</td>
                  <td>{log.message ?? ""}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="logs-footer">
        <div className="logs-info">
          <label className="debug-mode">
            <input
              type="checkbox"
              checked={showDebug}
              onChange={(event) => {
                setShowDebug(event.target.checked);
              }}
            />
            <span>Debug Mode</span>
          </label>
          <span id="logCount">
            {orderedLogs.length} of {filteredLogs.length} logs (max {MAX_LOGS})
          </span>
        </div>
        <div className="logs-footer-controls">

          <div className={`status-connection ${logConnectionStatus === "connected" ? "connected" : "disconnected"}`}>
            <div className={`status-indicator ${logConnectionStatus === "connected" ? "" : "disconnected"}`} />
            <span>{logStatusText}</span>
          </div>
          <button className="btn btn-danger" onClick={handleClearLogs} type="button">
            Clear Logs
          </button>
        </div>
      </div>
    </div>
  );
};

export default LogsPage;
