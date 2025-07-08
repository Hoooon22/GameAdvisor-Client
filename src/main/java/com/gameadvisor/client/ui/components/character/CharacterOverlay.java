package com.gameadvisor.client.ui.components.character;

import com.gameadvisor.client.model.GameWindowInfo;
import com.gameadvisor.client.model.ScreenAnalysisRequest;
import com.gameadvisor.client.model.ScreenAnalysisResponse;
import com.gameadvisor.client.network.ApiClient;
import com.gameadvisor.client.util.ScreenCaptureUtil;
import com.sun.jna.platform.win32.WinDef.RECT;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.awt.Rectangle;
import java.util.Random;
import com.gameadvisor.client.util.WindowUtils;
import com.sun.jna.platform.win32.WinDef.HWND;

/**
 * 캐릭터 오버레이 관리 클래스
 * 게임 창 하단에 캐릭터를 배치하고 관리
 */
public class CharacterOverlay {
    
    private Pane overlayPane;
    private AdvisorCharacter character;
    private SpeechBubble speechBubble;
    private Button screenAnalysisButton;
    private ApiClient apiClient;
    private GameWindowInfo currentGameInfo;
    
    // 캐릭터 위치 및 상태
    private double characterX = 0;
    private double characterY = 0;
    private boolean isCharacterActive = false;
    
    // 캐릭터 던지기 후 착지 위치 기억
    private boolean hasLandedPosition = false;
    private double landedX = 0;
    private double landedY = 0;
    
    // 자동 활동 타이머
    private Timeline idleActivityTimer;
    private Random random = new Random();
    
    // 물리 효과 완료 후 위치 업데이트 방지용 쿨다운
    private long lastPhysicsCompletedTime = 0;
    private static final long POSITION_UPDATE_COOLDOWN = 5000; // 5초로 증가
    
    // 활성 Timeline들 (드래그 시 중단하기 위해)
    private Timeline currentWalkSyncTimer;
    private Timeline currentBubbleUpdateTimer;
    
    // 말풍선 표시 상태 추적
    private boolean isSpeechBubbleActive = false;
    
    // 화면 분석 상태 추적
    private boolean isAnalyzing = false;
    
    // 클릭 회피 관련 변수
    private double lastClickX = -1;
    private double lastClickY = -1;
    private long lastClickTime = 0;
    private static final double AVOIDANCE_DISTANCE = 150; // 클릭으로부터 피하는 거리
    private static final long AVOIDANCE_DURATION = 3000; // 3초간 클릭 위치 기억
    
    public CharacterOverlay(Pane overlayPane) {
        this.overlayPane = overlayPane;
        this.apiClient = new ApiClient();
        initializeComponents();
        setupIdleActivity();
        setupClickDetection();
    }
    
    /**
     * 컴포넌트 초기화
     */
    private void initializeComponents() {
        character = new AdvisorCharacter();
        speechBubble = new SpeechBubble();
        speechBubble.setOnCloseCallback(this::onSpeechBubbleClosed); // 콜백 설정
        
        // 물리 효과 완료 시 착지 위치 기억 콜백 설정
        character.setOnPhysicsCompleted(() -> {
            saveLandingPosition();
            lastPhysicsCompletedTime = System.currentTimeMillis(); // 쿨다운 시작
            
            // 물리 효과 완료 후 말풍선 위치도 업데이트
            Platform.runLater(() -> {
                updateSpeechBubblePosition();
            });
            
            System.out.println("[DEBUG] 물리 효과 완료 - 착지 위치 저장됨, 말풍선 위치 업데이트, 쿨다운 시작");
        });
        
        // 드래그 시작 시 활성 Timeline들 중단하도록 콜백 설정
        character.setOnDragStarted(() -> {
            stopActiveTimelines();
            setupSceneDragHandling(); // Scene 레벨 드래그 핸들링 설정
            System.out.println("[DEBUG] 드래그 시작 - 모든 Timeline 중단, Scene 레벨 드래그 활성화");
        });
        
        // 캐릭터가 마우스 이벤트를 받을 수 있도록 설정
        character.setMouseTransparent(false);
        character.setPickOnBounds(true); // 캐릭터 영역 내 모든 마우스 이벤트 캐치
        speechBubble.setMouseTransparent(false); // 말풍선도 클릭 가능하도록 변경
        
        // 화면 분석 버튼 생성
        createScreenAnalysisButton();
        
        // 오버레이에 추가
        overlayPane.getChildren().addAll(character, speechBubble, screenAnalysisButton);
        
        // 초기에는 숨김
        character.setVisible(false);
        speechBubble.setVisible(false);
        screenAnalysisButton.setVisible(false);
    }
    
    /**
     * 공략 버튼 생성 및 설정
     */
    private void createScreenAnalysisButton() {
        screenAnalysisButton = new Button("📋");
        screenAnalysisButton.setPrefSize(30, 30);
        screenAnalysisButton.setStyle(
            "-fx-background-color: #FF9800; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-background-radius: 15; " +
            "-fx-border-radius: 15; " +
            "-fx-cursor: hand;"
        );
        
        // 버튼 호버 효과 (분석 중이 아닐 때만 적용)
        screenAnalysisButton.setOnMouseEntered(e -> {
            if (!isAnalyzing) {
                if (speechBubble != null && speechBubble.isMinimized()) {
                    // 복원 모드 호버 효과
                    screenAnalysisButton.setStyle(
                        "-fx-background-color: #66BB6A; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-cursor: hand; " +
                        "-fx-opacity: 1.0;"
                    );
                } else {
                    // 일반 분석 모드 호버 효과
                    screenAnalysisButton.setStyle(
                        "-fx-background-color: #e68900; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-cursor: hand; " +
                        "-fx-opacity: 1.0;"
                    );
                }
            }
        });
        
        screenAnalysisButton.setOnMouseExited(e -> {
            if (!isAnalyzing) {
                // 호버 해제 시 원래 상태로 복원
                updateScreenAnalysisButtonState();
            }
        });
        
        // 버튼 클릭 이벤트 - 말풍선 상태에 따라 동적으로 기능 변경
        screenAnalysisButton.setOnAction(e -> {
            if (speechBubble != null && speechBubble.isMinimized()) {
                // 말풍선이 최소화된 상태 - 복원 기능
                System.out.println("[DEBUG] 분석 버튼 클릭 - 말풍선 복원 시작");
                speechBubble.restoreFromMinimized();
                updateScreenAnalysisButtonState(); // 버튼 상태 즉시 업데이트
            } else {
                // 일반 상태 - 화면 분석 기능
                performScreenAnalysis();
            }
        });
        
        // 버튼을 마우스 투명 해제
        screenAnalysisButton.setMouseTransparent(false);
    }
    
