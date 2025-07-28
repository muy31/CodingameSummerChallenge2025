import java.util.*;
import java.io.*;
import java.math.*;

public class State {
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
