

export interface SubTaskExecutionStat {
  id: number;
  profileId: number;
  taskId: string;
  subTaskType: string;
  executionCount: number;
  staminaSpent: number;
  createdAt: string;
}

export interface SubTaskStatsResponse {
  meta: {
    profileId?: number;
    taskId?: string;
    subTaskType?: string;
    limit: number;
    count: number;
  };
  data: SubTaskExecutionStat[];
}

export const fetchSubTaskStats = async (params: {
  profileId?: number;
  taskId?: string;
  subTaskType?: string;
  limit?: number;
  signal?: AbortSignal;
}): Promise<SubTaskStatsResponse> => {
  const query = new URLSearchParams();
  if (params.profileId) {
    query.set("profileId", params.profileId.toString());
  }
  if (params.taskId) {
    query.set("taskId", params.taskId);
  }
  if (params.subTaskType) {
    query.set("subTaskType", params.subTaskType);
  }
  if (params.limit) {
    query.set("limit", params.limit.toString());
  }

  const response = await fetch(`/api/sub-task-stats?${query.toString()}`, {
    method: "GET",
    signal: params.signal,
  });

  if (!response.ok) {
    const message = `Failed to fetch sub-task stats: ${response.status}`;
    throw new Error(message);
  }

  return response.json();
};
