import java.util.*;
import java.io.*;
import java.math.*;

public class Agent {

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
