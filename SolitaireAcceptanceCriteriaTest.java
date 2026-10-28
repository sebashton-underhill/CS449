package test;

import static org.junit.Assert.*;

import org.junit.jupiter.api.Test;

import model.*;
import java.util.Set;

class SolitaireAcceptanceCriteriaTest {

    // AC 1.3: Game uses the selected board size.
    @Test
    void selectedBoardSizeIsUsed() {
        SolitaireGame game =
                new SolitaireGame(BoardType.ENGLISH, 7);

        assertEquals(7, game.getBoard().getSize());
    }

    // AC 2.1 and AC 2.3: Board types are available and used.
    @Test
    void allBoardTypesAreAvailable() {
        assertArrayEquals(
                new BoardType[]{
                        BoardType.ENGLISH,
                        BoardType.HEXAGON,
                        BoardType.DIAMOND
                },
                BoardType.values()
        );
    }

    @Test
    void selectedBoardTypeIsUsed() {
        SolitaireGame game =
                new SolitaireGame(BoardType.DIAMOND, 7);

        assertEquals(
                BoardType.DIAMOND,
                game.getBoard().getType()
        );
    }

    // AC 3.1: Start a new game.
    @Test
    void newGameInitializesBoard() {
        SolitaireGame game =
                new SolitaireGame(BoardType.ENGLISH, 7);

        assertEquals(32, game.getPegCount());
        assertFalse(
                game.getBoard().hasPeg(
                        game.getBoard().centerPosition()
                )
        );
    }

    // AC 3.2: Board has valid positions and an empty center.
    @Test
    void boardContainsValidPositionsAndEmptyCenter() {
        Board board = new Board(BoardType.ENGLISH, 7);

        assertFalse(board.getValidPositions().isEmpty());
        assertTrue(
                board.isValidPosition(board.centerPosition())
        );
        assertFalse(board.hasPeg(board.centerPosition()));
    }

    // AC 4.1: Make a valid move.
    @Test
    void validMoveUpdatesBoard() {
        Board board = new Board(BoardType.ENGLISH, 7);

        Position from = new Position(1, 3);
        Position jumped = new Position(2, 3);
        Position to = new Position(3, 3);

        int originalPegCount = board.pegCount();

        assertTrue(board.makeMove(new Move(from, to)));

        assertFalse(board.hasPeg(from));
        assertFalse(board.hasPeg(jumped));
        assertTrue(board.hasPeg(to));
        assertEquals(originalPegCount - 1, board.pegCount());
    }

    // AC 4.2: Reject invalid moves without changing the board.
    @Test
    void invalidMoveDoesNotChangeBoard() {
        Board board = new Board(BoardType.ENGLISH, 7);

        int originalPegCount = board.pegCount();
        var originalPegs = SetCopy.copy(board.getPegs());

        // Destination is occupied at the start of the game.
        Move invalidMove = new Move(
                new Position(3, 0),
                new Position(3, 2)
        );

        assertFalse(board.makeMove(invalidMove));
        assertEquals(originalPegCount, board.pegCount());
        assertEquals(originalPegs, board.getPegs());
    }

    // AC 5.1: The initial game is not won.
    @Test
    void initialBoardIsNotWon() {
        Board board = new Board(BoardType.ENGLISH, 7);

        assertFalse(board.isGameWon());
    }

    // AC 5.2: The initial board has legal moves, so it is not lost.
    @Test
    void initialBoardIsNotLost() {
        Board board = new Board(BoardType.ENGLISH, 7);

        assertFalse(board.isGameLost());
        assertFalse(board.isGameOver());
    }

    // Invalid board sizes must be rejected.
    @Test
    void evenBoardSizeIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Board(BoardType.ENGLISH, 6)
        );
    }

    // Starting a new game resets the previous game.
    @Test
    void newGameResetsBoard() {
        SolitaireGame game =
                new SolitaireGame(BoardType.ENGLISH, 7);

        game.makeMove(
                new Position(1, 3),
                new Position(3, 3)
        );

        assertEquals(31, game.getPegCount());

        game.newGame(BoardType.ENGLISH, 7);

        assertEquals(32, game.getPegCount());
        assertFalse(game.isGameOver());
    }
    
    private static class SetCopy {
        static <T> java.util.Set<T> copy(java.util.Set<T> values) {
            return new java.util.HashSet<>(values);
        }
    }
    
}