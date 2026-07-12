package com.maze.invisible_maze_server.model;


import org.springframework.web.socket.WebSocketSession;
import java.util.HashMap;
import java.util.Map;

public class GameRoom {
    private final String roomId;
    private final Map<String, WebSocketSession> sessions = new HashMap<>();
    private final Map<String, PlayerRole> playerRoles = new HashMap<>();

    public GameRoom(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }

    public Map<String, WebSocketSession> getSessions() {
        return sessions;
    }

    public void addPlayer(WebSocketSession session, PlayerRole role) {
        sessions.put(session.getId(), session);
        playerRoles.put(session.getId(), role);
    }

    public PlayerRole getRole(String sessionId) {
        return playerRoles.get(sessionId);
    }
}