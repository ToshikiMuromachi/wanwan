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
    ;
    BufferedImage bimage;
    BufferedImage monimage;

    private boolean utterance;
    double utteranceTime = 0;

    double x = 0;
    double y = 0;
    boolean yFlag;

    //コンストラクタ
    public WanwanGUI(){
        this.mainwindow = new JFrame("wanwan");
        this.mainwindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainwindow.setBounds(0,200,1024,768);
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
            this.monimage = ImageIO.read(new File("/home/toshiki/IdeaProjects/wanwan/data/dog.png"));
            this.bimage = ImageIO.read(new File("/home/toshiki/IdeaProjects/wanwan/data/monmon.png"));

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this.mainwindow,"No image.");
        }
    }

    /**
     * くまもん上下
     */
    public void monMove() {
        x= x + 5;
        //上下
        if (yFlag == false) {
            y = y + 0.2;
        } else {
            y = y - 0.2;
        }

        if (y > 10) {
            yFlag = true;
        }
        if (y < -10) {
            yFlag = false;
        }

        //フラグ処理
        if (utteranceTime >= 20 ) {
            setUtterance(false);
        }
    }



    /**
     * 定期的描画開始
     */
    public void start(){
        Timer t = new Timer();
        t.schedule(new RenderTask(), 0, 16);
    }

    /**
     * 描画
     */
    public void render(){
        Graphics2D g = (Graphics2D)this.strategy.getDrawGraphics();
        g.setBackground(Color.DARK_GRAY);
        g.clearRect(0, 0, this.mainwindow.getWidth(), this.mainwindow.getHeight());

        //くまモン描画
        //発話
        monMove();
        g.drawImage(this.bimage, 200, (int) (100 + y), null);
        if (getUtterance() == true ) {
            g.drawImage(this.monimage, 500, (int) (100 + y), null);
            utteranceTime++;
        } else {
            g.drawImage(this.bimage, 100, (int) (100 + y), null);
        }
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

    public boolean getUtterance() { return this.utterance; }
    public void setUtterance(boolean utterance) { this.utterance = utterance;}

}
