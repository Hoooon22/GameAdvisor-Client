package com.gameadvisor.client.ui.components.character;

import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

/**
 * 캐릭터의 말풍선 컴포넌트
 * 다양한 스타일과 애니메이션 효과를 지원
 */
public class SpeechBubble extends Group {
    
    public enum BubbleType {
        NORMAL,     // 일반 대화
        ADVICE,     // 조언
        WARNING,    // 경고
        SUCCESS,    // 성공
        THINKING,   // 생각
        STRATEGY    // 공략 (지속 표시)
    }
    
    private StackPane bubbleContainer;
    private TextArea textArea;
    private Polygon bubbleTail;
    private Button closeButton;
    private Button minimizeButton;
    private StackPane minimizedBar;
    private BubbleType currentType;
    private Timeline showAnimation;
    private Runnable onCloseCallback;
    private boolean isMinimized = false;
    private String currentMessage = "";
    
    // 말풍선 크기 설정
    private static final int MIN_WIDTH = 200;
    private static final int MAX_WIDTH = 450;
    private static final int MIN_HEIGHT = 80;
    private static final int MAX_HEIGHT = 300;
    private static final int CHARS_PER_LINE = 35;
    
    public SpeechBubble() {
        initializeBubble();
    }
    
    /**
     * 말풍선 초기화
     */
    private void initializeBubble() {
        // 텍스트 영역 생성 (스크롤 가능)
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setEditable(false); // 읽기 전용
        textArea.setFocusTraversable(true); // 포커스 받아서 스크롤 가능하도록 변경
        textArea.setPrefRowCount(3); // 기본 3줄
        
        // 마우스 투명도 해제하여 이벤트 수신 가능하도록
        textArea.setMouseTransparent(false);
        
        // 마우스 스크롤 이벤트 처리 개선
        textArea.setOnScroll(scrollEvent -> {
            System.out.println("[DEBUG] 텍스트 영역 스크롤 이벤트 발생: " + scrollEvent.getDeltaY());
            // 스크롤 이벤트는 consume하지 않고 자연스럽게 처리되도록 함
        });
        
        // 마우스 클릭 시 포커스 받도록 설정
        textArea.setOnMouseClicked(mouseEvent -> {
            textArea.requestFocus();
            System.out.println("[DEBUG] 텍스트 영역 클릭됨 - 포커스 요청");
        });
        
        // 패딩 설정 (버튼과 겹치지 않도록)
        StackPane.setMargin(textArea, new Insets(5, 60, 5, 10)); // 위, 오른쪽(버튼 공간), 아래, 왼쪽
        textArea.setStyle(
            "-fx-font-family: 'Malgun Gothic'; " +
            "-fx-font-size: 13px; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-control-inner-background: transparent; " +
            "-fx-background-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent; " +
            "-fx-highlight-fill: rgba(255,255,255,0.2); " +
            "-fx-highlight-text-fill: white; " +
            "-fx-text-box-border: transparent; " +
            "-fx-effect: dropshadow(gaussian, black, 2, 0.8, 1, 1); " +
            
            // 스크롤바 스타일링 - 개선된 버전
            ".scroll-bar:vertical { " +
            "    -fx-background-color: rgba(255,255,255,0.3); " +
            "    -fx-background-radius: 6px; " +
            "    -fx-pref-width: 10px; " +
            "    -fx-opacity: 0.8; " +
            "} " +
            ".scroll-bar:vertical .thumb { " +
            "    -fx-background-color: rgba(255,255,255,0.8); " +
            "    -fx-background-radius: 5px; " +
            "    -fx-min-height: 20px; " +
            "} " +
            ".scroll-bar:vertical .track { " +
            "    -fx-background-color: rgba(0,0,0,0.2); " +
            "    -fx-background-radius: 6px; " +
            "} " +
            ".scroll-bar:vertical .increment-button, .scroll-bar:vertical .decrement-button { " +
            "    -fx-opacity: 0; " +
            "    -fx-pref-height: 0; " +
            "} " +
            // 스크롤 호버 효과
            ".scroll-bar:vertical:hover { " +
            "    -fx-opacity: 1.0; " +
            "} " +
            ".scroll-bar:vertical .thumb:hover { " +
            "    -fx-background-color: rgba(255,255,255,1.0); " +
            "}"
        );
        
        // X 버튼 생성 (모든 말풍선에 표시)
        closeButton = new Button("✕");
        closeButton.setPrefSize(24, 24);
        closeButton.setMinSize(24, 24);
        closeButton.setMaxSize(24, 24);
        closeButton.setStyle(
            "-fx-background-color: rgba(220,20,20,0.8); " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-border-color: rgba(180,0,0,0.6); " +
            "-fx-border-width: 1px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 0; " +
            "-fx-effect: dropshadow(gaussian, black, 2, 0.6, 1, 1);"
        );
        
        // 호버 효과
        closeButton.setOnMouseEntered(e -> {
            System.out.println("[DEBUG] X 버튼 호버 진입");
            closeButton.setStyle(
                "-fx-background-color: rgba(255,30,30,0.9); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 12; " +
                "-fx-border-radius: 12; " +
                "-fx-border-color: rgba(200,0,0,0.8); " +
                "-fx-border-width: 1px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-effect: dropshadow(gaussian, black, 3, 0.8, 1, 1);"
            );
        });
        
        closeButton.setOnMouseExited(e -> {
            closeButton.setStyle(
                "-fx-background-color: rgba(220,20,20,0.8); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 12; " +
                "-fx-border-radius: 12; " +
                "-fx-border-color: rgba(180,0,0,0.6); " +
                "-fx-border-width: 1px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-effect: dropshadow(gaussian, black, 2, 0.6, 1, 1);"
            );
        });
        
        closeButton.setOnAction(e -> {
            System.out.println("[DEBUG] X 버튼 클릭됨 - 말풍선 닫기 시작");
            e.consume(); // 이벤트 전파 차단
            hide();
            if (onCloseCallback != null) {
                System.out.println("[DEBUG] 콜백 함수 실행");
                onCloseCallback.run();
            }
        });
        // 모든 말풍선에 닫기 버튼을 항상 표시
        closeButton.setVisible(true);
        
        // 최소화 버튼 생성 (모든 말풍선에 표시)
        minimizeButton = new Button("−");
        minimizeButton.setPrefSize(24, 24);
        minimizeButton.setMinSize(24, 24);
        minimizeButton.setMaxSize(24, 24);
        minimizeButton.setStyle(
            "-fx-background-color: rgba(50,150,50,0.8); " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-border-color: rgba(30,120,30,0.6); " +
            "-fx-border-width: 1px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 0; " +
            "-fx-effect: dropshadow(gaussian, black, 2, 0.6, 1, 1);"
        );
        
        // 최소화 버튼 호버 효과
        minimizeButton.setOnMouseEntered(e -> {
            System.out.println("[DEBUG] 최소화 버튼 호버 진입");
            minimizeButton.setStyle(
                "-fx-background-color: rgba(70,170,70,0.9); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 12; " +
                "-fx-border-radius: 12; " +
                "-fx-border-color: rgba(50,140,50,0.8); " +
                "-fx-border-width: 1px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-effect: dropshadow(gaussian, black, 3, 0.8, 1, 1);"
            );
        });
        
        minimizeButton.setOnMouseExited(e -> {
            minimizeButton.setStyle(
                "-fx-background-color: rgba(50,150,50,0.8); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 12; " +
                "-fx-border-radius: 12; " +
                "-fx-border-color: rgba(30,120,30,0.6); " +
                "-fx-border-width: 1px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-effect: dropshadow(gaussian, black, 2, 0.6, 1, 1);"
            );
        });
        
        minimizeButton.setOnAction(e -> {
            System.out.println("[DEBUG] 최소화 버튼 클릭됨");
            e.consume(); // 이벤트 전파 차단
            minimizeBubble();
        });
        // 모든 말풍선에 최소화 버튼을 항상 표시
        minimizeButton.setVisible(true);
        
        // 최소화된 바 생성 (윈도우 작업표시줄처럼)
        minimizedBar = new StackPane();
        Label minimizedLabel = new Label("📋 조언");
        minimizedLabel.setStyle(
            "-fx-font-family: 'Malgun Gothic'; " +
            "-fx-font-size: 12px; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-effect: dropshadow(gaussian, black, 2, 0.8, 1, 1);"
        );
        minimizedLabel.setPadding(new Insets(6, 12, 6, 12));
        
        minimizedBar.getChildren().add(minimizedLabel);
        minimizedBar.setPrefWidth(150);
        minimizedBar.setPrefHeight(30);
        minimizedBar.setMaxWidth(150);
        minimizedBar.setMaxHeight(30);
        minimizedBar.setStyle(
            "-fx-background-color: rgba(100,100,100,0.9); " +
            "-fx-background-radius: 15; " +
            "-fx-border-color: rgba(70,70,70,0.8); " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 15; " +
            "-fx-cursor: hand;"
        );
        
        // 최소화된 바 호버 효과
        minimizedBar.setOnMouseEntered(e -> {
            minimizedBar.setStyle(
                "-fx-background-color: rgba(120,120,120,1.0); " +
                "-fx-background-radius: 15; " +
                "-fx-border-color: rgba(90,90,90,1.0); " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 15; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, black, 5, 1.0, 2, 2);"
            );
        });
        
