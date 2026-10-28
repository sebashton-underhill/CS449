package view;

import model.*;

import javax.swing.*;
import java.awt.*;

public class SolitaireGUI extends JFrame {

    private final JComboBox<BoardType> boardTypeSelector;
    private final JComboBox<Integer> sizeSelector;
    private final JPanel boardPanel;
    private final JLabel statusLabel;

    private SolitaireGame game;
    private Position selectedPosition;

    public SolitaireGUI() {
        setTitle("Peg Solitaire");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        boardTypeSelector = new JComboBox<>(BoardType.values());

        sizeSelector = new JComboBox<>(
                new Integer[]{5, 7, 9, 11});

        JButton newGameButton = new JButton("New Game");

        JPanel controls = new JPanel();
        controls.add(new JLabel("Board Type:"));
        controls.add(boardTypeSelector);
        controls.add(new JLabel("Board Size:"));
        controls.add(sizeSelector);
        controls.add(newGameButton);

        boardPanel = new JPanel();
        statusLabel = new JLabel("Select a peg to begin.");

        add(controls, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        newGameButton.addActionListener(event -> startNewGame());

        startNewGame();

        setSize(700, 700);
        setLocationRelativeTo(null);
    }

    private void startNewGame() {
        BoardType type = (BoardType) boardTypeSelector.getSelectedItem();
        Integer size = (Integer) sizeSelector.getSelectedItem();

        if (type == null || size == null) {
            return;
        }

        game = new SolitaireGame(type, size);
        selectedPosition = null;

        statusLabel.setText("Select a peg to begin.");
        renderBoard();
    }

    private void renderBoard() {
        Board board = game.getBoard();
        int size = board.getSize();

        boardPanel.removeAll();
        boardPanel.setLayout(new GridLayout(size, size, 3, 3));

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                Position position = new Position(row, col);

                if (!board.isValidPosition(position)) {
                    boardPanel.add(new JLabel());
                    continue;
                }

                JButton cell = new JButton(
                        board.hasPeg(position) ? "●" : "○");

                cell.setFont(new Font("SansSerif", Font.BOLD, 20));
                cell.setFocusPainted(false);

                if (position.equals(selectedPosition)) {
                    cell.setBackground(Color.YELLOW);
                }

                cell.addActionListener(
                        event -> handleCellClick(position));

                boardPanel.add(cell);
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private void handleCellClick(Position position) {
        Board board = game.getBoard();

        if (selectedPosition == null) {
            if (board.hasPeg(position)) {
                selectedPosition = position;
                statusLabel.setText(
                        "Peg selected. Choose an empty destination.");
            } else {
                statusLabel.setText(
                        "Select a position containing a peg.");
            }

            renderBoard();
            return;
        }

        if (selectedPosition.equals(position)) {
            selectedPosition = null;
            statusLabel.setText("Selection cancelled.");
            renderBoard();
            return;
        }

        boolean moved = game.makeMove(selectedPosition, position);
        selectedPosition = null;

        if (!moved) {
            statusLabel.setText(
                    "Invalid move. The board has not changed.");
            renderBoard();
            return;
        }

        renderBoard();

        if (game.isGameWon()) {
            statusLabel.setText(
                    "YOU WIN! Outstanding! One peg remains.");
        } else if (game.isGameLost()) {
            statusLabel.setText(
                    "GAME OVER — You lose! No legal moves remain. "
                    + game.getPegCount() + " pegs remain.");
        } else {
            statusLabel.setText(
                    "Move successful. Pegs remaining: "
                            + game.getPegCount());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SolitaireGUI gui = new SolitaireGUI();
            gui.setVisible(true);
        });
    }
}