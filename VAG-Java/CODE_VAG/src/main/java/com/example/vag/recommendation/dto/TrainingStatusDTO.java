package com.example.vag.recommendation.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для передачи информации о статусе обучения модели рекомендаций.
 * Используется в REST API для мониторинга переобучения администратором.
 */
public class TrainingStatusDTO {

    // === СТАТУС МОДЕЛИ ===
    private boolean modelReady;
    private String trainingStatus;
    private String trainingStatusDescription;
    private String lastTrainingTime;
    private String lastError;
    private boolean modelExists;
    private String modelFileSizeMb;

    // === ЛОГИ ===
    private List<String> recentLogs;

    // === КОНСТРУКТОР ===
    public TrainingStatusDTO() {
    }

    public TrainingStatusDTO(Map<String, Object> statusMap) {
        this.modelReady = (Boolean) statusMap.getOrDefault("model_ready", false);
        this.trainingStatus = (String) statusMap.getOrDefault("training_status", "UNKNOWN");
        this.trainingStatusDescription = (String) statusMap.getOrDefault("training_status_description", "");
        this.lastTrainingTime = (String) statusMap.getOrDefault("last_training_time", "Никогда");
        this.lastError = (String) statusMap.getOrDefault("last_error", null);
        this.modelExists = (Boolean) statusMap.getOrDefault("model_exists", false);
        this.modelFileSizeMb = (String) statusMap.getOrDefault("model_file_size_mb", "0");
        this.recentLogs = (List<String>) statusMap.getOrDefault("recent_logs", List.of());
    }

    // === GETTERS & SETTERS ===

    public boolean isModelReady() {
        return modelReady;
    }

    public void setModelReady(boolean modelReady) {
        this.modelReady = modelReady;
    }

    public String getTrainingStatus() {
        return trainingStatus;
    }

    public void setTrainingStatus(String trainingStatus) {
        this.trainingStatus = trainingStatus;
    }

    public String getTrainingStatusDescription() {
        return trainingStatusDescription;
    }

    public void setTrainingStatusDescription(String trainingStatusDescription) {
        this.trainingStatusDescription = trainingStatusDescription;
    }

    public String getLastTrainingTime() {
        return lastTrainingTime;
    }

    public void setLastTrainingTime(String lastTrainingTime) {
        this.lastTrainingTime = lastTrainingTime;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public boolean isModelExists() {
        return modelExists;
    }

    public void setModelExists(boolean modelExists) {
        this.modelExists = modelExists;
    }

    public String getModelFileSizeMb() {
        return modelFileSizeMb;
    }

    public void setModelFileSizeMb(String modelFileSizeMb) {
        this.modelFileSizeMb = modelFileSizeMb;
    }

    public List<String> getRecentLogs() {
        return recentLogs;
    }

    public void setRecentLogs(List<String> recentLogs) {
        this.recentLogs = recentLogs;
    }

    @Override
    public String toString() {
        return "TrainingStatusDTO{" +
                "modelReady=" + modelReady +
                ", trainingStatus='" + trainingStatus + '\'' +
                ", lastTrainingTime='" + lastTrainingTime + '\'' +
                ", modelExists=" + modelExists +
                '}';
    }
}
