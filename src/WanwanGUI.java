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
        this.mainwindow.setBounds(0,200,640,480);
        //this.mainwindow.setLocationRelativeTo(null);
        this.mainwindow.setVisible(true);
        this.mainwindow.setResizable(false);
        this.mainwindow.setBackground(Color.DARK_GRAY);
        this.mainwindow.setAlwaysOnTop(true);
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

    /**
     * 定期的描画開始
     */
    void start(){
        Timer t = new Timer();
        t.schedule(new RenderTask(), 0, 16);
    }

    /**
     * 描画
     */
    void render(){
        Graphics2D g = (Graphics2D)this.strategy.getDrawGraphics();
        g.setBackground(Color.black);
        g.clearRect(0, 0, this.mainwindow.getWidth(), this.mainwindow.getHeight());

        if(spkey == true){
            cy -= 1;
        } else {
            cy += 1;
        }

        g.drawImage(this.bimage, 126, 50, null);
        g.setColor(Color.RED);
        g.fill(new Rectangle2D.Double(400,400,100,100));
        g.dispose();
        this.strategy.show();
    }

    /**
     * タスク実行
     */
    class RenderTask extends TimerTask {
        int count = 0;

        @Override
        public void run() {
            WanwanGUI.this.render();
        }

    }

    /**
     * テスト用
     * @param args
     */
    public static void main(String[] args) {
        WanwanGUI wanGUI = new WanwanGUI();
        wanGUI.start();
    }

}