    /**
     * 공략 분석 수행
     */
    private void performScreenAnalysis() {
        if (currentGameInfo == null) {
            makeCharacterSpeak("게임이 감지되지 않았습니다.", SpeechBubble.BubbleType.WARNING);
            return;
        }
        
        // 이미 분석 중인 경우 중복 요청 방지
        if (isAnalyzing) {
            makeCharacterSpeak("화면 분석이 진행 중입니다. 잠시만 기다려주세요.", SpeechBubble.BubbleType.WARNING);
            return;
        }
        
        // 분석 시작 - 상태 설정 및 버튼 비활성화
        isAnalyzing = true;
        updateScreenAnalysisButtonState();
        
        // 첫 번째 단계 메시지 표시
        System.out.println("[DEBUG] 화면 분석 시작 - 캡쳐 메시지 표시");
        makeCharacterSpeak("🔍 화면 캡쳐중...", SpeechBubble.BubbleType.THINKING);
        character.setState(AdvisorCharacter.AnimationState.THINKING);
        
        // 단계별 진행을 위한 Timeline 사용
        Timeline analysisProgress = new Timeline();
        
        // 1단계: 화면 캡쳐
        KeyFrame captureStep = new KeyFrame(Duration.millis(500), e -> {
            System.out.println("[DEBUG] 화면 캡쳐 단계 시작");
            performActualCapture();
        });
        
        analysisProgress.getKeyFrames().add(captureStep);
        analysisProgress.play();
    }
    
    private void performActualCapture() {
        // 백그라운드에서 공략 분석 수행
        Task<ScreenAnalysisResponse> strategyTask = new Task<ScreenAnalysisResponse>() {
            @Override
            protected ScreenAnalysisResponse call() throws Exception {
                try {
                    System.out.println("[DEBUG] 실제 캡쳐 작업 시작");
                    
                    // 게임 창의 클라이언트 영역만 캡쳐 (타이틀바, 테두리 제외)
                    HWND gameHwnd = currentGameInfo.getHwnd();
                    if (gameHwnd == null) {
                        throw new Exception("게임 윈도우 핸들을 찾을 수 없습니다.");
                    }
                    
                    // 게임 윈도우를 최상위로 가져오고 클라이언트 영역 준비
                    RECT gameClientRect = WindowUtils.prepareGameWindowForCapture(gameHwnd);
                    if (gameClientRect == null) {
                        throw new Exception("게임 윈도우 클라이언트 영역을 가져올 수 없습니다.");
                    }
                    
                    // 클라이언트 영역이 유효한 크기인지 확인
                    int width = gameClientRect.right - gameClientRect.left;
                    int height = gameClientRect.bottom - gameClientRect.top;
                    if (width <= 0 || height <= 0) {
                        throw new Exception("게임 윈도우 크기가 유효하지 않습니다: " + width + "x" + height);
                    }
                    
                    System.out.println("[DEBUG] 게임 클라이언트 영역 캡쳐: " + 
                        gameClientRect.left + "," + gameClientRect.top + " " + width + "x" + height);
                    
                    Rectangle captureRect = new Rectangle(
                        gameClientRect.left, 
                        gameClientRect.top, 
                        width, 
                        height
                    );
                    
                    // 화면 캡쳐 실행
                    String capturedImage = ScreenCaptureUtil.captureGameWindow(captureRect);
                    System.out.println("[DEBUG] 화면 캡쳐 완료");
                    
                    // 캡쳐 완료 메시지 표시
                    Platform.runLater(() -> {
                        System.out.println("[DEBUG] 캡쳐 완료 메시지 표시");
                        makeCharacterSpeak("✅ 화면 캡쳐 완료!\n🤖 AI 분석중...", SpeechBubble.BubbleType.THINKING);
                        
                        // AI 분석 단계로 진행하기 위한 Timeline
                        Timeline aiAnalysisStep = new Timeline(
                            new KeyFrame(Duration.millis(800), event -> {
                                System.out.println("[DEBUG] AI 분석 단계 메시지 표시");
                                makeCharacterSpeak("⚡ 서버와 통신중...\n잠시만 기다려주세요!", SpeechBubble.BubbleType.THINKING);
                            })
                        );
                        aiAnalysisStep.play();
                    });
                    
                    // 공략 중심 분석 요청 생성
                    String strategyPrompt = String.format(
                        "%s 게임의 현재 화면을 보고 다음 내용으로 상세한 공략 가이드를 제공해줘:\n\n" +
                        "1. 현재 상황 분석 (우선순위, 위험요소, 기회)\n" +
                        "2. 다음에 해야 할 구체적인 행동 (단계별 가이드)\n" +
                        "3. 전략적 팁과 주의사항\n" +
                        "4. 효율적인 리소스 관리 방법\n\n" +
                        "친근하고 이해하기 쉬운 한국어로 답변해주고, 이모지를 사용해서 재미있게 설명해줘!",
                        currentGameInfo.getGameName()
                    );
                    
                    ScreenAnalysisRequest request = new ScreenAnalysisRequest(
                        capturedImage,
                        currentGameInfo.getGameName(),
                        strategyPrompt
                    );
                    
                    System.out.println("[DEBUG] API 호출 시작");
                    
                    // API 호출
                    ScreenAnalysisResponse response = apiClient.analyzeScreen(request);
                    System.out.println("[DEBUG] API 호출 완료: " + (response != null ? "성공" : "실패"));
                    
                    return response;
                    
                } catch (Exception e) {
                    System.err.println("[ERROR] 화면 분석 중 오류 발생: " + e.getMessage());
                    e.printStackTrace();
                    // 에러 발생 시 즉시 UI 업데이트
                    Platform.runLater(() -> {
                        makeCharacterSpeak("❌ 캡쳐 중 오류가 발생했습니다:\n" + e.getMessage(), SpeechBubble.BubbleType.WARNING);
                    });
                    throw e;
                }
            }
        };
        
        strategyTask.setOnSucceeded(e -> {
            System.out.println("[DEBUG] 분석 작업 성공");
            ScreenAnalysisResponse response = strategyTask.getValue();
            Platform.runLater(() -> {
                // 분석 완료 - 상태 초기화 및 버튼 활성화
                isAnalyzing = false;
                updateScreenAnalysisButtonState();
                
                if (response != null && response.isSuccess()) {
                    System.out.println("[DEBUG] 분석 결과 표시");
                    character.setState(AdvisorCharacter.AnimationState.TALKING);
                    makeCharacterSpeak("🎉 분석 완료!\n\n" + response.getAnalysis(), SpeechBubble.BubbleType.STRATEGY);
                } else {
                    System.out.println("[DEBUG] 분석 실패 결과 표시");
                    character.setState(AdvisorCharacter.AnimationState.IDLE);
                    String errorMsg = response != null ? response.getErrorMessage() : "응답을 받지 못했습니다";
                    makeCharacterSpeak("❌ 공략 분석에 실패했습니다:\n" + errorMsg, SpeechBubble.BubbleType.WARNING);
                }
                
                // 분석 완료 후 일반적인 메시지 예약
                scheduleWelcomeBackMessage();
            });
        });
        
        strategyTask.setOnFailed(e -> {
            System.err.println("[ERROR] 분석 작업 실패");
            Platform.runLater(() -> {
                // 분석 실패 - 상태 초기화 및 버튼 활성화
                isAnalyzing = false;
                updateScreenAnalysisButtonState();
                
                character.setState(AdvisorCharacter.AnimationState.IDLE);
                Throwable exception = strategyTask.getException();
                String errorMessage = exception != null ? exception.getMessage() : "알 수 없는 오류";
                makeCharacterSpeak("❌ 공략 분석 중 오류가 발생했습니다:\n" + errorMessage, SpeechBubble.BubbleType.WARNING);
                System.err.println("공략 분석 실패: " + errorMessage);
                if (exception != null) {
                    exception.printStackTrace();
                }
                
                // 분석 실패 후에도 일반적인 메시지 예약
                scheduleWelcomeBackMessage();
            });
        });
        
        // 백그라운드 스레드에서 실행
        Thread strategyThread = new Thread(strategyTask);
        strategyThread.setDaemon(true);
        strategyThread.start();
    }
    
