import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Win the water fight by controlling the most territory, or out-soak your opponent!
 * Bot really excels when there's 3 bots
 **/
class Player {

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

class Tile {
    public static Map<Tile, Map<Tile, Integer>> AllDistances;
    public static Map<Tile, double[][]> damageReductionGradient = new HashMap<Tile, double[][]>(); // Tile is the location of the agent getting cover, and the x,y gives the damageReduction from shots originated from there

    int x;
    int y;
    int value;
    public final Set<Tile> neighbors;
    public final Set<Tile> covers;

    public Tile(int x, int y, int type) {
        this.x = x;
        this.y = y;
        this.value = type;
        this.neighbors = new HashSet<Tile>();
        this.covers = new HashSet<Tile>();
    }

    public Set<Tile> getNeighbors() {
        return neighbors;
    }

    static class AgentDistanceObj {
        int distance;
        Agent closest;
    }

    public AgentDistanceObj closestAgent(Collection<Agent> agents) {
        int smallestDist = Integer.MAX_VALUE;
        Agent closest = null;
        for (Agent a: agents) {
            int dist = a.wetness >= 50 ? 2*this.distanceTo(a.getPositionAsTile()): this.distanceTo(a.getPositionAsTile());
            if (dist < smallestDist) {
                smallestDist = dist;
                closest = a;
            }
        }

        AgentDistanceObj obj = new AgentDistanceObj();
        obj.closest = closest;
        obj.distance = smallestDist;

        return obj;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tile tile = (Tile) obj;
        return x == tile.x && y == tile.y;
    }

    @Override
    public String toString() {
        return "Tile(" + x + ", " + y + ")";
    }

    public int distanceTo(Tile g) {
        // For now return Manhattan distance
        return Math.abs(g.x - this.x) + Math.abs(g.y - this.y);
    }

    public int agentPathDistanceTo(Tile g) {
        Map<Tile, Integer> mp = AllDistances.get(this);
        if (mp == null || mp.get(g) == null)
            System.err.println("Issue finding distance: " + this + ", " + g); // Probably a wall!
        return mp != null ? mp.get(g) : 0;
    }

    public int bombDistanceTo(Tile g) {
        return Math.max(this.distanceVecTo(g)[0], this.distanceVecTo(g)[1]);
    }

    public int[] distanceVecTo(Tile g) {
        // For now return Manhattan distance
        return new int[]{Math.abs(g.x - this.x), Math.abs(g.y - this.y)};
    }

    public Set<Tile> getCovers(){
        return this.covers;
    }

    public static double[][] dmgReductionField(Tile[][] map, Tile agentXY) {
        double[][] field = new double[map.length][map[0].length];
        Set<Tile> covers = agentXY.getCovers();

        if (covers.isEmpty()) {
            return field; // Return the field of zeros if no cover is available.
        }

        for (Tile cover : covers) {
            double reductionValue;
            if (cover.value == 1) {
                reductionValue = 0.5; // 50% for low cover
            } else if (cover.value == 2) {
                reductionValue = 0.75; // 75% for high cover
            } else {
                continue; // Not a valid cover tile.
            }

            // Determine the direction of the "shadow" cast by the cover, away from the agent.
            int dirX = cover.x - agentXY.x; // To the right if positive (|dirX| = 1)
            int dirY = cover.y - agentXY.y; // Downward if positive

            // Start propagating the reduction from the tile on the other side of the cover.
            int currentX = cover.x + dirX;
            int currentY = cover.y + dirY;

            boolean spanX = dirX == 0;
            boolean spanY = dirY == 0;

            while (currentX >= 0 && currentX < map.length && currentY >= 0 && currentY < map[0].length) {
                // Use Math.max to ensure the highest cover value takes precedence.
                field[currentX][currentY] = Math.max(field[currentX][currentY], reductionValue);

                // Move to the next tile in the line of fire. Direction agnostic approach.
                if (spanX) {
                    for (int fixX = 0; fixX < map.length; fixX++) {
                        field[fixX][currentY] = Math.max(field[fixX][currentY], reductionValue);
                    }
                }

                if (spanY) {
                    for (int fixY = 0; fixY < map[currentX].length; fixY++) {
                        field[currentX][fixY] = Math.max(field[currentX][fixY], reductionValue);
                    }
                }

                currentX += dirX;
                currentY += dirY;
            }

            int[] coverDX = {-1, -1, -1, 0, 0, 0, 1, 1, 1};
            int[] coverDY = {-1, 0, 1, -1, 0, 1, -1, 0, 1};

            for (int dirI = 0; dirI < coverDX.length; dirI++) {
                currentX = cover.x + coverDX[dirI];
                currentY = cover.y + coverDY[dirI];

                if (currentX >= 0 && currentX < map.length && currentY >= 0 && currentY < map[0].length) {
                    field[currentX][currentY] = field[currentX][currentY] == 2? 2 : 0;
                }
            }

            //field[cover.x][cover.y] = 2;
            //field[agentXY.x][agentXY.y] = 1;
        }

        return field;
    }
}

class Agent {

