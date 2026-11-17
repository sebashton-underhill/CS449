package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Board {

    private final BoardType type;
    private final int size;
    private final Set<Position> validPositions;
    private final Set<Position> pegs;

    public Board(BoardType type, int size) {
        if (type == null) {
            throw new IllegalArgumentException("Board type cannot be null.");
        }

        if (size < 3 || size % 2 == 0) {
            throw new IllegalArgumentException(
                    "Board size must be odd and at least 3."
            );
        }

        this.type = type;
        this.size = size;
        this.validPositions = createPositions(type, size);
        this.pegs = new HashSet<>();

        initializeBoard();
    }

    public BoardType getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public Set<Position> getValidPositions() {
        return Collections.unmodifiableSet(validPositions);
    }

    public Set<Position> getPegs() {
        return Collections.unmodifiableSet(pegs);
    }

    public int getPegCount() {
        return pegs.size();
    }

    private Set<Position> createPositions(BoardType type, int size) {
        Set<Position> positions = new HashSet<>();

        int center = size / 2;

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {

                boolean valid;

                switch (type) {
                    case ENGLISH:
                        valid =
                                (row >= center - 1 && row <= center + 1)
                                || (col >= center - 1 && col <= center + 1);
                        break;

                    case HEXAGON:
                        int distance = Math.abs(row - center)
                                + Math.abs(col - center);
                        valid = distance <= center;
                        break;

                    case DIAMOND:
                        valid =
                                Math.abs(row - center)
                                + Math.abs(col - center) <= center;
                        break;

                    default:
                        valid = false;
                }

                if (valid) {
                    positions.add(new Position(row, col));
                }
            }
        }

        return positions;
    }

    public void initializeBoard() {
        pegs.clear();

        for (Position position : validPositions) {
            pegs.add(position);
        }

        Position center = new Position(size / 2, size / 2);
        pegs.remove(center);
    }

    public boolean isValidPosition(Position position) {
        return validPositions.contains(position);
    }

    public boolean hasPeg(Position position) {
        return pegs.contains(position);
    }

    public boolean isValidMove(Position from, Position to) {
        if (from == null || to == null) {
            return false;
        }

        if (!isValidPosition(from) || !isValidPosition(to)) {
            return false;
        }

        if (!pegs.contains(from) || pegs.contains(to)) {
            return false;
        }

        int rowDifference = to.row() - from.row();
        int colDifference = to.col() - from.col();

        boolean straightMove =
                (Math.abs(rowDifference) == 2 && colDifference == 0)
                || (Math.abs(colDifference) == 2 && rowDifference == 0);

        if (!straightMove) {
            return false;
        }

        Position middle = new Position(
                from.row() + rowDifference / 2,
                from.col() + colDifference / 2
        );

        return pegs.contains(middle);
    }

    public boolean makeMove(Position from, Position to) {
        if (!isValidMove(from, to)) {
            return false;
        }

        int middleRow = (from.row() + to.row()) / 2;
        int middleCol = (from.col() + to.col()) / 2;

        Position middle = new Position(middleRow, middleCol);

        pegs.remove(from);
        pegs.remove(middle);
        pegs.add(to);

        return true;
    }

    public List<Move> legalMoves() {
        List<Move> moves = new ArrayList<>();

        for (Position from : pegs) {
            int[][] directions = {
                    {-2, 0},
                    {2, 0},
                    {0, -2},
                    {0, 2}
            };

            for (int[] direction : directions) {
                Position to = new Position(
                        from.row() + direction[0],
                        from.col() + direction[1]
                );

                if (isValidMove(from, to)) {
                    moves.add(new Move(from, to));
                }
            }
        }

        return moves;
    }

    public boolean isGameWon() {
        return pegs.size() == 1;
    }

    public boolean isGameLost() {
        return !isGameWon() && legalMoves().isEmpty();
    }

    public boolean isGameOver() {
        return isGameWon() || isGameLost();
    }

    /**
     * Randomizes the board while preserving the current number of pegs.
     */
    public void randomize(Random random) {
        randomizePegs(random, pegs.size());
    }

    /**
     * Randomizes the board using the requested number of pegs.
     */
    public void randomizePegs(Random random, int pegCount) {
        if (random == null) {
            throw new IllegalArgumentException("Random cannot be null.");
        }

        if (pegCount < 0 || pegCount > validPositions.size()) {
            throw new IllegalArgumentException("Invalid peg count.");
        }

        List<Position> positions = new ArrayList<>(validPositions);
        Collections.shuffle(positions, random);

        pegs.clear();

        for (int i = 0; i < pegCount; i++) {
            pegs.add(positions.get(i));
        }
    }

    /**
     * Replaces the current peg arrangement.
     * Used when loading a recorded game.
     */
    public void setPegs(Collection<Position> newPegs) {
        if (newPegs == null) {
            throw new IllegalArgumentException("Peg collection cannot be null.");
        }

        if (!validPositions.containsAll(newPegs)) {
            throw new IllegalArgumentException(
                    "Peg collection contains invalid positions."
            );
        }

        pegs.clear();
        pegs.addAll(newPegs);
    }
}