    /**
     * 화면 분석 버튼 위치 업데이트 (캐릭터 오른쪽 위)
     */
    private void updateScreenAnalysisButtonPosition() {
        if (character != null && screenAnalysisButton != null) {
            double characterX = character.getLayoutX();
            double characterY = character.getLayoutY();
            double characterWidth = character.getCharacterWidth();
            
            // 캐릭터 오른쪽 위에 버튼 배치 (약간의 간격 두기)
            double buttonX = characterX + characterWidth + 5;
            double buttonY = characterY - 5;
            
            screenAnalysisButton.setLayoutX(buttonX);
            screenAnalysisButton.setLayoutY(buttonY);
        }
    }
    
    /**
     * 화면 분석 버튼 상태 업데이트 (활성화/비활성화/복원 모드)
     */
    private void updateScreenAnalysisButtonState() {
        if (screenAnalysisButton != null) {
            if (isAnalyzing) {
                // 분석 중일 때 - 버튼 비활성화 및 스타일 변경
                screenAnalysisButton.setDisable(true);
                screenAnalysisButton.setText("⏳");
                screenAnalysisButton.setStyle(
                    "-fx-background-color: #9E9E9E; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 14px; " +
                    "-fx-background-radius: 15; " +
                    "-fx-border-radius: 15; " +
                    "-fx-cursor: default; " +
                    "-fx-opacity: 0.6;"
                );
            } else if (speechBubble != null && speechBubble.isMinimized()) {
                // 말풍선이 최소화된 상태 - 복원 기능으로 변경
                screenAnalysisButton.setDisable(false);
                screenAnalysisButton.setText("📄");
                screenAnalysisButton.setStyle(
                    "-fx-background-color: #4CAF50; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 14px; " +
                    "-fx-background-radius: 15; " +
                    "-fx-border-radius: 15; " +
                    "-fx-cursor: hand; " +
                    "-fx-opacity: 1.0;"
                );
            } else {
                // 일반 상태 - 원래 분석 기능
                screenAnalysisButton.setDisable(false);
                screenAnalysisButton.setText("📋");
                screenAnalysisButton.setStyle(
                    "-fx-background-color: #FF9800; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 14px; " +
                    "-fx-background-radius: 15; " +
                    "-fx-border-radius: 15; " +
                    "-fx-cursor: hand; " +
                    "-fx-opacity: 1.0;"
                );
            }
        }
    }
    
    /**
     * 게임 감지 시 캐릭터 활성화
     */
    public void activateCharacter(GameWindowInfo gameInfo) {
        GameWindowInfo previousGameInfo = this.currentGameInfo;
        this.currentGameInfo = gameInfo;
        
        if (!isCharacterActive) {
            isCharacterActive = true;
            isSpeechBubbleActive = false; // 말풍선 상태 초기화
            hasLandedPosition = false; // 처음 활성화 시에는 착지 위치 없음
            positionCharacterAtGameBottom(gameInfo);
            character.setVisible(true);
            screenAnalysisButton.setVisible(true);
            
            // 환영 메시지 표시
            Platform.runLater(() -> {
                character.setState(AdvisorCharacter.AnimationState.TALKING);
                speechBubble.showMessage(
                    gameInfo.getGameName() + " 플레이를 시작하셨네요!\n도움이 필요하면 언제든 말씀하세요!",
                    SpeechBubble.BubbleType.NORMAL
                );
                updateScreenAnalysisButtonPosition();
            });
            
            // 자동 활동 시작
            startIdleActivity();
        } else {
            // 게임 창 정보가 실제로 변경되었는지 확인
            boolean gameWindowChanged = false;
            if (previousGameInfo != null) {
                gameWindowChanged = !gameInfo.getRect().equals(previousGameInfo.getRect()) ||
                                  !gameInfo.getGameName().equals(previousGameInfo.getGameName());
            }
            
            // 게임 창이 실제로 변경되었을 때만 위치 업데이트
            if (gameWindowChanged) {
                System.out.println("[DEBUG] 게임 창 변경 감지 - 위치 업데이트 실행");
                updateCharacterPosition(gameInfo);
            } else {
                // 변경되지 않았다면 경계만 업데이트
                character.setBounds(gameInfo.getRect().left, gameInfo.getRect().top, 
                                  gameInfo.getRect().right, gameInfo.getRect().bottom);
            }
        }
    }
    