    public static HashMap<Integer, Agent> activeAgents = new HashMap<Integer, Agent>();

    public static void setDead(){
        for (Map.Entry<Integer, Agent> entry: activeAgents.entrySet()){
            Agent e = entry.getValue();
            e.active = false;
        }
    }

    int agentId; // Agent unique id
    int player; // Player id of this agent
    int shootCooldown; // Number of turns between each of this agent's shots
    int optimalRange; // Maximum manhattan distance for greatest damage output
    int soakingPower; // Damage output within optimal conditions
    int splashBombs;
    boolean active;
    static final int splashBombRange = 4;

    public String toString(){
        return "Agent(" + this.agentId + ", " + this.active + ")@"+this.getPositionAsTile();
    }

    // Turn by turn info
    int x;
    int y;
    int cooldown; // Number of turns before this agent can shoot
    int splash;
    int wetness; // Damage (0-100) this agent has taken

    Tile goal;
    Agent shootId; // The enemy agent I want to shoot at
    Tile bombTarget;

    public Agent(int player, int id, int shootCooldown, int optimalRange, int soakingPower, int maxSplashBombs, boolean actual){
        this.player = player;
        this.agentId = id;
        this.shootCooldown = shootCooldown;
        this.splashBombs = maxSplashBombs;
        this.optimalRange = optimalRange;
        this.soakingPower = soakingPower;
        this.wetness = 0;
        this.active = true;

        if (actual) {
            activeAgents.put(id, this);
        }
    }

    public Agent clone(){
        Agent cl = new Agent(this.player, this.agentId, this.shootCooldown, this.optimalRange, this.soakingPower, this.splashBombs, false);
        cl.update(this.x, this.y, this.cooldown, this.splash, this.wetness, this.active);

        cl.shootId = this.shootId;
        cl.bombTarget = this.bombTarget;
        cl.goal = this.goal;

        return cl;
    }

    public void update(int x, int y, int cooldown, int splash, int wetness, boolean active){
        this.x = x;
        this.y = y;
        this.cooldown = cooldown;
        this.splash = splash;
        this.wetness = wetness;
        this.active = active;

        this.shootId = null;
        this.bombTarget = null;
        this.goal = null;
    }

    public Tile immediateNextTile(Tile[][] map, Tile target) {

        Tile nextTile = null;
        int shortestDist = Integer.MAX_VALUE;

        for (Tile t: this.getPositionAsTile().getNeighbors()) {
            int dist = Tile.AllDistances.get(t).get(target);
            if (dist < shortestDist) {
                nextTile = t;
                shortestDist = dist;
            }
        }

        return nextTile;
    }

    // Start with maximizing some specific goal first, then add on other constraints? My cover alg is really good, but
    // A good idea is to use a genetic neural net for simulation? Would have to recreate the simulation, and load the weights/architecture here
    // Start simulating the engine turn-by-turn for a depth of say 6 or 7 turns
    // Send broadcast messages to other agents to participate in some tactics, FSM-like
    // Add hivemind understanding that if one agent can kill bc of proximity, I don't need to shoot (as in all-together planning rather than turn-based)


    // Start simulating enemy movements to make the minimax decision

    public void setGoal(Tile goal){
        this.goal = goal;
    }

    public Tile getPositionAsTile(){
        return Player.map[this.x][this.y];
    }

