package com.maze.invisible_maze_server.model;

import org.springframework.web.socket.WebSocketSession;
import java.util.HashMap;
import java.util.Map;

public class GameRoom {
    private final String roomId;
    private final Map<String, WebSocketSession> sessions = new HashMap<>();
    private final Map<String, PlayerRole> playerRoles = new HashMap<>();
    private final int[][] mazeGrid; 

    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.mazeGrid = MazePresets.getRandomMaze(); // Auto-assigns a map asset
    }

    public String getRoomId() { return roomId; }
    public Map<String, WebSocketSession> getSessions() { return sessions; }
    public int[][] getMazeGrid() { return mazeGrid; }

    public void addPlayer(WebSocketSession session, PlayerRole role) {
        sessions.put(session.getId(), session);
        playerRoles.put(session.getId(), role);
    }

    public PlayerRole getRole(String sessionId) {
        return playerRoles.get(sessionId);
    }

    // Add these variables inside GameRoom.java
private int explorerRow = 0;
private int explorerCol = 0;

// Add these methods at the bottom of GameRoom.java before the final closing brace
public int getExplorerRow() { return explorerRow; }
public int getExplorerCol() { return explorerCol; }

public void setExplorerPosition(int row, int col) {
    this.explorerRow = row;
    this.explorerCol = col;
}
}