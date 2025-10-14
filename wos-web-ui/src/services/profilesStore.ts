import type { Profile } from "../types/api";

import { subscribeToMessage } from "./wsClient";

type Listener = (profiles: Profile[]) => void;

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

const ensureWebsocket = () => {
  if (websocketInitialized) {
    return;
  }
  websocketInitialized = true;
  subscribeToMessage<Profile[]>("profiles.snapshot", applySnapshot);
  subscribeToMessage<Profile[]>("profiles.update", applySnapshot);
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
