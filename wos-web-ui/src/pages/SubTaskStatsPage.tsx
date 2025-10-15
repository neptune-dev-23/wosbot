import { useCallback, useEffect, useMemo, useState } from "react";
import { FiRefreshCw } from "react-icons/fi";

import type { SubTaskExecutionStat } from "../services/subTaskStatsService";
import { fetchSubTaskStats } from "../services/subTaskStatsService";
import { formatTimestamp } from "../utils/format";
import { getProfilesSnapshot, subscribeProfilesStore } from "../services/profilesStore";
import type { Profile } from "../types/api";



const SubTaskStatsPage = () => {
  const [stats, setStats] = useState<SubTaskExecutionStat[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState({ profileId: "" });
  const [profiles, setProfiles] = useState<Profile[]>(() => getProfilesSnapshot());

  const profileNameMap = useMemo(() => {
    const map = new Map<number, string>();
    profiles.forEach((profile) => {
      if (profile && typeof profile.id === "number") {
        map.set(profile.id, profile.name || `Profile ${profile.id}`);
      }
    });
    return map;
  }, [profiles]);

  const loadStats = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchSubTaskStats({ limit: 1000, signal });
      setStats(response.data ?? []);
    } catch (err) {
      if (err instanceof DOMException && err.name === "AbortError") {
        return;
      }
      const message = err instanceof Error ? err.message : "Failed to fetch stats";
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    loadStats(controller.signal).catch(() => undefined);
    return () => controller.abort();
  }, [loadStats]);

  useEffect(() => {
    const unsubscribe = subscribeProfilesStore((next) => {
      setProfiles(next);
    });
    return () => unsubscribe();
  }, []);

  const filteredStats = useMemo(() => {
    return stats.filter((stat) => {
      if (filters.profileId && stat.profileId !== Number(filters.profileId)) {
        return false;
      }
      return true;
    });
  }, [filters.profileId, stats]);

  return (
    <div className="view active" id="subTaskStatsView">
      <div className="logs-header">
        <div className="logs-controls">
          <select
            className="filter-dropdown"
            value={filters.profileId}
            onChange={(event) => {
              setFilters((previous) => ({ ...previous, profileId: event.target.value }));
            }}
          >
            <option value="">All profiles</option>
            {profiles.map((profile) => (
              <option key={profile.id} value={profile.id}>
                {profile.name}
              </option>
            ))}
          </select>
          <button aria-label="Refresh stats" className="refresh-btn" onClick={() => loadStats()} type="button">
            <FiRefreshCw aria-hidden="true" className="refresh-icon" size={18} />
          </button>
        </div>
      </div>

      <div className="logs-table-container">
        <table className="logs-table">
          <thead>
            <tr>
              <th>PROFILE</th>
              <th>TASK</th>
              <th>SUB-TASK</th>
              <th>EXECUTIONS</th>
              <th>STAMINA SPENT</th>
              <th>TIMESTAMP</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr className="no-logs-row">
                <td colSpan={6}>Loading...</td>
              </tr>
            ) : error ? (
              <tr className="no-logs-row">
                <td colSpan={6}>{error}</td>
              </tr>
            ) : filteredStats.length === 0 ? (
              <tr className="no-logs-row">
                <td colSpan={6}>No stats match the current filters.</td>
              </tr>
            ) : (
              filteredStats.map((stat) => (
                <tr key={stat.id}>
                  <td>{profileNameMap.get(stat.profileId) ?? stat.profileId}</td>
                  <td>{stat.taskId}</td>
                  <td>{stat.subTaskType}</td>
                  <td>{stat.executionCount}</td>
                  <td>{stat.staminaSpent}</td>
                  <td>{formatTimestamp(stat.createdAt)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default SubTaskStatsPage;