package cl.camodev.wosbot.almac.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sub_task_execution_stat")
public class SubTaskExecutionStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "sub_task_type")
    private String subTaskType;

    @Column(name = "execution_count")
    private Integer executionCount;

    @Column(name = "stamina_spent")
    private Integer staminaSpent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SubTaskExecutionStat() {
    }

    public SubTaskExecutionStat(Long profileId, String taskId, String subTaskType, Integer executionCount, Integer staminaSpent) {
        this.profileId = profileId;
        this.taskId = taskId;
        this.subTaskType = subTaskType;
        this.executionCount = executionCount;
        this.staminaSpent = staminaSpent;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSubTaskType() {
        return subTaskType;
    }

    public void setSubTaskType(String subTaskType) {
        this.subTaskType = subTaskType;
    }

    public Integer getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Integer executionCount) {
        this.executionCount = executionCount;
    }

    public Integer getStaminaSpent() {
        return staminaSpent;
    }

    public void setStaminaSpent(Integer staminaSpent) {
        this.staminaSpent = staminaSpent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
