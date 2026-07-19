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
        System.out.println("🔌 [SERVER] Client connected. ID: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload().trim();
        System.out.println("📥 [SERVER] Received instruction: " + payload);

        // 1. CREATE GAME ROOM
        if (payload.equals("CREATE")) {
            GameRoom newRoom = roomManager.createRoom();
            roomManager.joinRoom(newRoom.getRoomId(), session);
            session.sendMessage(new TextMessage("LOBBY_STATUS:Room created! Code: " + newRoom.getRoomId()));
            
        // 2. JOIN EXISTING ROOM
        } else if (payload.startsWith("JOIN:")) {
            String codeToJoin = payload.split(":")[1].toUpperCase();
            String result = roomManager.joinRoom(codeToJoin, session);
            
            if (result.contains("ERROR")) {
                session.sendMessage(new TextMessage("LOBBY_STATUS:" + result)); 
            } else {
                GameRoom room = roomManager.getRoom(codeToJoin);
                String gridJson = convertMatrixToJson(room.getMazeGrid());

                for (WebSocketSession activeSession : room.getSessions().values()) {
                    String playerRole = room.getRole(activeSession.getId()).toString();
                    String gameDataPackage = "START_GAME|" + playerRole + "|" + gridJson;
                    activeSession.sendMessage(new TextMessage(gameDataPackage));
                }
            }

        // 3. EXPLORER MOVEMENT LOGIC
        } else if (payload.startsWith("MOVE:")) {
            String[] parts = payload.split(":")[1].split(",");
            int targetRow = Integer.parseInt(parts[0]);
            int targetCol = Integer.parseInt(parts[1]);

            GameRoom room = roomManager.getRoomBySession(session.getId());

            if (room != null) {
                int[][] grid = room.getMazeGrid();

                // Boundary checking against the 16x16 layout constraints
                if (targetRow >= 0 && targetRow < grid.length && targetCol >= 0 && targetCol < grid[0].length) {
                    if (grid[targetRow][targetCol] != 1) { // Not a wall
                        
                        room.setExplorerPosition(targetRow, targetCol);

                        // Check if the explorer reached the goal portal (cell value 3)
                        if (grid[targetRow][targetCol] == 3) {
                            boolean hasNextRound = room.advanceRound();
                            
                            if (hasNextRound) {
                                // Convert the next round's map layout to JSON string format
                                String nextGridJson = convertMatrixToJson(room.getMazeGrid());
                                
                                // Broadcast stage text string update to fix the counter element on screen
                                String stageMessage = "STAGE_UPDATE:" + room.getCurrentRound();
                                for (WebSocketSession activeSession : room.getSessions().values()) {
                                    activeSession.sendMessage(new TextMessage(stageMessage));
                                }
                                
                                // Broadcast the fresh layout and updated configurations to both clients
                                for (WebSocketSession activeSession : room.getSessions().values()) {
                                    String playerRole = room.getRole(activeSession.getId()).toString();
                                    String gameDataPackage = "START_GAME|" + playerRole + "|" + nextGridJson;
                                    activeSession.sendMessage(new TextMessage(gameDataPackage));
                                }
                                System.out.println("🔄 [SERVER] Room " + room.getRoomId() + " advanced to stage " + room.getCurrentRound());
                            } else {
                                // Final round cleared successfully
                                String victoryMessage = "LOBBY_STATUS:CONGRATULATIONS! YOU BEAT THE INVISIBLE MAZE CAMPAIGN!";
                                for (WebSocketSession activeSession : room.getSessions().values()) {
                                    activeSession.sendMessage(new TextMessage(victoryMessage));
                                }
                            }
                        } else {
                            // Standard step update broadcast notification
                            String moveNotification = "PLAYER_MOVED|" + targetRow + "|" + targetCol;
                            for (WebSocketSession activeSession : room.getSessions().values()) {
                                activeSession.sendMessage(new TextMessage(moveNotification));
                            }
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

    /**
     * Helper utility to convert a 2D integer matrix array into a standard, clean JSON matrix format
     * that is explicitly readable by JavaScript's JSON.parse() on the frontend layout side.
     */
    private String convertMatrixToJson(int[][] matrix) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < matrix.length; i++) {
            sb.append("[");
            for (int j = 0; j < matrix[i].length; j++) {
                sb.append(matrix[i][j]);
                if (j < matrix[i].length - 1) sb.append(",");
            }
            sb.append("]");
            if (i < matrix.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}