    public boolean isOkBombingPosition(Tile pos) {
        // Return false if bombing will commit suicide or if too far away
        return isOkBombingPosition(pos, this.bombTarget);
    }

    static boolean isOkBombingPosition(Tile pos, Tile target) {
        // Return false if bombing will commit suicide or if too far away
        if (target == null) return false;
        if (target.distanceTo(pos) > 4) return false;
        if (target.distanceTo(pos) <= 1) return false;
        // Diagonal (anti-suicide)
        return target.distanceTo(pos) != 2 || target.distanceVecTo(pos)[0] != 1;
    }

    public boolean inOkBombingPosition() {
        return this.isOkBombingPosition(this.getPositionAsTile());
    }

    public Tile optimalBombingPosition(Tile target) {
        // Get nearest accessible bombing location to bombTarget
        // Just sample some neighbors and return
        Set<Tile> evaluated = new HashSet<Tile>();
        Queue<Tile> tiles = new ArrayDeque<Tile>();
        tiles.add(this.getPositionAsTile());

        while (!tiles.isEmpty()){
            Tile current = tiles.poll();

            if (isOkBombingPosition(current, target)) {
                return current;
            }

            evaluated.add(current);

            for (Tile t: current.getNeighbors()) {
                if (!evaluated.contains(t)) tiles.add(t);
                evaluated.add(t);
            }
        }

        return null;
    }

    // Made agnostic
    // Make sure I update my position before calling this function
    public AgentIntList chooseBombTarget(Collection<Agent> allAgents, Tile[][] map, double thresholdScore) {

        // Returns who dies after a target has been chosen maximizing net wetness
        // Punish hurting of my own teammates
        if (this.splash <= 0) {
            this.bombTarget = null;
            return null;
        }

        Tile best = null;
        double bestScore = thresholdScore; // Prevent throwing just because someone is close enough
        AgentIntList explosion = null;

        for (int dX = -Agent.splashBombRange; dX <= Agent.splashBombRange; dX++) {
            for (int dY = -(Agent.splashBombRange - Math.abs(dX)); dY <= Agent.splashBombRange - Math.abs(dX); dY++) {
                int cX = dX + this.x;
                int cY = dY + this.y;

                // System.err.println("Evaluating throw at (" + cX + ", " + cY + ") from (" + this.x + ", " + this.y + ")");

                if (cX >= 0 && cX < map.length && cY >= 0 && cY < map[cX].length) {
                    double currentScore = 0;
                    // TODO: A potential issue of this below, however, is that I do not take into account agents that could move into my bomb
                    // Should calculateSplashDamage take into account all agents from a radius of 2 instead of 1 from the potential target?
                    AgentIntList potentialExplosion = calculateSplashDamage(cX, cY, map, allAgents); 
                    Tile bombTile = map[cX][cY];

                    for (Agent hitAgent : potentialExplosion.hitList) {
                        int numEscapes = 0;
                        for (Tile escapeTile : hitAgent.getPositionAsTile().getNeighbors()) {
                            if (escapeTile.bombDistanceTo(bombTile) > 1) {
                                numEscapes++;
                            }
                        }
                        
                        double damage = 30;
                        // If the agent can easily escape, reduce the value of hitting them.
                        if (numEscapes > 0) {
                            damage *= (0.45 / numEscapes); // Might tune later
                            // System.err.println(damage + " " + numEscapes);
                        }

                        // TODO: But also consider potential agents that could be hit, and add on the possibility token of bombing them (if they are an enemy)
                        // Consideration: By this point in time, I'm already aware of my teammate's updated positions, so only need to sample enemies 

                        // If the agent is an enemy, add to score. If teammate, subtract. Using total wetness instead of pure damage to prioritize kills
                        // currentScore += (hitAgent.player == this.player) ? -Math.min(100, damage + hitAgent.wetness) : Math.min(100, damage + hitAgent.wetness);
                        currentScore += (hitAgent.player == this.player) ? -damage : damage;
                    }

                    // Score based on enemies who might move INTO the blast
                    for (Agent agent : allAgents) {
                        if (!agent.active || agent.player == this.player) continue;
                        // If agent is 1 tile away from the blast (dist 2 from center)
                        if (agent.getPositionAsTile().distanceTo(bombTile) == 2 && agent.getPositionAsTile().bombDistanceTo(bombTile) > 1) {
                            // Assume a small chance the enemy makes a mistake, should actually be really small
                            double damage = 30 * 0.15; // 15% chance
                            currentScore += damage;
                        }
                    }

                    if (currentScore > 0) {
                        //System.err.println(this + " looking at throws at " + bombTile + " Damage: " + currentScore + " " + (currentScore > bestScore));
                        //System.err.println(potentialExplosion.hitList);
                    }

                    if (currentScore > bestScore) {
                        best = map[cX][cY];
                        bestScore = currentScore;
                        explosion = potentialExplosion;
                    }
                }
            }
        }

        this.bombTarget = best;
        if (explosion != null) {
            System.err.println("Agent " + agentId + " choosing to target " + best + ": " + bestScore + " killed: " + explosion.killedList);
        }
        return explosion;
    }

