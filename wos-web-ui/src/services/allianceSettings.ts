import type { AllianceSettings } from "../types/api";

const settingsCache = new Map<number, AllianceSettings>();
const inflightRequests = new Map<number, Promise<AllianceSettings>>();

const readJson = async (response: Response) => {
  if (!response.ok) {
    const body = await response.text().catch(() => "");
    throw new Error(body || `Alliance settings request failed with status ${response.status}`);
  }
  return (await response.json()) as AllianceSettings;
};

export const getAllianceSettings = async (profileId: number, forceRefresh = false) => {
  if (forceRefresh) {
    settingsCache.delete(profileId);
    inflightRequests.delete(profileId);
  } else if (settingsCache.has(profileId)) {
    return settingsCache.get(profileId)!;
  }

  if (inflightRequests.has(profileId)) {
    return inflightRequests.get(profileId)!;
  }

  const request = (async () => {
    try {
      const response = await fetch(`/api/profiles/${profileId}/alliance`);
      const data = await readJson(response);
      settingsCache.set(profileId, data);
      return data;
    } finally {
      inflightRequests.delete(profileId);
    }
  })();

  inflightRequests.set(profileId, request);
  return request;
};

export const updateAllianceSettings = async (profileId: number, payload: AllianceSettings) => {
  const response = await fetch(`/api/profiles/${profileId}/alliance`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
  const data = await readJson(response);
  settingsCache.set(profileId, data);
  return data;
};

export const clearAllianceSettingsCache = (profileId?: number) => {
  if (profileId == null) {
    settingsCache.clear();
    return;
  }
  settingsCache.delete(profileId);
};