    /**
     * 캐릭터 비활성화
     */
    public void deactivateCharacter() {
        isCharacterActive = false;
        isSpeechBubbleActive = false; // 말풍선 상태 초기화
        hasLandedPosition = false; // 비활성화 시 착지 위치 초기화
        character.setVisible(false);
        screenAnalysisButton.setVisible(false);
        speechBubble.hideImmediately();
        stopIdleActivity();
        stopActiveTimelines(); // 활성 Timeline들도 모두 중단
    }
    
    /**
     * 착지 위치 저장 (드래그 중에는 저장하지 않음)
     */
    private void saveLandingPosition() {
        // 드래그 중이면 위치 저장 하지 않음
        if (character.isBeingDragged()) {
            System.out.println("[DEBUG] 드래그 중 - 착지 위치 저장 건너뜀");
            return;
        }
        
        landedX = character.getLayoutX();
        landedY = character.getLayoutY();
        hasLandedPosition = true;
        characterX = landedX; // 추적 변수도 업데이트
        characterY = landedY;
        System.out.println("[DEBUG] 착지 위치 저장: (" + (int)landedX + ", " + (int)landedY + ")");
    }
    
    /**
     * 게임 창 하단에 캐릭터 배치
     */
    private void positionCharacterAtGameBottom(GameWindowInfo gameInfo) {
        // 캐릭터가 물리 효과 중이거나 드래그 중일 때는 위치를 강제로 변경하지 않음
        if (character.isInPhysicsMode() || character.isBeingDragged()) {
            // 경계만 업데이트
            RECT rect = gameInfo.getRect();
            character.setBounds(rect.left, rect.top, rect.right, rect.bottom);
            System.out.println("[DEBUG] 물리 모드 중 - 경계만 업데이트: minX=" + rect.left + ", minY=" + rect.top + 
                             ", maxX=" + rect.right + ", maxY=" + rect.bottom);
            return;
        }
        
        // 착지 위치가 있고 물리 효과 완료 후 쿨다운 중인지 확인
        long currentTime = System.currentTimeMillis();
        boolean inCooldown = (currentTime - lastPhysicsCompletedTime) < POSITION_UPDATE_COOLDOWN;
        
        if (hasLandedPosition && inCooldown) {
            System.out.println("[DEBUG] 착지 위치가 있고 쿨다운 중 - 착지 위치 유지");
            // 경계만 업데이트
            RECT rect = gameInfo.getRect();
            character.setBounds(rect.left, rect.top, rect.right, rect.bottom);
            
            // 착지 위치가 새로운 게임 창 경계 내에 있는지 확인하고 조정
            if (landedX < rect.left) {
                landedX = rect.left + 10;
            } else if (landedX + character.getCharacterWidth() > rect.right) {
                landedX = rect.right - character.getCharacterWidth() - 10;
            }
            
            if (landedY < rect.top) {
                landedY = rect.top + 10;
            } else if (landedY + character.getCharacterHeight() > rect.bottom) {
                landedY = rect.bottom - character.getCharacterHeight() - 5;
            }
            
            // 조정된 착지 위치로 캐릭터 이동
            Platform.runLater(() -> {
                character.setLayoutX(landedX);
                character.setLayoutY(landedY);
                updateSpeechBubblePosition();
            });
            
            return;
        }
        
        RECT rect = gameInfo.getRect();
        
        // 게임 창의 실제 크기와 위치 계산
        double gameLeft = rect.left;
        double gameTop = rect.top;
        double gameWidth = rect.right - rect.left;
        double gameHeight = rect.bottom - rect.top;
        
        // 착지 위치가 있으면 그 위치를 우선 사용 (게임 창 변경 시)
        if (hasLandedPosition && !inCooldown) {
            characterX = landedX;
            characterY = landedY;
            
            // 게임 창 경계 내에 있는지 확인하고 조정
            if (characterX < gameLeft) {
                characterX = gameLeft + 10;
            } else if (characterX + character.getCharacterWidth() > rect.right) {
                characterX = rect.right - character.getCharacterWidth() - 10;
            }
            
            if (characterY < gameTop) {
                characterY = gameTop + 10;
            } else if (characterY + character.getCharacterHeight() > rect.bottom) {
                characterY = rect.bottom - character.getCharacterHeight() - 5;
            }
            
            System.out.println("[DEBUG] 저장된 착지 위치 사용: (" + (int)characterX + ", " + (int)characterY + ")");
        } else {
            // 착지 위치가 없으면 게임 창 하단 중앙에 배치
            characterX = gameLeft + (gameWidth / 2) - (character.getCharacterWidth() / 2);
            characterY = rect.bottom - character.getCharacterHeight() - 5; // 게임 창 바닥에서 5px 위
            
            // 캐릭터가 게임 창 범위 내에 있는지 확인
            if (characterX < gameLeft) {
                characterX = gameLeft + 10; // 왼쪽 여백
            } else if (characterX + character.getCharacterWidth() > rect.right) {
                characterX = rect.right - character.getCharacterWidth() - 10; // 오른쪽 여백
            }
            
            System.out.println("[DEBUG] 기본 위치 사용 (하단 중앙): (" + (int)characterX + ", " + (int)characterY + ")");
        }
        
        Platform.runLater(() -> {
            character.setLayoutX(characterX);
            character.setLayoutY(characterY);
            
            // 캐릭터 물리 효과 경계 설정 (게임 창에 맞춤)
            character.setBounds(gameLeft, gameTop, rect.right, rect.bottom);
            
            System.out.println("[DEBUG] 캐릭터 경계값 설정: minX=" + gameLeft + ", minY=" + gameTop + 
                             ", maxX=" + rect.right + ", maxY=" + rect.bottom);
            
            // 말풍선 위치도 업데이트
            updateSpeechBubblePosition();
        });
        
        System.out.println("[DEBUG] 캐릭터 위치 설정: (" + characterX + ", " + characterY + ")");
        System.out.println("[DEBUG] 게임 창 정보: " + gameLeft + ", " + gameTop + ", " + gameWidth + "x" + gameHeight);
    }
    
