package com.gameadvisor.client.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 화면 캡쳐 유틸리티 클래스
 */
public class ScreenCaptureUtil {
    
    private static Robot robot;
    
    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.err.println("Robot 인스턴스 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 지정된 영역의 화면을 캡쳐하여 Base64 문자열로 반환
     */
    public static String captureScreenArea(int x, int y, int width, int height) {
        if (robot == null) {
            throw new RuntimeException("Robot 인스턴스가 초기화되지 않았습니다.");
        }
        
        try {
            // 화면 캡쳐
            Rectangle captureRect = new Rectangle(x, y, width, height);
            BufferedImage screenCapture = robot.createScreenCapture(captureRect);
            
            // BufferedImage를 Base64로 변환
            return imageToBase64(screenCapture);
            
        } catch (Exception e) {
            throw new RuntimeException("화면 캡쳐 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    /**
     * 전체 화면을 캡쳐하여 Base64 문자열로 반환
     */
    public static String captureFullScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return captureScreenArea(0, 0, screenSize.width, screenSize.height);
    }
    
    /**
     * BufferedImage를 Base64 문자열로 변환
     */
    private static String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
    
    /**
     * 게임 창 영역만 캡쳐
     */
    public static String captureGameWindow(Rectangle gameRect) {
        return captureScreenArea(
            gameRect.x, 
            gameRect.y, 
            gameRect.width, 
            gameRect.height
        );
    }
} 