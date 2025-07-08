package com.gameadvisor.client.model;

/**
 * 화면 분석 요청 모델
 */
public class ScreenAnalysisRequest {
    private String imageBase64;
    private String gameName;
    private String additionalContext;
    
    public ScreenAnalysisRequest() {}
    
    public ScreenAnalysisRequest(String imageBase64, String gameName, String additionalContext) {
        this.imageBase64 = imageBase64;
        this.gameName = gameName;
        this.additionalContext = additionalContext;
    }
    
    public String getImageBase64() {
        return imageBase64;
    }
    
    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
    
    public String getGameName() {
        return gameName;
    }
    
    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
    
    public String getAdditionalContext() {
        return additionalContext;
    }
    
    public void setAdditionalContext(String additionalContext) {
        this.additionalContext = additionalContext;
    }
} 