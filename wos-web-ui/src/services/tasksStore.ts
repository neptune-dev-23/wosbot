import type { TaskState } from "../types/api";

import { subscribeToMessage } from "./wsClient";

type TaskMap = Record<string, TaskState[]>;
type Listener = (tasks: TaskMap) => void;

let tasks: TaskMap = {};
let hasSnapshot = false;
const listeners = new Set<Listener>();
let websocketInitialized = false;

const notify = () => {
  listeners.forEach((listener) => {
    try {
      listener(tasks);
    } catch (error) {
      console.error("tasksStore listener error", error);
    }
  });
};

const cloneTask = (task: TaskState): TaskState => ({ ...task });

const normalizeSnapshot = (snapshot: Record<string, TaskState[]> | null | undefined): TaskMap => {
  if (!snapshot) {
    return {};
  }
  const normalized: TaskMap = {};
  Object.entries(snapshot).forEach(([profileId, list]) => {
    const key = String(profileId);
    normalized[key] = Array.isArray(list) ? list.map(cloneTask) : [];
  });
  return normalized;
};

interface TaskUpdateMessage {
  profileId?: number | string | null;
  task?: TaskState | null;
}

const mergeUpdate = (previous: TaskMap, update: TaskUpdateMessage): TaskMap => {
  if (!update || update.profileId == null || !update.task) {
    return previous;
  }

  const profileKey = String(update.profileId);
  const existingList = previous[profileKey] ?? [];
  const normalizedTask: TaskState = cloneTask(update.task);

  const numericProfileId =
    typeof update.profileId === "string"
      ? Number.parseInt(update.profileId, 10)
      : update.profileId ?? normalizedTask.profileId;
  if (typeof numericProfileId === "number" && Number.isFinite(numericProfileId)) {
    normalizedTask.profileId = numericProfileId;
  }

  const candidateTaskId = (normalizedTask as { taskId?: unknown }).taskId;
  if (typeof candidateTaskId === "string") {
    const parsedTaskId = Number.parseInt(candidateTaskId, 10);
    if (Number.isFinite(parsedTaskId)) {
      normalizedTask.taskId = parsedTaskId;
    } else {
      normalizedTask.taskId = undefined;
    }
  }

  const nextList = [...existingList];
  const matchIndex =
    normalizedTask.taskId != null
      ? nextList.findIndex((item) => item.taskId === normalizedTask.taskId)
      : nextList.findIndex((item) => item.taskName === normalizedTask.taskName);

  if (matchIndex >= 0) {
    nextList[matchIndex] = { ...nextList[matchIndex], ...normalizedTask };
  } else {
    nextList.push(normalizedTask);
  }

  return {
    ...previous,
    [profileKey]: nextList,
  };
};

const handleSnapshot = (snapshot: Record<string, TaskState[]> | null | undefined) => {
  hasSnapshot = true;
  tasks = normalizeSnapshot(snapshot);
  notify();
};

const handleUpdate = (update: TaskUpdateMessage) => {
  tasks = mergeUpdate(tasks, update);
  notify();
};

const ensureWebsocket = () => {
  if (websocketInitialized) {
    return;
  }
  websocketInitialized = true;
  subscribeToMessage<Record<string, TaskState[]>>("tasks.snapshot", handleSnapshot);
  subscribeToMessage<TaskUpdateMessage>("tasks.update", handleUpdate);
};

export const subscribeTasksStore = (listener: Listener) => {
  ensureWebsocket();
  listeners.add(listener);

  if (Object.keys(tasks).length > 0) {
    listener(tasks);
  }

  return () => {
    listeners.delete(listener);
  };
};

export const getTasksSnapshot = (): TaskMap => tasks;

export const hasTasksSnapshot = () => hasSnapshot;
