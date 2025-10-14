/* eslint-disable react-refresh/only-export-components */

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
import { getTasksSnapshot, hasTasksSnapshot, subscribeTasksStore } from "../services/tasksStore";

const FIVE_MINUTES_MS = 5 * 60 * 1000;
const ONE_MINUTE_MS = 60 * 1000;

export const parseDateToMs = (value?: string | null): number | null => {
  if (!value) {
    return null;
  }
  const time = new Date(value).getTime();
  return Number.isFinite(time) ? time : null;
};

const getTaskCategory = (task: TaskState, now: number): number => {
  if (task.executing) {
    return 0;
  }
  if (!task.scheduled) {
    return 3;
  }
  const nextMs = parseDateToMs(task.nextExecutionTime);
  if (nextMs !== null && nextMs <= now) {
    return 1;
  }
  return 2;
};

const getNextExecutionForSort = (task: TaskState): number => {
  const nextMs = parseDateToMs(task.nextExecutionTime);
  return nextMs ?? Number.POSITIVE_INFINITY;
};

export const sortTasksForDisplay = (taskList: TaskState[], now: number): TaskState[] =>
  [...taskList].sort((taskA, taskB) => {
    const catDiff = getTaskCategory(taskA, now) - getTaskCategory(taskB, now);
    if (catDiff !== 0) {
      return catDiff;
    }
    const timeDiff = getNextExecutionForSort(taskA) - getNextExecutionForSort(taskB);
    if (timeDiff !== 0) {
      return timeDiff;
    }
    return (taskA.taskName ?? "").localeCompare(taskB.taskName ?? "", undefined, { sensitivity: "base" });
  });

const getTaskStatusMeta = (task: TaskState): { statusClass: string; statusText: string } => {
  if (task.executing) {
    return { statusClass: "executing", statusText: "Executing" };
  }
  if (task.scheduled) {
    return { statusClass: "scheduled", statusText: "Scheduled" };
  }
  return { statusClass: "disabled", statusText: "Disabled" };
};

const getTaskTimingMeta = (
  task: TaskState,
  now: number,
): { timingClass: string; nextExecutionClass: string } => {
  if (task.executing) {
    return { timingClass: "task-card-imminent", nextExecutionClass: "next-execution-imminent" };
  }
  if (!task.scheduled) {
    return { timingClass: "task-card-disabled", nextExecutionClass: "" };
  }

  const nextMs = parseDateToMs(task.nextExecutionTime);
  if (nextMs === null) {
    return { timingClass: "task-card-upcoming", nextExecutionClass: "" };
  }

  const diff = nextMs - now;
  if (diff <= 0) {
    return { timingClass: "task-card-ready", nextExecutionClass: "next-execution-ready" };
  }
  if (diff <= ONE_MINUTE_MS) {
    return { timingClass: "task-card-imminent", nextExecutionClass: "next-execution-imminent" };
  }
  if (diff <= FIVE_MINUTES_MS) {
    return { timingClass: "task-card-upcoming", nextExecutionClass: "next-execution-imminent" };
  }
  return { timingClass: "task-card-upcoming", nextExecutionClass: "next-execution-upcoming" };
};

const getTaskDisplayMeta = (task: TaskState, now: number) => {
  const { statusClass, statusText } = getTaskStatusMeta(task);
  const { timingClass, nextExecutionClass } = getTaskTimingMeta(task, now);
  const cardClassName = ["task-card", statusClass, timingClass].filter(Boolean).join(" ");
  return {
    cardClassName,
    statusClass,
    statusText,
    nextExecutionClass,
  };
};

export type ProfileStatusKey =
  | "idle-running"
  | "running-task"
  | "waiting-slot"
  | "disabled"
  | "stopped"
  | "paused";

export const PROFILE_STATUS_ORDER: Record<ProfileStatusKey, number> = {
  "running-task": 0,
  "idle-running": 1,
  "waiting-slot": 2,
  disabled: 3,
  stopped: 4,
  paused: 5,
};

