import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class WanwanGUI {
    JFrame mainwindow;
    BufferStrategy strategy;
    boolean spkey = false;
    double cy = 200;
    BufferedImage bimage;


    //コンストラクタ
    public WanwanGUI(){
        this.mainwindow = new JFrame("wanwan");
        this.mainwindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainwindow.setSize(800, 720);
        this.mainwindow.setLocationRelativeTo(null);
        this.mainwindow.setVisible(true);
        this.mainwindow.setResizable(false);
        //バッファストラテジー
        this.mainwindow.setIgnoreRepaint(true);
        this.mainwindow.createBufferStrategy(2);
        this.strategy = this.mainwindow.getBufferStrategy();
        //読み込み
        try {
            this.bimage = ImageIO.read(new File("/home/toshiki/IdeaProjects/wanwan/data/dog.png"));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this.mainwindow,"No image.");
        }
    }

    void start(){
        Timer t = new Timer();
        t.schedule(new RenderTask(), 0, 16);
    }

    void render(){
        Graphics2D g = (Graphics2D)this.strategy.getDrawGraphics();
        g.setBackground(Color.black);
        g.clearRect(0, 0, this.mainwindow.getWidth(), this.mainwindow.getHeight());

        if(spkey == true){
            cy -= 1;
        } else {
            cy += 1;
        }

        g.setColor(Color.YELLOW);
        g.draw(new Ellipse2D.Double(100, this.cy, 100, 100));
        g.drawImage(this.bimage, (int)126, (int)this.cy+24, null);
        g.setColor(Color.RED);
        g.fill(new Rectangle2D.Double(400,400,100,100));
        g.dispose();
        this.strategy.show();
    }

    class RenderTask extends TimerTask {
        int count = 0;

        @Override
        public void run() {
            WanwanGUI.this.render();
        }

    }

    class MyKeyAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_SPACE){
                WanwanGUI.this.spkey = true;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_SPACE){
                WanwanGUI.this.spkey = false;
            }
        }

    }

    public static void main(String[] args) {
        WanwanGUI wanGUI = new WanwanGUI();
        wanGUI.start();
    }

}
