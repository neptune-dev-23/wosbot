import { useCallback, useEffect, useMemo, useState } from "react";
import { FiActivity, FiClock, FiRefreshCcw, FiTrendingUp, FiUsers, FiX, FiExternalLink } from "react-icons/fi";
import { Link } from "react-router-dom";

import type { Profile, TaskState, TaskStatsAggregate } from "../types/api";
import { fetchTaskStats } from "../services/taskStatsService";
import { formatDateTime, formatDuration } from "../utils/format";
import { getTasksSnapshot, subscribeTasksStore } from "../services/tasksStore";
import {
  ensureProfilesInitialized,
  getProfilesSnapshot,
  subscribeProfilesStore,
} from "../services/profilesStore";

type TaskMap = Record<string, TaskState[]>;
interface TaskStateWithProfile {
  profileId: number;
  profileName: string;
  task: TaskState;
}

const buildEntryKey = (entry: TaskStatsAggregate) =>
  `${entry.taskId ?? "unknown"}::${entry.taskName ?? "unknown"}`;

const percentage = (value: number) => `${Math.round((value ?? 0) * 1000) / 10}%`;

const MAX_PROFILES_TO_SHOW = 2;

const formatSampleProfiles = (sampleProfiles: string[] | undefined, profileCount: number): string | null => {
    if (!sampleProfiles || sampleProfiles.length === 0) {
        return null;
    }

    const displayedProfiles = sampleProfiles.slice(0, MAX_PROFILES_TO_SHOW);
    const remainingCount = profileCount - displayedProfiles.length;

    let result = displayedProfiles.join(", ");
    if (remainingCount > 0) {
        result += ` + ${remainingCount} other${remainingCount === 1 ? "" : "s"}`;
    }
    return result;
};

