import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Win the water fight by controlling the most territory, or out-soak your opponent!
 * Bot really excels when there's 3 bots
 **/
public class Player {

    public static int playerId;
    public static int enemyId;
    public static int agentGameCount;
    public static Tile[][] map;

    public static HashMap<Integer, Agent> playerAgents = new HashMap<Integer, Agent>();
    public static HashMap<Integer, Agent> enemyAgents = new HashMap<Integer, Agent>();

    /**
     * Calculates the shortest path distance from every valid tile (value == 0) to every other reachable valid tile.
     *
     * @param grid A 2D array of Tile objects representing the map.
     * @return A map where each key is a starting Tile, and the value is another map
     * containing all reachable Tiles from the key and their shortest distance.
     */
    public static void calculateAllPairsPathDistances(Tile[][] grid) {
        if (grid == null || grid.length == 0) {
            Tile.AllDistances = new HashMap<>();
        }

        // First, pre-calculate all neighbor relationships for the entire grid.
        // precomputeNeighbors(grid);

        Map<Tile, Map<Tile, Integer>> allDistances = new HashMap<>();
        int rows = grid.length;
        int cols = grid[0].length;

        // Iterate over every tile in the grid to use it as a starting point
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Tile startTile = grid[i][j];

                // We only calculate paths from valid, unoccupied tiles (value == 0)
                if (startTile.value == 0) {
                    // The BFS function now uses the pre-computed neighbors.
                    Map<Tile, Integer> distancesFromStart = bfs(startTile);
                    allDistances.put(startTile, distancesFromStart);
                }
            }
        }

        Tile.AllDistances = allDistances;
    }

    // Performs a Breadth-First Search (BFS) using the pre-computed neighbors.
    private static Map<Tile, Integer> bfs(Tile startTile) {
        Map<Tile, Integer> distances = new HashMap<>();
        Queue<Tile> queue = new ArrayDeque<>();

        // The distance from the start tile to itself is 0
        distances.put(startTile, 0);
        queue.add(startTile);

        while (!queue.isEmpty()) {
            Tile current = queue.poll();
            int currentDistance = distances.get(current);

            // Iterate over the pre-computed neighbors for the current tile.
            for (Tile neighbor : current.getNeighbors()) {
                // If the neighbor has not been visited yet, record its distance and add to the queue.
                if (!distances.containsKey(neighbor)) {
                    distances.put(neighbor, currentDistance + 1);
                    queue.add(neighbor);
                }
            }
        }
        return distances;
    }

    static void precomputeTileData(Tile[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Tile currentTile = grid[i][j];

                // Define potential neighbors (up, down, left, right)
                int[] dx = {-1, 1, 0, 0};
                int[] dy = {0, 0, -1, 1};

                for (int k = 0; k < 4; k++) {
                    int newX = currentTile.x + dx[k];
                    int newY = currentTile.y + dy[k];

                    // Check if the neighbor is within grid bounds
                    if (newX >= 0 && newX < rows && newY >= 0 && newY < cols) {
                        Tile neighbor = grid[newX][newY];

                        // Populate walkable neighbors if current tile is walkable
                        if (currentTile.value == 0 && neighbor.value == 0) {
                            currentTile.neighbors.add(neighbor);
                        }

                        // Populate adjacent covers
                        if (neighbor.value == 1 || neighbor.value == 2) {
                            currentTile.covers.add(neighbor);
                        }
                    }
                }
            }
        }
    }

    static void printArray(Object[][] arr){
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[x].length; y++) {
                System.err.print(arr[x][y].toString() + " ");
            }
            System.err.println();
        }
    }

    static void printArray(double[][] arr){
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[x].length; y++) {
                System.err.print(String.format("%+03.2f", arr[x][y]) + " ");
            }
            System.err.println();
        }
    }

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int myId = in.nextInt(); // Your player id (0 or 1)
        int agentDataCount = in.nextInt(); // Total number of agents in the game

        playerId = myId;
        enemyId = myId == 0 ? 1 : 0;
        agentGameCount = agentDataCount;

        for (int i = 0; i < agentDataCount; i++) {
            int agentId = in.nextInt(); // Unique identifier for this agent
            int player = in.nextInt(); // Player id of this agent
            int shootCooldown = in.nextInt(); // Number of turns between each of this agent's shots
            int optimalRange = in.nextInt(); // Maximum manhattan distance for greatest damage output
            int soakingPower = in.nextInt(); // Damage output within optimal conditions
            int splashBombs = in.nextInt(); // Number of splash bombs this can throw this game

            if (player == playerId) {
                playerAgents.put(agentId, new Agent(player, agentId, shootCooldown, optimalRange, soakingPower, splashBombs, true));
            } else {
                enemyAgents.put(agentId, new Agent(player, agentId, shootCooldown, optimalRange, soakingPower, splashBombs, true));
            }
        }

        int width = in.nextInt(); // Width of the game map
        int height = in.nextInt(); // Height of the game map
        map = new Tile[width][height];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int x = in.nextInt(); // X coordinate, 0 is left edge
                int y = in.nextInt(); // Y coordinate, 0 is top edge
                int tileType = in.nextInt();

                map[x][y] = new Tile(x, y, tileType);
            }
        }

        Tile.damageReductionGradient = new HashMap<Tile, double[][]>();
        precomputeTileData(map);
        calculateAllPairsPathDistances(map);


        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[x].length; y++) {
                Tile agentXY = map[x][y];
                Tile.damageReductionGradient.put(agentXY, Tile.dmgReductionField(map, agentXY));
            }
        }

        int turnCounter = 0;

        //printArray(Tile.damageReductionGradient.get(Player.map[13][3]));
        //System.err.println(Tile.damageReductionGradient.get(Player.map[13][3])[10][0]);

        // game loop
        while (true) {
            turnCounter++;
            // Set dead
            Agent.setDead();

            int agentCount = in.nextInt(); // Total number of agents still in the game

            int[] turnAgentIds = new int[agentCount];

            for (int i = 0; i < agentCount; i++) {
                int agentId = in.nextInt();
                int x = in.nextInt();
                int y = in.nextInt();
                int cooldown = in.nextInt(); // Number of turns before this agent can shoot
                int splashBombs = in.nextInt();
                int wetness = in.nextInt(); // Damage (0-100) this agent has taken

                Agent.activeAgents.get(agentId).update(x, y, cooldown, splashBombs, wetness, true); // Will also set alive
                turnAgentIds[i] = agentId;
            }

            // Initialize enemyTargets
            ArrayList<Agent> allAgents = new ArrayList<Agent>(playerAgents.values());
            allAgents.addAll(enemyAgents.values());

            //ArrayList<Agent> playerIndexedAgents = new ArrayList<Agent>(playerAgents.values());

            int myAgentCount = in.nextInt(); // Number of alive agents controlled by you

            int playerAgentOffset = playerId == 0 ? 0 : agentCount - myAgentCount;

            System.err.println(Arrays.toString(turnAgentIds) + " " + playerAgentOffset);
            System.err.println("My Agents: " + myAgentCount + " " + playerAgents.values());

            State currentState = new State(allAgents);
            List<State.Action> chosenMoves = currentState.getBestActionsForTurn(turnCounter);
            System.err.println("Chosen Moves: " + chosenMoves); // Figure out a way in the end game to ensure I control the center part of the territory

            for (State.Action a: chosenMoves) {
                playerAgents.get(a.agentId).setGoal(a.moveTarget);
                // Immediately update my position
                playerAgents.get(a.agentId).x = a.moveTarget.x;
                playerAgents.get(a.agentId).y = a.moveTarget.y;
            }

            for (int i = 0; i < myAgentCount; i++) {
                Agent currentAgent = playerAgents.get(turnAgentIds[i + playerAgentOffset]);
                System.err.println(currentAgent);

                // Shooting choice
                String combatAction = "";
                Agent.AgentIntList boom = currentAgent.chooseBombTarget(allAgents, map, 13.5);
                boolean killed = currentAgent.chooseShotTarget(allAgents);

                String action = currentAgent.agentId + "; ";

                int bombDamage = 0;
                double shootDamage = 0;

                if (currentAgent.bombTarget != null) {
                    bombDamage = currentAgent.calculateSplashDamage(currentAgent.bombTarget.x, currentAgent.bombTarget.y, map, allAgents).damage;
                }

                if (currentAgent.shootId != null) {
                    shootDamage = currentAgent.calculateDamageShootingAt(currentAgent.shootId.x, currentAgent.shootId.y, false);
                }

                String message = "";
                // message += currentAgent.bombTarget + " Position: " + currentAgent.optimalBombingPosition(currentAgent.bombTarget) + " Damage: " + bombDamage + " ";
                if (currentAgent.goal != null) {
                    message += "MESSAGE Moving to: " + currentAgent.goal;
                }

                if (bombDamage > 0 && bombDamage > shootDamage && currentAgent.splash > 0 && currentAgent.bombTarget != null && currentAgent.inOkBombingPosition()) {

                    System.err.println(currentAgent.distanceTo(currentAgent.bombTarget) + " explosion? " + currentAgent.bombTarget);

                    combatAction = "THROW " + currentAgent.bombTarget.x + " " + currentAgent.bombTarget.y +";";

                    // Now that we've decided this, we need to resolve the new wetness of these agents, and if they're dead!
                    allAgents.removeAll(boom.killedList);

                    // The above actually isn't necessary, as simply deactivating would do the trick
                    for (Agent a: boom.hitList) {
                        a.wetness += 30;
                        if (a.wetness >= 100) {
                            a.active = false;
                        }
                    }
                } else if (currentAgent.shootId != null /*&& currentAgent.cooldown == 0*/) {
                    if (killed) {
                        // Disregard agent from future combats, unnecessary actually!
                        allAgents.remove(currentAgent.shootId);
                    }
                    combatAction = "SHOOT " + currentAgent.shootId.agentId + ";";
                    // message += "Shooting: " + currentAgent.shootId.agentId + " and killed? " + killed;

                    currentAgent.shootId.wetness += (int) shootDamage;
                    if (currentAgent.shootId.wetness >= 100) {
                        currentAgent.shootId.active = false;
                    }

                } else {
                    combatAction = "HUNKER_DOWN;";
                }

                // One line per agent: <agentId>;<action1;action2;...> actions are "MOVE x y | SHOOT id | THROW x y | HUNKER_DOWN | MESSAGE text"
                if (currentAgent.goal != null) {
                    action += "MOVE " + currentAgent.goal.x + " " + currentAgent.goal.y + ";";
                } else {
                    action += "MOVE " + currentAgent.x + " " + currentAgent.y + ";";
                }

                action += combatAction;

                action += message;
                System.out.println(action);
            }
        }
    }
}
