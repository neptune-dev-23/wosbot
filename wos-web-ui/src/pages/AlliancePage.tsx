import { useCallback, useEffect, useMemo, useState, type ChangeEvent } from "react";
import { FiRotateCcw, FiSave, FiShield } from "react-icons/fi";

import type { AllianceSettings, Profile } from "../types/api";
import {
  ensureProfilesInitialized,
  getProfilesSnapshot,
  subscribeProfilesStore,
} from "../services/profilesStore";
import { getAllianceSettings, updateAllianceSettings } from "../services/allianceSettings";

type AutojoinMode = AllianceSettings["autojoinMode"];

type AllianceFormState = {
  techContribution: boolean;
  chestClaim: boolean;
  honorChest: boolean;
  helpRequests: boolean;
  triumph: boolean;
  lifeEssence: boolean;
  techOffsetMinutes: string;
  chestOffsetMinutes: string;
  triumphOffsetMinutes: string;
  lifeEssenceOffsetMinutes: string;
  autojoinEnabled: boolean;
  autojoinQueues: string;
  autojoinMode: AutojoinMode;
};

type ToggleKey =
  | "techContribution"
  | "chestClaim"
  | "honorChest"
  | "helpRequests"
  | "triumph"
  | "lifeEssence";

type OffsetKey =
  | "techOffsetMinutes"
  | "chestOffsetMinutes"
  | "triumphOffsetMinutes"
  | "lifeEssenceOffsetMinutes";

type StatusTone = "info" | "success" | "error";

const createDefaultFormState = (): AllianceFormState => ({
  techContribution: false,
  chestClaim: false,
  honorChest: false,
  helpRequests: false,
  triumph: false,
  lifeEssence: false,
  techOffsetMinutes: "0",
  chestOffsetMinutes: "0",
  triumphOffsetMinutes: "0",
  lifeEssenceOffsetMinutes: "0",
  autojoinEnabled: false,
  autojoinQueues: "1",
  autojoinMode: "allTroops",
});

const parseNonNegative = (value: string) => {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed) || parsed < 0) {
    return 0;
  }
  return parsed;
};

const normalizeSettings = (settings: AllianceSettings): AllianceSettings => ({
  techContribution: Boolean(settings.techContribution),
  techOffsetMinutes: Math.max(0, settings.techOffsetMinutes ?? 0),
  chestClaim: Boolean(settings.chestClaim),
  chestOffsetMinutes: Math.max(0, settings.chestOffsetMinutes ?? 0),
  honorChest: Boolean(settings.honorChest),
  helpRequests: Boolean(settings.helpRequests),
  triumph: Boolean(settings.triumph),
  triumphOffsetMinutes: Math.max(0, settings.triumphOffsetMinutes ?? 0),
  lifeEssence: Boolean(settings.lifeEssence),
  lifeEssenceOffsetMinutes: Math.max(0, settings.lifeEssenceOffsetMinutes ?? 0),
  autojoinEnabled: Boolean(settings.autojoinEnabled),
  autojoinQueues: Math.max(1, settings.autojoinQueues ?? 1),
  autojoinMode: settings.autojoinMode === "useFormation" ? "useFormation" : "allTroops",
});

const toFormState = (settings: AllianceSettings): AllianceFormState => ({
  techContribution: settings.techContribution,
  chestClaim: settings.chestClaim,
  honorChest: settings.honorChest,
  helpRequests: settings.helpRequests,
  triumph: settings.triumph,
  lifeEssence: settings.lifeEssence,
  techOffsetMinutes: String(settings.techOffsetMinutes ?? 0),
  chestOffsetMinutes: String(settings.chestOffsetMinutes ?? 0),
  triumphOffsetMinutes: String(settings.triumphOffsetMinutes ?? 0),
  lifeEssenceOffsetMinutes: String(settings.lifeEssenceOffsetMinutes ?? 0),
  autojoinEnabled: settings.autojoinEnabled,
  autojoinQueues: String(settings.autojoinQueues ?? 1),
  autojoinMode: settings.autojoinMode,
});

