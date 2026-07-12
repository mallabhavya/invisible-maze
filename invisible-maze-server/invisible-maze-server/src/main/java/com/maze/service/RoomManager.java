package com.maze.service;

import com.maze.invisible_maze_server.model.GameRoom;
import com.maze.invisible_maze_server.model.PlayerRole;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomManager {
    private final Map<String, GameRoom> activeRooms = new ConcurrentHashMap<>();

    public GameRoom createRoom() {
        String roomId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        GameRoom newRoom = new GameRoom(roomId);
        activeRooms.put(roomId, newRoom);
        System.out.println("🏨 [ROOM MANAGER] Created room: " + roomId);
        return newRoom;
    }

    public GameRoom getRoom(String roomId) {
        return activeRooms.get(roomId.toUpperCase());
    }

    public String joinRoom(String roomId, WebSocketSession session) {
        GameRoom room = activeRooms.get(roomId.toUpperCase());
        if (room == null) {
            return "ERROR: Room not found.";
        }

        int currentPlayers = room.getSessions().size();

        if (currentPlayers == 0) {
            room.addPlayer(session, PlayerRole.NAVIGATOR);
            return "SUCCESS: Joined as NAVIGATOR";
        } else if (currentPlayers == 1) {
            room.addPlayer(session, PlayerRole.EXPLORER);
            return "SUCCESS: Joined as EXPLORER";
        } else {
            return "ERROR: Room is already full.";
        }
    }

    public void removeRoom(String roomId) {
        activeRooms.remove(roomId.toUpperCase());
        System.out.println("🗑️ [ROOM MANAGER] Closed and removed room: " + roomId);
    }
}