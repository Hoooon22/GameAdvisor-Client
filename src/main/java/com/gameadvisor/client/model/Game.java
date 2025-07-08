package com.gameadvisor.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 서버의 Game 엔티티와 동일한 구조를 가지는 DTO 클래스
@JsonIgnoreProperties(ignoreUnknown = true)
public class Game {

    private Long id;
    private String name;
    private String displayName;
    private String processName;
    private String vectorTableName;
    private Boolean isActive;
    private String description;

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProcessName() {
        return processName;
    }

    public String getVectorTableName() {
        return vectorTableName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public String getDescription() {
        return description;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public void setVectorTableName(String vectorTableName) {
        this.vectorTableName = vectorTableName;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // 편의 메서드들
    public boolean isSupported() {
        return Boolean.TRUE.equals(isActive);
    }

    public String getFullDisplayName() {
        return displayName != null ? displayName : name;
    }

    // 게임별 특화 정보를 위한 메서드들
    public boolean isBloonsTD() {
        return "BloonsTD".equalsIgnoreCase(name);
    }

    public boolean isMasterDuel() {
        return "MasterDuel".equalsIgnoreCase(name);
    }

    @Override
    public String toString() {
        return "Game{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", processName='" + processName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
} 