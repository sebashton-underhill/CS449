package test;

import model.AutomatedPlayer;
import model.Board;
import model.BoardType;
import model.GameMode;
import model.GameRecorder;
import model.GameReplay;
import model.Move;
import model.Position;
import model.SolitaireGame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SolitaireAcceptanceCriteriaTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void englishBoardCanBeCreated() {
        Board board = new Board(BoardType.ENGLISH, 7);

        assertEquals(BoardType.ENGLISH, board.getType());
        assertEquals(7, board.getSize());
        assertFalse(board.getValidPositions().isEmpty());
    }

    @Test
    void hexagonBoardCanBeCreated() {
        Board board = new Board(BoardType.HEXAGON, 7);

        assertEquals(BoardType.HEXAGON, board.getType());
        assertEquals(7, board.getSize());
        assertFalse(board.getValidPositions().isEmpty());
    }

    @Test
    void boardStartsWithOneEmptyCenterPosition() {
        Board board = new Board(BoardType.ENGLISH, 7);

        Position center = new Position(3, 3);

        assertFalse(board.hasPeg(center));
        assertEquals(
                board.getValidPositions().size() - 1,
                board.getPegCount()
        );
    }

    @Test
    void invalidBoardSizeIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Board(BoardType.ENGLISH, 4)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Board(BoardType.ENGLISH, 2)
        );
    }

    @Test
    void validMoveRemovesTwoPegsAndAddsOne() {
        Board board = new Board(BoardType.ENGLISH, 7);

        Position from = new Position(1, 3);
        Position to = new Position(3, 3);

        int originalPegCount = board.getPegCount();

        assertTrue(board.isValidMove(from, to));
        assertTrue(board.makeMove(from, to));

        assertEquals(originalPegCount - 1, board.getPegCount());
        assertFalse(board.hasPeg(from));
        assertFalse(board.hasPeg(new Position(2, 3)));
        assertTrue(board.hasPeg(to));
    }

    @Test
    void invalidMoveDoesNotChangeBoard() {
        Board board = new Board(BoardType.ENGLISH, 7);

        int originalPegCount = board.getPegCount();

        Position from = new Position(0, 0);
        Position to = new Position(0, 2);

        assertFalse(board.isValidMove(from, to));
        assertFalse(board.makeMove(from, to));

        assertEquals(originalPegCount, board.getPegCount());
    }

    @Test
    void newGameResetsBoard() {
        SolitaireGame game =
                new SolitaireGame(BoardType.ENGLISH, 7);

        int originalPegCount = game.getPegCount();

        game.makeMove(
                new Position(1, 3),
                new Position(3, 3)
        );

        assertEquals(originalPegCount - 1, game.getPegCount());

        game.newGame(BoardType.ENGLISH, 7);

        assertEquals(originalPegCount, game.getPegCount());
    }

    @Test
    void randomizationPreservesPegCount() {
        Board board = new Board(BoardType.ENGLISH, 7);

        int originalPegCount = board.getPegCount();

        board.randomize(new Random(42));

        assertEquals(originalPegCount, board.getPegCount());
        assertTrue(
                board.getValidPositions().containsAll(board.getPegs())
        );
    }

    @Test
    void randomizationDoesNotCreateInvalidPegPositions() {
        Board board = new Board(BoardType.HEXAGON, 7);

        board.randomize(new Random(42));

        for (Position peg : board.getPegs()) {
            assertTrue(board.isValidPosition(peg));
        }
    }

    @Test
    void automatedPlayerCanChooseLegalMove() {
        SolitaireGame game =
                new SolitaireGame(BoardType.ENGLISH, 7);

        AutomatedPlayer player = new AutomatedPlayer();

        Move move = player.chooseMove(game);

        assertNotNull(move);
        assertTrue(
                game.getBoard().isValidMove(move.from(), move.to())
        );
    }

    @Test
    void automatedPlayerCanPlayUntilGameOver() {
        SolitaireGame game =
                new SolitaireGame(BoardType.ENGLISH, 7);

        AutomatedPlayer player = new AutomatedPlayer();

        int safetyCounter = 0;

        while (!game.isGameOver() && safetyCounter < 1000) {
            boolean moved = player.makeNextMove(game);

            assertTrue(moved);

            safetyCounter++;
        }

        assertTrue(game.isGameOver());
        assertTrue(safetyCounter < 1000);
    }

    @Test
    void automatedPlayerDoesNotMakeMoveAfterGameOver() {

    	SolitaireGame game =
            new SolitaireGame(BoardType.ENGLISH, 7);

    	Board board = game.getBoard();

    	// Remove all pegs to create a completed game.
    	board.setPegs(new HashSet<>());

    	AutomatedPlayer player = new AutomatedPlayer();

    	// Verify that the game has ended.
    	assertTrue(game.isGameOver());

	    // The automated player should not select a move.
	    assertNull(player.chooseMove(game));
	
	    // The automated player should not execute a move.
	    assertFalse(player.makeNextMove(game));
	
	    // Verify that the board remains unchanged.
	    assertEquals(0, board.getPegCount());
	}


    @Test
    void recordingCanBeSavedAndLoaded() throws Exception {
        SolitaireGame game =
                new SolitaireGame(BoardType.ENGLISH, 7);

        GameRecorder recorder =
                new GameRecorder(game, GameMode.MANUAL);

        Move move = new Move(
                new Position(1, 3),
                new Position(3, 3)
        );

        assertTrue(game.makeMove(move.from(), move.to()));

        recorder.recordMove(move);

        Path recordingFile =
                temporaryDirectory.resolve("game-recording.txt");

        recorder.save(recordingFile);

        assertTrue(java.nio.file.Files.exists(recordingFile));

        GameReplay replay = GameReplay.load(recordingFile);

        assertNotNull(replay);
        assertNotNull(replay.getGame());
    }

    @Test
    void replayCanAdvanceThroughRecordedMoves() throws Exception {
        SolitaireGame game =
                new SolitaireGame(BoardType.ENGLISH, 7);

        GameRecorder recorder =
                new GameRecorder(game, GameMode.MANUAL);

        Move move = new Move(
                new Position(1, 3),
                new Position(3, 3)
        );

        assertTrue(game.makeMove(move.from(), move.to()));

        recorder.recordMove(move);

        Path recordingFile =
                temporaryDirectory.resolve("replay-test.txt");

        recorder.save(recordingFile);

        GameReplay replay = GameReplay.load(recordingFile);

        assertTrue(replay.step());

        assertEquals(
                game.getPegCount(),
                replay.getGame().getPegCount()
        );
    }
}