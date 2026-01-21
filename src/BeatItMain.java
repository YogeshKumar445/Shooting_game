import javax.swing.*;

public class BeatItMain extends JFrame {
    public BeatItMain() {
        setTitle("Beat It - 2D-Shooting game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);

        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        panel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BeatItMain());
    }
}






