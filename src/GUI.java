import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.util.TimerTask;

public class GUI extends JFrame {
    int x = 20;
    private GUIPanel guiPanel;

    public GUI() {
        setTitle("wanwan");
        setBounds(0,200,640,480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);

        setLayout(new FlowLayout());

        guiPanel = new GUIPanel();
        guiPanel.setPreferredSize(new Dimension(640,480));
        guiPanel.setBackground(Color.DARK_GRAY);


        Container contentPane = getContentPane();
        contentPane.add(guiPanel);

        //バッファストラテジー
        //setIgnoreRepaint(true);
        //createBufferStrategy(2);
        //this.strategy = getBufferStrategy();
    }

    public void updateGUI() {
        guiPanel.updateGUI();
    }


    public void render() {
        //Graphics2D g = (Graphics2D)this.strategy.getDrawGraphics();
        //g.setBackground(Color.DARK_GRAY);
        //g.setColor(Color.WHITE);
        //g.fill(new Rectangle2D.Double(10,10,50,50));
        //g.dispose();
        //this.strategy.show();
    }

    class RenderTask extends TimerTask {

        @Override
        public void run() {
            repaint();
        }
    }

}

