import { useCallback, useEffect, useState } from "react";
import { FiUsers } from "react-icons/fi";

import type { Profile } from "../types/api";

const ProfilesPage = () => {
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [profilesLoading, setProfilesLoading] = useState(false);
  const [profilesError, setProfilesError] = useState<string | null>(null);

  const fetchProfiles = useCallback(async () => {
    try {
      setProfilesLoading(true);
      setProfilesError(null);

      const response = await fetch("/api/profiles");
      if (!response.ok) {
        throw new Error(`Failed to fetch profiles: ${response.status}`);
      }

      const data = (await response.json()) as Profile[];
      setProfiles(data);
    } catch (error) {
      console.error(error);
      setProfilesError(error instanceof Error ? error.message : "Unknown error");
    } finally {
      setProfilesLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProfiles().catch(() => undefined);
  }, [fetchProfiles]);

  return (
    <div className="view active" id="profilesView">
      <div className="header">
        <h1>
          <FiUsers aria-hidden="true" className="header-icon" size={24} />
          <span>Profiles</span>
        </h1>
      </div>
      <div className="content-container">
        <div className="profiles-grid" id="profilesGrid">
          {profilesLoading ? (
            <div className="loading">Loading profiles...</div>
          ) : profilesError ? (
            <div className="loading">Error loading profiles. Please try again.</div>
          ) : profiles.length === 0 ? (
            <div className="loading">No profiles found.</div>
          ) : (
            profiles.map((profile) => (
              <div className="profile-card" key={profile.id}>
                <div className="profile-card-header">
                  <div className="profile-name">{profile.name ?? "Unnamed"}</div>
                  <div className={`profile-status ${profile.running ? "" : "inactive"}`}>
                    {profile.running ? "Running" : profile.paused ? "Paused" : "Inactive"}
                  </div>
                </div>
                <div className="profile-info">
                  <div className="profile-info-item">
                    <span className="profile-info-label">ID:</span>
                    <span className="profile-info-value">{profile.id}</span>
                  </div>
                  <div className="profile-info-item">
                    <span className="profile-info-label">Emulator:</span>
                    <span className="profile-info-value">#{profile.emulatorNumber ?? "N/A"}</span>
                  </div>
                  <div className="profile-info-item">
                    <span className="profile-info-label">Server:</span>
                    <span className="profile-info-value">{profile.server ?? "N/A"}</span>
                  </div>
                  <div className="profile-info-item">
                    <span className="profile-info-label">State:</span>
                    <span className="profile-info-value">{profile.state ?? "Unknown"}</span>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default ProfilesPage;
