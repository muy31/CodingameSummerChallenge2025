import java.util.*;
import java.io.*;
import java.math.*;

public class Tile {
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
