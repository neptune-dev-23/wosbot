import type { Profile, TaskState } from "../types/api";
import { formatDateTime } from "./format";

export const FIVE_MINUTES_MS = 5 * 60 * 1000;
export const ONE_MINUTE_MS = 60 * 1000;

export const parseDateToMs = (value?: string | null): number | null => {
  if (!value) {
    return null;
  }
  const time = new Date(value).getTime();
  return Number.isFinite(time) ? time : null;
};

export const getTaskCategory = (task: TaskState, now: number): number => {
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

export const getTaskStatusMeta = (task: TaskState): { statusClass: string; statusText: string } => {
  if (task.executing) {
    return { statusClass: "executing", statusText: "Executing" };
  }
  if (task.scheduled) {
    return { statusClass: "scheduled", statusText: "Scheduled" };
  }
  return { statusClass: "disabled", statusText: "Disabled" };
};

export const getTaskTimingMeta = (
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

export const getTaskDisplayMeta = (task: TaskState, now: number) => {
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
  | "paused"
  | "idle-not-queued";

export const PROFILE_STATUS_ORDER: Record<ProfileStatusKey, number> = {
  "running-task": 0,
  "idle-running": 1,
  "waiting-slot": 2,
  "idle-not-queued": 3,
  stopped: 4,
  paused: 5,
  disabled: 6,
};

const PROFILE_STATUS_LABEL: Record<ProfileStatusKey, string> = {
  "idle-running": "Idle (Emulator Ready)",
  "running-task": "Running Task",
  "waiting-slot": "Waiting for Slot",
  "idle-not-queued": "Idle (Not Queued)",
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
    .filter((task) => task.scheduled)
    .map((task) => {
      const next = parseDateToMs(task.nextExecutionTime);
      return next === null ? null : { task, next };
    })
    .filter((entry): entry is { task: TaskState; next: number } => entry !== null)
    .sort((a, b) => a.next - b.next);

  const readyTask = scheduledTasks.find((entry) => entry.next <= now);
  const upcomingTask = scheduledTasks[0];
  const queuePosition = profile?.queuePosition ?? 2147483647;
  const prioritySort = typeof profile?.priority === "number" ? profile.priority : Number.POSITIVE_INFINITY;

  let statusKey: ProfileStatusKey;
  let statusDetail = profile?.state ?? "";
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
  } else if (profile?.enabled === false) {
    statusKey = "disabled";
    statusDetail = profile?.state ?? "Profile is disabled";
    nextExecutionLabel = "None";
    nextExecutionSort = Number.POSITIVE_INFINITY;
  } else if (queuePosition === 0) { // Active
    if (executingTask) {
      statusKey = "running-task";
      statusDetail = executingTask.taskName ? `Running ${executingTask.taskName}` : "Processing queued task";
      const timingMeta = getTaskTimingMeta(executingTask, now);
      nextExecutionClass = timingMeta.nextExecutionClass;
      nextExecutionLabel = executingTask.nextExecutionTime
        ? formatDateTime(executingTask.nextExecutionTime)
        : "In progress";
      nextExecutionSort = parseDateToMs(executingTask.nextExecutionTime) ?? now;
    } else if (readyTask) {
      statusKey = "idle-running";
      statusDetail = "Task ready to execute";
      const timingMeta = getTaskTimingMeta(readyTask.task, now);
      nextExecutionClass = timingMeta.nextExecutionClass;
      nextExecutionLabel = "Ready now";
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
      statusDetail = profile?.state ?? "Emulator ready, awaiting tasks";
      nextExecutionLabel = "None";
      nextExecutionSort = Number.POSITIVE_INFINITY;
    }
  } else if (queuePosition > 0 && queuePosition !== 2147483647) { // Waiting in queue
    statusKey = "waiting-slot";
    statusDetail = `Waiting for slot`;
    if (upcomingTask) {
      const timingMeta = getTaskTimingMeta(upcomingTask.task, now);
      nextExecutionClass = timingMeta.nextExecutionClass;
      nextExecutionLabel = upcomingTask.task.nextExecutionTime ? formatDateTime(upcomingTask.task.nextExecutionTime) : "Queued";
      nextExecutionSort = upcomingTask.next;
    } else {
      nextExecutionLabel = "Queued";
    }
  } else { // queuePosition === MAX_VALUE, i.e., Idle, not in queue
    statusKey = "idle-not-queued";
    statusDetail = "Idle (Not in task queue)";
    if (upcomingTask) {
      const timingMeta = getTaskTimingMeta(upcomingTask.task, now);
      nextExecutionClass = timingMeta.nextExecutionClass;
      nextExecutionLabel =
        upcomingTask.task.nextExecutionTime ? formatDateTime(upcomingTask.task.nextExecutionTime) : "Scheduled";
      nextExecutionSort = upcomingTask.next;
    } else {
      nextExecutionLabel = "No tasks scheduled";
      nextExecutionSort = Number.POSITIVE_INFINITY;
    }
  }

  let statusText = PROFILE_STATUS_LABEL[statusKey];
  if (statusKey === "waiting-slot" && queuePosition !== null && queuePosition > 0 && queuePosition !== 2147483647) {
    statusText = `Waiting for Slot (#${queuePosition})`;
  }

  return {
    statusKey,
    statusClass: statusKey,
    statusText: statusText,
    statusDetail,
    nextExecutionClass,
    nextExecutionLabel,
    orderRank: PROFILE_STATUS_ORDER[statusKey],
    nextExecutionSort,
    prioritySort,
  };
};
