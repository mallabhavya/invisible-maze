package com.maze.service;

import com.maze.invisible_maze_server.model.GameRoom;
import com.maze.invisible_maze_server.model.PlayerRole;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomManager {

    private final Map<String, GameRoom> activeRooms = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, GameRoom> sessionRoomMap = new ConcurrentHashMap<>();

    public GameRoom createRoom(WebSocketSession session) {
        String code = generateRoomCode();
        GameRoom room = new GameRoom(code);
        
        room.addPlayer(session, PlayerRole.NAVIGATOR);
        activeRooms.put(code, room);
        sessionRoomMap.put(session, room);
        
        return room;
    }

    public GameRoom joinRoom(WebSocketSession session, String roomCode) {
        GameRoom room = activeRooms.get(roomCode);
        if (room != null && !room.isFull()) {
            room.addPlayer(session, PlayerRole.EXPLORER);
            sessionRoomMap.put(session, room);
            return room;
        }
        return null;
    }

    public GameRoom getRoom(String roomCode) {
        return activeRooms.get(roomCode);
    }

    public GameRoom getRoomBySession(WebSocketSession session) {
        return sessionRoomMap.get(session);
    }

    public void handleDisconnect(WebSocketSession session) {
        GameRoom room = sessionRoomMap.remove(session);
        if (room != null) {
            room.notifyDisconnect(session);
            room.removePlayer(session);

            if (room.isEmpty()) {
                activeRooms.remove(room.getRoomCode());
                System.out.println("Room " + room.getRoomCode() + " destroyed.");
            }
        }
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(4);
        do {
            sb.setLength(0);
            for (int i = 0; i < 4; i++) {
                sb.append(chars.charAt(rnd.nextInt(chars.length())));
            }
        } while (activeRooms.containsKey(sb.toString()));
        return sb.toString();
    }
}