package com.maze.invisible_maze_server.websocket;

import com.maze.invisible_maze_server.model.GameRoom;
import com.maze.invisible_maze_server.model.PlayerRole;
import com.maze.service.RoomManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
public class MazeWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private RoomManager roomManager;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload().trim();
        System.out.println("[WS RECV] " + payload);

        String[] parts = payload.split("\\|");
        String action = parts[0];

        switch (action) {
            // Updated to handle both "CREATE" (what your frontend sends) and "CREATE_ROOM"
            case "CREATE":
            case "CREATE_ROOM": {
                GameRoom room = roomManager.createRoom(session);
                System.out.println("[WS ROOM CREATED] Code: " + room.getRoomId());
                
                sendMessage(session, "ROOM_CREATED|" + room.getRoomId());
                break;
            }

            // Updated to handle both "JOIN" and "JOIN_ROOM" just in case
            case "JOIN":
            case "JOIN_ROOM": {
                if (parts.length < 2) return;
                String roomCode = parts[1].trim().toUpperCase();
                
                GameRoom room = roomManager.joinRoom(session, roomCode);
                
                if (room != null) {
                    sendMessage(session, "ROOM_JOINED|" + room.getRoomId());
                    room.startStage(1);
                } else {
                    sendMessage(session, "ERROR|Room full or invalid room code");
                }
                break;
            }

            case "MOVE": {
                if (parts.length < 3) return;
                int targetR = Integer.parseInt(parts[1]);
                int targetC = Integer.parseInt(parts[2]);

                GameRoom room = roomManager.getRoomBySession(session);
                if (room != null) {
                    room.processPlayerMove(session, targetR, targetC);
                }
                break;
            }

            case "STAGE_CLEAR": {
                GameRoom room = roomManager.getRoomBySession(session);
                if (room != null) {
                    room.advanceRound();
                }
                break;
            }

            default:
                System.out.println("[WS UNHANDLED ACTION] " + action);
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        roomManager.handleDisconnect(session);
    }

    private void sendMessage(WebSocketSession session, String text) {
        try {
            if (session.isOpen()) {
                System.out.println("[WS SEND] " + text);
                session.sendMessage(new TextMessage(text));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}