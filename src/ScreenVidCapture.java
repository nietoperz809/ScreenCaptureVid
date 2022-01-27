import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ScreenVidCapture {
    public JButton startButton;
    public JButton stopButton;
    Rectangle screenRect = new Rectangle (Toolkit.getDefaultToolkit ().getScreenSize ());
    Robot robot = new Robot ();
    int pics;
    boolean running = false;
    boolean makevid = false;
    IMediaWriter writer;
    ArrayList<BufferedImage> list = new ArrayList<> ();
    private JPanel meinpanel;
    private JLabel label;
    private JLabel label2;
    private JTextField textfield;

    public ScreenVidCapture () throws Exception {
        stopButton.setEnabled (false);

        startButton.addActionListener (e -> {
            System.out.println ("Start");
            stopButton.setEnabled (true);
            startButton.setEnabled (false);
            String file = textfield.getText () + File.separator +
                    System.currentTimeMillis () + ".mp4";
            System.out.println (file);
            writer = ToolFactory.makeWriter (file);
            writer.addVideoStream(0, 0,
                    ICodec.ID.CODEC_ID_MPEG4,
                    screenRect.width / 2,
                    screenRect.height / 2);
            running = true;
        });

        stopButton.addActionListener (e -> {
            System.out.println ("Stop");
            stopButton.setEnabled (false);
            running = false;
            makevid = true;
        });

        new Timer ().scheduleAtFixedRate (new TimerTask () {
            @Override
            public void run () {
                try {
                    if (running) {
                        BufferedImage image = robot.createScreenCapture (screenRect);
                        list.add (image);
                        pics++;
                        label.setText ("" + pics);
                    }
                    if (makevid) {
                        for (int index = 1; index <= list.size (); index++) {
                            BufferedImage screen = list.get (index-1);
                            BufferedImage bgrScreen = convertToType (screen, BufferedImage.TYPE_3BYTE_BGR);
                            writer.encodeVideo (0, bgrScreen, 300 * index, TimeUnit.MILLISECONDS);
                            if (--pics == 0) {
                                startButton.setEnabled (true);
                            }
                            label2.setText (""+pics);
                        }
                        list.clear ();
                        pics = 0;
                        makevid = false;
                        System.out.println ("done");
                        writer.close ();
                    }
                } catch (Exception e) {
                    e.printStackTrace ();
                }
            }
        }, 0, 50);
    }

    public static BufferedImage convertToType (BufferedImage sourceImage, int targetType) {
        BufferedImage image;
        if (sourceImage.getType () == targetType) {
            image = sourceImage;
        } else {
            image = new BufferedImage (sourceImage.getWidth (),
                    sourceImage.getHeight (), targetType);
            image.getGraphics ().drawImage (sourceImage, 0, 0, null);
        }
        return image;
    }

    public static void main (String[] args) throws Exception {
        JFrame frame = new JFrame ("Tester");
        frame.setContentPane (new ScreenVidCapture ().meinpanel);
        frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
        frame.pack ();
        frame.setVisible (true);
    }

    private void createUIComponents () {
        // TODO: place custom component creation code here
    }
}
