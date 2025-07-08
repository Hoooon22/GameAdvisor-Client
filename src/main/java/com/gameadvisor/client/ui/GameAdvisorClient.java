package com.gameadvisor.client.ui;

import com.gameadvisor.client.model.Game;
import com.gameadvisor.client.ui.components.character.CharacterOverlay;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import javafx.scene.paint.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gameadvisor.client.network.ApiClient;
import com.gameadvisor.client.service.ProcessScanService;
import com.gameadvisor.client.model.GameWindowInfo;
import com.sun.jna.platform.win32.WinDef.RECT;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import com.gameadvisor.client.util.WindowUtils;

public class GameAdvisorClient extends Application {

    private List<Game> knownGames = new ArrayList<>();
    private CharacterOverlay characterOverlay;
    private Stage overlayStage;

    @Override
    public void start(Stage primaryStage) {
        // 초기 상태창 (작은 창)
        createInitialStatusWindow(primaryStage);
        
        // 게임 목록 가져오기 및 프로세스 감지 시작
        new Thread(() -> {
            ApiClient apiClient = new ApiClient();
            try {
                knownGames = apiClient.getGames();
                javafx.application.Platform.runLater(() -> {
                    if (knownGames.isEmpty()) {
                        updateStatusWindow(primaryStage, "서버에서 게임 목록을 불러오지 못했습니다.\n서버가 실행 중인지 확인하세요.");
                    } else {
                        updateStatusWindow(primaryStage, "게임 탐지 대기 중...\n게임을 실행해주세요!");
                        startGameDetection(primaryStage);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    updateStatusWindow(primaryStage, "서버 연결 오류\n" + e.getMessage());
                });
            }
        }).start();

        primaryStage.show();
    }
    
    private void createInitialStatusWindow(Stage stage) {
        Label statusLabel = new Label("게임 어드바이저 초기화 중...");
        statusLabel.setFont(new Font("Malgun Gothic", 14));
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 10px; -fx-padding: 15px;");

        StackPane root = new StackPane(statusLabel);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root, 300, 100);
        scene.setFill(Color.TRANSPARENT);

        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        double centerX = (screenBounds.getWidth() - 300) / 2;
        double centerY = (screenBounds.getHeight() - 100) / 2;

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setX(centerX);
        stage.setY(centerY);
        stage.setScene(scene);
        stage.setTitle("GameAdvisor");
        
        // 상태창 정보를 저장
        stage.getProperties().put("statusLabel", statusLabel);
    }
    
    private void updateStatusWindow(Stage stage, String message) {
        Label statusLabel = (Label) stage.getProperties().get("statusLabel");
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    private void createGameOverlay() {
        if (overlayStage != null) {
            return; // 이미 생성되었으면 리턴
        }
        
        // 완전히 투명한 오버레이 창 생성
        overlayStage = new Stage();
        
        // 캐릭터 오버레이를 위한 투명한 Pane
        Pane overlayPane = new Pane();
        overlayPane.setPickOnBounds(false); // 빈 영역의 마우스 이벤트를 게임으로 통과
        overlayPane.setStyle("-fx-background-color: transparent;");
        
        // 캐릭터 오버레이 초기화
        characterOverlay = new CharacterOverlay(overlayPane);

        // 투명한 루트
        StackPane root = new StackPane(overlayPane);
        root.setStyle("-fx-background-color: transparent;");
        root.setPickOnBounds(false); // 빈 영역의 마우스 이벤트 통과

        // 전체 화면 크기로 Scene 생성
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
        scene.setFill(Color.TRANSPARENT); // 완전히 투명한 배경
        
        overlayStage.initStyle(StageStyle.TRANSPARENT);
        overlayStage.setAlwaysOnTop(true); // 항상 게임 위에 표시
        overlayStage.setX(0);
        overlayStage.setY(0);
        overlayStage.setScene(scene);
        overlayStage.setTitle("GameAdvisor Overlay");
        
        // 종료 시 정리
        overlayStage.setOnCloseRequest(event -> {
            if (characterOverlay != null) {
                characterOverlay.cleanup();
            }
        });
    }
    
    private void startGameDetection(Stage statusStage) {
        com.sun.jna.platform.win32.WinDef.HWND statusHwnd = com.gameadvisor.client.util.WindowUtils.getHWNDFromStage(statusStage);
        ProcessScanService service = new ProcessScanService(knownGames, statusHwnd);
        service.setPeriod(Duration.seconds(1));

        service.setOnRunning(e -> {
            updateStatusWindow(statusStage, "게임을 찾고 있습니다...");
        });
        
        service.setOnSucceeded(e -> {
            List<GameWindowInfo> infos = service.getValue();
            if (infos == null || infos.isEmpty() || infos.get(0).getRect() == null) {
                // 게임 탐지 실패 시
                if (overlayStage != null && overlayStage.isShowing()) {
                    overlayStage.hide();
                    statusStage.show(); // 상태창 다시 표시
                }
                updateStatusWindow(statusStage, "게임 윈도우를 찾을 수 없습니다.\n게임을 실행해주세요.");
                return;
            }

            // 게임 탐지 성공 시
            GameWindowInfo info = infos.get(0);
            RECT rect = info.getRect();
            
            // 최소화 상태 확인
            if (rect.left <= -32000 && rect.top <= -32000) {
                if (overlayStage != null && overlayStage.isShowing()) {
                    overlayStage.hide();
                    statusStage.show();
                }
                updateStatusWindow(statusStage, info.getGameName() + " 감지됨 (최소화 상태)");
                return;
            }

            // 게임이 정상적으로 탐지된 경우
            if (overlayStage == null) {
                createGameOverlay(); // 오버레이 창 생성
            }
            
            // 상태창 숨기고 오버레이 표시
            statusStage.hide();
            overlayStage.show();
            
            // 캐릭터 활성화
            characterOverlay.activateCharacter(info);
            
            System.out.println("[DEBUG] 탐지된 게임: " + info);
            
            // 게임별 특별 조언 제공 (첫 감지 시에만)
            if (!characterOverlay.isCharacterActive()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(3000); // 3초 후 게임별 조언
                        characterOverlay.provideGameSpecificAdvice(info.getGameName());
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        });
        
        service.setOnFailed(e -> {
            updateStatusWindow(statusStage, "오류: 프로세스를 스캔할 수 없습니다.");
            if (overlayStage != null && overlayStage.isShowing()) {
                overlayStage.hide();
                statusStage.show();
            }
            if (service.getException() != null) {
                service.getException().printStackTrace();
            }
        });
        
        service.start();
        
        // 종료 시 정리
        statusStage.setOnCloseRequest(event -> {
            service.cancel();
            if (characterOverlay != null) {
                characterOverlay.cleanup();
            }
            if (overlayStage != null) {
                overlayStage.close();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}