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
}

export interface TaskState {
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
