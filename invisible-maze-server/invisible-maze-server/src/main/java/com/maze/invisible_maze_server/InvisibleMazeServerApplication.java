package com.maze.invisible_maze_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.maze") // 👈 This tells Spring to scan everything under com.maze
public class InvisibleMazeServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvisibleMazeServerApplication.class, args);
	}

}