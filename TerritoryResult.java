/**
 * A class to hold the result of the territory calculation. Unused.
 */
class TerritoryResult {
    int player0Territory;
    int player1Territory;

    public TerritoryResult(int player0Territory, int player1Territory) {
        this.player0Territory = player0Territory;
        this.player1Territory = player1Territory;
    }

    @Override
    public String toString() {
        return "Player 0 Territory: " + player0Territory + ", Player 1 Territory: " + player1Territory;
    }
}
