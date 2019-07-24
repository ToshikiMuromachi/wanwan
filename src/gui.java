import javax.swing.*;
import java.awt.*;

public class gui extends JFrame {
    public gui() {
        setTitle("wanwan");
        setBounds(200,200,640,480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new FlowLayout());

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(100,50));
        panel.setBackground(Color.DARK_GRAY);


        Container contentPane = getContentPane();
        contentPane.add(panel);
    }
}
