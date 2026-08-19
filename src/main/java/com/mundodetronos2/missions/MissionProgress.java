package com.mundodetronos2.missions;

public class MissionProgress {
    private String missionId;
    private int currentCount = 0;
    private String status = "AVAILABLE"; // AVAILABLE, ACTIVE, COMPLETED, EXPIRED

    public MissionProgress() {}

    public MissionProgress(String missionId) {
        this.missionId = missionId;
        this.currentCount = 0;
        this.status = "AVAILABLE";
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(int currentCount) {
        this.currentCount = Math.max(0, currentCount);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
