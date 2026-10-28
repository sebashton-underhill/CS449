package model;

public class SolitaireGame {

    private Board board;

    public SolitaireGame(BoardType type, int size) {
        newGame(type, size);
    }

    public void newGame(BoardType type, int size) {
        board = new Board(type, size);
    }

    public Board getBoard() {
        return board;
    }

    public boolean makeMove(Position from, Position to) {
        return board.makeMove(new Move(from, to));
    }

    public boolean isGameWon() {
        return board.isGameWon();
    }

    public boolean isGameLost() {
        return board.isGameLost();
    }

    public boolean isGameOver() {
        return board.isGameOver();
    }

    public int getPegCount() {
        return board.pegCount();
    }

    public String getRating() {
        int pegs = board.pegCount();

        if (pegs == 1) {
            return "Outstanding";
        } else if (pegs == 2) {
            return "Very Good";
        } else if (pegs == 3) {
            return "Good";
        } else {
            return "Average";
        }
    }
}