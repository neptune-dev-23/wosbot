

import { useCallback, useEffect, useMemo, useState } from "react";
import { FiCheckSquare, FiChevronDown } from "react-icons/fi";
import { useSearchParams } from "react-router-dom";

import type { Profile, TaskState } from "../types/api";
import { formatDateTime, formatDuration } from "../utils/format";
import {
  ensureProfilesInitialized,
  getProfilesSnapshot,
  hasProfilesSnapshot,
  subscribeProfilesStore,
} from "../services/profilesStore";
import { useCurrentTime } from "../hooks/useCurrentTime";
import { getTasksSnapshot, hasTasksSnapshot, subscribeTasksStore } from "../services/tasksStore";

import {
  getProfileSummaryMeta,
  getTaskCategory,
  getTaskDisplayMeta,
  parseDateToMs,
  PROFILE_STATUS_ORDER,
  sortTasksForDisplay,
} from "../utils/tasks";

const TasksPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const profileParam = searchParams.get("profile") ?? "";
  const expandParam = searchParams.get("expand") ?? "";
  const focusParam = searchParams.get("focus") ?? "";
  const [tasks, setTasks] = useState<Record<string, TaskState[]>>(() => getTasksSnapshot());
  const [profiles, setProfiles] = useState<Profile[]>(() => getProfilesSnapshot());
  const [tasksLoaded, setTasksLoaded] = useState(hasTasksSnapshot());
  const [profilesLoaded, setProfilesLoaded] = useState(hasProfilesSnapshot());
  const [taskProfileFilter, setTaskProfileFilter] = useState(() => (focusParam ? "" : profileParam));
  const [expandedProfiles, setExpandedProfiles] = useState<Record<string, boolean>>(() => {
    if (focusParam) {
      return { [focusParam]: true };
    }
    if (expandParam) {
      return { [expandParam]: true };
    }
    if (profileParam) {
      return { [profileParam]: true };
    }
    return {};
  });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribeTasks = subscribeTasksStore((next) => {
      setTasks(next);
      setTasksLoaded(true);
    });
    const unsubscribeProfiles = subscribeProfilesStore((next) => {
      setProfiles(next);
      setProfilesLoaded(true);
      setError(null);
    });

    if (!hasProfilesSnapshot()) {
      ensureProfilesInitialized()?.catch((initialError) => {
        const message =
          initialError instanceof Error ? initialError.message : "Failed to load profiles";
        setError(message);
        setProfilesLoaded(true);
      });
    }

    return () => {
      unsubscribeTasks();
      unsubscribeProfiles();
    };
  }, []);

  useEffect(() => {
    if (focusParam) {
      return;
    }
    if (profileParam !== taskProfileFilter) {
      setTaskProfileFilter(profileParam);
    }
  }, [focusParam, profileParam, taskProfileFilter]);

  useEffect(() => {
    if (focusParam) {
      return;
    }
    const target = expandParam || profileParam;
    if (!target) {
      return;
    }
    setExpandedProfiles((previous) => {
      if (previous[target]) {
        return previous;
      }
      return {
        ...previous,
        [target]: true,
      };
    });
  }, [focusParam, expandParam, profileParam]);

  const handleProfileFilterChange = useCallback(
    (value: string) => {
      setTaskProfileFilter(value);
      if (value) {
        setExpandedProfiles((previous) => {
          if (previous[value]) {
            return previous;
          }
          return {
            ...previous,
            [value]: true,
          };
        });
      }

      const next = new URLSearchParams(searchParams);
      next.delete("focus");
      if (value) {
        next.set("profile", value);
        next.set("expand", value);
      } else {
        next.delete("profile");
        next.delete("expand");
      }
      setSearchParams(next, { replace: true });
    },
    [searchParams, setSearchParams],
  );

  const toggleProfileExpansion = useCallback(
    (profileId: string) => {
      const nextExpanded = !(expandedProfiles[profileId] ?? false);
      setExpandedProfiles((previous) => {
        if (nextExpanded) {
          if (previous[profileId]) {
            return previous;
          }
          return {
            ...previous,
            [profileId]: true,
          };
        }

        if (!previous[profileId]) {
          return previous;
        }

        const updated = { ...previous };
        delete updated[profileId];
        return updated;
      });

      const next = new URLSearchParams(searchParams);
      next.delete("focus");
      if (nextExpanded) {
        next.set("expand", profileId);
      } else if (next.get("expand") === profileId) {
        next.delete("expand");
      }
      setSearchParams(next, { replace: true });
    },
    [expandedProfiles, searchParams, setSearchParams],
  );

  const isProfileExpanded = useCallback(
    (profileId: string) => expandedProfiles[profileId] ?? false,
    [expandedProfiles],
  );

  const profilesById = useMemo(() => {
    const map = new Map<string, Profile>();
    profiles.forEach((profile) => {
      map.set(String(profile.id), profile);
    });
    return map;
  }, [profiles]);

  const allProfileEntries = useMemo(() => {
    const seen = new Set<string>();
    const entries: Array<[string, TaskState[]]> = [];

    profiles.forEach((profile) => {
      const key = String(profile.id);
      entries.push([key, tasks[key] ?? []]);
      seen.add(key);
    });

    Object.entries(tasks).forEach(([profileId, taskList]) => {
      if (!seen.has(profileId)) {
        entries.push([profileId, taskList ?? []]);
      }
    });

    return entries;
  }, [profiles, tasks]);

  const filteredTaskEntries = useMemo(() => {
    if (!taskProfileFilter) {
      return allProfileEntries;
    }
    return allProfileEntries.filter(([profileId]) => profileId === taskProfileFilter);
  }, [allProfileEntries, taskProfileFilter]);

  const isLoading = !tasksLoaded || !profilesLoaded;
  const combinedError = error;
  const now = useCurrentTime();
  const preparedEntries = filteredTaskEntries.map(([profileId, taskList]) => {
    const profile = profilesById.get(profileId);
    const profileName = profile?.name ?? `Profile ${profileId}`;
    const sortedTasks = sortTasksForDisplay(taskList ?? [], now);
    const summaryMeta = getProfileSummaryMeta(profile, sortedTasks, now);
    const enabledTasksCount = sortedTasks.filter((t) => t.scheduled).length;
    const readyTasksCount = sortedTasks.filter((t) => {
      const category = getTaskCategory(t, now);
      return category === 0 || category === 1;
    }).length;
    const tasksCountDisplay = `${readyTasksCount} / ${enabledTasksCount}`;
    return {
      profileId,
      profile,
      profileName,
      sortedTasks,
      summaryMeta,
      tasksCountDisplay,
    };
  });

  preparedEntries.sort((entryA, entryB) => {
    const rankDiff = entryA.summaryMeta.orderRank - entryB.summaryMeta.orderRank;
    if (rankDiff !== 0) {
      return rankDiff;
    }

    if (entryA.summaryMeta.orderRank === PROFILE_STATUS_ORDER["waiting-slot"]) {
      const posA = entryA.profile?.queuePosition ?? Number.MAX_SAFE_INTEGER;
      const posB = entryB.profile?.queuePosition ?? Number.MAX_SAFE_INTEGER;
      const posDiff = posA - posB;
      if (posDiff !== 0) {
        return posDiff;
      }
    }

    if (entryA.summaryMeta.orderRank === PROFILE_STATUS_ORDER.disabled) {
      const priorityA = entryA.summaryMeta.prioritySort;
      const priorityB = entryB.summaryMeta.prioritySort;
      if (priorityA !== priorityB) {
        return priorityA - priorityB;
      }
    }

    const timeA = entryA.summaryMeta.nextExecutionSort;
    const timeB = entryB.summaryMeta.nextExecutionSort;
    const timeAFinite = Number.isFinite(timeA);
    const timeBFinite = Number.isFinite(timeB);

    if (timeAFinite || timeBFinite) {
      if (!timeAFinite) {
        return 1;
      }
      if (!timeBFinite) {
        return -1;
      }
      const diff = timeA - timeB;
      if (diff !== 0) {
        return diff;
      }
    }

    return entryA.profileName.localeCompare(entryB.profileName, undefined, { sensitivity: "base" });
  });

  return (
    <div className="view active" id="tasksView">
      <div className="header">
        <h1>
          <FiCheckSquare aria-hidden="true" className="header-icon" size={24} />
          <span>Task Manager</span>
        </h1>
        <div className="controls">
          <div className="filter-group">
            <label htmlFor="taskProfileFilter">Profile:</label>
            <select
              className="filter-input"
              id="taskProfileFilter"
              value={taskProfileFilter}
              onChange={(event) => handleProfileFilterChange(event.target.value)}
            >
              <option value="">All Profiles</option>
              {profiles.map((profile) => (
                <option key={profile.id} value={String(profile.id)}>
                  {profile.name ?? "Unnamed"}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>
      <div className="content-container">
        <div className="tasks-container" id="tasksContainer">
          {isLoading ? (
            <div className="loading">Loading tasks...</div>
          ) : combinedError ? (
            <div className="loading">Error loading tasks. Please try again.</div>
          ) : preparedEntries.length === 0 ? (
            <div className="loading">
              {Object.keys(tasks).length === 0 ? "No tasks found." : "No tasks match the selected profile."}
            </div>
          ) : (
            preparedEntries.map(
              ({ profileId, profileName, profile, sortedTasks, summaryMeta, tasksCountDisplay }, index) => {
              if (index === 0) {
                // console.log("--- TASK PAGE DIAGNOSTIC ---");
                // console.log("Profile:", profileName);
                // console.log("Summary Meta:", summaryMeta);
                // console.log("Next 3 Tasks:", sortedTasks.slice(0, 3));
              }
              const expanded = isProfileExpanded(profileId);

              return (
                <div className={`profile-tasks-section ${expanded ? "expanded" : "collapsed"}`} key={profileId}>
                  <button
                    type="button"
                    className="profile-summary"
                    onClick={() => toggleProfileExpansion(profileId)}
                    aria-expanded={expanded}
                    aria-controls={`profile-tasks-${profileId}`}
                  >
                    <div className={`profile-summary-card task-card ${summaryMeta.statusClass}`}>
                      <div className="profile-summary-left">
                        <div className="profile-summary-title">
                          <span className="profile-summary-name">{profileName}</span>
                          <FiChevronDown
                            aria-hidden="true"
                            className={`profile-summary-caret ${expanded ? "open" : ""}`}
                            size={18}
                          />
                        </div>
                        <div className="profile-summary-status">
                          <span className={`task-status-badge ${summaryMeta.statusClass}`}>{summaryMeta.statusText}</span>
                          <span className="profile-summary-detail">{summaryMeta.statusDetail}</span>
                        </div>
                        {profile?.state ? <div className="profile-summary-profile-state">{profile.state}</div> : null}
                      </div>
                      <div className="profile-summary-right">
                        {(() => {
                          const isExecuting = summaryMeta.statusKey === "running-task";
                          const remainingMs =
                            summaryMeta.nextExecutionSort !== Number.POSITIVE_INFINITY
                              ? Math.max(summaryMeta.nextExecutionSort - now, 0)
                              : -1;
                          const isReady = !isExecuting && remainingMs === 0;

                          let countdownDisplay;
                          if (isExecuting) {
                            countdownDisplay = "Executing";
                          } else if (isReady) {
                            countdownDisplay = "Ready";
                          } else if (remainingMs > -1) {
                            countdownDisplay = formatDuration(remainingMs);
                          } else {
                            countdownDisplay = summaryMeta.nextExecutionLabel;
                          }

                          let countdownClassName = `summary-value next-execution-value ${summaryMeta.nextExecutionClass}`;
                          if (isExecuting) {
                            countdownClassName += " next-execution-executing";
                          } else if (isReady) {
                            countdownClassName += " next-execution-ready";
                          }

                          return (
                            <div className="profile-summary-meta">
                              <span className="summary-label">Next Task</span>
                              <span className={countdownClassName} title={summaryMeta.nextExecutionLabel || undefined}>
                                {countdownDisplay}
                              </span>
                            </div>
                          );
                        })()}
                        <div className="profile-summary-meta">
                          <span className="summary-label">Tasks (Ready/Sched.)</span>
                          <span className="summary-value">{tasksCountDisplay}</span>
                        </div>
                      </div>
                    </div>
                  </button>
                  {expanded ? (
                    <div className="tasks-grid" id={`profile-tasks-${profileId}`}>
                      {sortedTasks.length > 0 ? (
                        sortedTasks.map((task, index) => {
                          const { cardClassName, statusClass, statusText, nextExecutionClass } = getTaskDisplayMeta(task, now);
                          const nextExecutionMs = parseDateToMs(task.nextExecutionTime);
                          const nextExecutionTime = task.nextExecutionTime;
                          const nextExecutionCountdown =
                            nextExecutionMs !== null ? formatDuration(Math.max(nextExecutionMs - now, 0)) : null;
                          const taskKey = task.taskId != null ? `task-${task.taskId}` : `${task.taskName ?? "task"}-${index}`;

                          return (
                            <div className={cardClassName} key={taskKey}>
                              <div className="task-name">{task.taskName ?? "Unknown Task"}</div>
                              <div className="task-details">
                                <div className="task-detail">
                                  <span className="task-detail-label">Status:</span>
                                  <span className={`task-status-badge ${statusClass}`}>{statusText}</span>
                                </div>
                                {task.lastExecutionTime ? (
                                  <div className="task-detail">
                                    <span className="task-detail-label">Last Run:</span>
                                    <span className="task-detail-value">{formatDateTime(task.lastExecutionTime)}</span>
                                  </div>
                                ) : null}
                                {nextExecutionMs !== null && nextExecutionTime ? (
                                  <div className="task-detail">
                                    <span className="task-detail-label">Next Run:</span>
                                    <span
                                      className={`task-detail-value next-execution-value ${nextExecutionClass} ${
                                        task.executing
                                          ? "next-execution-executing"
                                          : nextExecutionCountdown === "0s"
                                          ? "next-execution-ready"
                                          : ""
                                      }`}
                                      title={formatDateTime(nextExecutionTime)}
                                    >
                                      {task.executing
                                        ? "Executing"
                                        : nextExecutionCountdown === "0s"
                                        ? "Ready"
                                        : nextExecutionCountdown}
                                    </span>
                                  </div>
                                ) : null}
                              </div>
                            </div>
                          );
                        })
                      ) : (
                        <div className="loading">No tasks for this profile.</div>
                      )}
                    </div>
                  ) : null}
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};

export default TasksPage;
