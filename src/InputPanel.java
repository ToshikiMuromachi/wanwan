import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InputPanel extends JPanel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    Mixer mixer = null;

    public InputPanel(){
    }

    private ActionListener setInput = new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent arg0) {
            for(Mixer.Info info : Shared.getMixerInfo(false, true)){
                if(arg0.getActionCommand().equals(info.toString())){
                    Mixer newValue = AudioSystem.getMixer(info);
                    InputPanel.this.firePropertyChange("mixer", mixer, newValue);
                    InputPanel.this.mixer = newValue;
                    break;
                }
            }
        }
    };

}