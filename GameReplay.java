package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class GameReplay {

    private final SolitaireGame game;
    private final List<String> events;

    private int currentEvent;

    private GameReplay(
            SolitaireGame game,
            List<String> events
    ) {
        this.game = game;
        this.events = events;
        this.currentEvent = 0;
    }

    public static GameReplay load(Path file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null.");
        }

        List<String> lines = Files.readAllLines(file);

        BoardType boardType = null;
        int boardSize = 0;
        GameMode gameMode = GameMode.MANUAL;
        Set<Position> initialPegs = new HashSet<>();

        List<String> events = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("BOARD=")) {
                boardType = BoardType.valueOf(
                        line.substring("BOARD=".length())
                );
            } else if (line.startsWith("SIZE=")) {
                boardSize = Integer.parseInt(
                        line.substring("SIZE=".length())
                );
            } else if (line.startsWith("MODE=")) {
                gameMode = GameMode.valueOf(
                        line.substring("MODE=".length())
                );
            } else if (line.startsWith("INITIAL=")) {
                initialPegs = decodePositions(
                        line.substring("INITIAL=".length())
                );
            } else if (line.startsWith("MOVE=")
                    || line.startsWith("RANDOMIZE=")) {
                events.add(line);
            }
        }

        if (boardType == null || boardSize == 0) {
            throw new IOException("Invalid recording file.");
        }

        SolitaireGame game =
                new SolitaireGame(boardType, boardSize);

        game.getBoard().setPegs(initialPegs);

        return new GameReplay(game, events);
    }

    public SolitaireGame getGame() {
        return game;
    }

    public boolean step() {
        if (currentEvent >= events.size()) {
            return false;
        }

        String event = events.get(currentEvent);

        if (event.startsWith("MOVE=")) {
            replayMove(event);
        } else if (event.startsWith("RANDOMIZE=")) {
            replayRandomization(event);
        }

        currentEvent++;

        return true;
    }

    public void replayAll() {
        while (step()) {
            // Continue until every event has been replayed.
        }
    }

    private void replayMove(String event) {
        String moveData = event.substring("MOVE=".length());

        String[] positions = moveData.split(";");

        if (positions.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid move in recording."
            );
        }

        Position from = decodePosition(positions[0]);
        Position to = decodePosition(positions[1]);

        boolean successful = game.makeMove(from, to);

        if (!successful) {
            throw new IllegalStateException(
                    "Recorded move is not valid: " + event
            );
        }
    }

    private void replayRandomization(String event) {
        String pegData = event.substring("RANDOMIZE=".length());

        Set<Position> positions = decodePositions(pegData);

        game.getBoard().setPegs(positions);
    }

    private static Set<Position> decodePositions(String data) {
        Set<Position> positions = new HashSet<>();

        if (data == null || data.isEmpty()) {
            return positions;
        }

        String[] encodedPositions = data.split("\\|");

        for (String encodedPosition : encodedPositions) {
            positions.add(decodePosition(encodedPosition));
        }

        return positions;
    }

    private static Position decodePosition(String encodedPosition) {
        String[] coordinates = encodedPosition.split(",");

        if (coordinates.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid position: " + encodedPosition
            );
        }

        int row = Integer.parseInt(coordinates[0]);
        int col = Integer.parseInt(coordinates[1]);

        return new Position(row, col);
    }
}