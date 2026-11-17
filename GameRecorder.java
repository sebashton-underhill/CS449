package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GameRecorder {

    private final BoardType boardType;
    private final int boardSize;
    private final GameMode gameMode;

    private final List<String> events = new ArrayList<>();

    public GameRecorder(SolitaireGame game, GameMode gameMode) {
        if (game == null || gameMode == null) {
            throw new IllegalArgumentException(
                    "Game and game mode cannot be null."
            );
        }

        this.boardType = game.getBoard().getType();
        this.boardSize = game.getBoard().getSize();
        this.gameMode = gameMode;

        events.add("BOARD=" + boardType);
        events.add("SIZE=" + boardSize);
        events.add("MODE=" + gameMode);

        events.add("INITIAL=" + encodePositions(
                game.getBoard().getPegs()
        ));
    }

    public void recordMove(Move move) {
        if (move == null) {
            throw new IllegalArgumentException("Move cannot be null.");
        }

        events.add(
                "MOVE="
                        + encodePosition(move.from())
                        + ";"
                        + encodePosition(move.to())
        );
    }

    public void recordRandomization(Collection<Position> pegs) {
        if (pegs == null) {
            throw new IllegalArgumentException(
                    "Peg collection cannot be null."
            );
        }

        events.add("RANDOMIZE=" + encodePositions(pegs));
    }

    public void save(Path file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null.");
        }

        Files.write(file, events);
    }

    private String encodePositions(Collection<Position> positions) {
        StringBuilder result = new StringBuilder();

        for (Position position : positions) {
            if (result.length() > 0) {
                result.append("|");
            }

            result.append(encodePosition(position));
        }

        return result.toString();
    }

    private String encodePosition(Position position) {
        return position.row() + "," + position.col();
    }
}