const toPayload = (state: AllianceFormState): AllianceSettings => ({
  techContribution: state.techContribution,
  techOffsetMinutes: parseNonNegative(state.techOffsetMinutes),
  chestClaim: state.chestClaim,
  chestOffsetMinutes: parseNonNegative(state.chestOffsetMinutes),
  honorChest: state.honorChest,
  helpRequests: state.helpRequests,
  triumph: state.triumph,
  triumphOffsetMinutes: parseNonNegative(state.triumphOffsetMinutes),
  lifeEssence: state.lifeEssence,
  lifeEssenceOffsetMinutes: parseNonNegative(state.lifeEssenceOffsetMinutes),
  autojoinEnabled: state.autojoinEnabled,
  autojoinQueues: Math.max(1, parseNonNegative(state.autojoinQueues)),
  autojoinMode: state.autojoinMode,
});

const settingsEqual = (a: AllianceSettings, b: AllianceSettings) =>
  a.techContribution === b.techContribution &&
  a.techOffsetMinutes === b.techOffsetMinutes &&
  a.chestClaim === b.chestClaim &&
  a.chestOffsetMinutes === b.chestOffsetMinutes &&
  a.honorChest === b.honorChest &&
  a.helpRequests === b.helpRequests &&
  a.triumph === b.triumph &&
  a.triumphOffsetMinutes === b.triumphOffsetMinutes &&
  a.lifeEssence === b.lifeEssence &&
  a.lifeEssenceOffsetMinutes === b.lifeEssenceOffsetMinutes &&
  a.autojoinEnabled === b.autojoinEnabled &&
  a.autojoinQueues === b.autojoinQueues &&
  a.autojoinMode === b.autojoinMode;

const describeError = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  try {
    return String(error);
  } catch {
    return fallback;
  }
};

const AUTOMATION_TASKS: Array<{
  key: ToggleKey;
  label: string;
  helper?: string;
  offset?: { key: OffsetKey; label: string };
}> = [
  {
    key: "techContribution",
    label: "Tech contribution",
    helper: "Donate tech points to alliance research queues.",
    offset: { key: "techOffsetMinutes", label: "Offset time (minutes)" },
  },
  {
    key: "chestClaim",
    label: "Claim chests",
    helper: "Sweep alliance gifts and chests after cooldowns expire.",
    offset: { key: "chestOffsetMinutes", label: "Offset time (minutes)" },
  },
  {
    key: "honorChest",
    label: "Claim honor chest",
    helper: "Pick up the daily honor chest reward for members.",
  },
  {
    key: "triumph",
    label: "Claim Triumph",
    helper: "Redeem Triumph rewards once available.",
    offset: { key: "triumphOffsetMinutes", label: "Offset time (minutes)" },
  },
  {
    key: "lifeEssence",
    label: "Claim allies essence",
    helper: "Collect alliance life essence with a retry buffer if unavailable.",
    offset: { key: "lifeEssenceOffsetMinutes", label: "Fail offset time (minutes)" },
  },
  {
    key: "helpRequests",
    label: "Help request",
    helper: "Send and respond to alliance help requests.",
  },
];

