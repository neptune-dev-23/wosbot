
import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { FiCheckSquare, FiChevronDown, FiClock } from "react-icons/fi";
import { useLocation } from "react-router-dom";

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

interface RescheduleContext {
  profileId: string;
  profileName: string;
  task: TaskState;
}

const padTimePart = (value: number) => String(value).padStart(2, "0");

const toLocalDateTimeInputValue = (date: Date) => {
  const year = date.getFullYear();
  const month = padTimePart(date.getMonth() + 1);
  const day = padTimePart(date.getDate());
  const hours = padTimePart(date.getHours());
  const minutes = padTimePart(date.getMinutes());
  return `${year}-${month}-${day}T${hours}:${minutes}`;
};

const normalizeInputToPayload = (value: string) => {
  if (!value) {
    return null;
  }
  if (value.length === 16) {
    return `${value}:00`;
  }
  return value;
};

const TasksPage = () => {
  const location = useLocation();
  const focusProfileId = (location.state as { focusProfileId?: string })?.focusProfileId;

  const [tasks, setTasks] = useState<Record<string, TaskState[]>>(() => getTasksSnapshot());
  const [profiles, setProfiles] = useState<Profile[]>(() => getProfilesSnapshot());
  const [tasksLoaded, setTasksLoaded] = useState(hasTasksSnapshot());
  const [profilesLoaded, setProfilesLoaded] = useState(hasProfilesSnapshot());
  const [taskProfileFilter, setTaskProfileFilter] = useState("");
  const [expandedProfiles, setExpandedProfiles] = useState<Record<string, boolean>>({});
  const [error, setError] = useState<string | null>(null);
  const [rescheduleTarget, setRescheduleTarget] = useState<RescheduleContext | null>(null);
  const [rescheduleValue, setRescheduleValue] = useState<string>(() => toLocalDateTimeInputValue(new Date()));
  const [rescheduleError, setRescheduleError] = useState<string | null>(null);
  const [rescheduleSubmitting, setRescheduleSubmitting] = useState(false);
  const [lastTasksUpdateTime, setLastTasksUpdateTime] = useState(() => Date.now());

  useEffect(() => {
    const unsubscribeTasks = subscribeTasksStore((next) => {
      setTasks(next);
      setTasksLoaded(true);
      setLastTasksUpdateTime(Date.now());
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
    if (focusProfileId) {
      setExpandedProfiles((prev) => ({ ...prev, [focusProfileId]: true }));
      // When focusing a profile, we don't want to also have a filter active
      setTaskProfileFilter("");
    }
  }, [focusProfileId]);

  const handleProfileFilterChange = useCallback((value: string) => {
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
  }, []);

  const toggleProfileExpansion = useCallback((profileId: string) => {
    setExpandedProfiles((previous) => {
      const updated = { ...previous };
      if (updated[profileId]) {
        delete updated[profileId];
      } else {
        updated[profileId] = true;
      }
      return updated;
    });
  }, []);

  const isProfileExpanded = useCallback(
    (profileId: string) => expandedProfiles[profileId] ?? false,
    [expandedProfiles],
  );

  useEffect(() => {
    if (!rescheduleTarget) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setRescheduleTarget(null);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [rescheduleTarget]);

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

    const preparedEntries = useMemo(() => {
        const sortTime = lastTasksUpdateTime;
        const entries = filteredTaskEntries.map(([profileId, taskList]) => {
            const profile = profilesById.get(profileId);
            const profileName = profile?.name ?? `Profile ${profileId}`;
            const sortedTasks = sortTasksForDisplay(taskList ?? [], sortTime);
            const summaryMeta = getProfileSummaryMeta(profile, sortedTasks, sortTime);
            const enabledTasksCount = sortedTasks.filter((t) => t.scheduled).length;
            const readyTasksCount = sortedTasks.filter((t) => {
                const category = getTaskCategory(t, sortTime);
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

        entries.sort((entryA, entryB) => {
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

            return entryA.profileName.localeCompare(entryB.profileName, undefined, {sensitivity: "base"});
        });

        return entries;
    }, [filteredTaskEntries, profilesById, lastTasksUpdateTime]);

  const openRescheduleModal = useCallback(
    (profileId: string, profileName: string, task: TaskState) => {
      setRescheduleTarget({ profileId, profileName, task });
      setRescheduleValue(toLocalDateTimeInputValue(new Date()));
      setRescheduleError(null);
    },
    [],
  );

  const handleRescheduleCancel = useCallback(() => {
    setRescheduleTarget(null);
    setRescheduleError(null);
  }, []);

  const handleRescheduleSubmit = useCallback(
    async (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (!rescheduleTarget) {
        return;
      }

      const { task, profileId } = rescheduleTarget;
      if (task.taskId == null) {
        setRescheduleError("This task cannot be rescheduled because it is missing an identifier.");
        return;
      }

      const normalized = normalizeInputToPayload(rescheduleValue);
      if (!normalized) {
        setRescheduleError("Please pick a new date and time.");
        return;
      }

      const profileIdValue = task.profileId ?? Number.parseInt(profileId, 10);
      if (!Number.isFinite(profileIdValue)) {
        setRescheduleError("Missing profile information, cannot reschedule right now.");
        return;
      }

      const payload = {
        profileId: profileIdValue,
        taskId: task.taskId,
        taskName: task.taskName,
        scheduled: true,
        nextExecutionTime: normalized,
      };

      setRescheduleSubmitting(true);
      setRescheduleError(null);

      try {
        const response = await fetch("/api/tasks/reschedule", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(payload),
        });

        if (!response.ok) {
          let message = "Failed to reschedule task.";
          try {
            const data = (await response.json()) as { message?: string; error?: string };
            message = data?.message ?? data?.error ?? message;
          } catch {
            // ignore parse failure
          }
          throw new Error(message);
        }

        setRescheduleTarget(null);
      } catch (submitError) {
        const message =
          submitError instanceof Error ? submitError.message : "Failed to reschedule task.";
        setRescheduleError(message);
      } finally {
        setRescheduleSubmitting(false);
      }
    },
    [rescheduleTarget, rescheduleValue],
  );

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
                ({profileId, profileName, profile, sortedTasks, summaryMeta, tasksCountDisplay}) => {
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
                  <div className={`tasks-grid-wrapper ${expanded ? "expanded" : "collapsed"}`} aria-hidden={!expanded}>
                    <div className="tasks-grid" id={`profile-tasks-${profileId}`}>
                      {expanded
                        ? sortedTasks.length > 0
                          ? sortedTasks.map((task, index) => {
                              const { cardClassName, statusClass, statusText, nextExecutionClass } = getTaskDisplayMeta(
                                task,
                                now,
                              );
                              const nextExecutionMs = parseDateToMs(task.nextExecutionTime);
                              const nextExecutionTime = task.nextExecutionTime;
                              const nextExecutionCountdown =
                                nextExecutionMs !== null ? formatDuration(Math.max(nextExecutionMs - now, 0)) : null;
                              const taskKey =
                                task.taskId != null ? `task-${task.taskId}` : `${task.taskName ?? "task"}-${index}`;

                              return (
                                <div className={cardClassName} key={taskKey}>
                                  <button
                                    type="button"
                                    className="task-reschedule-button"
                                    onClick={() => openRescheduleModal(profileId, profileName, task)}
                                    aria-label={`Reschedule ${task.taskName ?? "task"}`}
                                    title="Reschedule"
                                  >
                                    <FiClock aria-hidden="true" size={16} />
                                  </button>
                                  <div className="task-name">{task.taskName ?? "Unknown Task"}</div>
                                  <div className="task-details">
                                    <div className="task-detail">
                                      <span className="task-detail-label">Status:</span>
                                      <span className={`task-status-badge ${statusClass}`}>{statusText}</span>
                                    </div>
                                    {task.lastExecutionTime ? (
                                      <div className="task-detail">
                                        <span className="task-detail-label">Last Run:</span>
                                        <span className="task-detail-value">
                                          {formatDateTime(task.lastExecutionTime)}
                                        </span>
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
                          : <div className="loading">No tasks for this profile.</div>
                        : null}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
      {rescheduleTarget ? (
        <div className="task-reschedule-overlay" role="presentation" onClick={handleRescheduleCancel}>
          <div
            className="task-reschedule-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="task-reschedule-title"
            onClick={(event) => event.stopPropagation()}
          >
            <header className="task-reschedule-header">
              <FiClock aria-hidden="true" size={20} />
              <div>
                <h2 id="task-reschedule-title">Reschedule Task</h2>
                <p>
                  {rescheduleTarget.task.taskName ?? "Task"} · {rescheduleTarget.profileName}
                </p>
              </div>
            </header>
            <form className="task-reschedule-form" onSubmit={handleRescheduleSubmit}>
              <label className="task-reschedule-label" htmlFor="task-reschedule-datetime">
                Next execution time
              </label>
              <input
                id="task-reschedule-datetime"
                className="task-reschedule-input"
                type="datetime-local"
                value={rescheduleValue}
                onChange={(event) => setRescheduleValue(event.target.value)}
                min={toLocalDateTimeInputValue(new Date())}
                step={60}
                required
              />
              {rescheduleTarget.task.nextExecutionTime ? (
                <p className="task-reschedule-hint">
                  Currently scheduled for {formatDateTime(rescheduleTarget.task.nextExecutionTime)}.
                </p>
              ) : (
                <p className="task-reschedule-hint">This task does not have a scheduled run yet.</p>
              )}
              {rescheduleError ? <p className="task-reschedule-error">{rescheduleError}</p> : null}
              <div className="task-reschedule-actions">
                <button
                  type="button"
                  className="task-reschedule-cancel"
                  onClick={handleRescheduleCancel}
                  disabled={rescheduleSubmitting}
                >
                  Cancel
                </button>
                <button type="submit" className="task-reschedule-submit" disabled={rescheduleSubmitting}>
                  {rescheduleSubmitting ? "Scheduling…" : "Schedule"}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  );
};

export default TasksPage;