const TaskStatsPage = () => {
  const [stats, setStats] = useState<TaskStatsAggregate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [tasksMap, setTasksMap] = useState<TaskMap>(() => getTasksSnapshot());
  const [profiles, setProfiles] = useState<Profile[]>(() => getProfilesSnapshot());

  useEffect(() => {
    const unsubscribe = subscribeTasksStore((next) => {
      setTasksMap(next);
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    const unsubscribe = subscribeProfilesStore((next) => {
      setProfiles(next);
    });

    if (getProfilesSnapshot().length === 0) {
      ensureProfilesInitialized()?.catch(() => undefined);
    }

    return () => unsubscribe();
  }, []);

  const loadStats = useCallback(
    async (showLoading: boolean, signal?: AbortSignal) => {
      if (showLoading) {
        setLoading(true);
      } else {
        setRefreshing(true);
      }
      setError(null);
      try {
          const response = await fetchTaskStats({limit: 10000, signal});
        setStats(response.data ?? []);
        setLastUpdated(new Date());
      } catch (err) {
        if (err instanceof DOMException && err.name === "AbortError") {
          return;
        }
        const message = err instanceof Error ? err.message : "Failed to fetch stats";
        setError(message);
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [],
  );

  useEffect(() => {
    const controller = new AbortController();
    loadStats(true, controller.signal).catch(() => undefined);

    const interval = window.setInterval(() => {
      const refresher = new AbortController();
      loadStats(false, refresher.signal).catch(() => undefined);
    }, 60000);

    return () => {
      controller.abort();
      window.clearInterval(interval);
    };
  }, [loadStats]);

  const sortedStats = useMemo(() => {
    return [...stats].sort((a, b) => {
      if (b.totalRuns !== a.totalRuns) {
        return b.totalRuns - a.totalRuns;
      }
      const bTime = b.lastFinishedAt ? new Date(b.lastFinishedAt).getTime() : 0;
      const aTime = a.lastFinishedAt ? new Date(a.lastFinishedAt).getTime() : 0;
      return bTime - aTime;
    });
  }, [stats]);

  const keyedStats = useMemo(
    () => sortedStats.map((entry) => ({ entry, key: buildEntryKey(entry) })),
    [sortedStats],
  );

  const profileNameMap = useMemo(() => {
    const map = new Map<number, string>();
    profiles.forEach((profile) => {
      if (profile && typeof profile.id === "number") {
        map.set(profile.id, profile.name || `Profile ${profile.id}`);
      }
    });
    return map;
  }, [profiles]);

  const selectedEntry = useMemo(() => {
    if (!selectedKey) {
      return null;
    }
    return keyedStats.find((item) => item.key === selectedKey)?.entry ?? null;
  }, [keyedStats, selectedKey]);

  const getTaskStates = useCallback(
    (entry: TaskStatsAggregate): TaskStateWithProfile[] => {
      const results: TaskStateWithProfile[] = [];
      const targetTaskId = entry.taskId ?? null;
      const targetTaskName = entry.taskName ?? null;

      Object.entries(tasksMap).forEach(([profileKey, list]) => {
        const numericProfileId = Number.parseInt(profileKey, 10);
        if (!Number.isFinite(numericProfileId)) {
          return;
        }
        const tasks = Array.isArray(list) ? list : [];
        tasks.forEach((task) => {
          const matchesById = targetTaskId != null && task.taskId === targetTaskId;
          const matchesByName =
            targetTaskId == null && targetTaskName != null && task.taskName === targetTaskName;
          if (!matchesById && !matchesByName) {
            return;
          }
          results.push({
            profileId: numericProfileId,
            profileName: profileNameMap.get(numericProfileId) ?? `Profile ${numericProfileId}`,
            task,
          });
        });
      });

      return results.sort((a, b) => {
        const aTime = a.task.nextExecutionTime ? new Date(a.task.nextExecutionTime).getTime() : Number.POSITIVE_INFINITY;
        const bTime = b.task.nextExecutionTime ? new Date(b.task.nextExecutionTime).getTime() : Number.POSITIVE_INFINITY;
        return aTime - bTime;
      });
    },
    [profileNameMap, tasksMap],
  );

  const matchingTaskStates = useMemo(() => {
    if (!selectedEntry) {
      return [];
    }
    return getTaskStates(selectedEntry);
  }, [getTaskStates, selectedEntry]);

  const defaultTaskLink = useMemo(() => {
    if (matchingTaskStates.length === 0) {
        return {pathname: "/tasks"};
    }
      const targetProfile = String(matchingTaskStates[0].profileId);
      return {
          pathname: "/tasks",
          state: {focusProfileId: targetProfile},
      };
  }, [matchingTaskStates]);

  const nextExecutionForSelected = useMemo(() => {
    const upcoming = matchingTaskStates
      .map((item) => item.task.nextExecutionTime)
      .filter((value): value is string => !!value);
    if (upcoming.length === 0) {
      return null;
    }
    return upcoming.reduce((earliest, candidate) => {
      const candidateTime = new Date(candidate).getTime();
      if (!earliest) {
        return candidate;
      }
      const earliestTime = new Date(earliest).getTime();
      return candidateTime < earliestTime ? candidate : earliest;
    }, upcoming[0]);
  }, [matchingTaskStates]);

  const lastExecutionForSelected = useMemo(() => {
    const lastRuns = matchingTaskStates
      .map((item) => item.task.lastExecutionTime)
      .filter((value): value is string => !!value);
    if (lastRuns.length === 0) {
      return null;
    }
    return lastRuns.reduce((latest, candidate) => {
      const candidateTime = new Date(candidate).getTime();
      if (!latest) {
        return candidate;
      }
      const latestTime = new Date(latest).getTime();
      return candidateTime > latestTime ? candidate : latest;
    }, lastRuns[0]);
  }, [matchingTaskStates]);

  const handleOpenDetails = (key: string) => {
    setSelectedKey(key);
  };

  const handleCloseDetails = () => {
    setSelectedKey(null);
  };

  useEffect(() => {
    if (!selectedKey) {
      return;
    }

    const handleKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setSelectedKey(null);
      }
    };
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [selectedKey]);

  const lastUpdatedLabel = lastUpdated ? lastUpdated.toLocaleTimeString() : null;

  return (
      <div className="view active task-stats-page">
      <header className="task-stats-header">
        <div>
          <h1>Task Execution Stats</h1>
          <p className="task-stats-subtitle">
            Review runtime performance aggregated across profiles. Click a card to drill into task history and hop back
            to Task Manager when you need deeper control.
          </p>
        </div>
        <div className="task-stats-header-actions">
          {lastUpdatedLabel ? <span className="task-stats-updated">Updated {lastUpdatedLabel}</span> : null}
          <button
            type="button"
            className="task-stats-refresh-button"
            onClick={() => loadStats(false)}
            disabled={refreshing}
          >
            <FiRefreshCcw />
            {refreshing ? "Refreshing..." : "Refresh"}
          </button>
        </div>
      </header>

          <div className="task-stats-content">
              {loading && (
                  <div className="task-stats-placeholder">
                      <div className="task-stats-spinner"/>
                      <span>Loading execution history…</span>
                  </div>
              )}

              {!loading && error && (
                  <div className="task-stats-error">
                      <p>{error}</p>
                      <button type="button" onClick={() => loadStats(true)} className="btn btn-primary">
                          Retry
                      </button>
                  </div>
              )}

              {!loading && !error && keyedStats.length === 0 && (
                  <div className="task-stats-empty">
                      <p>No executions recorded yet. Tasks will appear here once they run at least once.</p>
                  </div>
              )}

              {!loading && !error && keyedStats.length > 0 && (
                  <div className="task-stats-grid">
                      {keyedStats.map(({entry, key}) => (
                          <button
                              key={key}
                              type="button"
                              className="task-stats-card"
                              onClick={() => handleOpenDetails(key)}
                              aria-label={`Open stats for ${entry.taskName ?? `Task ${entry.taskId ?? ""}`}`}
                          >
                              <header className="task-stats-card-header">
                                  <span
                                      className="task-stats-task-name">{entry.taskName ?? `Task ${entry.taskId ?? ""}`}</span>
                                  <span className="task-stats-profile">
                    <FiUsers/>
                    <span>
                      {entry.profileCount > 0
                          ? `${entry.profileCount} profile${entry.profileCount === 1 ? "" : "s"}`
                          : "No profiles yet"}
                    </span>
                  </span>
                              </header>
                              {entry.sampleProfiles?.length ? (
                                  <p className="task-stats-sample">
                                      {formatSampleProfiles(entry.sampleProfiles, entry.profileCount)}
                                  </p>
                              ) : null}
                              <div className="task-stats-card-body">
                                  <div className="task-stats-metric">
                                      <FiActivity/>
                                      <div>
                                          <span className="metric-label">Total Runs</span>
                                          <span className="metric-value">{entry.totalRuns}</span>
                                      </div>
                                  </div>
                                  <div className="task-stats-metric">
                                      <FiTrendingUp/>
                                      <div>
                                          <span className="metric-label">Success Rate</span>
                                          <span className="metric-value">{percentage(entry.successRate)}</span>
                                      </div>
                                  </div>
                                  <div className="task-stats-metric">
                                      <FiClock/>
                                      <div>
                                          <span className="metric-label">Avg Duration</span>
                                          <span
                                              className="metric-value">{formatDuration(entry.averageDurationMillis)}</span>
                                      </div>
                                  </div>
                                  <div className="task-stats-metric">
                                      <FiRefreshCcw/>
                                      <div>
                                          <span className="metric-label">P95 Duration</span>
                                          <span
                                              className="metric-value">{formatDuration(entry.p95DurationMillis)}</span>
                                      </div>
                                  </div>
                              </div>
                              <footer className="task-stats-card-footer">
                  <span className="task-stats-last-run">
                    Last run: {entry.lastFinishedAt ? formatDateTime(entry.lastFinishedAt) : "N/A"}
                  </span>
                                  <span className="task-stats-card-link">View details</span>
                              </footer>
                          </button>
                      ))}
                  </div>
              )}
          </div>

      {selectedEntry && (
        <div
          className="task-stats-modal-backdrop"
          role="dialog"
          aria-modal="true"
          onClick={(event) => {
            if (event.target === event.currentTarget) {
              handleCloseDetails();
            }
          }}
        >
          <div className="task-stats-modal">
            <header className="task-stats-modal-header">
              <div>
                <h2>{selectedEntry.taskName ?? `Task ${selectedEntry.taskId ?? ""}`}</h2>
                <p>
                  {selectedEntry.profileCount > 0
                    ? `Across ${selectedEntry.profileCount} profile${selectedEntry.profileCount === 1 ? "" : "s"}`
                    : "No completed profile runs yet"}
                </p>
                {selectedEntry.sampleProfiles?.length ? (
                  <p className="task-stats-sample">
                      Recently active: {formatSampleProfiles(selectedEntry.sampleProfiles, selectedEntry.profileCount)}
                  </p>
                ) : null}
              </div>
              <button type="button" className="task-stats-close-button" onClick={handleCloseDetails}>
                <FiX />
              </button>
            </header>
            <section className="task-stats-modal-body">
              <dl className="task-stats-details">
                <div>
                  <dt>Total runs</dt>
                  <dd>{selectedEntry.totalRuns}</dd>
                </div>
                <div>
                  <dt>Successes</dt>
                  <dd>{selectedEntry.successCount}</dd>
                </div>
                <div>
                  <dt>Failures</dt>
                  <dd>{selectedEntry.failureCount}</dd>
                </div>
                <div>
                  <dt>Success rate</dt>
                  <dd>{percentage(selectedEntry.successRate)}</dd>
                </div>
                <div>
                  <dt>Min duration</dt>
                  <dd>{formatDuration(selectedEntry.minDurationMillis)}</dd>
                </div>
                <div>
                  <dt>Max duration</dt>
                  <dd>{formatDuration(selectedEntry.maxDurationMillis)}</dd>
                </div>
                <div>
                  <dt>Average duration</dt>
                  <dd>{formatDuration(selectedEntry.averageDurationMillis)}</dd>
                </div>
                <div>
                  <dt>P95 duration</dt>
                  <dd>{formatDuration(selectedEntry.p95DurationMillis)}</dd>
                </div>
                <div>
                  <dt>Profiles covered</dt>
                  <dd>{selectedEntry.profileCount}</dd>
                </div>
                <div>
                  <dt>Last started</dt>
                  <dd>{selectedEntry.lastStartedAt ? formatDateTime(selectedEntry.lastStartedAt) : "N/A"}</dd>
                </div>
                <div>
                  <dt>Last finished</dt>
                  <dd>{selectedEntry.lastFinishedAt ? formatDateTime(selectedEntry.lastFinishedAt) : "N/A"}</dd>
                </div>
                <div>
                  <dt>Last execution</dt>
                  <dd>{lastExecutionForSelected ? formatDateTime(lastExecutionForSelected) : "N/A"}</dd>
                </div>
                <div>
                  <dt>Next scheduled</dt>
                  <dd>{nextExecutionForSelected ? formatDateTime(nextExecutionForSelected) : "N/A"}</dd>
                </div>
              </dl>
              {matchingTaskStates.length > 0 ? (
                <div className="task-stats-schedule">
                  <h3>Upcoming executions</h3>
                  <ul>
                    {matchingTaskStates.map((item) => {
                      const nextLabel = item.task.nextExecutionTime
                        ? formatDateTime(item.task.nextExecutionTime)
                        : "Not scheduled";
                      const lastLabel = item.task.lastExecutionTime
                        ? formatDateTime(item.task.lastExecutionTime)
                        : "N/A";
                        const profileLink = {
                            pathname: "/tasks",
                            state: {focusProfileId: String(item.profileId)},
                        };
                      return (
                        <li key={`${item.profileId}-${item.task.taskId ?? item.task.taskName ?? "unknown"}`}>
                          <div>
                            <span className="task-stats-schedule-profile">{item.profileName}</span>
                            <span className="task-stats-schedule-times">
                              <span>Next: {nextLabel}</span>
                              <span>Last: {lastLabel}</span>
                            </span>
                          </div>
                          <Link to={profileLink} onClick={handleCloseDetails}>
                            <FiExternalLink />
                            <span>Open</span>
                          </Link>
                        </li>
                      );
                    })}
                  </ul>
                </div>
              ) : (
                <div className="task-stats-schedule task-stats-schedule-empty">
                  <p>No active schedule information detected for this task.</p>
                </div>
              )}
              {selectedEntry.lastErrorMessage ? (
                <div className="task-stats-error-block">
                  <h3>Most recent error</h3>
                  <p>{selectedEntry.lastErrorMessage}</p>
                </div>
              ) : null}
            </section>
            <footer className="task-stats-modal-footer">
              <Link to={defaultTaskLink} className="task-stats-link-button" onClick={handleCloseDetails}>
                <FiExternalLink />
                Open in Task Manager
              </Link>
              <button type="button" className="task-stats-secondary-button" onClick={handleCloseDetails}>
                Close
              </button>
            </footer>
          </div>
        </div>
      )}
    </div>
  );
};

export default TaskStatsPage;
