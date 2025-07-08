package com.gameadvisor.client.model;

import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.HWND;
import java.util.Objects;

public class GameWindowInfo {
    private final String gameName;
    private final String processName;
    private final RECT rect;
    private final HWND hwnd;
    
    public GameWindowInfo(String gameName, String processName, RECT rect, HWND hwnd) {
        this.gameName = gameName;
        this.processName = processName;
        this.rect = rect;
        this.hwnd = hwnd;
    }
    
    public String getGameName() { return gameName; }
    public String getProcessName() { return processName; }
    public RECT getRect() { return rect; }
    public HWND getHwnd() { return hwnd; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        GameWindowInfo that = (GameWindowInfo) obj;
        return Objects.equals(gameName, that.gameName) &&
               Objects.equals(processName, that.processName) &&
               Objects.equals(hwnd, that.hwnd) &&
               rect.left == that.rect.left &&
               rect.right == that.rect.right &&
               rect.top == that.rect.top &&
               rect.bottom == that.rect.bottom;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(gameName, processName, hwnd, 
                          rect.left, rect.right, rect.top, rect.bottom);
    }
    
    @Override
    public String toString() {
        return String.format("GameWindowInfo{name='%s', process='%s', rect=(%d,%d,%d,%d)}", 
                           gameName, processName, rect.left, rect.top, rect.right, rect.bottom);
    }
} 