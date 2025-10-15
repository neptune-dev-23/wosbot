import type { ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import { FiCheckSquare, FiUsers } from "react-icons/fi";
import { useNavigate } from "react-router-dom";

import type { Profile, ProfileConfig, TaskState } from "../types/api";
import { formatDateTime, formatDuration } from "../utils/format";
import {
  ensureProfilesInitialized,
  getProfilesSnapshot,
  hasProfilesSnapshot,
  subscribeProfilesStore,
} from "../services/profilesStore";
import { getTasksSnapshot, hasTasksSnapshot, subscribeTasksStore } from "../services/tasksStore";
import { useCurrentTime } from "../hooks/useCurrentTime";
import { getProfileSummaryMeta, parseDateToMs, PROFILE_STATUS_ORDER, sortTasksForDisplay } from "../utils/tasks";

const ProfilesPage = () => {
  const navigate = useNavigate();
  const [profiles, setProfiles] = useState<Profile[]>(() => getProfilesSnapshot());
  const [tasksByProfile, setTasksByProfile] = useState<Record<string, TaskState[]>>(() => getTasksSnapshot());
  const [profilesLoaded, setProfilesLoaded] = useState(hasProfilesSnapshot());
  const [tasksLoaded, setTasksLoaded] = useState(hasTasksSnapshot());
  const [error, setError] = useState<string | null>(null);
  const [selectedProfileId, setSelectedProfileId] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribeProfiles = subscribeProfilesStore((next) => {
      setProfiles(next);
      setProfilesLoaded(true);
      setError(null);
    });

    const unsubscribeTasks = subscribeTasksStore((next) => {
      setTasksByProfile(next);
      setTasksLoaded(true);
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
      unsubscribeProfiles();
      unsubscribeTasks();
    };
  }, []);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setSelectedProfileId(null);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  const now = useCurrentTime();

  const profileSummaries = useMemo(() => {
    const entries = profiles.map((profile) => {
      const profileId = String(profile.id);
      const tasks = tasksByProfile[profileId] ?? [];
      const sortedTasks = sortTasksForDisplay(tasks, now);
      const summaryMeta = getProfileSummaryMeta(profile, sortedTasks, now);
      const nextExecutionCountdown =
        Number.isFinite(summaryMeta.nextExecutionSort) && summaryMeta.nextExecutionSort !== Number.POSITIVE_INFINITY
          ? formatDuration(Math.max(summaryMeta.nextExecutionSort - now, 0))
          : "";
      return {
        profile,
        profileId,
        sortedTasks,
        summaryMeta,
        nextExecutionCountdown,
        statusClassName: `profile-status ${summaryMeta.statusClass}`,
        cardClassName: `profile-card interactive ${summaryMeta.statusClass}`,
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
        const priorityDiff = entryA.summaryMeta.prioritySort - entryB.summaryMeta.prioritySort;
        if (priorityDiff !== 0) {
          return priorityDiff;
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

      const nameA = entryA.profile.name ?? `Profile ${entryA.profileId}`;
      const nameB = entryB.profile.name ?? `Profile ${entryB.profileId}`;
      return nameA.localeCompare(nameB, undefined, { sensitivity: "base" });
    });

    return entries;
  }, [profiles, tasksByProfile, now]);

  const selectedProfile = useMemo(
    () => profiles.find((profile) => String(profile.id) === selectedProfileId) ?? null,
    [profiles, selectedProfileId],
  );

  const selectedProfileEntry = useMemo(
    () => profileSummaries.find((entry) => entry.profileId === selectedProfileId) ?? null,
    [profileSummaries, selectedProfileId],
  );

  const selectedTasks = selectedProfileEntry?.sortedTasks ?? [];
  const selectedSummaryMeta = selectedProfileEntry?.summaryMeta ?? null;


  type DetailValue = string | number | boolean | Record<string, unknown> | ProfileConfig[] | null | undefined;

  const details = useMemo(() => {
    if (!selectedProfile) {
      return [] as Array<{ label: string; value: DetailValue }>;
    }

    const baseDetails: Array<{ label: string; value: DetailValue }> = [
      { label: "ID", value: selectedProfile.id },
      { label: "Name", value: selectedProfile.name ?? null },
      { label: "Status", value: selectedProfile.status ?? null },
      { label: "Running", value: selectedProfile.running ?? false },
      { label: "Paused", value: selectedProfile.paused ?? false },
      { label: "Queue Active", value: selectedProfile.queueActive ?? false },
      { label: "Emulator", value: selectedProfile.emulatorNumber ?? null },
      { label: "Server", value: selectedProfile.server ?? null },
      { label: "State", value: selectedProfile.state ?? null },
      { label: "Enabled", value: selectedProfile.enabled ?? false },
      { label: "Priority", value: typeof selectedProfile.priority === "number" ? selectedProfile.priority : null },
      { label: "Reconnection Time (s)", value: selectedProfile.reconnectionTime ?? null },
      { label: "Queue Position", value: selectedProfile.queuePosition ?? null },
    ];

    if (selectedSummaryMeta) {
      baseDetails.push({ label: "Status Detail", value: selectedSummaryMeta.statusDetail });
      const etaCountdown = selectedProfileEntry?.nextExecutionCountdown ?? "";
      const etaLabel = selectedSummaryMeta.nextExecutionLabel;
      const etaValue = etaCountdown
        ? etaLabel && etaLabel !== "None"
          ? `${etaCountdown} (${etaLabel})`
          : etaCountdown
        : etaLabel;
      baseDetails.push({ label: "Next Task ETA", value: etaValue ?? "None" });
    }

    if (selectedProfile.globalsettings && Object.keys(selectedProfile.globalsettings).length > 0) {
      baseDetails.push({ label: "Global Settings", value: selectedProfile.globalsettings });
    }

    return baseDetails;
  }, [selectedProfile, selectedSummaryMeta, selectedProfileEntry]);

  const isLoading = !profilesLoaded || !tasksLoaded;

  const closeModal = () => setSelectedProfileId(null);

  const openTaskQueue = () => {
    if (!selectedProfile) {
      return;
    }
    const profileId = String(selectedProfile.id);
    setSelectedProfileId(null);
    navigate("/tasks", { state: { focusProfileId: profileId } });
  };

  return (
    <div className="view active" id="profilesView">
      <div className="header">
        <h1>
          <FiUsers aria-hidden="true" className="header-icon" size={24} />
          <span>Profiles</span>
        </h1>
      </div>
      <div className="content-container">
        <div className="profiles-grid" id="profilesGrid">
          {isLoading ? (
            <div className="loading">Loading profiles...</div>
          ) : error ? (
            <div className="loading">Error loading profiles. Please try again.</div>
          ) : profileSummaries.length === 0 ? (
            <div className="loading">No profiles found.</div>
          ) : (
            profileSummaries.map(({ profile, profileId, summaryMeta, statusClassName, cardClassName }) => (
              <div
                className={cardClassName}
                key={profileId}
                role="button"
                tabIndex={0}
                onClick={() => setSelectedProfileId(String(profile.id))}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    setSelectedProfileId(String(profile.id));
                  }
                }}
              >
                <div className="profile-card-header">
                  <div className="profile-name">{profile.name ?? "Unnamed"}</div>
                  <div className={statusClassName}>{summaryMeta.statusText}</div>
                </div>
                <div className="profile-info">
                  <div className="profile-info-item">
                    <span className="profile-info-label">ID:</span>
                    <span className="profile-info-value">{profile.id}</span>
                  </div>
                  <div className="profile-info-item">
                    <span className="profile-info-label">Emulator:</span>
                    <span className="profile-info-value">#{profile.emulatorNumber ?? "N/A"}</span>
                  </div>
                  <div className="profile-info-item">
                    <span className="profile-info-label">Server:</span>
                    <span className="profile-info-value">{profile.server ?? "N/A"}</span>
                  </div>
                  <div className="profile-info-item">
                    <span className="profile-info-label">State:</span>
                    <span className="profile-info-value">{profile.state ?? "Unknown"}</span>
                  </div>
                  {profile.queueActive !== undefined ? (
                    <div className="profile-info-item">
                      <span className="profile-info-label">Queue:</span>
                      <span className="profile-info-value">{profile.queueActive ? "Active" : "Idle"}</span>
                    </div>
                  ) : null}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {selectedProfile ? (
        <div
          className="profile-modal-overlay"
          role="presentation"
          onClick={closeModal}
        >
          <div
            className="profile-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="profileModalTitle"
            onClick={(event) => event.stopPropagation()}
          >
            <button className="profile-modal-close" type="button" onClick={closeModal} aria-label="Close profile">
              {"\u00d7"}
            </button>
            <div className="profile-modal-header">
              <div className="profile-modal-image" aria-hidden="true">
                <span>Profile Image</span>
              </div>
              <div className="profile-modal-title">
                <h2 id="profileModalTitle">{selectedProfile.name ?? "Unnamed"}</h2>
                {selectedSummaryMeta ? (
                  <>
                    <div className={`profile-status ${selectedSummaryMeta.statusClass}`}>
                      {selectedSummaryMeta.statusText}
                    </div>
                    <div className="profile-status-detail">{selectedSummaryMeta.statusDetail}</div>
                  </>
                ) : (
                  <div className={`profile-status ${selectedProfile.running ? "running" : "idle"}`}>
                    {selectedProfile.running ? "Running" : "Idle"}
                  </div>
                )}
              </div>
            </div>

            <div className="profile-modal-body">
              <div className="profile-modal-actions">
                <button type="button" className="profile-modal-link" onClick={openTaskQueue}>
                  <FiCheckSquare aria-hidden="true" size={16} />
                  <span>Open Task Queue</span>
                </button>
              </div>
              <section className="profile-modal-section">
                <h3>Profile Details</h3>
                <dl className="profile-details-grid">
                  {details.map(({ label, value }) => {
                    let content: ReactNode;

                    if (value === null || value === undefined || (typeof value === "string" && value.trim() === "")) {
                      content = "N/A";
                    } else if (typeof value === "boolean") {
                      content = value ? "Yes" : "No";
                    } else if (typeof value === "number") {
                      content = String(value);
                    } else if (Array.isArray(value) || (typeof value === "object" && value !== null)) {
                      content = <pre className="profile-detail-pre">{JSON.stringify(value, null, 2)}</pre>;
                    } else {
                      content = value;
                    }

                    return (
                      <div className="profile-detail-row" key={label}>
                        <dt>{label}</dt>
                        <dd>{content}</dd>
                      </div>
                    );
                  })}
                </dl>
              </section>

              <section className="profile-modal-section">
                <h3>Tasks ({selectedTasks.length})</h3>
                {selectedTasks.length === 0 ? (
                  <div className="profile-modal-empty">No tasks associated with this profile.</div>
                ) : (
                  <table className="profile-modal-table">
                    <thead>
                      <tr>
                        <th>Name</th>
                        <th>Status</th>
                        <th>Scheduled</th>
                        <th>Last Run</th>
                        <th>Next Run</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedTasks.map((task, index) => {
                        const nextExecutionMs = parseDateToMs(task.nextExecutionTime);
                        const nextExecutionCountdown =
                          nextExecutionMs !== null ? formatDuration(Math.max(nextExecutionMs - now, 0)) : null;
                        const nextExecutionTitle = task.nextExecutionTime ? formatDateTime(task.nextExecutionTime) : undefined;
                        return (
                          <tr key={`${task.taskId ?? task.taskName ?? "task"}-${index}`}>
                            <td>{task.taskName ?? "Unknown Task"}</td>
                            <td>{task.executing ? "Executing" : task.scheduled ? "Scheduled" : "Disabled"}</td>
                            <td>{task.scheduled ? "Yes" : "No"}</td>
                            <td>{task.lastExecutionTime ? formatDateTime(task.lastExecutionTime) : "N/A"}</td>
                                                        <td title={nextExecutionTitle}>
                              {task.executing ? (
                                <span className="next-execution-executing">Executing</span>
                              ) : nextExecutionCountdown === "0s" ? (
                                <span className="next-execution-ready">Ready</span>
                              ) : (
                                nextExecutionCountdown ?? "N/A"
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                )}
              </section>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
};

export default ProfilesPage;

