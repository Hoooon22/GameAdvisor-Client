package com.gameadvisor.client.util;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import java.util.ArrayList;
import java.util.List;

public class WindowUtils {
    
    // User32 확장 인터페이스 (누락된 메서드들 추가)
    public interface ExtendedUser32 extends User32 {
        ExtendedUser32 INSTANCE = Native.load("user32", ExtendedUser32.class);
        
        boolean ClientToScreen(HWND hWnd, POINT lpPoint);
        boolean IsIconic(HWND hWnd);
        boolean ShowWindow(HWND hWnd, int nCmdShow);
        boolean GetClientRect(HWND hWnd, RECT lpRect);
    }
    
    public static RECT getWindowRectByProcessName(String processName) {
        List<HWND> hwndList = new ArrayList<>();
        User32.INSTANCE.EnumWindows((hWnd, data) -> {
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hWnd, windowText, 512);
            char[] className = new char[512];
            User32.INSTANCE.GetClassName(hWnd, className, 512);
            // 프로세스 ID 얻기
            IntByReference pid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hWnd, pid);
            String exeName = getProcessName(pid.getValue());
            System.out.println("[DEBUG] EnumWindows: HWND=" + hWnd + ", PID=" + pid.getValue() + ", exeName=" + exeName + ", target=" + processName);
            if (exeName != null && exeName.equalsIgnoreCase(processName)) {
                hwndList.add(hWnd);
                System.out.println("[DEBUG] HWND 매칭 성공: " + hWnd + ", PID=" + pid.getValue());
            }
            return true;
        }, null);
        if (!hwndList.isEmpty()) {
            RECT rect = new RECT();
            boolean gotRect = User32.INSTANCE.GetWindowRect(hwndList.get(0), rect);
            System.out.println("[DEBUG] GetWindowRect: " + gotRect + ", RECT=" + (gotRect ? rect.toString() : "null"));
            return gotRect ? rect : null;
        }
        System.out.println("[DEBUG] HWND를 찾지 못함");
        return null;
    }

    private static String getProcessName(int pid) {
        // 윈도우에서 PID로 프로세스명 얻기 (tasklist 사용)
        try {
            Process process = new ProcessBuilder("tasklist", "/FI", "PID eq " + pid, "/FO", "CSV", "/NH").start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream(), "MS949"))) {
                String line = reader.readLine();
                System.out.println("[DEBUG] getProcessName(" + pid + ") tasklist 결과: " + line);
                if (line != null && line.startsWith("\"")) {
                    String[] parts = line.split(",");
                    if (parts.length > 0) {
                        String name = parts[0].replaceAll("\"", "");
                        System.out.println("[DEBUG] getProcessName(" + pid + ") 추출: " + name);
                        return name;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // PID로 최상위 윈도우 핸들(HWND) 찾기
    public static com.sun.jna.platform.win32.WinDef.HWND findMainWindowByPid(int pid) {
        final com.sun.jna.platform.win32.WinDef.HWND[] result = new com.sun.jna.platform.win32.WinDef.HWND[1];
        User32.INSTANCE.EnumWindows((hWnd, data) -> {
            IntByReference procId = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hWnd, procId);
            // 최상위 & Visible 윈도우만
            boolean isMain = User32.INSTANCE.IsWindowVisible(hWnd) && User32.INSTANCE.GetParent(hWnd) == null;
            if (procId.getValue() == pid && isMain) {
                result[0] = hWnd;
                return false; // stop
            }
            return true;
        }, null);
        return result[0];
    }

    // HWND로 RECT 구하기
    public static RECT getWindowRectByHwnd(com.sun.jna.platform.win32.WinDef.HWND hwnd) {
        if (hwnd == null) return null;
        RECT rect = new RECT();
        boolean gotRect = User32.INSTANCE.GetWindowRect(hwnd, rect);
        return gotRect ? rect : null;
    }

    /**
     * 윈도우의 클라이언트 영역(게임 내용 부분만)을 가져오기
     * 타이틀바, 테두리 등을 제외한 실제 게임 화면 영역만 반환
     */
    public static RECT getClientRectByHwnd(HWND hwnd) {
        if (hwnd == null) return null;
        
        RECT clientRect = new RECT();
        boolean gotClientRect = ExtendedUser32.INSTANCE.GetClientRect(hwnd, clientRect);
        if (!gotClientRect) return null;
        
        // 클라이언트 좌표를 스크린 좌표로 변환
        POINT topLeft = new POINT();
        topLeft.x = 0;
        topLeft.y = 0;
        boolean converted = ExtendedUser32.INSTANCE.ClientToScreen(hwnd, topLeft);
        if (!converted) return null;
        
        // 스크린 좌표로 변환된 클라이언트 영역 반환
        RECT screenClientRect = new RECT();
        screenClientRect.left = topLeft.x;
        screenClientRect.top = topLeft.y;
        screenClientRect.right = topLeft.x + clientRect.right;
        screenClientRect.bottom = topLeft.y + clientRect.bottom;
        
        System.out.println("[DEBUG] Client Rect: " + screenClientRect.left + "," + screenClientRect.top + "," + 
                          screenClientRect.right + "," + screenClientRect.bottom + 
                          " (size: " + (screenClientRect.right - screenClientRect.left) + "x" + 
                          (screenClientRect.bottom - screenClientRect.top) + ")");
        
        return screenClientRect;
    }

    /**
     * 게임 프로세스의 클라이언트 영역을 캡쳐하기 위한 최적화된 메서드
     * 1. 게임 윈도우를 최상위로 가져오기
     * 2. 클라이언트 영역만 반환
     */
    public static RECT prepareGameWindowForCapture(HWND hwnd) {
        if (hwnd == null) return null;
        
        // 게임 윈도우를 최상위로 가져오기
        boolean brought = bringToFront(hwnd);
        System.out.println("[DEBUG] 게임 윈도우 최상위 가져오기: " + brought);
        
        // 잠시 대기 (윈도우가 완전히 전면에 나타날 시간)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 클라이언트 영역 반환
        return getClientRectByHwnd(hwnd);
    }

    /**
     * 윈도우가 최소화되어 있는지 확인
     */
    public static boolean isMinimized(HWND hwnd) {
        if (hwnd == null) return true;
        return ExtendedUser32.INSTANCE.IsIconic(hwnd);
    }

    /**
     * 최소화된 윈도우 복원
     */
    public static boolean restoreWindow(HWND hwnd) {
        if (hwnd == null) return false;
        return ExtendedUser32.INSTANCE.ShowWindow(hwnd, User32.SW_RESTORE);
    }

    // HWND를 화면 맨 앞으로 가져오기
    public static boolean bringToFront(HWND hwnd) {
        if (hwnd == null) return false;
        
        // 최소화된 경우 복원
        if (isMinimized(hwnd)) {
            restoreWindow(hwnd);
        }
        
        return User32.INSTANCE.SetForegroundWindow(hwnd);
    }

    // HWND를 오버레이(overlayHwnd) 바로 뒤로 보내기
    public static boolean sendToBack(HWND hwnd, HWND overlayHwnd) {
        if (hwnd == null) return false;
        // HWND_BOTTOM = new HWND(Pointer.createConstant(1L))
        HWND HWND_BOTTOM = new HWND(Pointer.createConstant(1L));
        // SWP_NOSIZE(0x0001) | SWP_NOMOVE(0x0002) | SWP_NOACTIVATE(0x0010)
        int flags = 0x0001 | 0x0002 | 0x0010;
        return User32.INSTANCE.SetWindowPos(hwnd, HWND_BOTTOM, 0, 0, 0, 0, flags);
    }

    // JavaFX Stage에서 HWND 얻기 (윈도우에서만 동작, reflection 활용)
    public static HWND getHWNDFromStage(javafx.stage.Stage stage) {
        try {
            // Stage -> Window -> nativeHandle (reflection)
            java.lang.reflect.Method getPeer = stage.getClass().getDeclaredMethod("impl_getPeer");
            getPeer.setAccessible(true);
            Object tkStage = getPeer.invoke(stage);
            java.lang.reflect.Method getRawHandle = tkStage.getClass().getDeclaredMethod("getRawHandle");
            getRawHandle.setAccessible(true);
            long handle = (Long) getRawHandle.invoke(tkStage);
            return new HWND(new Pointer(handle));
        } catch (Throwable t) {
            return null;
        }
    }
} 