package com.maze.service;

import com.maze.invisible_maze_server.model.GameRoom;
import com.maze.invisible_maze_server.model.PlayerRole;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RoomManager {
    private final Map<String, GameRoom> rooms = new HashMap<>();

    public GameRoom createRoom() {
        String roomId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        GameRoom room = new GameRoom(roomId);
        rooms.put(roomId, room);
        return room;
    }

    public String joinRoom(String roomId, WebSocketSession session) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return "ERROR: Room not found.";
        }

        int currentPlayers = room.getSessions().size();
        if (currentPlayers >= 2) {
            return "ERROR: Room is full.";
        }

        if (currentPlayers == 0) {
            room.addPlayer(session, PlayerRole.NAVIGATOR);
            return "NAVIGATOR";
        } else {
            room.addPlayer(session, PlayerRole.EXPLORER);

            // Set Initial Spawn coordinates on the grid matching the value 2
            int[][] grid = room.getMazeGrid();
            for (int r = 0; r < grid.length; r++) {
                for (int c = 0; c < grid[r].length; c++) {
                    if (grid[r][c] == 2) {
                        room.setExplorerPosition(r, c);
                        break;
                    }
                }
            }
            return "EXPLORER";
        }
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public GameRoom getRoomBySession(String sessionId) {
        for (GameRoom room : rooms.values()) {
            if (room.getSessions().containsKey(sessionId)) {
                return room;
            }
        }
        return null;
    }
}