    /**
     * 캐릭터 위치 업데이트 (게임 창 크기 변경 시)
     */
    private void updateCharacterPosition(GameWindowInfo gameInfo) {
        // 드래그 중이면 아무 것도 하지 않음 (경계 업데이트도 하지 않음)
        if (character.isBeingDragged()) {
            System.out.println("[DEBUG] 드래그 중 - 모든 위치 업데이트 건너뜀");
            return;
        }
        
        // 착지 위치가 있고 물리 효과 완료 후 쿨다운 중인지 확인
        long currentTime = System.currentTimeMillis();
        boolean inCooldown = (currentTime - lastPhysicsCompletedTime) < POSITION_UPDATE_COOLDOWN;
        
        if (hasLandedPosition && inCooldown) {
            System.out.println("[DEBUG] 착지 위치가 있고 쿨다운 중 - 위치 업데이트 건너뜀 (남은 시간: " + 
                             (POSITION_UPDATE_COOLDOWN - (currentTime - lastPhysicsCompletedTime)) + "ms)");
            // 경계만 업데이트하고 착지 위치 유지
            RECT rect = gameInfo.getRect();
            character.setBounds(rect.left, rect.top, rect.right, rect.bottom);
            
            // 착지 위치가 새로운 게임 창 경계 내에 맞도록 조정 (드래그 중이 아닐 때만)
            boolean needsAdjustment = false;
            if (landedX < rect.left) {
                landedX = rect.left + 10;
                needsAdjustment = true;
            } else if (landedX + character.getCharacterWidth() > rect.right) {
                landedX = rect.right - character.getCharacterWidth() - 10;
                needsAdjustment = true;
            }
            
            if (landedY < rect.top) {
                landedY = rect.top + 10;
                needsAdjustment = true;
            } else if (landedY + character.getCharacterHeight() > rect.bottom) {
                landedY = rect.bottom - character.getCharacterHeight() - 5;
                needsAdjustment = true;
            }
            
            if (needsAdjustment && !character.isBeingDragged()) {
                Platform.runLater(() -> {
                    character.setLayoutX(landedX);
                    character.setLayoutY(landedY);
                    updateSpeechBubblePosition();
                });
                System.out.println("[DEBUG] 착지 위치 경계 조정: (" + (int)landedX + ", " + (int)landedY + ")");
            }
            
            return;
        }
        
        if (isCharacterActive && !character.isInPhysicsMode() && !character.isBeingDragged()) {
            positionCharacterAtGameBottom(gameInfo);
        } else if (isCharacterActive) {
            // 물리 모드 중일 때는 경계만 업데이트 (드래그 중이 아닐 때만)
            if (!character.isBeingDragged()) {
                RECT rect = gameInfo.getRect();
                character.setBounds(rect.left, rect.top, rect.right, rect.bottom);
            }
        }
    }
    
    /**
     * 말풍선 위치 업데이트 (캐릭터의 실제 위치 기반)
     */
    private void updateSpeechBubblePosition() {
        Platform.runLater(() -> {
            if (currentGameInfo != null) {
                RECT rect = currentGameInfo.getRect();
                speechBubble.positionAboveCharacter(
                    character.getLayoutX(), 
                    character.getLayoutY(), 
                    character.getCharacterWidth(),
                    rect.left,                    // 게임 창 왼쪽 경계
                    rect.top,                     // 게임 창 위쪽 경계  
                    rect.right,                   // 게임 창 오른쪽 경계
                    rect.bottom                   // 게임 창 아래쪽 경계
                );
            } else {
                // 게임 정보가 없으면 기본 메서드 사용
                speechBubble.positionAboveCharacter(
                    character.getLayoutX(), 
                    character.getLayoutY(), 
                    character.getCharacterWidth()
                );
            }
            updateScreenAnalysisButtonPosition();
        });
    }
    
    /**
     * 말풍선이 닫힐 때 호출되는 공통 처리 메서드
     */
    private void onSpeechBubbleClosed() {
        isSpeechBubbleActive = false;
        
        // 자동 활동 재시작
        if (isCharacterActive) {
            startIdleActivity();
        }
        
        // 캐릭터 상태를 IDLE로 복원
        if (!character.isInPhysicsMode()) {
            character.setState(AdvisorCharacter.AnimationState.IDLE);
        }
        
        // 분석 버튼 상태 업데이트 (말풍선 상태 변경 반영)
        updateScreenAnalysisButtonState();
        
        System.out.println("[DEBUG] 말풍선 종료 - 다른 동작들 재개, 분석 버튼 상태 업데이트");
    }

    /**
     * 캐릭터의 실제 위치로 추적 변수 동기화 (드래그 중이거나 말풍선 표시 중에는 동기화하지 않음)
     */
    public void syncCharacterPosition() {
        // 드래그 중이거나 말풍선이 활성화되어 있으면 위치 동기화 하지 않음
        if (character.isBeingDragged()) {
            System.out.println("[DEBUG] 드래그 중 - 위치 동기화 건너뜀");
            return;
        }
        
        if (isSpeechBubbleActive) {
            System.out.println("[DEBUG] 말풍선 표시 중 - 위치 동기화 건너뜀");
            return;
        }
        
        characterX = character.getLayoutX();
        characterY = character.getLayoutY();
        System.out.println("[DEBUG] 캐릭터 위치 동기화: (" + characterX + ", " + characterY + ")");
    }
    
