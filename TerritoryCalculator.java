import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Main class to calculate territory control. Unused.
 */
public class TerritoryCalculator {

    /**
     * Calculates the number of tiles closer to each player's agents.
     *
     */
    public static TerritoryResult calculateTerritory(Tile[][] map, Collection<Agent> agents) {
        if (map == null || map.length == 0 || map[0].length == 0) {
            return new TerritoryResult(0, 0);
        }

        int width = map.length;
        int height = map[0].length;

        // Step 1: Create distance maps for each player.
        // Initialize with a large value to represent infinity.
        int[][] distancesToPlayer0 = new int[width][height];
        int[][] distancesToPlayer1 = new int[width][height];

        for (int i = 0; i < width; i++) {
            Arrays.fill(distancesToPlayer0[i], Integer.MAX_VALUE);
            Arrays.fill(distancesToPlayer1[i], Integer.MAX_VALUE);
        }

        // Step 2: Run a multi-source BFS for each player to calculate shortest distances.
        calculateDistancesForPlayer(map, agents, 0, distancesToPlayer0);
        calculateDistancesForPlayer(map, agents, 1, distancesToPlayer1);

        // Step 3: Compare the distance maps to determine territory.
        int player0Territory = 0;
        int player1Territory = 0;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (distancesToPlayer0[i][j] < distancesToPlayer1[i][j]) {
                    player0Territory++;
                } else if (distancesToPlayer1[i][j] < distancesToPlayer0[i][j]) {
                    player1Territory++;
                }
                // If distances are equal, the tile is contested and belongs to neither.
            }
        }

        return new TerritoryResult(player0Territory, player1Territory);
    }

    /**
     * Performs a multi-source flood fill to find the shortest distance
     * from every tile to the nearest agent of a specific player.
     *
     */
    private static void calculateDistancesForPlayer(Tile[][] map, Collection<Agent> allAgents, int player, int[][] distanceMap) {
        int width = map.length;
        int height = map[0].length;

        // Use ArrayDeque for a more efficient queue implementation.
        Queue<Tile> queue = new ArrayDeque<>();

        // Initialize the queue with all agents of the specified player.
        for (Agent agent : allAgents) {
            if (agent.player == player && agent.active) {
                distanceMap[agent.x][agent.y] = 0;
                queue.add(map[agent.x][agent.y]);
            }
        }

        // Standard BFS logic
        int[] dx = {0, 0, 1, -1}; // Directions for neighbors (right, left, down, up)
        int[] dy = {1, -1, 0, 0};

        while (!queue.isEmpty()) {
            Tile current = queue.poll();

            for (int i = 0; i < 4; i++) {
                int newX = current.x + dx[i];
                int newY = current.y + dy[i];

                // Check if the neighbor is within map bounds
                if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
                    // If we found a shorter path to this neighbor, update it and add to queue
                    if (distanceMap[newX][newY] > distanceMap[current.x][current.y] + 1) {
                        distanceMap[newX][newY] = distanceMap[current.x][current.y] + 1;
                        queue.add(map[newX][newY]);
                    }
                }
            }
        }
    }
}
