import type { TaskStatsAggregate, TaskStatsResponse } from "../types/api";

interface FetchTaskStatsOptions {
  profileId?: number;
  taskId?: number;
  limit?: number;
  signal?: AbortSignal;
}

const buildQuery = (options: FetchTaskStatsOptions) => {
  const params = new URLSearchParams();
  if (options.profileId != null) {
    params.set("profileId", String(options.profileId));
  }
  if (options.taskId != null) {
    params.set("taskId", String(options.taskId));
  }
  if (options.limit != null && options.limit > 0) {
    params.set("limit", String(options.limit));
  }
  return params.toString();
};

export const fetchTaskStats = async (options: FetchTaskStatsOptions = {}): Promise<TaskStatsResponse> => {
  const query = buildQuery(options);
  const response = await fetch(`/api/stats/tasks${query ? `?${query}` : ""}`, {
    method: "GET",
    signal: options.signal,
  });

  if (!response.ok) {
    const message = `Failed to fetch task stats: ${response.status}`;
    throw new Error(message);
  }

  const json = (await response.json()) as TaskStatsResponse | { data?: TaskStatsAggregate[] };

  if (!("meta" in json)) {
    return {
      meta: {
        limit: options.limit ?? 1000,
        profileId: options.profileId,
        taskId: options.taskId,
        groups: Array.isArray(json.data) ? json.data.length : 0,
      },
      data: Array.isArray(json.data) ? json.data : [],
    };
  }

  return json;
};
