export interface LogMessage {
  timestamp?: string;
  severity: string;
  profile?: string;
  task?: string;
  message: string;
}

export interface Profile {
  id: number;
  name?: string;
  emulatorNumber?: number;
  server?: string;
  state?: string;
  running?: boolean;
  paused?: boolean;
  queueActive?: boolean;
  status?: string;
  enabled?: boolean;
  priority?: number;
  reconnectionTime?: number;
  queuePosition?: number;
  configs?: ProfileConfig[];
  globalsettings?: Record<string, string>;
}

export interface ProfileConfig {
  id?: number;
  configurationName?: string;
  value?: string;
}

export interface AllianceSettings {
  techContribution: boolean;
  techOffsetMinutes: number;
  chestClaim: boolean;
  chestOffsetMinutes: number;
  honorChest: boolean;
  helpRequests: boolean;
  triumph: boolean;
  triumphOffsetMinutes: number;
  lifeEssence: boolean;
  lifeEssenceOffsetMinutes: number;
  autojoinEnabled: boolean;
  autojoinQueues: number;
  autojoinMode: "allTroops" | "useFormation";
}

export interface TaskState {
  taskId?: number;
  profileId?: number;
  taskName?: string;
  executing?: boolean;
  scheduled?: boolean;
  lastExecutionTime?: string;
  nextExecutionTime?: string;
}

export interface BotState {
  running?: boolean;
  paused?: boolean;
}

export interface TaskStatsAggregate {
  taskId?: number | null;
  taskName?: string | null;
  totalRuns: number;
  successCount: number;
  failureCount: number;
  successRate: number;
  minDurationMillis: number;
  maxDurationMillis: number;
  averageDurationMillis: number;
  p95DurationMillis: number;
  lastStartedAt?: string | null;
  lastFinishedAt?: string | null;
  lastErrorMessage?: string | null;
  profileCount: number;
  sampleProfiles: string[];
}

export interface TaskStatsResponse {
  meta: {
    profileId?: number | null;
    taskId?: number | null;
    limit: number;
    groups: number;
  };
  data: TaskStatsAggregate[];
}
