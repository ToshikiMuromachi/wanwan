import be.tarsos.dsp.util.PitchConverter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class wanwanPanel extends JPanel {

    /**
     *
     */
    private static final long serialVersionUID = -5330666476785715988L;
    private double patternLength;//in seconds
    private double currentMarker;
    private long lastReset;
    private int score;
    private double patternLengthInQuarterNotes;

    private static final double CENTS_DEVIATION = 30.0;

    double[] pattern={400,400,600,400,900,200,400,400,600,400,1100,900}; // in cents
    double[] timing ={3  ,1  ,4  ,4  ,4  ,2  ,3  ,1  ,4  ,4  ,4  ,6   }; //in eight notes

    ArrayList<Double> startTimeStamps;
    ArrayList<Double> pitches;

    private boolean silentSection = false;
    ArrayList<Double> silentSectionTImes;

    public wanwanPanel(){
        for(double timeInQuarterNotes : timing){
            patternLengthInQuarterNotes+=timeInQuarterNotes;
        }
        patternLength = 12;
        currentMarker = 0;
        startTimeStamps = new ArrayList<Double>();
        pitches = new ArrayList<Double>();
        silentSectionTImes = new ArrayList<Double>();
    }

    @Override
    public void paint(final Graphics g) {
        final Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setBackground(Color.GRAY);
        graphics.clearRect(0, 0, getWidth(), getHeight());
        int x = (int) (currentMarker / (float) patternLength * getWidth());

        //現在バー描画
        //System.out.println("x:" + x +"current-last:" + (System.currentTimeMillis() - lastReset));
        if(x < 20 && System.currentTimeMillis() - lastReset > 1000){
            lastReset = System.currentTimeMillis();
            //score();
            pitches.clear();
            startTimeStamps.clear();
            silentSectionTImes.clear();
        }
        graphics.drawLine(x, 0, x, getHeight());


        //ノード描画

        //ピッチプロット
        //graphics.setColor(Color.RED);
        for(int i = 0 ; i < pitches.size() ; i++){
            double pitchInCents = pitches.get(i);
            double startTimeStamp = startTimeStamps.get(i) % patternLength;
            int patternX = (int) ( startTimeStamp / (double) patternLength * getWidth());
            //int patternY = 180 - (int) (pitchInCents / 1200.0 * 180);
            //int patternY = getHeight() - (int) (pitchInCents / 1200.0 * getHeight());
            int patternY = getHeight() - (int) (pitchInCents / 1500.0 * getHeight());
            graphics.setColor(Color.RED);
            if (i == (pitches.size() -1)) {
                graphics.setColor(Color.YELLOW);
            }
            graphics.fillRoundRect(patternX, patternY, 10,10,10 ,10);

            //無音区間追加
            if(i == (pitches.size()-1)) {
                if(getSilentSection() == true) {
                    double sts = startTimeStamp;
                    silentSectionTImes.add(sts);
                    setSilentSection(false);
                }
            }
        }
        //無音区間プロット
        graphics.setColor(Color.BLUE);
        for(int i = 0 ; i < silentSectionTImes.size() ; i++) {
            double startTimeStamp = silentSectionTImes.get(i) % patternLength;
            int silentX = (int) ( startTimeStamp / (double) patternLength * getWidth());
            graphics.fillRect(silentX, 50, 20,getHeight()-100);
        }

        //五線を書く
        graphics.setColor(Color.WHITE);
        for (int i = 0; i < 12; i++) {
            g.drawLine(0, 100 + i*70, 1980, 100 + i*70);
        }

    }

//    private void score(){
//        score = 0;
//        for(int i = 0 ; i < pitches.size() ; i++){
//            double pitchInCents = pitches.get(i);
//            double startTimeStamp = startTimeStamps.get(i) % patternLength;
//            if(startTimeStamp > 0.5 && startTimeStamp <= 0.5 + 0.5 * pattern.length){
//                double lengthPerQuarterNote = patternLength/patternLengthInQuarterNotes; // in seconds per quarter note
//                double currentXPosition = 0.5; // seconds of pause before start
//                for(int j = 0 ; j < pattern.length ; j++){
//                    double lengthInSeconds = timing[j] * lengthPerQuarterNote;//seconds
//                    if(startTimeStamp > currentXPosition && startTimeStamp <= currentXPosition + lengthInSeconds && Math.abs(pitchInCents-pattern[j]) < CENTS_DEVIATION){
//                        score++;
//                    }
//                    currentXPosition += lengthInSeconds; //in seconds
//                }
//            }
//        }
//    }

    /**
     * 周波数を図にプロットする。無音区間推定を行う。
     * @param timeStamp
     * @param frequency
     */
    public void setMarker(double timeStamp,double frequency) {
        currentMarker = timeStamp % patternLength;
        //ignore everything outside 80-2000Hz
        if (frequency > 80 && frequency < 2000) {
            double pitchInCents = PitchConverter.hertzToRelativeCent(frequency);
            pitches.add(pitchInCents);
            startTimeStamps.add(timeStamp);
        }


        this.repaint();
    }

    public boolean getSilentSection() { return this.silentSection; }
    public void setSilentSection(boolean silentSection) { this.silentSection = silentSection; }
}