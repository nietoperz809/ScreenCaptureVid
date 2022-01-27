import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
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
    private JPanel meinpanel;
    private JLabel label;
    private JTextField textfield;

    int halfbut2 (int i)
    {
        int ret = i/2;
        if (ret%2 == 1)
            ret++;
        return ret;
    }

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
                    /* ICodec.ID.CODEC_ID_MPEG4 */  ICodec.ID.CODEC_ID_H264,
                    halfbut2 (screenRect.width),
                    halfbut2 (screenRect.height));
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
                        BufferedImage bgrScreen = convertBitmap (image, BufferedImage.TYPE_3BYTE_BGR);
                        pics++;
                        writer.encodeVideo (0, bgrScreen,
                                100 * pics, TimeUnit.MILLISECONDS);
                        label.setText ("" + pics);
                    }
                    if (makevid) {
                        makevid = false;
                        writer.close ();
                        startButton.setEnabled (true);
                    }
                } catch (Exception e) {
                    e.printStackTrace ();
                }
            }
        }, 0, 100);
    }

    public static BufferedImage convertBitmap (BufferedImage sourceImage, int targetType) {
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
