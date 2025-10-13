import { useCallback, useEffect, useMemo, useState } from "react";
import { FiCheckSquare } from "react-icons/fi";

import type { Profile, TaskState } from "../types/api";
import { formatDateTime } from "../utils/format";

const TasksPage = () => {
  const [tasks, setTasks] = useState<Record<string, TaskState[]>>({});
  const [tasksLoading, setTasksLoading] = useState(false);
  const [tasksError, setTasksError] = useState<string | null>(null);
  const [taskProfileFilter, setTaskProfileFilter] = useState("");

  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [profilesLoading, setProfilesLoading] = useState(false);
  const [profilesError, setProfilesError] = useState<string | null>(null);

  const fetchTasks = useCallback(async () => {
    try {
      setTasksLoading(true);
      setTasksError(null);

      const response = await fetch("/api/tasks");
      if (!response.ok) {
        throw new Error(`Failed to fetch tasks: ${response.status}`);
      }

      const data = (await response.json()) as Record<string, TaskState[]>;
      setTasks(data ?? {});
    } catch (error) {
      console.error(error);
      setTasksError(error instanceof Error ? error.message : "Unknown error");
    } finally {
      setTasksLoading(false);
    }
  }, []);

  const fetchProfiles = useCallback(async () => {
    try {
      setProfilesLoading(true);
      setProfilesError(null);

      const response = await fetch("/api/profiles");
      if (!response.ok) {
        throw new Error(`Failed to fetch profiles: ${response.status}`);
      }

      const data = (await response.json()) as Profile[];
      setProfiles(data);
    } catch (error) {
      console.error(error);
      setProfilesError(error instanceof Error ? error.message : "Unknown error");
    } finally {
      setProfilesLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProfiles().catch(() => undefined);
    fetchTasks().catch(() => undefined);
  }, [fetchProfiles, fetchTasks]);

  const profilesById = useMemo(() => {
    const map = new Map<string, Profile>();
    profiles.forEach((profile) => {
      map.set(String(profile.id), profile);
    });
    return map;
  }, [profiles]);

  const filteredTaskEntries = useMemo(() => {
    const entries = Object.entries(tasks);
    if (!taskProfileFilter) {
      return entries;
    }
    return entries.filter(([profileId]) => profileId === taskProfileFilter);
  }, [taskProfileFilter, tasks]);

  const isLoading = tasksLoading || profilesLoading;
  const combinedError = tasksError ?? profilesError;

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
              onChange={(event) => setTaskProfileFilter(event.target.value)}
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
          ) : filteredTaskEntries.length === 0 ? (
            <div className="loading">
              {Object.keys(tasks).length === 0 ? "No tasks found." : "No tasks match the selected profile."}
            </div>
          ) : (
            filteredTaskEntries.map(([profileId, taskList]) => {
              const profile = profilesById.get(profileId);
              const profileName = profile?.name ?? `Profile ${profileId}`;

              return (
                <div className="profile-tasks-section" key={profileId}>
                  <div className="profile-tasks-header">
                    <div className="profile-tasks-name">{profileName}</div>
                  </div>
                      <div className="tasks-grid">
                    {taskList && taskList.length > 0 ? (
                      (() => {
                        const now = Date.now();
                        return [...taskList]
                          .sort((taskA, taskB) => {
                            const timeA = taskA.nextExecutionTime ? new Date(taskA.nextExecutionTime).getTime() : Number.POSITIVE_INFINITY;
                            const timeB = taskB.nextExecutionTime ? new Date(taskB.nextExecutionTime).getTime() : Number.POSITIVE_INFINITY;
                            const safeTimeA = Number.isFinite(timeA) ? timeA : Number.POSITIVE_INFINITY;
                            const safeTimeB = Number.isFinite(timeB) ? timeB : Number.POSITIVE_INFINITY;
                            return safeTimeA - safeTimeB;
                          })
                          .map((task, index) => {
                            const status = task.executing ? "executing" : task.scheduled ? "scheduled" : "disabled";
                            const statusText = task.executing ? "Executing" : task.scheduled ? "Scheduled" : "Disabled";
                            const nextExecutionMs = task.nextExecutionTime ? new Date(task.nextExecutionTime).getTime() : Number.NaN;
                            const normalizedNextExecutionMs = Number.isFinite(nextExecutionMs) ? nextExecutionMs : null;
                            let timingClass = "";
                            let nextExecutionClass = "";

                            if (normalizedNextExecutionMs !== null) {
                              const diff = normalizedNextExecutionMs - now;
                              if (diff <= 0) {
                                timingClass = "task-card-ready";
                                nextExecutionClass = "next-execution-ready";
                              } else if (diff <= 60_000) {
                                timingClass = "task-card-soon";
                                nextExecutionClass = "next-execution-soon";
                              } else if (diff >= 15 * 60_000) {
                                nextExecutionClass = "next-execution-late";
                              }
                            }

                            if (status === "executing") {
                              timingClass = "task-card-ready";
                              nextExecutionClass = "next-execution-ready";
                            }

                            return (
                              <div className={`task-card ${status} ${timingClass}`} key={`${task.taskName ?? "task"}-${index}`}>
                                <div className="task-name">{task.taskName ?? "Unknown Task"}</div>
                                <div className="task-details">
                                  <div className="task-detail">
                                    <span className="task-detail-label">Status:</span>
                                    <span className={`task-status-badge ${status}`}>{statusText}</span>
                                  </div>
                                  {task.lastExecutionTime ? (
                                    <div className="task-detail">
                                      <span className="task-detail-label">Last Run:</span>
                                      <span className="task-detail-value">{formatDateTime(task.lastExecutionTime)}</span>
                                    </div>
                                  ) : null}
                                  {task.nextExecutionTime ? (
                                    <div className="task-detail">
                                      <span className="task-detail-label">Next Run:</span>
                                      <span className={`task-detail-value next-execution-value ${nextExecutionClass}`}>
                                        {formatDateTime(task.nextExecutionTime)}
                                      </span>
                                    </div>
                                  ) : null}
                                </div>
                              </div>
                            );
                          });
                      })()
                    ) : (
                      <div className="loading">No tasks for this profile.</div>
                    )}
                  </div>
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