const PROFILE_STATUS_LABEL: Record<ProfileStatusKey, string> = {
  "idle-running": "Idle (Emulator Ready)",
  "running-task": "Running Task",
  "waiting-slot": "Waiting for Slot",
  disabled: "Disabled",
  stopped: "Stopped",
  paused: "Paused",
};

export interface ProfileSummaryMeta {
  statusKey: ProfileStatusKey;
  statusClass: string;
  statusText: string;
  statusDetail: string;
  nextExecutionClass: string;
  nextExecutionLabel: string;
  orderRank: number;
  nextExecutionSort: number;
  prioritySort: number;
}

const describeIdleWindow = (diffMs: number): string => {
  if (diffMs <= ONE_MINUTE_MS) {
    return "Next task within 1 minute";
  }
  if (diffMs <= FIVE_MINUTES_MS) {
    return "Next task within 5 minutes";
  }
  return "Awaiting next schedule";
};

export const getProfileSummaryMeta = (
  profile: Profile | undefined,
  tasks: TaskState[],
  now: number,
): ProfileSummaryMeta => {
  const executingTask = tasks.find((task) => task.executing);
  const scheduledTasks = tasks
    .map((task) => {
      const next = parseDateToMs(task.nextExecutionTime);
      return next === null ? null : { task, next };
    })
    .filter((entry): entry is { task: TaskState; next: number } => entry !== null)
    .sort((a, b) => a.next - b.next);

  const readyTask = scheduledTasks.find((entry) => entry.next <= now);
  const upcomingTask = scheduledTasks[0];
  const queueActive = profile?.queueActive;
  const queuePosition = profile?.queuePosition ?? null;
  const hasTasks = tasks.length > 0;
  const prioritySort = typeof profile?.priority === "number" ? profile.priority : Number.POSITIVE_INFINITY;

  let statusKey: ProfileStatusKey = "idle-running";
  let statusDetail = profile?.state ?? "Emulator ready";
  let nextExecutionClass = "";
  let nextExecutionLabel = "None";
  let nextExecutionSort = Number.POSITIVE_INFINITY;

  if (profile?.paused) {
    statusKey = "paused";
    statusDetail = profile.state ?? "Task queue paused";
    if (upcomingTask) {
      const timingMeta = getTaskTimingMeta(upcomingTask.task, now);
      nextExecutionClass = timingMeta.nextExecutionClass;
      nextExecutionLabel = upcomingTask.task.nextExecutionTime
        ? formatDateTime(upcomingTask.task.nextExecutionTime)
        : "Pending resume";
      nextExecutionSort = upcomingTask.next;
    } else {
      nextExecutionLabel = "Pending resume";
    }
  } else if (executingTask) {
    statusKey = "running-task";
    statusDetail = executingTask.taskName ? `Running ${executingTask.taskName}` : "Processing queued task";
    const timingMeta = getTaskTimingMeta(executingTask, now);
    nextExecutionClass = timingMeta.nextExecutionClass;
    nextExecutionLabel = executingTask.nextExecutionTime
      ? formatDateTime(executingTask.nextExecutionTime)
      : "In progress";
    nextExecutionSort = parseDateToMs(executingTask.nextExecutionTime) ?? now;
  } else {
    const enabled = profile?.enabled ?? true;
    const isDisabled = !enabled || (!hasTasks && (queuePosition === null || queuePosition === undefined));

    if (isDisabled) {
      statusKey = "disabled";
      statusDetail = profile?.state ?? "Queue disabled";
      nextExecutionLabel = "None";
      nextExecutionSort = Number.POSITIVE_INFINITY;
    } else if (profile?.running === false) {
      statusKey = "stopped";
      statusDetail = profile?.state ?? "Task queue is stopped";
      nextExecutionLabel = "None";
      nextExecutionSort = Number.POSITIVE_INFINITY;
    } else if ((queuePosition !== null && queuePosition > 0 && queuePosition !== 2147483647) || queueActive === false) {
      statusKey = "waiting-slot";
      statusDetail =
        queuePosition && queuePosition > 0
          ? `Waiting for slot (#${queuePosition})`
          : "Waiting for available slot";
      if (upcomingTask) {
        const timingMeta = getTaskTimingMeta(upcomingTask.task, now);
        nextExecutionClass = timingMeta.nextExecutionClass;
        nextExecutionLabel = upcomingTask.task.nextExecutionTime
          ? formatDateTime(upcomingTask.task.nextExecutionTime)
          : "Queued";
        nextExecutionSort = upcomingTask.next;
      } else {
        nextExecutionLabel = "Queued";
      }
    } else if (readyTask) {
      statusKey = "idle-running";
      statusDetail = "Task ready to execute";
      const timingMeta = getTaskTimingMeta(readyTask.task, now);
      nextExecutionClass = timingMeta.nextExecutionClass;
      nextExecutionLabel = readyTask.task.nextExecutionTime ? formatDateTime(readyTask.task.nextExecutionTime) : "Ready now";
      nextExecutionSort = readyTask.next;
    } else if (upcomingTask) {
      statusKey = "idle-running";
      const diff = upcomingTask.next - now;
      statusDetail = describeIdleWindow(diff);
      const timingMeta = getTaskTimingMeta(upcomingTask.task, now);
      nextExecutionClass = timingMeta.nextExecutionClass;
      nextExecutionLabel =
        upcomingTask.task.nextExecutionTime ? formatDateTime(upcomingTask.task.nextExecutionTime) : "Scheduled";
      nextExecutionSort = upcomingTask.next;
    } else {
      statusKey = "idle-running";
      statusDetail = profile?.state ?? "Emulator ready";
      nextExecutionLabel = "None";
      nextExecutionSort = Number.POSITIVE_INFINITY;
    }
  }

  return {
    statusKey,
    statusClass: statusKey,
    statusText: PROFILE_STATUS_LABEL[statusKey],
    statusDetail,
    nextExecutionClass,
    nextExecutionLabel,
    orderRank: PROFILE_STATUS_ORDER[statusKey],
    nextExecutionSort,
    prioritySort,
  };
};

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
  const now = Date.now();
  const preparedEntries = filteredTaskEntries.map(([profileId, taskList]) => {
    const profile = profilesById.get(profileId);
    const profileName = profile?.name ?? `Profile ${profileId}`;
    const sortedTasks = sortTasksForDisplay(taskList ?? [], now);
    const summaryMeta = getProfileSummaryMeta(profile, sortedTasks, now);
    const nextExecutionCountdown =
      Number.isFinite(summaryMeta.nextExecutionSort) && summaryMeta.nextExecutionSort !== Number.POSITIVE_INFINITY
        ? formatDuration(Math.max(summaryMeta.nextExecutionSort - now, 0))
        : "";
    return {
      profileId,
      profile,
      profileName,
      sortedTasks,
      summaryMeta,
      nextExecutionCountdown,
    };
  });

  preparedEntries.sort((entryA, entryB) => {
    const rankDiff = entryA.summaryMeta.orderRank - entryB.summaryMeta.orderRank;
    if (rankDiff !== 0) {
      return rankDiff;
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
              ({ profileId, profileName, profile, sortedTasks, summaryMeta, nextExecutionCountdown }) => {
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
                        {nextExecutionCountdown ? (
                          <div className="profile-summary-meta">
                            <span className="summary-label">Next Task</span>
                            <span
                              className={`summary-value next-execution-value ${summaryMeta.nextExecutionClass}`}
                              title={summaryMeta.nextExecutionLabel || undefined}
                            >
                              {nextExecutionCountdown}
                            </span>
                          </div>
                        ) : (
                          <div className="profile-summary-meta">
                            <span className="summary-label">Next Task</span>
                            <span
                              className={`summary-value next-execution-value ${summaryMeta.nextExecutionClass}`}
                              title={summaryMeta.nextExecutionLabel || undefined}
                            >
                              {summaryMeta.nextExecutionLabel}
                            </span>
                          </div>
                        )}
                        <div className="profile-summary-meta">
                          <span className="summary-label">Tasks</span>
                          <span className="summary-value">{sortedTasks.length}</span>
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
                                      className={`task-detail-value next-execution-value ${nextExecutionClass}`}
                                      title={formatDateTime(nextExecutionTime)}
                                    >
                                      {nextExecutionCountdown}
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
