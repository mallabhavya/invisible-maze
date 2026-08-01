package com.maze.invisible_maze_server.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ProceduralMazeGenerator {

    public static int[][] generate(int stage) {
        int size = 15 + (stage * 4);
        int[][] maze = new int[size][size];

        for (int[] row : maze) {
            Arrays.fill(row, 1);
        }

        Random random = new Random();
        carvePath(1, 1, maze, size, random);

        maze[1][1] = 2;               // Explorer Spawn
        maze[size - 2][size - 2] = 3;  // Fountain Goal

        return maze;
    }

    private static void carvePath(int r, int c, int[][] maze, int size, Random random) {
        maze[r][c] = 0;

        List<int[]> directions = Arrays.asList(
            new int[]{-2, 0}, new int[]{2, 0},
            new int[]{0, -2}, new int[]{0, 2}
        );
        Collections.shuffle(directions, random);

        for (int[] dir : directions) {
            int nr = r + dir[0];
            int nc = c + dir[1];

            if (nr > 0 && nr < size - 1 && nc > 0 && nc < size - 1 && maze[nr][nc] == 1) {
                maze[r + dir[0] / 2][c + dir[1] / 2] = 0;
                carvePath(nr, nc, maze, size, random);
            }
        }
    }
}