import { useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { FiChevronLeft, FiChevronRight, FiRefreshCw, FiSearch } from "react-icons/fi";

import type { LogMessage } from "../types/api";
import { formatTimestamp } from "../utils/format";

const AUTO_SCROLL_THRESHOLD = 5;
const MAX_LOGS = 1000;
const SEVERITY_LEVELS = ["INFO", "WARNING", "ERROR", "DEBUG"] as const;

const pageSizeFromStorage = () => {
  if (typeof window === "undefined") {
    return 50;
  }
  const stored = window.localStorage.getItem("logPageSize");
  const parsed = stored ? Number.parseInt(stored, 10) : 50;
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 50;
};

const LogsPage = () => {
  const [logs, setLogs] = useState<LogMessage[]>([]);
  const [logConnectionStatus, setLogConnectionStatus] = useState<"connecting" | "connected" | "disconnected">("connecting");
  const [showDebug, setShowDebug] = useState(false);
  const [filters, setFilters] = useState({ search: "", profile: "", severity: "" });
  const [pageSize, setPageSize] = useState<number>(pageSizeFromStorage);
  const [currentPage, setCurrentPage] = useState(1);
  const [autoScroll, setAutoScroll] = useState(true);

  const logContainerRef = useRef<HTMLDivElement | null>(null);
  const previousScrollHeightRef = useRef(0);
  const previousScrollTopRef = useRef(0);

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

  const totalPages = Math.max(1, Math.ceil(orderedLogs.length / pageSize));
  const clampedCurrentPage = Math.min(Math.max(currentPage, 1), totalPages);
  const startIndex = (clampedCurrentPage - 1) * pageSize;
  const endIndex = Math.min(startIndex + pageSize, orderedLogs.length);
  const paginatedLogs = orderedLogs.slice(startIndex, endIndex);
  const showingStart = orderedLogs.length === 0 ? 0 : startIndex + 1;
  const showingEnd = endIndex;

  const logStatusText =
    logConnectionStatus === "connected"
      ? "Connected"
      : logConnectionStatus === "connecting"
      ? "Connecting..."
      : "Disconnected - Reconnecting...";

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    window.localStorage.setItem("logPageSize", String(pageSize));
  }, [pageSize]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    } else if (currentPage < 1) {
      setCurrentPage(1);
    }
  }, [currentPage, totalPages]);

  useEffect(() => {
    const container = logContainerRef.current;
    if (!container) {
      return;
    }
    const handleScroll = () => {
      previousScrollTopRef.current = container.scrollTop;
      if (clampedCurrentPage !== 1) {
        setAutoScroll(false);
        return;
      }
      const atTop = container.scrollTop <= AUTO_SCROLL_THRESHOLD;
      setAutoScroll(atTop);
    };

    container.addEventListener("scroll", handleScroll);
    return () => container.removeEventListener("scroll", handleScroll);
  }, [clampedCurrentPage]);

  useLayoutEffect(() => {
    const container = logContainerRef.current;
    if (!container) {
      return;
    }

    const previousHeight = previousScrollHeightRef.current;
    const shouldAutoScroll = autoScroll && clampedCurrentPage === 1;

    if (shouldAutoScroll) {
      container.scrollTo({ top: 0, behavior: "auto" });
    } else {
      const scrollDelta = container.scrollHeight - previousHeight;
      const nextScrollTop = Math.max(0, previousScrollTopRef.current + scrollDelta);
      container.scrollTop = nextScrollTop;
      previousScrollTopRef.current = nextScrollTop;
    }

    previousScrollHeightRef.current = container.scrollHeight;
  }, [autoScroll, clampedCurrentPage, paginatedLogs]);

  useEffect(() => {
    let isCancelled = false;
    let retryTimer: ReturnType<typeof setTimeout> | null = null;
    let source: EventSource | null = null;

    const connect = () => {
      if (isCancelled) {
        return;
      }
      setLogConnectionStatus("connecting");

      const eventSource = new EventSource("/logs/stream");
      source = eventSource;

      eventSource.addEventListener("log", (event) => {
        if (isCancelled) {
          return;
        }
        try {
          const parsed = JSON.parse((event as MessageEvent).data) as LogMessage;
          setLogs((previous) => {
            const next = [...previous, parsed];
            if (next.length > MAX_LOGS) {
              next.shift();
            }
            return next;
          });
        } catch (error) {
          console.error("Failed to parse log event", error);
        }
      });

      eventSource.onopen = () => {
        if (!isCancelled) {
          setLogConnectionStatus("connected");
        }
      };

      eventSource.onerror = () => {
        if (isCancelled) {
          return;
        }
        setLogConnectionStatus("disconnected");
        eventSource.close();
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

  const handleClearLogs = () => {
    setLogs([]);
    setCurrentPage(1);
    setAutoScroll(true);
    const container = logContainerRef.current;
    if (container) {
      container.scrollTo({ top: 0, behavior: "auto" });
      previousScrollHeightRef.current = container.scrollHeight;
      previousScrollTopRef.current = 0;
    }
  };

  const handleRefreshLogs = () => {
    setCurrentPage(1);
    setAutoScroll(true);
    const container = logContainerRef.current;
    if (container) {
      container.scrollTo({ top: 0, behavior: "auto" });
    }
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
              setCurrentPage(1);
            }}
          />
        </div>
        <div className="logs-controls">
          <select
            className="filter-dropdown"
            value={filters.profile}
            onChange={(event) => {
              setFilters((previous) => ({ ...previous, profile: event.target.value }));
              setCurrentPage(1);
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
              setCurrentPage(1);
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
            {paginatedLogs.length === 0 ? (
              <tr className="no-logs-row">
                <td colSpan={5}>{logs.length === 0 ? "Waiting for logs..." : "No logs match the current filters."}</td>
              </tr>
            ) : (
              paginatedLogs.map((log, index) => (
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
                setCurrentPage(1);
              }}
            />
            <span>Debug Mode</span>
          </label>
          <span id="logCount">
            Showing {showingStart}-{showingEnd} of {orderedLogs.length} logs ({logs.length} total)
          </span>
        </div>
        <div className="logs-pagination">
          <div className="pagination-size">
            <label htmlFor="logPageSize">Logs per page:</label>
            <select
              id="logPageSize"
              className="pagination-dropdown"
              value={pageSize}
              onChange={(event) => {
                const nextSize = Number.parseInt(event.target.value, 10);
                setPageSize(nextSize);
                setCurrentPage(1);
                setAutoScroll(true);
              }}
            >
              <option value={10}>10</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
              <option value={250}>250</option>
            </select>
          </div>
          <div className="pagination-controls">
            <button
              className="pagination-btn"
              onClick={() => setCurrentPage((previous) => Math.max(previous - 1, 1))}
              disabled={clampedCurrentPage <= 1}
              type="button"
            >
              <FiChevronLeft aria-hidden="true" size={16} />
              <span>Prev</span>
            </button>
            <span className="pagination-info">Page {clampedCurrentPage} of {Math.max(1, totalPages)}</span>
            <button
              className="pagination-btn"
              onClick={() => setCurrentPage((previous) => Math.min(previous + 1, totalPages))}
              disabled={clampedCurrentPage >= totalPages}
              type="button"
            >
              <span>Next</span>
              <FiChevronRight aria-hidden="true" size={16} />
            </button>
          </div>
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
