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
