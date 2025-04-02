import java.awt.EventQueue;
import javax.swing.JFrame;

public class Snake extends JFrame {
    private static final long serialVersionUID = 1L;

    public Snake() {
        Board panel = new Board();
        this.add(panel);
        this.setTitle("snake");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setResizable(false);
        this.setVisible(true);
        this.setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            JFrame ex = new Snake();
        });
    }
}