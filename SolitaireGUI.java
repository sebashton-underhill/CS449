import javax.swing.*;
import java.awt.*;
public class SolitaireGUI extends JFrame {
    public SolitaireGUI() {
        // Window settings
        setTitle("Solitaire Game");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // Main panel
        JPanel panel = new JPanel();
        panel.setLayout(null);
        // Text
        JLabel title = new JLabel("Solitaire");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setBounds(235, 20, 150, 40);
        panel.add(title);
        JLabel instructions = new JLabel("Choose your game settings:");
        instructions.setBounds(30, 75, 250, 30);
        panel.add(instructions);
        // Horizontal line
        JSeparator line = new JSeparator();
        line.setBounds(30, 110, 530, 10);
        panel.add(line);
        // Radio buttons
        JLabel difficultyLabel = new JLabel("Difficulty:");
        difficultyLabel.setBounds(30, 135, 100, 25);
        panel.add(difficultyLabel);
        JRadioButton easyButton = new JRadioButton("Easy");
        easyButton.setBounds(30, 165, 80, 25);
        JRadioButton hardButton = new JRadioButton("Hard");
        hardButton.setBounds(120, 165, 80, 25);
        ButtonGroup difficultyGroup = new ButtonGroup();
        difficultyGroup.add(easyButton);
        difficultyGroup.add(hardButton);
        easyButton.setSelected(true);
        panel.add(easyButton);
        panel.add(hardButton);
        // Check box
        JCheckBox hintsCheckBox = new JCheckBox("Enable hints");
        hintsCheckBox.setBounds(30, 205, 150, 30);
        panel.add(hintsCheckBox);
        // Another horizontal line
        JSeparator line2 = new JSeparator();
        line2.setBounds(30, 250, 530, 10);
        panel.add(line2);
        // Status text
        JLabel status = new JLabel("Ready to start a game!");
        status.setBounds(30, 275, 300, 30);
        panel.add(status);
        // Start button
        JButton startButton = new JButton("Start Game");
        startButton.setBounds(30, 320, 130, 35);
        startButton.addActionListener(e -> {
            String difficulty;
            if (easyButton.isSelected()) {
                difficulty = "Easy";
            } else {
                difficulty = "Hard";
            }
            if (hintsCheckBox.isSelected()) {
                status.setText("Starting " + difficulty + " game with hints!");
            } else {
                status.setText("Starting " + difficulty + " game!");
            }
        });
        panel.add(startButton);
        add(panel);
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SolitaireGUI gui = new SolitaireGUI();
            gui.setVisible(true);
        });
    }
}