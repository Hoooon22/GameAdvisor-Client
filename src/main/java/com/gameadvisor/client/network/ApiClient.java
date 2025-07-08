package com.gameadvisor.client.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.gameadvisor.client.model.Game;
import com.gameadvisor.client.model.ScreenAnalysisRequest;
import com.gameadvisor.client.model.ScreenAnalysisResponse;

public class ApiClient {
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8080/api";

    public ApiClient() {
        // 타임아웃 설정을 늘려서 이미지 분석 요청을 처리할 수 있도록 함
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public List<Game> getGames() throws Exception {
        Request request = new Request.Builder().url(BASE_URL + "/games").build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Failed to fetch game list: " + response);
                return new ArrayList<>();
            }
            return mapper.readValue(response.body().string(), new TypeReference<List<Game>>() {});
        }
    }
    
    /**
     * 화면 분석 요청
     */
    public ScreenAnalysisResponse analyzeScreen(ScreenAnalysisRequest analysisRequest) throws Exception {
        String json = mapper.writeValueAsString(analysisRequest);
        
        RequestBody requestBody = RequestBody.create(
            json, 
            MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(BASE_URL + "/advice/screen")
                .post(requestBody)
                .build();
                
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("화면 분석 요청 실패: " + response);
                throw new Exception("화면 분석 요청 실패: " + response.code());
            }
            
            String responseBody = response.body().string();
            return mapper.readValue(responseBody, ScreenAnalysisResponse.class);
        }
    }
} 