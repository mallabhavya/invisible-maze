package com.maze.invisible_maze_server.config;

import com.maze.invisible_maze_server.websocket.MazeWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MazeWebSocketHandler mazeWebSocketHandler;

    public WebSocketConfig(MazeWebSocketHandler mazeWebSocketHandler) {
        this.mazeWebSocketHandler = mazeWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mazeWebSocketHandler, "/game")
                .setAllowedOrigins("*"); // This lets our local index.html file talk to it safely
    }
}