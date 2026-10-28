package model;

import java.util.*;

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
                    "Board size must be odd and at least 3.");
        }

        this.type = type;
        this.size = size;
        this.validPositions = createPositions(type, size);
        this.pegs = new HashSet<>();

        initializeBoard();
    }

    private Set<Position> createPositions(BoardType type, int size) {
        Set<Position> positions = new HashSet<>();
        int middle = size / 2;

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                boolean valid = switch (type) {
                    case ENGLISH ->
                            (row >= size / 3 && row < size - size / 3)
                            || (col >= size / 3 && col < size - size / 3);

                    case HEXAGON ->
                            Math.abs(row - middle)
                            + Math.abs(col - middle)
                            + Math.abs((row - middle) - (col - middle))
                            <= size - 1;

                    case DIAMOND ->
                            Math.abs(row - middle)
                            + Math.abs(col - middle) <= middle;
                };

                if (valid) {
                    positions.add(new Position(row, col));
                }
            }
        }

        return positions;
    }

    private void initializeBoard() {
        pegs.clear();

        for (Position position : validPositions) {
            pegs.add(position);
        }

        // The center position starts empty.
        pegs.remove(centerPosition());
    }

    public Position centerPosition() {
        return new Position(size / 2, size / 2);
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

    public boolean hasPeg(Position position) {
        return pegs.contains(position);
    }

    public int pegCount() {
        return pegs.size();
    }

    public boolean isValidPosition(Position position) {
        return validPositions.contains(position);
    }

    private int[][] getDirections() {
        return switch (type) {
            case ENGLISH -> new int[][] {
                    {1, 0}, {-1, 0},
                    {0, 1}, {0, -1}
            };

            case HEXAGON -> new int[][] {
                    {1, 0}, {-1, 0},
                    {0, 1}, {0, -1},
                    {1, -1}, {-1, 1}
            };

            case DIAMOND -> new int[][] {
                    {1, 0}, {-1, 0},
                    {0, 1}, {0, -1},
                    {1, 1}, {1, -1},
                    {-1, 1}, {-1, -1}
            };
        };
    }

    public boolean isValidMove(Move move) {
        if (move == null || move.from() == null || move.to() == null) {
            return false;
        }

        Position from = move.from();
        Position to = move.to();

        if (!isValidPosition(from) || !isValidPosition(to)) {
            return false;
        }

        if (!pegs.contains(from) || pegs.contains(to)) {
            return false;
        }

        int rowDifference = to.row() - from.row();
        int colDifference = to.col() - from.col();

        for (int[] direction : getDirections()) {
            if (rowDifference == 2 * direction[0]
                    && colDifference == 2 * direction[1]) {

                Position jumped = new Position(
                        from.row() + direction[0],
                        from.col() + direction[1]);

                return pegs.contains(jumped);
            }
        }

        return false;
    }

    public boolean makeMove(Move move) {
        if (!isValidMove(move)) {
            return false;
        }

        Position from = move.from();
        Position to = move.to();

        Position jumped = new Position(
                (from.row() + to.row()) / 2,
                (from.col() + to.col()) / 2);

        pegs.remove(from);
        pegs.remove(jumped);
        pegs.add(to);

        return true;
    }

    public List<Move> legalMoves() {
        List<Move> moves = new ArrayList<>();

        for (Position from : pegs) {
            for (int[] direction : getDirections()) {
                Position to = new Position(
                        from.row() + 2 * direction[0],
                        from.col() + 2 * direction[1]);

                Move move = new Move(from, to);

                if (isValidMove(move)) {
                    moves.add(move);
                }
            }
        }

        return moves;
    }

    public boolean isGameWon() {
        return pegCount() == 1;
    }

    public boolean isGameLost() {
        return pegCount() > 1 && legalMoves().isEmpty();
    }

    public boolean isGameOver() {
        return isGameWon() || isGameLost();
    }
}