import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

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

public class wanwan extends JFrame implements PitchDetectionHandler {

    private final wanwanPanel panel;
    private AudioDispatcher dispatcher;
    private Mixer currentMixer;
    private PitchEstimationAlgorithm algo;
    private double silentStartTime = 0;
    private int silentTime = 0;


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

        inputPanel.addPropertyChangeListener("mixer",
                new PropertyChangeListener() {
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
            if (info.toString().equals("default [default], version 4.18.0-21-generic")) {
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

        //textArea.append("Started listening with " + experiment.Shared.toLocalString(mixer.getMixerInfo().getName()) + "\n\tparams: " + threshold + "dB\n");

        final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true,
                false);
        final DataLine.Info dataLineInfo = new DataLine.Info(
                TargetDataLine.class, format);
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

    public static void main(String... strings) throws InterruptedException,
            InvocationTargetException, UnsupportedAudioFileException, IOException, LineUnavailableException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    JFrame frame = new wanwan();
                    //frame.pack();
                    frame.setSize(1980,1020);
                    frame.setVisible(true);

                    Clip clip = null;
                    clip = createClip(new File("/home/toshiki/IdeaProjects/TarsosDSPTest/monmon_2.wav"));
                    //ここで再生メソッドの呼び出し
                    //clip.start();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (UnsupportedAudioFileException e) {
                    e.printStackTrace();
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
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
        //System.out.print(message);
        panel.setMarker(timeStamp, pitch);


        //ピッチ
        //現在無音であるか判別 開始5秒以上経っている&ピッチの結果が取得できていない&無音区間開始フラグが立っている
        if(timeStamp > 5.0 && pitchDetectionResult.getPitch() == -1.0) {
            setSilentStartTime(audioEvent.getTimeStamp()); //無音区間開始時間を記録する
            setSilentTime(getSilentTime()+1); //無音区間経過時間を記録
        }else{
            setSilentTime(0);
        }

        //ユーザー発話がない場合の処理
        // (ユーザー発話が10ターン以上無いかを調べる&ユーザー発話フラグが立っていないか)
        if (getSilentTime() > 20 && panel.getSilentSection() == false) {
            panel.setSilentSection(true); //無音区間フラグを立てる
            setSilentTime(0);
            //System.out.println("silentsection"+timeStamp + " : " +getSilentTime());
        }
    }

    public static Clip createClip(File path) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        //指定されたURLのオーディオ入力ストリームを取得
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(path)) {

            //ファイルの形式取得
            AudioFormat af = ais.getFormat();

            //単一のオーディオ形式を含む指定した情報からデータラインの情報オブジェクトを構築
            DataLine.Info dataLine = new DataLine.Info(Clip.class, af);

            //指定された Line.Info オブジェクトの記述に一致するラインを取得
            Clip c = (Clip) AudioSystem.getLine(dataLine);

            //再生準備完了
            c.open(ais);
            return c;
        }
    }


    public  double getSilentStartTime() { return this.silentStartTime; }
    public void setSilentStartTime(double silentStartTime) {
        this.silentStartTime = silentStartTime;
    }

    public  int getSilentTime() { return this.silentTime; }
    public void setSilentTime(int silentTime) {
        this.silentTime = silentTime;
    }


}