    static class AgentIntList {
        Set<Agent> killedList;
        Set<Agent> hitList;
        int damage;
    }

    public AgentIntList calculateSplashDamage(int cx, int cy, Tile[][] map, Collection<Agent> agents) {
        Set<Tile> tiles = new HashSet<Tile>();
        Set<Agent> killedEnemies = new HashSet<Agent>();
        Set<Agent> hitEnemies = new HashSet<Agent>();

        for (int dX = -1; dX <= 1; dX++) {
            for (int dY = -1; dY <= 1; dY++) {
                if (cx + dX >= 0 && cy + dY >= 0 && cx + dX < map.length && cy + dY < map[cx + dX].length) {
                    tiles.add(map[cx + dX][cy + dY]);
                }
            }
        }

        int damage = 0;
        for (Agent a: agents) {
            if (a.active) {
                if (tiles.contains(a.getPositionAsTile())) {
                    damage += a.player == this.player ? -30 : 30; // Prevent team killing
                    hitEnemies.add(a);
                    if (a.wetness + 30 >= 100 && a.player != this.player) {
                        killedEnemies.add(a);
                    }
                }
            }
        }

        AgentIntList res = new AgentIntList();
        res.killedList = killedEnemies;
        res.hitList = hitEnemies;
        res.damage = damage;

        return res;
    }

    // Is already agnostic!!
    public boolean chooseShotTarget(Collection<Agent> agents) {

        if (this.cooldown > 0) {
            this.shootId = null;
            return false;
        }

        Agent bestTarget = null;
        int bestScore = 2;
        boolean willKill = false;
        for (Agent agent: agents) {
            // Ensure this agent is alive
            if (agent.active && this.distanceTo(agent.x, agent.y) <= 2*this.optimalRange && agent.player != this.player) {

                double minDamageTakenByEnemy = Double.POSITIVE_INFINITY;
                List<Tile> enemyMoves = new ArrayList<>(agent.getPositionAsTile().getNeighbors());
                enemyMoves.add(agent.getPositionAsTile()); // Enemy can also stay still
                
                for (Tile enemyMoveTile : enemyMoves) {
                    boolean hunkered = agent.cooldown > 0 && agent.splash == 0 || this.distanceTo(agent.x, agent.y) > 2*agent.optimalRange; // Should replace with closest agent

                    // Calculate damage from my current position to the enemy's potential new position
                    double damageAtTile = this.calculateDamageShootingAt(enemyMoveTile.x, enemyMoveTile.y, hunkered);

                    if (hunkered) {
                        //System.err.println("Assuming agent will hunker: " + agent + " " + enemyMoveTile + " " + damageAtTile);
                    }
                    
                    minDamageTakenByEnemy = Math.min(minDamageTakenByEnemy, damageAtTile);
                }

                System.err.println("For shooting at: " + agent + ", minimum: " + minDamageTakenByEnemy);
                
                int score = (int) (minDamageTakenByEnemy + agent.wetness);
                
                // Give a big bonus for killing an agent
                if (agent.wetness < 100 && agent.wetness + minDamageTakenByEnemy >= 100) {
                    score += 100;
                }

                // Give a bonus for pushing an agent over 50
                if (agent.wetness < 50 && agent.wetness + minDamageTakenByEnemy >= 50) {
                    score += 50;
                }

                // --- Score this Target ---
                // The score is based on the guaranteed damage we can inflict.
                
                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = agent;
                    willKill = (score >= 100);
                } else if (score == bestScore) {
                    // Return closer
                    if (bestTarget != null)
                        bestTarget = this.distanceTo(agent.x, agent.y) < this.distanceTo(bestTarget.x, bestTarget.y) ? agent : bestTarget;
                }
            }
        }

