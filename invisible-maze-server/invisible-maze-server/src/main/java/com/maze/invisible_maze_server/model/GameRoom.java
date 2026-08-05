package com.maze.invisible_maze_server.model;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {

    private final String roomCode;
    private final Map<WebSocketSession, PlayerRole> players = new ConcurrentHashMap<>();
    
    private int currentRound = 1;
    private final int MAX_STAGES = 10;
    private int[][] currentMaze;
    private boolean gameActive = false;

    private int explorerRow = 1;
    private int explorerCol = 1;

    public GameRoom(String roomCode) {
        this.roomCode = roomCode;
        this.currentMaze = ProceduralMazeGenerator.generate(currentRound);
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getRoomId() {
        return roomCode;
    }

    public Set<WebSocketSession> getSessions() {
        return players.keySet();
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public int[][] getMazeGrid() {
        return currentMaze;
    }

    public PlayerRole getRole(WebSocketSession session) {
        return players.get(session);
    }

    public PlayerRole getAvailableRole() {
        boolean hasNavigator = false;
        for (PlayerRole role : players.values()) {
            if (role == PlayerRole.NAVIGATOR) hasNavigator = true;
        }
        return hasNavigator ? PlayerRole.EXPLORER : PlayerRole.NAVIGATOR;
    }

    public void setExplorerPosition(int row, int col) {
        this.explorerRow = row;
        this.explorerCol = col;
    }

    public void addPlayer(WebSocketSession session, PlayerRole role) {
        players.put(session, role);
    }

    public void removePlayer(WebSocketSession session) {
        players.remove(session);
        gameActive = false;
    }

    public boolean isFull() {
        return players.size() >= 2;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public boolean isGameActive() {
        return gameActive;
    }

    public void startStage(int stage) {
        this.currentRound = stage;
        this.currentMaze = ProceduralMazeGenerator.generate(stage);
        
        this.explorerRow = 1;
        this.explorerCol = 1;
        this.gameActive = true;

        String mazeJson = Arrays.deepToString(currentMaze);

        for (Map.Entry<WebSocketSession, PlayerRole> entry : players.entrySet()) {
            WebSocketSession session = entry.getKey();
            PlayerRole role = entry.getValue();

            sendMessage(session, "STAGE_UPDATE:" + currentRound);
            sendMessage(session, "START_GAME|" + role.name() + "|" + mazeJson);
        }
    }

    public void advanceRound() {
        if (currentRound < MAX_STAGES) {
            startStage(currentRound + 1);
        } else {
            gameActive = false;
            broadcast("LOBBY_STATUS:CONGRATULATIONS! You completed all 10 stages!");
        }
    }

    public synchronized void processPlayerMove(WebSocketSession session, int targetR, int targetC) {
        PlayerRole role = players.get(session);
        if (role != PlayerRole.EXPLORER || !gameActive) return;

        int deltaR = Math.abs(targetR - explorerRow);
        int deltaC = Math.abs(targetC - explorerCol);

        boolean isOneStep = (deltaR == 1 && deltaC == 0) || (deltaR == 0 && deltaC == 1);
        if (!isOneStep) {
            System.out.println("[Anti-Cheat] Blocked invalid step from (" 
                + explorerRow + "," + explorerCol + ") to (" + targetR + "," + targetC + ")");
            return;
        }

        if (targetR >= 0 && targetR < currentMaze.length && targetC >= 0 && targetC < currentMaze[0].length) {
            int cellValue = currentMaze[targetR][targetC];

            if (cellValue != 1) { // 1 = Wall
                this.explorerRow = targetR;
                this.explorerCol = targetC;

                broadcast("PLAYER_MOVED|" + targetR + "|" + targetC);

                if (cellValue == 3) { // 3 = Goal Fountain
                    advanceRound();
                }
            }
        }
    }

    public void broadcast(String message) {
        for (WebSocketSession session : players.keySet()) {
            if (session.isOpen()) {
                sendMessage(session, message);
            }
        }
    }

    public void notifyDisconnect(WebSocketSession disconnectedSession) {
        for (WebSocketSession session : players.keySet()) {
            if (!session.equals(disconnectedSession) && session.isOpen()) {
                sendMessage(session, "LOBBY_STATUS:Teammate disconnected. Waiting for cleanup...");
            }
        }
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}