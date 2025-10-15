import type { Profile } from "../types/api";

import { subscribeToMessage } from "./wsClient";

type Listener = (profiles: Profile[]) => void;

interface ProfileStatusUpdatePayload {
  id?: number | string | null;
  status?: string | null;
}

interface ProfileUpdatePayload {
  changed?: Profile[] | null;
  removed?: Array<number | string | null> | null;
  statuses?: ProfileStatusUpdatePayload[] | null;
}

let profiles: Profile[] = [];
let hasSnapshot = false;
let fetchPromise: Promise<void> | null = null;
const listeners = new Set<Listener>();
let websocketInitialized = false;

const notify = () => {
  listeners.forEach((listener) => {
    try {
      listener(profiles);
    } catch (error) {
      console.error("profilesStore listener error", error);
    }
  });
};

const applySnapshot = (payload: Profile[] | null | undefined) => {
  hasSnapshot = true;
  profiles = Array.isArray(payload) ? payload.slice() : [];
  notify();
};

const normalizeId = (value: number | string | null | undefined) => {
  if (value == null) {
    return null;
  }
  const numeric = Number(value);
  if (Number.isNaN(numeric)) {
    return null;
  }
  return numeric;
};

const applyUpdate = (payload: ProfileUpdatePayload | Profile[] | null | undefined) => {
  if (Array.isArray(payload)) {
    applySnapshot(payload as Profile[]);
    return;
  }
  if (!payload || typeof payload !== "object") {
    return;
  }

  const { changed, removed, statuses } = payload as ProfileUpdatePayload;
  let next = profiles;
  let mutated = false;

  const ensureWritable = () => {
    if (!mutated) {
      next = profiles.slice();
      mutated = true;
    }
  };

  const serializeForDiff = (profile: Profile | undefined | null) => {
    if (!profile) {
      return "";
    }
    const clone: Partial<Profile> = { ...profile };
    delete clone.status;
    try {
      return JSON.stringify(clone);
    } catch {
      return "";
    }
  };

  if (Array.isArray(changed)) {
    changed.forEach((profile) => {
      if (!profile || profile.id == null) {
        return;
      }
      const id = Number(profile.id);
      const index = next.findIndex((existing) => existing.id === id);
      if (index >= 0) {
        const existing = next[index];
        const existingSignature = serializeForDiff(existing);
        const incomingSignature = serializeForDiff(profile);
        if (existingSignature === incomingSignature) {
          return;
        }
        ensureWritable();
        next[index] = { ...existing, ...profile };
      } else {
        ensureWritable();
        next.push(profile);
      }
    });
  }

  if (Array.isArray(statuses) && statuses.length > 0) {
    statuses.forEach((entry) => {
      const id = normalizeId(entry?.id);
      if (id == null) {
        return;
      }
      const index = next.findIndex((existing) => existing.id === id);
      if (index >= 0) {
        const statusValue = entry?.status ?? null;
        if (next[index].status !== statusValue) {
          ensureWritable();
          next[index] = {
            ...next[index],
            status: statusValue ?? undefined,
          };
        }
      }
    });
  }

  if (Array.isArray(removed) && removed.length > 0) {
    const idsToRemove = removed
      .map((value) => normalizeId(value))
      .filter((value): value is number => value != null);
    if (idsToRemove.length > 0) {
      const removalSet = new Set(idsToRemove);
      const filtered = next.filter((profile) => !removalSet.has(profile.id));
      if (filtered.length !== next.length) {
        ensureWritable();
        next = filtered;
      }
    }
  }

  if (mutated) {
    hasSnapshot = true;
    profiles = next;
    notify();
  }
};

const ensureWebsocket = () => {
  if (websocketInitialized) {
    return;
  }
  websocketInitialized = true;
  subscribeToMessage<Profile[]>("profiles.snapshot", applySnapshot);
  subscribeToMessage<ProfileUpdatePayload | Profile[]>("profiles.update", applyUpdate);
};

const fetchProfilesOnce = async () => {
  if (hasSnapshot || fetchPromise) {
    return fetchPromise;
  }

  fetchPromise = (async () => {
    try {
      const response = await fetch("/api/profiles");
      if (!response.ok) {
        throw new Error(`Failed to fetch profiles: ${response.status}`);
      }
      const data = (await response.json()) as Profile[];
      applySnapshot(data);
    } catch (error) {
      console.error("Failed to load profiles", error);
      throw error;
    } finally {
      fetchPromise = null;
    }
  })();

  return fetchPromise;
};

export const subscribeProfilesStore = (listener: Listener) => {
  ensureWebsocket();
  listeners.add(listener);

  if (profiles.length > 0) {
    listener(profiles);
  } else if (!hasSnapshot) {
    fetchProfilesOnce().catch(() => undefined);
  }

  return () => {
    listeners.delete(listener);
  };
};

export const getProfilesSnapshot = (): Profile[] => profiles;

export const ensureProfilesInitialized = () => {
  ensureWebsocket();
  return fetchProfilesOnce()?.catch(() => undefined);
};

export const hasProfilesSnapshot = () => hasSnapshot;
