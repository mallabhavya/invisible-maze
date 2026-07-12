package com.maze.invisible_maze_server.websocket;

import com.maze.service.RoomManager;
import com.maze.invisible_maze_server.model.GameRoom;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Arrays;

@Component
public class MazeWebSocketHandler extends TextWebSocketHandler {

    private final RoomManager roomManager;

    public MazeWebSocketHandler(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("🔌 [SERVER] Client connected. ID: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload().trim();
        System.out.println("📥 [SERVER] Received instruction: " + payload);

        if (payload.equals("CREATE")) {
            GameRoom newRoom = roomManager.createRoom();
            String result = roomManager.joinRoom(newRoom.getRoomId(), session);
            session.sendMessage(new TextMessage("LOBBY_STATUS:Room created! Code: " + newRoom.getRoomId()));
            
        } else if (payload.startsWith("JOIN:")) {
            String codeToJoin = payload.split(":")[1].toUpperCase();
            String result = roomManager.joinRoom(codeToJoin, session);
            
            if (result.contains("ERROR")) {
                session.sendMessage(new TextMessage("LOBBY_STATUS:" + result)); 
            } else {
                GameRoom room = roomManager.getRoom(codeToJoin);
                String gridJson = Arrays.deepToString(room.getMazeGrid());

                for (WebSocketSession activeSession : room.getSessions().values()) {
                    String playerRole = room.getRole(activeSession.getId()).toString();
                    String gameDataPackage = "START_GAME|" + playerRole + "|" + gridJson;
                    activeSession.sendMessage(new TextMessage(gameDataPackage));
                }
            }
        } else if (payload.startsWith("MOVE:")) {
            String[] parts = payload.split(":")[1].split(",");
            int targetRow = Integer.parseInt(parts[0]);
            int targetCol = Integer.parseInt(parts[1]);

            GameRoom room = roomManager.getRoomBySession(session.getId());

            if (room != null) {
                int[][] grid = room.getMazeGrid();

                if (targetRow >= 0 && targetRow < grid.length && targetCol >= 0 && targetCol < grid[0].length) {
                    if (grid[targetRow][targetCol] != 1) { 
                        room.setExplorerPosition(targetRow, targetCol);

                        String moveNotification = "PLAYER_MOVED|" + targetRow + "|" + targetCol;
                        for (WebSocketSession activeSession : room.getSessions().values()) {
                            activeSession.sendMessage(new TextMessage(moveNotification));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("❌ [SERVER] Client disconnected. ID: " + session.getId());
    }
}