    /**
     * 캐릭터에게 메시지 말하게 하기
     */
    public void makeCharacterSpeak(String message, SpeechBubble.BubbleType bubbleType) {
        if (!isCharacterActive) return;
        
        Platform.runLater(() -> {
            // 말풍선 활성화 시작
            isSpeechBubbleActive = true;
            
            // 자동 활동 및 기존 Timeline들 일시 중단
            stopIdleActivity();
            stopActiveTimelines();
            
            // 물리 모드가 아닐 때만 상태 변경
            if (!character.isInPhysicsMode()) {
                character.setState(AdvisorCharacter.AnimationState.TALKING);
            }
            updateSpeechBubblePosition();
            speechBubble.showMessage(message, bubbleType);
            
            // 말풍선을 최상위로 가져오기
            speechBubble.toFront();
            
            // 분석 버튼 상태 업데이트 (말풍선 표시 반영)
            updateScreenAnalysisButtonState();
            
            // STRATEGY 타입이 아닌 경우만 자동 종료 타이머 설정
            if (bubbleType != SpeechBubble.BubbleType.STRATEGY) {
                // 텍스트 길이에 따른 표시 시간 계산
                double displayDuration = speechBubble.calculateDisplayDuration(message);
                
                // 말풍선이 표시되는 동안 지속적으로 위치 업데이트 (물리 모드든 아니든)
                Timeline bubbleUpdateTimer = new Timeline(
                    new KeyFrame(Duration.millis(50), e -> {
                        if (!character.isBeingDragged()) { // 드래그 중이 아닐 때만 업데이트
                            updateSpeechBubblePosition();
                        }
                    })
                );
                bubbleUpdateTimer.setCycleCount((int)(displayDuration * 20)); // 표시 시간에 맞춰 업데이트 횟수 계산
                bubbleUpdateTimer.setOnFinished(e -> {
                    // 말풍선이 완전히 사라진 후 다른 동작들 재개
                    Platform.runLater(() -> {
                        onSpeechBubbleClosed();
                    });
                });
                bubbleUpdateTimer.play();
                System.out.println("[DEBUG] 말풍선 표시 시작 - 다른 동작들 일시 중단 (" + displayDuration + "초간)");
            } else {
                // STRATEGY 타입은 지속 표시되므로 위치 업데이트만 무한히 수행
                Timeline strategyUpdateTimer = new Timeline(
                    new KeyFrame(Duration.millis(100), e -> {
                        if (!character.isBeingDragged() && speechBubble.isShowing()) {
                            updateSpeechBubblePosition();
                        }
                    })
                );
                strategyUpdateTimer.setCycleCount(Timeline.INDEFINITE);
                strategyUpdateTimer.play();
                currentBubbleUpdateTimer = strategyUpdateTimer; // 참조 저장하여 나중에 정리 가능
                System.out.println("[DEBUG] 공략 말풍선 표시 시작 - 지속 표시 모드");
            }
        });
    }
    