        if (bestTarget != null /*&& bestScore > bestTarget.wetness*/) {
            this.shootId = bestTarget;
        } else {
            this.shootId = null;
        }
        return willKill;
    }

    public double calculateDamageShootingAt(int x, int y, boolean hunkered) {
        double reduction = 1 - Tile.damageReductionGradient.get(Player.map[x][y])[this.x][this.y]; // Calculate the damage reduction for the target's tile
        reduction -= hunkered ? 0.25 : 0;

        int shootDistance = distanceTo(x, y);
        if (shootDistance <= optimalRange) {
            return soakingPower * reduction;
        } else if (shootDistance <= optimalRange * 2) {
            return (soakingPower / 2.0) * reduction;
        }

        return 0;
    }

    public int distanceTo(int x, int y){
        Tile me = getPositionAsTile();
        Tile t = Player.map[x][y]; // Grab tile from some map rather than new one
        return me.distanceTo(t);
    }

    public int distanceTo(Tile g) {
        Tile me = getPositionAsTile();
        return me.distanceTo(g);
    }
}

class State {
    Map<Integer, Agent> allAgents;

    // Heuristic weights - TUNE THESE!
    static final double W_DAMAGE_TAKEN = -2.0;
    static final double W_DAMAGE_PROBABLE = 0.5; // Probability of getting shot if I'm not being targeted
    static final double W_TERRITORY_CAPTURE = 4.0;
    static final double W_STRATEGIC_GOAL = 25.0; // High base weight for strategic goals
    static final double W_COVER = 1.0;
    static final double W_SPLASH_CLUSTER = -30.0; // Penalty for clustering around agents with bombs
    static final double W_SPLASH_SPOT = -10.0; // Penalty for moving into spaces that agents are likely going to shoot bombs
    static final double W_FOCUS_TARGET = 15.0;
    static Map<Integer, Tile> strategicGoals;

    public State(Collection<Agent> agents){
        this.allAgents = new HashMap<>();
        for (Agent a : agents) this.allAgents.put(a.agentId, a.clone());
    }
    
    static class Action {
        int agentId;
        Tile moveTarget;
        public Action(int agentId, Tile moveTarget) { this.agentId = agentId; this.moveTarget = moveTarget; }
        public String toCommandString() { return agentId + ";MOVE " + moveTarget.x + " " + moveTarget.y; }
        public String toString() { return this.toCommandString(); }
    }