const AlliancePage = () => {
  const [profiles, setProfiles] = useState<Profile[]>(() => getProfilesSnapshot());
  const [selectedProfileId, setSelectedProfileId] = useState<number | null>(() =>
    profiles.length > 0 ? profiles[0].id : null,
  );
  const [initialSettings, setInitialSettings] = useState<AllianceSettings | null>(null);
  const [formState, setFormState] = useState<AllianceFormState>(() => createDefaultFormState());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState<{ message: string; tone: StatusTone } | null>(null);

  useEffect(() => {
    const unsubscribe = subscribeProfilesStore((next) => {
      setProfiles(next);
    });
    ensureProfilesInitialized();
    return unsubscribe;
  }, []);

  useEffect(() => {
    if (profiles.length === 0) {
      setSelectedProfileId(null);
      return;
    }
    if (selectedProfileId == null) {
      setSelectedProfileId(profiles[0].id);
      return;
    }
    if (!profiles.some((profile) => profile.id === selectedProfileId)) {
      setSelectedProfileId(profiles[0].id);
    }
  }, [profiles, selectedProfileId]);

  const loadSettings = useCallback(
    async (profileId: number, forceRefresh = false) => {
      setLoading(true);
      try {
        const settings = await getAllianceSettings(profileId, forceRefresh);
        const normalized = normalizeSettings(settings);
        setInitialSettings(normalized);
        setFormState(toFormState(normalized));
        setStatus(null);
      } catch (error) {
        setInitialSettings(null);
        setFormState(createDefaultFormState());
        setStatus({
          message: describeError(error, "Failed to load alliance settings"),
          tone: "error",
        });
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  useEffect(() => {
    if (selectedProfileId == null) {
      setInitialSettings(null);
      setFormState(createDefaultFormState());
      return;
    }
    setInitialSettings(null);
    setFormState(createDefaultFormState());
    loadSettings(selectedProfileId, true);
  }, [selectedProfileId, loadSettings]);

  useEffect(() => {
    if (status?.tone === "success") {
      const timer = window.setTimeout(() => setStatus(null), 3000);
      return () => window.clearTimeout(timer);
    }
    return undefined;
  }, [status]);

  const toggleTask = (key: ToggleKey) => (event: ChangeEvent<HTMLInputElement>) => {
    const { checked } = event.target;
    setFormState((prev) => ({ ...prev, [key]: checked }));
  };

  const updateOffset = (key: OffsetKey) => (event: ChangeEvent<HTMLInputElement>) => {
    const { value } = event.target;
    setFormState((prev) => ({ ...prev, [key]: value }));
  };

  const updateQueues = (event: ChangeEvent<HTMLSelectElement>) => {
    const { value } = event.target;
    setFormState((prev) => ({ ...prev, autojoinQueues: value }));
  };

  const updateAutojoinMode = (event: ChangeEvent<HTMLInputElement>) => {
    const nextMode = event.target.value as AutojoinMode;
    setFormState((prev) => ({ ...prev, autojoinMode: nextMode }));
  };

  const toggleAutojoin = (event: ChangeEvent<HTMLInputElement>) => {
    const { checked } = event.target;
    setFormState((prev) => ({ ...prev, autojoinEnabled: checked }));
  };

  const derivedSettings = useMemo(() => toPayload(formState), [formState]);
  const isDirty = initialSettings ? !settingsEqual(initialSettings, derivedSettings) : false;
  const hasProfileSelected = selectedProfileId != null;
  const readyForEdits = hasProfileSelected && initialSettings != null && !loading;
  const inputsDisabled = !readyForEdits || saving;
  const canSave = readyForEdits && isDirty && !saving;
  const canReset = hasProfileSelected && !saving && (isDirty || readyForEdits);
  const statusClassName = status ? `settings-message ${status.tone}` : undefined;

  const handleProfileChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const { value } = event.target;
    if (!value) {
      setSelectedProfileId(null);
      return;
    }
    const numeric = Number.parseInt(value, 10);
    setSelectedProfileId(Number.isNaN(numeric) ? null : numeric);
  };

  const handleSave = async () => {
    if (!hasProfileSelected || !initialSettings) {
      return;
    }
    try {
      setSaving(true);
      setStatus({ message: "Saving alliance settings…", tone: "info" });
      const updated = await updateAllianceSettings(selectedProfileId, derivedSettings);
      const normalized = normalizeSettings(updated);
      setInitialSettings(normalized);
      setFormState(toFormState(normalized));
      setStatus({ message: "Alliance settings saved", tone: "success" });
    } catch (error) {
      setStatus({
        message: describeError(error, "Failed to update alliance settings"),
        tone: "error",
      });
    } finally {
      setSaving(false);
    }
  };

  const handleReset = () => {
    if (!hasProfileSelected || saving) {
      return;
    }
    if (initialSettings && isDirty) {
      setFormState(toFormState(initialSettings));
      setStatus(null);
      return;
    }
    loadSettings(selectedProfileId, true);
  };

  return (
    <div className="view active" id="allianceView">
      <div className="header">
        <h1>
          <FiShield aria-hidden="true" className="header-icon" size={24} />
          <span>Alliance Automation</span>
        </h1>
        <p className="header-subtitle">
          Mirror the desktop alliance controls to manage donations, rewards, and rally support from the web dashboard.
        </p>
      </div>
      <div className="content-container page-content">
        <section className="page-section">
          <div className="settings-toolbar">
            <div className="settings-inline">
              <label htmlFor="alliance-profile-select">Profile</label>
              <select
                id="alliance-profile-select"
                onChange={handleProfileChange}
                value={selectedProfileId ?? ""}
              >
                <option value="">Select a profile</option>
                {profiles.map((profile) => (
                  <option key={profile.id} value={profile.id}>
                    {profile.name ?? `Profile ${profile.id}`}
                  </option>
                ))}
              </select>
            </div>
            <div className="settings-actions">
              {status ? <span className={statusClassName}>{status.message}</span> : null}
              <button
                className="btn btn-info"
                disabled={!canReset || loading}
                onClick={handleReset}
                type="button"
              >
                <FiRotateCcw aria-hidden="true" />
                Reset
              </button>
              <button
                className="btn btn-primary"
                disabled={!canSave}
                onClick={handleSave}
                type="button"
              >
                <FiSave aria-hidden="true" />
                Save Changes
              </button>
            </div>
          </div>
          {!hasProfileSelected ? (
            <p className="settings-helper">
              Add a profile to configure alliance automation tasks from the dashboard.
            </p>
          ) : null}
          {loading ? <p className="settings-helper">Loading settings…</p> : null}
        </section>

        <section className="page-section">
          <h2>Alliance Tasks</h2>
          <div className="settings-grid">
            {AUTOMATION_TASKS.map((task) => {
              const toggleId = `alliance-${task.key}`;
              const offsetId = task.offset ? `${toggleId}-offset` : null;
              const isEnabled = formState[task.key];

              return (
                <div className="page-panel settings-card" key={task.key}>
                  <label className="settings-toggle" htmlFor={toggleId}>
                    <input
                      checked={isEnabled}
                      disabled={inputsDisabled}
                      id={toggleId}
                      onChange={toggleTask(task.key)}
                      type="checkbox"
                    />
                    <span>{task.label}</span>
                  </label>
                  {task.helper ? <p className="settings-helper">{task.helper}</p> : null}
                  {task.offset && offsetId ? (
                    <div className="settings-offset">
                      <label htmlFor={offsetId}>{task.offset.label}</label>
                      <input
                        disabled={inputsDisabled || !isEnabled}
                        id={offsetId}
                        min="0"
                        onChange={updateOffset(task.offset.key)}
                        type="number"
                        value={formState[task.offset.key]}
                      />
                    </div>
                  ) : null}
                </div>
              );
            })}
          </div>
        </section>

        <section className="page-section">
          <h2>Autojoin Preferences</h2>
          <div className="page-panel settings-card">
            <label className="settings-toggle" htmlFor="alliance-autojoin-toggle">
              <input
                checked={formState.autojoinEnabled}
                disabled={inputsDisabled}
                id="alliance-autojoin-toggle"
                onChange={toggleAutojoin}
                type="checkbox"
              />
              <span>Enable Autojoin</span>
            </label>
            <p className="settings-helper">
              Queue alliance rallies automatically. Configure the number of concurrent queues and troop selection.
            </p>
            <div className="settings-inline">
              <label htmlFor="alliance-autojoin-queues">Number of queues</label>
              <select
                disabled={inputsDisabled || !formState.autojoinEnabled}
                id="alliance-autojoin-queues"
                onChange={updateQueues}
                value={formState.autojoinQueues}
              >
                {[1, 2, 3, 4, 5, 6].map((count) => (
                  <option key={count} value={count}>
                    {count}
                  </option>
                ))}
              </select>
            </div>

            <div aria-label="Autojoin troop selection" className="settings-radio-group" role="radiogroup">
              <label className="settings-radio-option" htmlFor="autojoin-mode-all-troops">
                <input
                  checked={formState.autojoinMode === "allTroops"}
                  disabled={inputsDisabled || !formState.autojoinEnabled}
                  id="autojoin-mode-all-troops"
                  name="autojoin-mode"
                  onChange={updateAutojoinMode}
                  type="radio"
                  value="allTroops"
                />
                <span>All troops</span>
              </label>
              <label className="settings-radio-option" htmlFor="autojoin-mode-use-formation">
                <input
                  checked={formState.autojoinMode === "useFormation"}
                  disabled={inputsDisabled || !formState.autojoinEnabled}
                  id="autojoin-mode-use-formation"
                  name="autojoin-mode"
                  onChange={updateAutojoinMode}
                  type="radio"
                  value="useFormation"
                />
                <span>Use formation</span>
              </label>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
};

export default AlliancePage;
