package com.maze.invisible_maze_server.model;

import org.springframework.web.socket.WebSocketSession;
import java.util.HashMap;
import java.util.Map;

public class GameRoom {
    private final String roomId;
    private final Map<String, WebSocketSession> sessions = new HashMap<>();
    private final Map<String, PlayerRole> playerRoles = new HashMap<>();
    
    private int[][] mazeGrid; 
    private int currentRound = 0; // Starts at Round 0 (Map 1)
    private int explorerRow = 1;
    private int explorerCol = 1;

    public GameRoom(String roomId) {
        this.roomId = roomId;
        loadRoundMap();
    }

    

    public void loadRoundMap() {
        this.mazeGrid = MazePresets.getMazeByIndex(this.currentRound);
        // Automatically find where the start position '2' is located inside the new grid
        for (int r = 0; r < mazeGrid.length; r++) {
            for (int c = 0; c < mazeGrid[r].length; c++) {
                if (mazeGrid[r][c] == 2) {
                    this.explorerRow = r;
                    this.explorerCol = c;
                    return;
                }
            }
        }
    }

    public boolean advanceRound() {
        if (this.currentRound + 1 < MazePresets.getMaximumMaps()) {
            this.currentRound++;
            loadRoundMap();
            return true; // Moved to the next round map successfully
        }
        return false; // Game completely finished!
    }

    public int getCurrentRound() { return currentRound + 1; } // Return 1-indexed for display
    public String getRoomId() { return roomId; }
    public Map<String, WebSocketSession> getSessions() { return sessions; }
    public int[][] getMazeGrid() { return mazeGrid; }
    public int getExplorerRow() { return explorerRow; }
    public int getExplorerCol() { return explorerCol; }
    public void setExplorerPosition(int row, int col) { this.explorerRow = row; this.explorerCol = col; }
    public void addPlayer(WebSocketSession session, PlayerRole role) { sessions.put(session.getId(), session); playerRoles.put(session.getId(), role); }
    public PlayerRole getRole(String sessionId) { return playerRoles.get(sessionId); }
}