        minimizedBar.setOnMouseExited(e -> {
            minimizedBar.setStyle(
                "-fx-background-color: rgba(100,100,100,0.9); " +
                "-fx-background-radius: 15; " +
                "-fx-border-color: rgba(70,70,70,0.8); " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 15; " +
                "-fx-cursor: hand;"
            );
        });
        
        minimizedBar.setOnMouseClicked(e -> {
            System.out.println("[DEBUG] 최소화 바 클릭됨 - 복원 시작");
            e.consume();
            restoreBubble();
        });
        
        minimizedBar.setVisible(false); // 기본적으로 숨김
        minimizedBar.setManaged(false); // 레이아웃에서 완전히 제외
        
        // 말풍선 컨테이너 생성
        bubbleContainer = new StackPane();
        bubbleContainer.getChildren().addAll(textArea, closeButton, minimizeButton);
        
        // X 버튼과 최소화 버튼을 오른쪽 위에 위치시키기
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(5, 5, 0, 0));
        
        StackPane.setAlignment(minimizeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(minimizeButton, new Insets(5, 32, 0, 0)); // X 버튼 왼쪽에 배치
        
        // 더 강한 그림자 효과 추가
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(12.0);
        dropShadow.setOffsetX(4.0);
        dropShadow.setOffsetY(4.0);
        dropShadow.setColor(Color.color(0, 0, 0, 0.6));
        bubbleContainer.setEffect(dropShadow);
        
        // 말풍선 꼬리 생성 (아래쪽 삼각형)
        bubbleTail = new Polygon();
        bubbleTail.getPoints().addAll(new Double[]{
            0.0, 0.0,   // 위쪽 중앙
            -12.0, 18.0, // 왼쪽 아래
            12.0, 18.0   // 오른쪽 아래
        });
        
        // 꼬리에도 더 강한 그림자 효과 추가
        DropShadow tailShadow = new DropShadow();
        tailShadow.setRadius(10.0);
        tailShadow.setOffsetX(3.0);
        tailShadow.setOffsetY(3.0);
        tailShadow.setColor(Color.color(0, 0, 0, 0.5));
        bubbleTail.setEffect(tailShadow);
        
        this.getChildren().addAll(bubbleContainer, bubbleTail, minimizedBar);
        
        // 기본적으로 숨김
        this.setVisible(false);
        this.setOpacity(0.0);
    }
    
    /**
     * 텍스트 길이에 따른 말풍선 크기 계산
     */
    private void calculateBubbleSize(String message) {
        int textLength = message.length();
        int estimatedLines = Math.max(1, (textLength / CHARS_PER_LINE) + 1);
        
        // 너비 계산 (텍스트 길이에 따라 조정)
        int width = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, textLength * 8 + 50));
        
        // 높이 계산 (줄 수에 따라 조정)
        int height = Math.min(MAX_HEIGHT, Math.max(MIN_HEIGHT, estimatedLines * 25 + 40));
        
        // TextArea 크기 설정
        textArea.setPrefWidth(width - 20); // 패딩 고려
        textArea.setPrefHeight(height - 20);
        textArea.setMaxWidth(width - 20);
        textArea.setMaxHeight(height - 20);
        
        // 컨테이너 크기 설정
        bubbleContainer.setPrefWidth(width);
        bubbleContainer.setPrefHeight(height);
        bubbleContainer.setMaxWidth(width);
        bubbleContainer.setMaxHeight(height);
        
        System.out.println(String.format("[DEBUG] 말풍선 크기 조정: 텍스트 길이=%d, 예상 줄 수=%d, 크기=%dx%d", 
            textLength, estimatedLines, width, height));
    }

    /**
     * 말풍선 표시 - 중요한 말풍선 보호 기능 추가
     */
    public void showMessage(String message, BubbleType type) {
        // 현재 STRATEGY 타입이 표시 중이고 새 메시지가 STRATEGY가 아닌 경우 무시
        if (isShowing() && currentType == BubbleType.STRATEGY && type != BubbleType.STRATEGY) {
            System.out.println("[DEBUG] 공략 조언이 표시 중이므로 새로운 일반 메시지 무시: " + message);
            return;
        }
        
        // 현재 말풍선이 최소화 상태이고 새 메시지가 STRATEGY가 아닌 경우 무시
        if (isMinimized && type != BubbleType.STRATEGY) {
            System.out.println("[DEBUG] 말풍선이 최소화 상태이므로 새로운 일반 메시지 무시: " + message);
            return;
        }
        
        currentType = type;
        currentMessage = message;
        textArea.setText(message);
        
        // 텍스트 길이에 따른 크기 조정
        calculateBubbleSize(message);
        
        // 최소화 상태 초기화
        isMinimized = false;
        bubbleContainer.setVisible(true);
        bubbleTail.setVisible(true);
        minimizedBar.setVisible(false);
        
        // 타입에 따른 스타일 설정
        setupBubbleStyle(type);
        
        // 꼬리 위치 조정 (말풍선 중앙 하단)
        bubbleTail.setLayoutX(bubbleContainer.getWidth() / 2);
        bubbleTail.setLayoutY(bubbleContainer.getHeight());
        
        // 모든 말풍선에 X 버튼과 최소화 버튼 항상 표시
        closeButton.setVisible(true);
        minimizeButton.setVisible(true);
        
        System.out.println("[DEBUG] 말풍선 표시됨 - 타입: " + type + ", 메시지: " + message.substring(0, Math.min(50, message.length())) + "...");
        
        // 말풍선을 최상위로 가져오기
        this.toFront();
        
        // 페이드 인 애니메이션 (투명도를 낮춰서 배경이 잘 보이도록)
        this.setVisible(true);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(0.85); // 투명도 개선
        fadeIn.play();
        
        // AI 관련 메시지(THINKING, STRATEGY, WARNING)가 아닌 경우만 자동으로 숨기기
        if (type != BubbleType.STRATEGY && type != BubbleType.THINKING && type != BubbleType.WARNING) {
            // 텍스트 길이에 따른 표시 시간 계산
            double displayDuration = calculateDisplayDuration(message);
            Timeline autoHide = new Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(displayDuration), e -> hide())
            );
            autoHide.play();
        }
    }
    
    /**
     * 타입에 따른 말풍선 스타일 설정
     */
    private void setupBubbleStyle(BubbleType type) {
        Color bubbleColor;
        Color borderColor;
        String textColor = "#FFFFFF"; // 기본 흰색 텍스트
        String shadowColor = "#000000"; // 텍스트 그림자 색상
        
        switch (type) {
            case ADVICE:
                bubbleColor = Color.web("#1565C0", 0.6); // 진한 블루, 60% 불투명으로 개선
                borderColor = Color.web("#0D47A1"); // 더 진한 블루
                break;
            case WARNING:
                bubbleColor = Color.web("#EF6C00", 0.6); // 진한 오렌지, 60% 불투명으로 개선
                borderColor = Color.web("#BF360C"); // 더 진한 오렌지
                break;
            case SUCCESS:
                bubbleColor = Color.web("#2E7D32", 0.6); // 진한 그린, 60% 불투명으로 개선
                borderColor = Color.web("#1B5E20"); // 더 진한 그린
                break;
            case THINKING:
                bubbleColor = Color.web("#6A1B9A", 0.6); // 진한 퍼플, 60% 불투명으로 개선
                borderColor = Color.web("#4A148C"); // 더 진한 퍼플
                // 생각하는 말풍선은 원형으로 변경
                bubbleContainer.setStyle("-fx-background-radius: 50%; -fx-border-radius: 50%;");
                break;
            case STRATEGY:
                bubbleColor = Color.web("#D32F2F", 0.65); // 진한 빨강, 65% 불투명 (공략은 조금 더 진하게)
                borderColor = Color.web("#B71C1C"); // 더 진한 빨강
                break;
            case NORMAL:
            default:
                bubbleColor = Color.web("#424242", 0.6); // 진한 그레이, 60% 불투명으로 개선
                borderColor = Color.web("#212121"); // 더 진한 그레이
                break;
        }
        
        // 배경 설정 (반투명 효과)
        BackgroundFill backgroundFill = new BackgroundFill(
            bubbleColor, 
            new CornerRadii(18), 
            Insets.EMPTY
        );
        bubbleContainer.setBackground(new Background(backgroundFill));
        
        // 텍스트 색상 및 그림자 효과 업데이트
        textArea.setStyle(
            "-fx-font-family: 'Malgun Gothic'; " +
            "-fx-font-size: 13px; " +
            "-fx-text-fill: " + textColor + "; " +
            "-fx-font-weight: bold; " +
            "-fx-control-inner-background: transparent; " +
            "-fx-background-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent; " +
            "-fx-highlight-fill: rgba(255,255,255,0.2); " +
            "-fx-highlight-text-fill: white; " +
            "-fx-text-box-border: transparent; " +
            "-fx-effect: dropshadow(gaussian, " + shadowColor + ", 2, 0.8, 1, 1); " +
            
            // 스크롤바 스타일링
            ".scroll-bar:vertical { " +
            "    -fx-background-color: rgba(255,255,255,0.3); " +
            "    -fx-background-radius: 6px; " +
            "    -fx-pref-width: 8px; " +
            "} " +
            ".scroll-bar:vertical .thumb { " +
            "    -fx-background-color: rgba(255,255,255,0.7); " +
            "    -fx-background-radius: 4px; " +
            "} " +
            ".scroll-bar:vertical .track { " +
            "    -fx-background-color: rgba(0,0,0,0.1); " +
            "    -fx-background-radius: 6px; " +
            "} " +
            ".scroll-bar:vertical .increment-button, .scroll-bar:vertical .decrement-button { " +
            "    -fx-opacity: 0; " +
            "    -fx-pref-height: 0; " +
            "}"
        );
        
        // 테두리 설정 (더 굵게)
        if (type != BubbleType.THINKING) {
            bubbleContainer.setStyle(
                "-fx-border-color: " + toRgbString(borderColor) + "; " +
                "-fx-border-width: 3px; " +
                "-fx-border-radius: 18px; " +
                "-fx-background-radius: 18px;"
            );
        }
        
        // 꼬리 색상 설정 (투명도 적용)
        bubbleTail.setFill(bubbleColor);
        bubbleTail.setStroke(borderColor);
        bubbleTail.setStrokeWidth(3);
    }
    
    /**
     * 텍스트 길이에 따른 표시 시간 계산
     * 최소 2초, 최대 12초, 글자 수에 따라 조정
     */
    public double calculateDisplayDuration(String message) {
        if (message == null || message.trim().isEmpty()) {
            return 2.0; // 기본 최소 시간
        }
        
        // 실제 표시될 텍스트 길이 계산 (공백 제거)
        String cleanText = message.trim();
        int textLength = cleanText.length();
        
        // 기본 시간 (2초) + 글자당 추가 시간
        // 한국어 평균 읽기 속도: 분당 350글자 정도
        // 1글자당 약 0.17초 (60초 ÷ 350글자)
        double baseTime = 2.0;
        double readingTimePerChar = 0.1; // 조금 여유있게 설정
        
        double calculatedTime = baseTime + (textLength * readingTimePerChar);
        
        // 최소 2초, 최대 12초로 제한
        return Math.max(2.0, Math.min(12.0, calculatedTime));
    }
    
    /**
     * Color를 RGB 문자열로 변환
     */
    private String toRgbString(Color color) {
        return String.format("rgb(%d,%d,%d)", 
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255)
        );
    }
    
    /**
     * 말풍선 숨기기
     */
    public void hide() {
        if (!this.isVisible()) return; // 이미 숨겨진 경우 무시
        
        System.out.println("[DEBUG] 말풍선 숨기기 시작 - 현재 투명도: " + this.getOpacity());
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), this);
        fadeOut.setFromValue(this.getOpacity()); // 현재 투명도에서 시작
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            this.setVisible(false);
            // 최소화 상태도 초기화
            if (isMinimized) {
                isMinimized = false;
                bubbleContainer.setVisible(true);
                bubbleTail.setVisible(true);
                minimizedBar.setVisible(false);
            }
            System.out.println("[DEBUG] 말풍선 숨기기 완료");
        });
        fadeOut.play();
    }
    
    /**
     * 즉시 숨기기
     */
    public void hideImmediately() {
        this.setVisible(false);
        this.setOpacity(0.0);
    }
    
    /**
     * 말풍선이 표시 중인지 확인
     */
    public boolean isShowing() {
        return this.isVisible() && this.getOpacity() > 0.1;
    }
    
    /**
     * 현재 말풍선 타입 반환
     */
    public BubbleType getCurrentType() {
        return currentType;
    }

    /**
     * 말풍선이 닫힐 때 호출될 콜백 설정
     */
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }
    
    /**
     * 캐릭터 위에 말풍선 위치시키기 (화면 경계 고려)
     */
    public void positionAboveCharacter(double characterX, double characterY, double characterWidth, 
                                     double screenMinX, double screenMinY, double screenMaxX, double screenMaxY) {
        
        // 말풍선 크기 (실제 렌더링 후 크기 적용을 위해 최소값 사용)
        double bubbleWidth = Math.max(bubbleContainer.getWidth(), 320);
        double bubbleHeight = Math.max(bubbleContainer.getHeight(), 100);
        
        // 기본 위치: 캐릭터 위쪽 중앙
        double bubbleX = characterX + (characterWidth / 2) - (bubbleWidth / 2);
        double bubbleY = characterY - bubbleHeight - 20; // 20픽셀 간격
        
        // X 좌표 경계 조정 (좌우 화면 밖으로 나가지 않게)
        if (bubbleX < screenMinX + 10) {
            bubbleX = screenMinX + 10; // 왼쪽 여백
        } else if (bubbleX + bubbleWidth > screenMaxX - 10) {
            bubbleX = screenMaxX - bubbleWidth - 10; // 오른쪽 여백
        }
        
        // Y 좌표 경계 조정 (위쪽으로 잘리지 않게)
        if (bubbleY < screenMinY + 10) {
            // 위쪽에 공간이 없으면 캐릭터 아래쪽에 표시
            bubbleY = characterY + 80; // 캐릭터 높이 추정값 + 여백
            
            // 아래쪽에도 공간이 없으면 캐릭터 옆에 표시
            if (bubbleY + bubbleHeight > screenMaxY - 10) {
                bubbleY = characterY - (bubbleHeight / 2); // 캐릭터 옆 중앙
                
                // 캐릭터 오른쪽에 공간이 있으면 오른쪽에, 없으면 왼쪽에
                if (characterX + characterWidth + bubbleWidth + 30 < screenMaxX) {
                    bubbleX = characterX + characterWidth + 20; // 오른쪽
                } else {
                    bubbleX = characterX - bubbleWidth - 20; // 왼쪽
                }
            }
        }
        
        // 최종 위치 적용
        this.setLayoutX(bubbleX);
        this.setLayoutY(bubbleY);
        
        System.out.println("[DEBUG] 말풍선 위치 조정: 캐릭터(" + (int)characterX + "," + (int)characterY + 
                          ") → 말풍선(" + (int)bubbleX + "," + (int)bubbleY + ") 크기(" + (int)bubbleWidth + "x" + (int)bubbleHeight + ")");
        
        // 꼬리 위치 조정 (말풍선이 캐릭터 위에 있을 때만 하단 중앙)
        if (bubbleY < characterY) {
            // 말풍선이 캐릭터 위에 있음 - 꼬리를 하단 중앙에
            double tailX = Math.max(20, Math.min(bubbleWidth - 20, 
                (characterX + characterWidth/2) - bubbleX)); // 캐릭터 중앙을 향하도록
            bubbleTail.setLayoutX(tailX);
            bubbleTail.setLayoutY(bubbleHeight);
        } else {
            // 말풍선이 캐릭터 옆이나 아래에 있음 - 꼬리를 캐릭터 방향으로
            bubbleTail.setLayoutX(bubbleWidth / 2);
                         bubbleTail.setLayoutY(0); // 상단에
         }
    }
    
    /**
     * 캐릭터 위에 말풍선 위치시키기 (기본 화면 크기 사용)
     * @deprecated 화면 경계를 전달하는 오버로드 메서드 사용 권장
     */
    @Deprecated
    public void positionAboveCharacter(double characterX, double characterY, double characterWidth) {
        // 기본 게임 창 크기로 호출
        positionAboveCharacter(characterX, characterY, characterWidth, 0, 0, 1600, 1000);
    }
    
    /**
     * 말풍선을 최소화 (분석 버튼으로 복원 가능)
     */
    private void minimizeBubble() {
        if (isMinimized) return;
        
        System.out.println("[DEBUG] 말풍선 최소화 시작 - 분석 버튼이 복원 기능으로 변경됨");
        isMinimized = true;
        
        // 전체 말풍선과 꼬리 숨기기
        bubbleContainer.setVisible(false);
        bubbleTail.setVisible(false);
        
        // 최소화된 바는 사용하지 않음 (분석 버튼이 대신 기능)
        // minimizedBar.setVisible(true);  // 제거됨
        
        // 부드러운 전환 효과
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), bubbleContainer);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            System.out.println("[DEBUG] 말풍선 최소화 완료 - 이제 분석 버튼을 클릭하여 복원할 수 있습니다");
        });
        fadeOut.play();
        
        System.out.println("[DEBUG] 말풍선 최소화 완료");
    }
    
    /**
     * 말풍선을 복원 (최소화에서 전체 표시로)
     */
    private void restoreBubble() {
        if (!isMinimized) return;
        
        System.out.println("[DEBUG] 말풍선 복원 시작 - 분석 버튼에서 호출됨");
        isMinimized = false;
        
        // 최소화된 바는 이미 숨겨져 있음
        // minimizedBar.setVisible(false);  // 제거됨
        
        // 전체 말풍선과 꼬리 표시
        bubbleContainer.setVisible(true);
        bubbleTail.setVisible(true);
        
        // 부드러운 전환 효과 (직접 페이드 인)
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), bubbleContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(0.95);
        fadeIn.setOnFinished(e -> {
            System.out.println("[DEBUG] 말풍선 복원 완료 - 분석 버튼이 원래 기능으로 복원됨");
        });
        fadeIn.play();
        
        System.out.println("[DEBUG] 말풍선 복원 완료");
    }
    
    /**
     * 현재 최소화 상태인지 확인
     */
    public boolean isMinimized() {
        return isMinimized;
    }
    
    /**
     * 외부에서 말풍선 복원하기 (분석 버튼용)
     */
    public void restoreFromMinimized() {
        restoreBubble();
    }
} 