    public List<Action> getBestActionsForTurn(int turnCounter) {
        List<Action> chosenActions = new ArrayList<>();
        State planningState = new State(this.allAgents.values());
        Set<Tile> claimedTiles = new HashSet<>();

        List<Agent> myAgents = new ArrayList<>();
        List<Agent> enemyAgents = new ArrayList<>();
        for(Agent a : this.allAgents.values()) {
            if(a.active) {
                if(a.player == Player.playerId) myAgents.add(a);
                else enemyAgents.add(a);
            }
        }
        myAgents.sort(Comparator.comparingInt(a -> -a.wetness));

        // --- Determine Game State Context ---
        double agentAdvantage = myAgents.size() / (double) Math.max(1, enemyAgents.size());
        Agent teamTarget = findTeamTarget(enemyAgents);
        //if (turnCounter == 1) {
            strategicGoals = assignStrategicGoals(myAgents, enemyAgents);
        //}

        // Guess bomb positions for the enemy
        // Assume they will immediately use bomb
        planningState.allAgents.values().forEach(a -> {
            if (a.player == Player.enemyId) {
                a.chooseBombTarget(this.allAgents.values(), Player.map, 0);
            }
        });
        planningState.allAgents.values().forEach(a -> {
            if (a.player == Player.enemyId) {
                a.chooseShotTarget(myAgents);    
            }
        }); // Find out who enemy is likely to shoot

        System.err.print("I think agents will shoot at: ");
        planningState.allAgents.values().forEach(a -> {
            if (a.player == Player.enemyId) {
                System.err.print(a.agentId + "->" + a.shootId + " ");
            }
        });

        System.err.print("\nI think agents will throw at: ");
        planningState.allAgents.values().forEach(a -> {
            if (a.player == Player.enemyId) {
                System.err.print(a.agentId + "->" + a.bombTarget + " ");
            }
        });
        System.err.print("\n");

        System.err.println("Strategic Goals: " + strategicGoals);
        System.err.println("Team Target: " + teamTarget + " Advantage: " + agentAdvantage);

        for (Agent agent : myAgents) {
            Tile strategicGoal = strategicGoals.get(agent.agentId);
            Action bestActionForAgent = findBestMoveForAgent(agent, planningState, agentAdvantage, teamTarget, strategicGoal, claimedTiles, turnCounter);
            chosenActions.add(bestActionForAgent);
            
            claimedTiles.add(bestActionForAgent.moveTarget);
            planningState.allAgents.get(agent.agentId).x = bestActionForAgent.moveTarget.x;
            planningState.allAgents.get(agent.agentId).y = bestActionForAgent.moveTarget.y;
        }
        return chosenActions;
    }
    
