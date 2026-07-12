package com.maze.invisible_maze_server.model;

import java.util.Random;

public class MazePresets {
    private static final Random random = new Random();

    private static final int[][][] MAPS = {
        // Map 1: The S-Curve
        {
            {2, 0, 1, 1, 1, 1},
            {1, 0, 0, 0, 1, 1},
            {1, 1, 1, 0, 1, 1},
            {1, 0, 0, 0, 0, 1},
            {1, 0, 1, 1, 0, 1},
            {1, 1, 1, 1, 0, 3}
        },
        // Map 2: The Zig-Zag
        {
            {2, 1, 1, 1, 1, 1},
            {0, 0, 0, 1, 1, 1},
            {1, 1, 0, 0, 0, 1},
            {1, 1, 1, 1, 0, 1},
            {1, 0, 0, 0, 0, 1},
            {1, 3, 1, 1, 1, 1}
        },
        // Map 3: The Fork
        {
            {2, 0, 0, 1, 1, 1},
            {1, 1, 0, 1, 3, 1},
            {1, 0, 0, 0, 0, 1},
            {1, 0, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0},
            {1, 1, 1, 1, 1, 1}
        },
        // Map 4: Spiral
        {
            {1, 1, 1, 1, 1, 1},
            {1, 2, 0, 0, 0, 1},
            {1, 1, 1, 1, 0, 1},
            {1, 3, 0, 1, 0, 1},
            {1, 1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 1}
        },
        // Map 5: Split Lanes
        {
            {2, 0, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 1},
            {1, 0, 1, 1, 0, 1},
            {1, 0, 1, 3, 0, 1},
            {1, 0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 1}
        }
    };

    public static int[][] getRandomMaze() {
        return MAPS[random.nextInt(MAPS.length)];
    }
}