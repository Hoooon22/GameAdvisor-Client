package com.gameadvisor.client.model;

/**
 * 화면 분석 응답 모델
 */
public class ScreenAnalysisResponse {
    private String analysis;
    private String advice;
    private String characterName;
    private String gameContext;
    private String timestamp;
    private boolean success;
    private String errorMessage;
    
    public ScreenAnalysisResponse() {}
    
    public String getAnalysis() {
        return analysis;
    }
    
    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }
    
    public String getAdvice() {
        return advice;
    }
    
    public void setAdvice(String advice) {
        this.advice = advice;
    }
    
    public String getCharacterName() {
        return characterName;
    }
    
    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }
    
    public String getGameContext() {
        return gameContext;
    }
    
    public void setGameContext(String gameContext) {
        this.gameContext = gameContext;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
} 