    /**
     * 캐릭터 걷기 (게임 창 내에서)
     */
    public void makeCharacterWalk() {
        if (!isCharacterActive || currentGameInfo == null || character.isInPhysicsMode() || character.isBeingDragged() || isSpeechBubbleActive) return;
        
        RECT rect = currentGameInfo.getRect();
        double gameWidth = rect.right - rect.left;
        
        // 게임 창 내에서 랜덤한 위치로 이동 (하단 고정)
        double minX = rect.left + 20;
        double maxX = rect.right - character.getCharacterWidth() - 20;
        
        if (maxX <= minX) return; // 게임 창이 너무 작으면 이동하지 않음
        
        double targetX = minX + random.nextDouble() * (maxX - minX);
        double targetY = rect.bottom - character.getCharacterHeight() - 5;
        
        Platform.runLater(() -> {
            // 캐릭터의 현재 실제 위치 기반으로 상대적 이동 계산
            double currentX = character.getLayoutX();
            double deltaX = targetX - currentX;
            
            character.walkTo(deltaX, 0);
            
            // 이전 Timeline들 중단
            stopActiveTimelines();
            
            // 걷기가 완료된 후 위치 동기화 및 착지 위치 업데이트
            currentWalkSyncTimer = new Timeline(
                new KeyFrame(Duration.seconds(2.1), e -> {
                    if (!character.isBeingDragged()) { // 드래그 중이 아닐 때만 동기화
                        syncCharacterPosition(); // 걷기 완료 후 동기화
                        saveLandingPosition(); // 새로운 위치를 착지 위치로 저장
                    }
                    currentWalkSyncTimer = null; // Timer 참조 해제
                })
            );
            currentWalkSyncTimer.play();
            
            // 걷는 동안 말풍선 위치 지속 업데이트
            currentBubbleUpdateTimer = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    if (!character.isBeingDragged()) { // 드래그 중이 아닐 때만 업데이트
                        updateSpeechBubblePosition();
                    }
                })
            );
            currentBubbleUpdateTimer.setCycleCount(20); // 2초간 업데이트
            currentBubbleUpdateTimer.setOnFinished(e -> currentBubbleUpdateTimer = null); // Timer 참조 해제
            currentBubbleUpdateTimer.play();
        });
    }
    
    /**
     * 클릭 감지 설정 - 게임 화면 클릭을 감지하여 캐릭터가 피하도록 함
     */
    private void setupClickDetection() {
        overlayPane.setOnMouseClicked(e -> {
            // 클릭 위치가 캐릭터나 버튼이 아닌 경우에만 회피 동작
            if (!isCharacterActive) return;
            
            // 캐릭터나 버튼 영역이 아닌 경우에만 처리 (게임 화면 클릭)
            if (e.getTarget() == overlayPane) {
                lastClickX = e.getSceneX();
                lastClickY = e.getSceneY();
                lastClickTime = System.currentTimeMillis();
                
                System.out.println("[DEBUG] 게임 화면 클릭 감지: (" + (int)lastClickX + ", " + (int)lastClickY + ")");
                
                // 즉시 회피 동작 시작
                startAvoidanceMovement();
                
                e.consume(); // 이벤트 소비하여 게임에 영향 주지 않음
            }
        });
    }

    /**
     * 자동 활동 설정
     */
    private void setupIdleActivity() {
        idleActivityTimer = new Timeline(
            new KeyFrame(Duration.seconds(5), e -> checkForAvoidanceMovement()) // 5초마다 회피 체크
        );
        idleActivityTimer.setCycleCount(Timeline.INDEFINITE);
    }
    
    /**
     * 자동 활동 시작
     */
    private void startIdleActivity() {
        if (idleActivityTimer != null) {
            idleActivityTimer.play();
        }
    }
    
    /**
     * 자동 활동 중지
     */
    private void stopIdleActivity() {
        if (idleActivityTimer != null) {
            idleActivityTimer.stop();
        }
    }
    
    /**
     * 활성 Timeline들 중단 (드래그 시작 시 호출)
     */
    private void stopActiveTimelines() {
        if (currentWalkSyncTimer != null) {
            currentWalkSyncTimer.stop();
            currentWalkSyncTimer = null;
            System.out.println("[DEBUG] 걷기 동기화 Timer 중단");
        }
        
        if (currentBubbleUpdateTimer != null) {
            currentBubbleUpdateTimer.stop();
            currentBubbleUpdateTimer = null;
            System.out.println("[DEBUG] 말풍선 업데이트 Timer 중단");
        }
    }
    
    /**
     * 클릭 회피 움직임 체크 - 주기적으로 호출되어 클릭 위치를 확인하고 회피 동작 수행
     */
    private void checkForAvoidanceMovement() {
        if (!isCharacterActive) return;
        
        // 캐릭터가 물리 효과 중이거나 드래그 중이거나 말풍선이 표시 중이거나 AI 분석 중이면 자동 활동하지 않음
        if (character.isInPhysicsMode() || character.isBeingDragged() || isSpeechBubbleActive || isAnalyzing) {
            return;
        }
        
        // 최근 클릭이 있었다면 회피 동작 수행
        long currentTime = System.currentTimeMillis();
        if (lastClickTime > 0 && (currentTime - lastClickTime) < AVOIDANCE_DURATION) {
            startAvoidanceMovement();
        } else {
            // 클릭이 없거나 시간이 지났으면 간단한 행동들만 수행
            performSimpleActivity();
        }
    }
    
    /**
     * 간단한 활동 수행 (클릭 회피가 아닌 경우)
     */
    private void performSimpleActivity() {
        // AI 분석 중이면 일반적인 활동 중지
        if (isAnalyzing) {
            return;
        }
        
        int activity = random.nextInt(3); // 걷기 제외하고 3가지만
        
        switch (activity) {
            case 0:
                // 생각하기
                Platform.runLater(() -> {
                    character.setState(AdvisorCharacter.AnimationState.THINKING);
                });
                break;
            case 1:
                // 조언 말하기 (AI 분석 중이 아닐 때만)
                String[] tips = {
                    "열심히 플레이하고 계시네요! 👍",
                    "잠깐 휴식을 취하는 것도 좋아요! ☕",
                    "집중해서 플레이하고 계시는군요! 🎯",
                    "게임을 즐기고 계신가요? 😊",
                    "어려운 부분이 있으면 도움을 요청하세요! 🆘"
                };
                makeCharacterSpeak(tips[random.nextInt(tips.length)], SpeechBubble.BubbleType.ADVICE);
                break;
            case 2:
                // 가만히 서있기 (기본 상태)
                Platform.runLater(() -> {
                    character.setState(AdvisorCharacter.AnimationState.IDLE);
                });
                break;
        }
    }
    
    /**
     * 클릭 위치로부터 회피 동작 시작
     */
    private void startAvoidanceMovement() {
        if (!isCharacterActive || currentGameInfo == null || character.isInPhysicsMode() || character.isBeingDragged() || isSpeechBubbleActive) {
            return;
        }
        
        if (lastClickX < 0 || lastClickY < 0) {
            return; // 클릭 위치가 없음
        }
        
        double characterCenterX = character.getLayoutX() + character.getCharacterWidth() / 2;
        double characterCenterY = character.getLayoutY() + character.getCharacterHeight() / 2;
        
        // 클릭 위치와 캐릭터 간의 거리 계산
        double distanceToClick = Math.sqrt(
            Math.pow(characterCenterX - lastClickX, 2) + 
            Math.pow(characterCenterY - lastClickY, 2)
        );
        
        System.out.println("[DEBUG] 클릭과의 거리: " + (int)distanceToClick + "px");
        
        // 회피 거리 내에 있으면 피하는 움직임 수행
        if (distanceToClick < AVOIDANCE_DISTANCE) {
            System.out.println("[DEBUG] 클릭 위치에서 회피 시작!");
            moveAwayFromClick();
        }
    }
    
    /**
     * 클릭 위치로부터 피하는 움직임 수행
     */
    private void moveAwayFromClick() {
        RECT rect = currentGameInfo.getRect();
        
        double characterCenterX = character.getLayoutX() + character.getCharacterWidth() / 2;
        double characterCenterY = character.getLayoutY() + character.getCharacterHeight() / 2;
        
        // 클릭 위치에서 반대 방향으로 피하는 벡터 계산
        double avoidanceVectorX = characterCenterX - lastClickX;
        double avoidanceVectorY = characterCenterY - lastClickY;
        
        // 벡터 정규화
        double vectorLength = Math.sqrt(avoidanceVectorX * avoidanceVectorX + avoidanceVectorY * avoidanceVectorY);
        if (vectorLength > 0) {
            avoidanceVectorX /= vectorLength;
            avoidanceVectorY /= vectorLength;
        }
        
        // 피하는 거리 (더 멀리 피하도록)
        double avoidanceDistance = 120;
        
        // 목표 위치 계산 (클릭 위치에서 반대 방향으로)
        double targetX = characterCenterX + avoidanceVectorX * avoidanceDistance;
        double targetY = rect.bottom - character.getCharacterHeight() - 5; // Y는 하단 고정
        
        // 게임 창 경계 내로 제한
        double minX = rect.left + 20;
        double maxX = rect.right - character.getCharacterWidth() - 20;
        
        if (maxX <= minX) return; // 게임 창이 너무 작으면 이동하지 않음
        
        // X 좌표를 게임 창 경계 내로 클램핑
        final double finalTargetX = Math.max(minX, Math.min(maxX, targetX - character.getCharacterWidth() / 2));
        
        System.out.println("[DEBUG] 회피 목표 위치: (" + (int)finalTargetX + ", " + (int)targetY + ")");
        
        Platform.runLater(() -> {
            // 캐릭터의 현재 실제 위치 기반으로 상대적 이동 계산
            double currentX = character.getLayoutX();
            double deltaX = finalTargetX - currentX;
            
            // 이동 거리가 너무 작으면 최소한의 이동 보장
            if (Math.abs(deltaX) < 30) {
                deltaX = deltaX >= 0 ? 50 : -50;
                // 경계 체크
                if (currentX + deltaX < minX) deltaX = minX - currentX;
                if (currentX + deltaX > maxX) deltaX = maxX - currentX;
            }
            
            System.out.println("[DEBUG] 회피 이동: deltaX=" + (int)deltaX);
            
            character.walkTo(deltaX, 0);
            
            // 이전 Timeline들 중단
            stopActiveTimelines();
            
            // 걷기가 완료된 후 위치 동기화 및 착지 위치 업데이트
            currentWalkSyncTimer = new Timeline(
                new KeyFrame(Duration.seconds(2.1), e -> {
                    if (!character.isBeingDragged()) { // 드래그 중이 아닐 때만 동기화
                        syncCharacterPosition(); // 걷기 완료 후 동기화
                        saveLandingPosition(); // 새로운 위치를 착지 위치로 저장
                    }
                    currentWalkSyncTimer = null; // Timer 참조 해제
                })
            );
            currentWalkSyncTimer.play();
            
            // 걷는 동안 말풍선 위치 지속 업데이트
            currentBubbleUpdateTimer = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    if (!character.isBeingDragged()) { // 드래그 중이 아닐 때만 업데이트
                        updateSpeechBubblePosition();
                    }
                })
            );
            currentBubbleUpdateTimer.setCycleCount(20); // 2초간 업데이트
            currentBubbleUpdateTimer.setOnFinished(e -> currentBubbleUpdateTimer = null); // Timer 참조 해제
            currentBubbleUpdateTimer.play();
        });
    }
    
    /**
     * 게임별 특별 조언 제공
     */
    public void provideGameSpecificAdvice(String gameName) {
        if (!isCharacterActive) return;
        
        String advice = getGameSpecificAdvice(gameName);
        makeCharacterSpeak(advice, SpeechBubble.BubbleType.ADVICE);
    }
    
    /**
     * 게임별 조언 생성
     */
    private String getGameSpecificAdvice(String gameName) {
        String lowerGameName = gameName.toLowerCase();
        
        if (lowerGameName.contains("league") || lowerGameName.contains("lol")) {
            return "LoL 플레이 중이시네요! 미니맵을 자주 확인하세요! 🗺️";
        } else if (lowerGameName.contains("overwatch")) {
            return "오버워치! 팀과의 협력이 중요해요! 🤝";
        } else if (lowerGameName.contains("minecraft")) {
            return "마인크래프트에서 창의력을 발휘해보세요! ⛏️";
        } else if (lowerGameName.contains("valorant")) {
            return "발로란트! 정확한 에임과 전략이 중요해요! 🎯";
        } else if (lowerGameName.contains("steam")) {
            return "Steam 게임을 플레이 중이시네요! 즐거운 시간 되세요! 🎮";
        } else {
            return "멋진 게임이네요! 즐거운 플레이 되세요! 🎉";
        }
    }
    
    /**
     * 리소스 정리
     */
    public void cleanup() {
        stopIdleActivity();
        stopActiveTimelines(); // 활성 Timeline들 정리
        
        if (character != null) {
            character.cleanup();
        }
        if (speechBubble != null) {
            speechBubble.hideImmediately();
        }
    }
    
    /**
     * 캐릭터 활성 상태 반환
     */
    public boolean isCharacterActive() {
        return isCharacterActive;
    }
    
    /**
     * Scene 레벨 드래그 핸들링 설정 (캐릭터가 화면 밖으로 나가도 드래그 지속)
     */
    private void setupSceneDragHandling() {
        if (overlayPane.getScene() != null) {
            // Scene에 마우스 이벤트 핸들러 추가하여 전역 드래그 처리
            overlayPane.getScene().setOnMouseDragged(e -> {
                if (character.isBeingDragged()) {
                    // 캐릭터가 드래그 중일 때만 Scene 레벨에서 처리
                    e.consume();
                    System.out.println("[DEBUG] Scene 레벨 드래그 처리 중");
                }
            });
            
            overlayPane.getScene().setOnMouseReleased(e -> {
                if (character.isBeingDragged()) {
                    // 캐릭터가 드래그 중일 때만 Scene 레벨에서 처리
                    e.consume();
                    System.out.println("[DEBUG] Scene 레벨 마우스 릴리즈 처리");
                    // Scene 레벨 핸들러 제거
                    removeSceneDragHandling();
                }
            });
            
            System.out.println("[DEBUG] Scene 레벨 드래그 핸들러 설정 완료");
        }
    }
    
    /**
     * Scene 레벨 드래그 핸들링 제거
     */
    private void removeSceneDragHandling() {
        if (overlayPane.getScene() != null) {
            overlayPane.getScene().setOnMouseDragged(null);
            overlayPane.getScene().setOnMouseReleased(null);
            System.out.println("[DEBUG] Scene 레벨 드래그 핸들러 제거 완료");
        }
    }
    
    /**
     * 분석 완료 후 일반적인 메시지 예약
     */
    private void scheduleWelcomeBackMessage() {
        // 5초 후에 일반적인 환영 메시지 표시 (사용자가 AI 결과를 충분히 읽을 시간 제공)
        Timeline welcomeBackTimer = new Timeline(
            new KeyFrame(Duration.seconds(5), e -> {
                if (!isAnalyzing && isCharacterActive && !isSpeechBubbleActive) {
                    String[] welcomeMessages = {
                        "게임을 잘 진행하고 있군요! 😊",
                        "필요할 때 언제든 도움을 요청하세요! 🤝",
                        "즐거운 플레이 되세요! 🎮",
                        "화면 분석이 도움이 되었길 바라요! ✨"
                    };
                    String message = welcomeMessages[random.nextInt(welcomeMessages.length)];
                    Platform.runLater(() -> {
                        character.setState(AdvisorCharacter.AnimationState.TALKING);
                        makeCharacterSpeak(message, SpeechBubble.BubbleType.NORMAL);
                    });
                }
            })
        );
        welcomeBackTimer.play();
    }
} 