package com.maze.invisible_maze_server.websocket;

import com.maze.service.RoomManager;
import com.maze.invisible_maze_server.model.GameRoom;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MazeWebSocketHandler extends TextWebSocketHandler {

    private final RoomManager roomManager;

    public MazeWebSocketHandler(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("🔌 [SERVER] Client connected. Handshake complete. Session ID: " + session.getId());
        // We wait for them to click a button before placing them in a room now!
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload().trim();
        System.out.println("📥 [SERVER] Received instruction: " + payload + " from Session: " + session.getId());

        if (payload.equals("CREATE")) {
            // Player wants a brand new room
            GameRoom newRoom = roomManager.createRoom();
            String result = roomManager.joinRoom(newRoom.getRoomId(), session);
            session.sendMessage(new TextMessage("Room created successfully! Code: " + newRoom.getRoomId() + " (" + result + ")"));
            
        } else if (payload.startsWith("JOIN:")) {
            // Player typed a room code (e.g., "JOIN:A1B2")
            String codeToJoin = payload.split(":")[1].toUpperCase();
            String result = roomManager.joinRoom(codeToJoin, session);
            
            if (result.contains("ERROR")) {
                session.sendMessage(new TextMessage(result)); // Send error message back (e.g. Room not found or full)
            } else {
                session.sendMessage(new TextMessage("Successfully entered room " + codeToJoin + "! (" + result + ")"));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("❌ [SERVER] Client disconnected. Session ID: " + session.getId());
    }
}