    /**
     * REWRITTEN: Assigns agents to strategic points along a dynamic "front line".
     */
    private Map<Integer, Tile> assignStrategicGoals(List<Agent> myAgents, List<Agent> enemyAgents) {
        Map<Integer, Tile> assignments = new HashMap<>();
        if (myAgents.isEmpty()) return assignments;

        // 1. Calculate Centroids
        double myCentroidX = 0, myCentroidY = 0;
        for (Agent a : myAgents) { myCentroidX += a.x; myCentroidY += a.y; }
        myCentroidX /= myAgents.size();
        myCentroidY /= myAgents.size();

        double enemyCentroidX, enemyCentroidY;
        if (enemyAgents.isEmpty()) {
            enemyCentroidX = Player.map.length / 2.0;
            enemyCentroidY = Player.map[0].length / 2.0;
        } else {
            enemyCentroidX = 0; enemyCentroidY = 0;
            for (Agent a : enemyAgents) { enemyCentroidX += a.x; enemyCentroidY += a.y; }
            enemyCentroidX /= enemyAgents.size();
            enemyCentroidY /= enemyAgents.size();
        }

        // 2. Define the "Front Line"
        double midX = (myCentroidX + enemyCentroidX) / 2.0;
        double midY = (myCentroidY + enemyCentroidY) / 2.0;
        double perpX = -(enemyCentroidY - myCentroidY);
        double perpY = enemyCentroidX - myCentroidX;
        double mag = Math.sqrt(perpX * perpX + perpY * perpY);
        if (mag > 0.1) { perpX /= mag; perpY /= mag; } 
        else { perpX = 1; perpY = 0; } // Default to horizontal line if centroids are too close

        // 3. Generate Strategic Points along the line
        List<Tile> strategicPoints = new ArrayList<>();
        double spread = (Player.map.length + Player.map[0].length) / (double) (myAgents.size() + 1) / 2.0;
        for (int i = 0; i < myAgents.size(); i++) {
            double offset = (i - (myAgents.size() - 1) / 2.0) * spread;
            int targetX = (int)Math.round(midX + offset * perpX);
            int targetY = (int)Math.round(midY + offset * perpY);
            targetX = Math.max(0, Math.min(Player.map.length - 1, targetX));
            targetY = Math.max(0, Math.min(Player.map[0].length - 1, targetY));
            strategicPoints.add(findNearestWalkable(Player.map[targetX][targetY]));
        }

        // 4. Assign agents to nearest unassigned strategic point
        List<Agent> unassignedAgents = new ArrayList<>(myAgents);
        for (Tile point : strategicPoints) {
            Agent bestAgent = null;
            int bestDist = Integer.MAX_VALUE;
            for (Agent agent : unassignedAgents) {
                int dist = agent.getPositionAsTile().distanceTo(point);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestAgent = agent;
                }
            }
            if (bestAgent != null) {
                assignments.put(bestAgent.agentId, point);
                unassignedAgents.remove(bestAgent);
            }
        }
        return assignments;
    }

    // Helper to find a walkable tile if a strategic point is inside a wall.
    private Tile findNearestWalkable(Tile start) {
        if (start.value == 0) return start;
        Queue<Tile> queue = new ArrayDeque<>();
        Set<Tile> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        // Standard BFS logic
        int[] dx = {0, 0, 1, -1}; // Directions for neighbors (right, left, down, up)
        int[] dy = {1, -1, 0, 0};

        while (!queue.isEmpty()) {
            Tile current = queue.poll();

            for (int i = 0; i < 4; i++) {
                int newX = current.x + dx[i];
                int newY = current.y + dy[i];

                if (newX >= 0 && newX < Player.map.length && newY >= 0 && newY < Player.map[0].length) {

                    Tile neighbor = Player.map[newX][newY];

                    if(!visited.contains(neighbor)){
                        if(neighbor.value == 0) return neighbor;
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        // This is flawed because walls don't have neighbors lol
        return start; // Fallback 
    }
    
    private Agent findTeamTarget(List<Agent> enemies) {
        if (enemies.isEmpty()) return null;
        Agent bestTarget = null;
        double lowestScore = Double.POSITIVE_INFINITY;
        for (Agent enemy : enemies) {
            double under50Bonus = enemy.wetness < 50 ? 50 - enemy.wetness*1.5 : 100 - enemy.wetness*1.5;
            double score = (under50Bonus) * enemy.getPositionAsTile().distanceTo(Player.map[Player.map.length/2][Player.map[0].length/2]);
            if (score < lowestScore) {
                lowestScore = score;
                bestTarget = enemy;
            }
        }
        return bestTarget;
    }

    private Action findBestMoveForAgent(Agent agent, State currentState, double agentAdvantage, Agent teamTarget, Tile strategicGoal, Set<Tile> claimedTiles, int turnCounter) {
        Action bestAction = new Action(agent.agentId, agent.getPositionAsTile());
        double bestScore = Double.NEGATIVE_INFINITY;

        List<Tile> possibleMoveTiles = new ArrayList<>(agent.getPositionAsTile().neighbors);
        possibleMoveTiles.add(agent.getPositionAsTile());

        for (Tile moveTile : possibleMoveTiles) {
             if (moveTile.value != 0) continue;
             double score = evaluateMove(agent, moveTile, currentState, agentAdvantage, teamTarget, strategicGoal, claimedTiles, turnCounter);
             if (score > bestScore) {
                bestScore = score;
                bestAction = new Action(agent.agentId, moveTile);
            }
        }
        return bestAction;
    }

    private double evaluateMove(Agent agent, Tile moveTile, State currentState, double agentAdvantage, Agent teamTarget, Tile strategicGoal, Set<Tile> claimedTiles, int turnCounter) {
        if (claimedTiles.contains(moveTile)) {
            return Double.NEGATIVE_INFINITY;
        }

        double score = 0.0;
        
        List<Agent> enemies = new ArrayList<>();
        List<Agent> teammates = new ArrayList<>();
        for(Agent a : currentState.allAgents.values()){
            if(!a.active) continue;
            if(a.player == Player.enemyId) enemies.add(a);
            else if(a.agentId != agent.agentId) teammates.add(a);
        }

        // --- 1. Safety (with 1-ply lookahead) ---
        double potentialDamage = 0;
        double coverBonus = 0;

        double[][] dmgRedAtMove = Tile.damageReductionGradient.get(moveTile);
        for (Agent enemy : enemies) {
            coverBonus += dmgRedAtMove[enemy.x][enemy.y];

            // Adding some pseudo-lookahead pizzazz
            for (Tile neighbor : moveTile.getNeighbors()) {
                coverBonus += Tile.damageReductionGradient.get(neighbor)[enemy.x][enemy.y] / 2.0;
                for (Tile nextN: neighbor.getNeighbors()) {
                    coverBonus += Tile.damageReductionGradient.get(nextN)[enemy.x][enemy.y] / 3.0;
                }
            }
            
            double baseDamage = enemy.calculateDamageShootingAt(moveTile.x, moveTile.y, false); // Assume worst case, change to agent.cooldown > 0 for aa bit more assumption

            // If I'm likely to be shot
            if (enemy.shootId == agent) {
                potentialDamage += baseDamage;
            } 
            
            // If I'm moving to a place to be shot (therefore 1.5 if I'm going to be shot, 0.5 otherwise)
            if (enemy.cooldown == 0) {
                potentialDamage += W_DAMAGE_PROBABLE * baseDamage; // Assuming I'll hunker if I can't shoot
            }
        }

        double wetnessMultiplier = 1.0 + (agent.wetness / 50.0); // Higher wetness equals more damage consideration
        score += potentialDamage * W_DAMAGE_TAKEN * wetnessMultiplier;
        score += coverBonus * W_COVER;

        System.err.print(agent.agentId + "->" + moveTile + ", damage + cover: " + score);

        // --- 2. Splash Bomb Awareness --- (Prevents agents from clustering when the enemy has splash bombs left)
        double splashPenalty = 0;
        for (Agent enemy : enemies) {
            if (enemy.splash > 0 && enemy.getPositionAsTile().distanceTo(moveTile) <= 6) {
                for (Agent teammate : teammates) {
                    if (moveTile.bombDistanceTo(teammate.getPositionAsTile()) <= 2) {
                        splashPenalty += W_SPLASH_CLUSTER;
                    }
                }
            }
        }
        score += splashPenalty;

        // 2.b. Run away from likely splashed areas (if I'm worried about my health)
        double bombAreaPenalty = 0;
        for (Agent enemy : enemies) {
            if (enemy.bombTarget != null) {
                if (enemy.bombTarget.bombDistanceTo(moveTile) < 2) {
                    bombAreaPenalty += W_SPLASH_SPOT;
                }
            }
        }
        score += bombAreaPenalty * wetnessMultiplier;

        System.err.print(", splash Scare: " + (splashPenalty + bombAreaPenalty*wetnessMultiplier));
        double oS = score;

        // --- 3. Territory Control ---
        int distToClosestEnemyAfterMove = 999;
        for (Agent enemy : enemies) {
            distToClosestEnemyAfterMove = Math.min(distToClosestEnemyAfterMove, moveTile.distanceTo(enemy.getPositionAsTile()));
        }
        int controlDelta = distToClosestEnemyAfterMove - 0;
        if (controlDelta < 4) {
            double territoryCaptureScore = (4.0 - controlDelta);
            double advantageMultiplier = agentAdvantage > 1.0 ? agentAdvantage : 1.0;
            score += territoryCaptureScore * W_TERRITORY_CAPTURE * advantageMultiplier;
        }

        System.err.print(", territory " + (score - oS));
        oS = score;
        
        // --- 4. Strategic Positioning (with decay) ---
        if (strategicGoal != null) {
            int oldDist = agent.getPositionAsTile().agentPathDistanceTo(strategicGoal); // use path distance instead
            int newDist = moveTile.agentPathDistanceTo(strategicGoal); // use path distance instead
            if (newDist < oldDist) {
                // The strategic goal is most important in the first ~50 turns.
                double strategicGoalDecay = Math.max(0, 1.0 - (turnCounter / 50.0));
                score += W_STRATEGIC_GOAL * strategicGoalDecay;
            }
        }

        System.err.print( ", positioning: " + (score - oS));
        oS = score;

        // --- 5. Teamwork: Focus Fire ---
        if (teamTarget != null) {
            int oldDist = agent.getPositionAsTile().distanceTo(teamTarget.getPositionAsTile());
            int newDist = moveTile.distanceTo(teamTarget.getPositionAsTile());
            if (newDist < oldDist) {
                double rangeMultiplier = 4.0 / Math.max(1.0, agent.optimalRange);
                double advantageMultiplier = agentAdvantage > 1.0 ? agentAdvantage : 1.0;
                score += W_FOCUS_TARGET * rangeMultiplier * advantageMultiplier;
            }
        }
        
        System.err.println(", Teamwork: " + (score - oS));

        return score;
    }
}
