
package model;

import java.util.List;

/**
 * A simple automated player that selects the first currently legal move.
 */
public class AutomatedPlayer {

    /**
     * Selects the first available legal move.
     *
     * @param game the current solitaire game
     * @return the selected move, or null if no move is available
     */
    public Move chooseMove(SolitaireGame game) {
        if (game == null) {
            return null;
        }

        if (game.isGameOver()) {
            return null;
        }

        List<Move> legalMoves = game.getBoard().legalMoves();

        if (legalMoves == null || legalMoves.isEmpty()) {
            return null;
        }

        return legalMoves.get(0);
    }

    /**
     * Selects and executes the next available move.
     *
     * @param game the current solitaire game
     * @return true if a move was successfully executed, false otherwise
     */
    public boolean makeNextMove(SolitaireGame game) {
        if (game == null) {
            return false;
        }

        if (game.isGameOver()) {
            return false;
        }

        Move move = chooseMove(game);

        if (move == null) {
            return false;
        }

        return game.makeMove(move.from(), move.to());
    }
}
