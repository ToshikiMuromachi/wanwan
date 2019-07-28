import be.tarsos.dsp.*;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.dsp.resample.RateTransposer;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class Wanwan extends JFrame implements PitchDetectionHandler {

    private final WanwanPanel panel;
    private AudioDispatcher dispatcher;
    private Mixer currentMixer;
    private PitchEstimationAlgorithm algo;
    private double silentStartTime = 0; //無音区間開始時タイムスタンプ
    private double silentStartPitch = 0; //無音区間開始時ピッチ
    private int silentTime = 0; //無音区間フラグ
    private double silentRecentTimeStamp = 0;  //現時点に最も近い無音区間タイムスタンプ
    HashMap<Double, Double> pitchesAll = new HashMap<>(); //ピッチ全格納リスト 時間,ピッチ

    //音声再生関係
    private WaveformSimilarityBasedOverlapAdd wsola;
    private GainProcessor gain;
    private AudioPlayer audioPlayer;
    private RateTransposer rateTransposer;
    private double currentFactor;// pitch shift factor
    private double sampleRate;
    private boolean loop;

    //エージェント発話ピッチ
    //int[] pitchList = {104,125,146,167,187,229,250,271,292,312};
    int[] pitchList = {100,200,300,400,500,600,700,800,900,1000};
    File wanfile = new File("/home/toshiki/IdeaProjects/wanwan/data/dog.wav");
    File wanfiles = new File("/home/toshiki/IdeaProjects/wanwan/data/");

    //GUI
    private  GUI frameWanwan;

    private ActionListener algoChangeListener = new ActionListener(){
        @Override
        public void actionPerformed(final ActionEvent e) {
            String name = e.getActionCommand();
            PitchEstimationAlgorithm newAlgo = PitchEstimationAlgorithm.valueOf(name);
            algo = newAlgo;
            try {
                setNewMixer(currentMixer);
            } catch (LineUnavailableException e1) {
                e1.printStackTrace();
            } catch (UnsupportedAudioFileException e1) {
                e1.printStackTrace();
            }
        }};

    public Wanwan(){
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Pitch Detector");
        this.setBounds(600, 100, 1920, 1080);

        panel = new WanwanPanel();


        algo = PitchEstimationAlgorithm.YIN;

        JPanel inputPanel = new InputPanel();

        inputPanel.addPropertyChangeListener("mixer", new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent arg0) {
                        try {
                            setNewMixer((Mixer) arg0.getNewValue());
                        } catch (LineUnavailableException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (UnsupportedAudioFileException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });

        JPanel containerPanel = new JPanel(new GridLayout(1,0));
        containerPanel.add(inputPanel);
        this.add(containerPanel,BorderLayout.NORTH);

        JPanel otherContainer = new JPanel(new BorderLayout());
        otherContainer.add(panel,BorderLayout.CENTER);

        this.add(otherContainer,BorderLayout.CENTER);

        //マイク決め打ち
        for(Mixer.Info info : Shared.getMixerInfo(false, true)) {
            System.out.println("Mixer.Info:" + info + ":");
            if (info.toString().matches("default.*")) {
                System.out.println(info);
                System.out.println(AudioSystem.getMixer(info));
                Mixer newValue = AudioSystem.getMixer(info);
                Mixer mixer = newValue;
                try {
                    setNewMixer(mixer);
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                } catch (UnsupportedAudioFileException e) {
                    e.printStackTrace();
                }
                System.out.println("-----\nマイク入力 : " + mixer + "\n-----");
                break;
            }
        }

        //GUI
        frameWanwan = new GUI();

    }




    private void setNewMixer(Mixer mixer) throws LineUnavailableException, UnsupportedAudioFileException {

        if(dispatcher!= null){
            dispatcher.stop();
        }
        currentMixer = mixer;

        float sampleRate = 44100;
        int bufferSize = 1536;
        int overlap = 0;

        final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine line;
        line = (TargetDataLine) mixer.getLine(dataLineInfo);
        final int numberOfSamples = bufferSize;
        line.open(format, numberOfSamples);
        line.start();
        final AudioInputStream stream = new AudioInputStream(line);

        JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
        // create a new dispatcher
        dispatcher = new AudioDispatcher(audioStream, bufferSize,
                overlap);

        // add a processor, handle percussion event.
        dispatcher.addAudioProcessor(new PitchProcessor(algo, sampleRate, bufferSize, this));

        // run the dispatcher (on a new thread).
        new Thread(dispatcher,"Audio dispatching").start();
    }

    /**
     *
     */
    private static final long serialVersionUID = 4787721035066991486L;

    public static void main(String... strings) throws InterruptedException, InvocationTargetException, UnsupportedAudioFileException, IOException, LineUnavailableException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    JFrame frame = new Wanwan();
                    //frame.pack();
                    frame.setSize(1980,1020);
                    frame.setVisible(true);

                    JFrame frameWanwan = new GUI();
                    frameWanwan.setSize(640,480);
                    frameWanwan.setVisible(true);

                    WanwanGUI wanGUI = new WanwanGUI();
                    wanGUI.start();

                    //Wanwan sound = new Wanwan();
                    //Clip clip = null;
//                    createClip("/home/toshiki/IdeaProjects/TarsosDSPTest/monmon_2.wav","/home/toshiki/data/a/a.wav",0);
                    //ここで再生メソッドの呼び出しthis.
                    //clip.start();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 周波数を取得
     * @param pitchDetectionResult
     * @param audioEvent
     */
    @Override
    public void handlePitch(PitchDetectionResult pitchDetectionResult,AudioEvent audioEvent) {
        double timeStamp = audioEvent.getTimeStamp();
        float pitch = pitchDetectionResult.getPitch();
        String message = String.format("Pitch detected at %.2fs: %.2fHz\n", timeStamp,pitch);
        pitchesAll.put(timeStamp, (double) pitch);
        //System.out.print(message);        //ピッチ取得結果
        //System.out.println(timeStamp+" : "+ pitchesAll.get(timeStamp));     //全ピッチをマップに格納(使ってない)
        panel.setMarker(timeStamp, pitch);

        //GUI更新
        frameWanwan.updateGUI();



        //ピッチ
        //現在無音であるか判別 開始5秒以上経っている&ピッチの結果が取得できていない&無音区間開始フラグが立っている
        if(timeStamp > 5.0 && pitchDetectionResult.getPitch() == -1.0) {
            setSilentStartTime(audioEvent.getTimeStamp()); //無音区間開始時間を記録する
            setSilentTime(getSilentTime()+1); //無音区間経過時間を記録
        }else{
            setSilentTime(0);
            panel.setSilentSection(0);
        }

        //ユーザー発話がない場合の処理
        // (ユーザー発話が10ターン以上無いかを調べる
        // 　&ユーザー発話フラグが立っていないか
        // 　&無音区間のタイムスタンプを見て前回のタイムスタンプに近ければリジェクト)
        //同じ発話区間ということをわからせない限り、前回のタイムスタンプからの結果でしか判断ができない。
        //setSilentSectionをbooleanからintの3値のフラグに変更。0:無音区間でない　1:無音区間(プロット必要) 2:無音区間(継続)
        if (getSilentTime() > 15 && panel.getSilentSection() == 0 && (timeStamp - getSilentRecentTimeStamp()) > 3) {
            //setSilentTime(0);
            setSilentRecentTimeStamp(timeStamp);
            //無音区間開始時のピッチを記録
            setSilentStartPitch(panel.getSilentRecentTimePitch());
            //発話生成 音声ファイル再生
            startFile(wanfiles, pitchList);
            panel.setSilentSection(1); //無音区間フラグを立てる
            //無音区間表示
            //System.out.println("silentsection"+" : " +getSilentRecentTimeStamp()+" : "+panel.getSilentRecentTimePitch()+"Hz");
        }
        //System.out.println(panel.getSilentSection());
    }

    /**
     *最も近いピッチを探す
     *
     */
    public int getNearestValue(int[] pitchList,double silentStartPitch) {
        int playNumber = 0;

        double min = Math.abs(pitchList[0] - silentStartPitch);
        for(int i=1; i < pitchList.length; i++) {
            if (Math.abs(pitchList[i] - silentStartPitch) < min ) {
                playNumber = i;
                min = Math.abs(pitchList[i] - silentStartPitch);
            }
        }

        return playNumber;
    }

    /**
     * サウンドの入力ストリーム取得
     */
    public Clip createClip(File file) {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(file); //オーディオストリームを開く
            AudioFormat af = stream.getFormat(); //ファイルの形式取得
            DataLine.Info dataLine = new DataLine.Info(Clip.class,af); //単一のオーディオ形式を含む指定した情報からデータラインの情報オブジェクトを構築
            Clip c = (Clip)AudioSystem.getLine(dataLine); //指定された Line.Info オブジェクトの記述に一致するラインを取得
            c.open(stream); //再生準備完了
            return c;
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     *最も近いピッチで音を再生
     *
     */
    public void startFile(File file, int[] pitchList) {
        //最も近いピッチ探させる
        int playNumber = getNearestValue(pitchList, getSilentStartPitch());
        System.out.println("PITCH:" + String.format("%.2f", getSilentStartPitch()) + "Hz PLAY:" + playNumber +" min:" +pitchList[playNumber] );

        //最も近いピッチを持つ音声ファイルを流す。
        File fileInput;
        switch (playNumber) {
            case 0:
                fileInput = new File("/home/toshiki/IdeaProjects/wanwan/data/dog104.wav");
                break;
            case 1:
                fileInput = new File("/home/toshiki/IdeaProjects/wanwan/data/dog125.wav");
                break;
            case 2:
                fileInput = new File("/home/toshiki/IdeaProjects/wanwan/data/dog146.wav");
                break;
            case 3:
                fileInput = new File("/home/toshiki/IdeaProjects/wanwan/data/dog167.wav");
                break;
            case 4:
                fileInput = new File("/home/toshiki/IdeaProjects/wanwan/data/dog187.wav");
                break;
            case 5:
                fileInput = new File("/home/toshiki/IdeaProjects/wanwan/data/dog229.wav");
                break;
            case 6:
                fileInput = new File("/home/toshiki/IdeaProjects/wanwan/data/dog250.wav");
                break;
            case 7:
                fileInput = new File("/home/toshiki/IdeaProjects/wanwan/data/dog271.wav");
                break;
            case 8:
                fileInput = new File("/home/toshiki/IdeaProjects/wanwan/data/dog292.wav");
                break;
            case 9:
                fileInput = new File("/home/toshiki/IdeaProjects/wanwan/data/dog312.wav");
                break;
            default:
                fileInput = new File("/home/toshiki/IdeaProjects/wanwan/data/dog.wav");
                break;
        }

        //音再生
        Clip clip = createClip(fileInput);
        clip.start();

    }

    public static void createClip(String source,String target,double cents) throws UnsupportedAudioFileException, IOException {

    }

    //ピッチの高さ変更メソッド
    public static double centToFactor(double cents){
        return 1 / Math.pow(Math.E,cents*Math.log(2)/1200/Math.log(Math.E));
    }

    public  double getSilentStartTime() { return this.silentStartTime; }
    public void setSilentStartTime(double silentStartTime) {
        this.silentStartTime = silentStartTime;
    }

    public  double getSilentStartPitch() { return this.silentStartPitch; }
    public void setSilentStartPitch(double silentStartPitch) {
        this.silentStartPitch = silentStartPitch;
    }

    public  int getSilentTime() { return this.silentTime; }
    public void setSilentTime(int silentTime) {
        this.silentTime = silentTime;
    }

    public  double getSilentRecentTimeStamp() { return this.silentRecentTimeStamp; }
    public void setSilentRecentTimeStamp(double SilentRecentTimeStamp) { this.silentRecentTimeStamp = SilentRecentTimeStamp; }
}
