import be.tarsos.dsp.*;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.WaveformWriter;
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
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

public class wanwan extends JFrame implements PitchDetectionHandler {

    private final wanwanPanel panel;
    private AudioDispatcher dispatcher;
    private Mixer currentMixer;
    private PitchEstimationAlgorithm algo;
    private double silentStartTime = 0; //無音区間開始時タイムスタンプ
    private int silentTime = 0; //無音区間フラグ
    private double silentRecentTimeStamp = 0;  //現時点に最も近い無音区間タイムスタンプ
    HashMap<Double, Double> pitchesAll = new HashMap<>(); //ピッチ全格納リスト 時間,ピッチ

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

    public wanwan(){
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Pitch Detector");
        this.setBounds(400, 100, 1920, 1080);

        panel = new wanwanPanel();


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
                    JFrame frame = new wanwan();
                    //frame.pack();
                    frame.setSize(1980,1020);
                    frame.setVisible(true);

                    //startFile("/home/toshiki/IdeaProjects/TarsosDSPTest/monmon_2.wav",null);
                    Clip clip = null;
//                    createClip("/home/toshiki/IdeaProjects/TarsosDSPTest/monmon_2.wav","/home/toshiki/data/a/a.wav",0);
                    //ここで再生メソッドの呼び出し
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
            panel.setSilentSection(1); //無音区間フラグを立てる
            //setSilentTime(0);
            setSilentRecentTimeStamp(timeStamp);
            //無音区間表示
            System.out.println("silentsection"+" : " +getSilentRecentTimeStamp()+" : "+panel.getSilentRecentTimePitch()+"Hz");
        }
        //System.out.println(panel.getSilentSection());
    }

    public void startFile(String file,Mixer mixer){
//        File inputFile = new File(file);
//        if(dispatcher != null){
//            dispatcher.stop();
//        }
//        AudioFormat format;
//        try {
//            if(inputFile != null){
//                format = AudioSystem.getAudioFileFormat(inputFile).getFormat();
//            }else{
//                format = new AudioFormat(44100, 16, 1, true,true);
//            }
//            rateTransposer = new RateTransposer(currentFactor);
//            gain = new GainProcessor(1.0);
//            audioPlayer = new AudioPlayer(format);
//            sampleRate = format.getSampleRate();
//
//            //can not time travel, unfortunately. It would be nice to go back and kill Hitler or something...
//            if(originalTempoCheckBox.getModel().isSelected() && inputFile != null){
//                wsola = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(currentFactor, sampleRate));
//            } else {
//                wsola = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(1, sampleRate));
//            }
//            if(inputFile == null){
//                DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
//                TargetDataLine line;
//                line = (TargetDataLine) mixer.getLine(dataLineInfo);
//                line.open(format, wsola.getInputBufferSize());
//                line.start();
//                final AudioInputStream stream = new AudioInputStream(line);
//                JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
//                // create a new dispatcher
//                dispatcher = new AudioDispatcher(audioStream, wsola.getInputBufferSize(),wsola.getOverlap());
//            }else{
//                if(format.getChannels() != 1){
//                    dispatcher = AudioDispatcherFactory.fromFile(inputFile,wsola.getInputBufferSize() * format.getChannels(),wsola.getOverlap() * format.getChannels());
//                    dispatcher.addAudioProcessor(new MultichannelToMono(format.getChannels(),true));
//                }else{
//                    dispatcher = AudioDispatcherFactory.fromFile(inputFile,wsola.getInputBufferSize(),wsola.getOverlap());
//                }
//                //dispatcher = AudioDispatcher.fromFile(inputFile,wsola.getInputBufferSize(),wsola.getOverlap());
//            }
//            wsola.setDispatcher(dispatcher);
//            dispatcher.addAudioProcessor(wsola);
//            dispatcher.addAudioProcessor(rateTransposer);
//            dispatcher.addAudioProcessor(gain);
//            dispatcher.addAudioProcessor(audioPlayer);
//            dispatcher.addAudioProcessor(new AudioProcessor() {
//
//                @Override
//                public void processingFinished() {
//                    if(loop){
//                        dispatcher =null;
//                        startFile(inputFile,null);
//                    }
//
//                }
//
//                @Override
//                public boolean process(AudioEvent audioEvent) {
//                    return true;
//                }
//            });
//
//            Thread t = new Thread(dispatcher);
//            t.start();
//        } catch (UnsupportedAudioFileException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (LineUnavailableException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    public static void createClip(String source,String target,double cents) throws UnsupportedAudioFileException, IOException {
        File inputFile = new File(source);
        AudioFormat format = AudioSystem.getAudioFileFormat(inputFile).getFormat();
        double sampleRate = format.getSampleRate();
        double factor = wanwan.centToFactor(cents);
        RateTransposer rateTransposer = new RateTransposer(factor);
        WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(factor, sampleRate));
        WaveformWriter writer = new WaveformWriter(format,target);
        AudioDispatcher dispatcher;
        if(format.getChannels() != 1){
            dispatcher = AudioDispatcherFactory.fromFile(inputFile,wsola.getInputBufferSize() * format.getChannels(),wsola.getOverlap() * format.getChannels());
            dispatcher.addAudioProcessor(new MultichannelToMono(format.getChannels(),true));
        }else{
            dispatcher = AudioDispatcherFactory.fromFile(inputFile,wsola.getInputBufferSize(),wsola.getOverlap());
        }
        wsola.setDispatcher(dispatcher);
        dispatcher.addAudioProcessor(wsola);
        dispatcher.addAudioProcessor(rateTransposer);
        dispatcher.addAudioProcessor(writer);
        dispatcher.run();
        System.out.println("aaaaaaaaaaaaaaaa");


//        //指定されたURLのオーディオ入力ストリームを取得
//        try (AudioInputStream ais = AudioSystem.getAudioInputStream(path)) {
//
//            //ファイルの形式取得
//            AudioFormat af = ais.getFormat();
//
//            //単一のオーディオ形式を含む指定した情報からデータラインの情報オブジェクトを構築
//            DataLine.Info dataLine = new DataLine.Info(Clip.class, af);
//
//            //指定された Line.Info オブジェクトの記述に一致するラインを取得
//            Clip c = (Clip) AudioSystem.getLine(dataLine);
//
//            //再生準備完了
//            c.open(ais);
//
////
////            int centValue = Integer.valueOf(((JSpinner) arg0.getSource())
////                    .getValue().toString());
////            currentFactor = centToFactor(centValue);
//            return c;
//        }
    }
    //ピッチの高さ変更メソッド
    public static double centToFactor(double cents){
        return 1 / Math.pow(Math.E,cents*Math.log(2)/1200/Math.log(Math.E));
    }


    public  double getSilentStartTime() { return this.silentStartTime; }
    public void setSilentStartTime(double silentStartTime) {
        this.silentStartTime = silentStartTime;
    }

    public  int getSilentTime() { return this.silentTime; }
    public void setSilentTime(int silentTime) {
        this.silentTime = silentTime;
    }

    public  double getSilentRecentTimeStamp() { return this.silentRecentTimeStamp; }
    public void setSilentRecentTimeStamp(double SilentRecentTimeStamp) { this.silentRecentTimeStamp = SilentRecentTimeStamp; }
}
