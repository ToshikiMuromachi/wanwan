import javax.swing.*;
import java.awt.*;

public class GUIPanel extends JPanel {
    int x;

    /**
     * 描画
     * @param g
     */
    @Override
    public void paint(final Graphics g) {
        final Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setBackground(new Color(105,105,105));
        graphics.clearRect(0, 0, getWidth(), getHeight());

        graphics.setColor(new Color(255,255,153));
        int silentY = getHeight() - (int) (500 / 1500.0 * getHeight());

        x++;
        graphics.fillRoundRect(x, 20, 20,20,20 ,20);
        System.out.println(x);
    }

    public void updateGUI() {
        this.repaint();
    }
}
