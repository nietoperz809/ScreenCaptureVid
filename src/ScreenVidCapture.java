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
import java.util.concurrent.atomic.AtomicInteger;

public class ScreenVidCapture {
    static final int STATE0 = 0;
    static final int STATE1 = 1;
    static final int STATE2 = 2;
    public JButton startButton;
    public JButton stopButton;
    final Rectangle screenRect = new Rectangle (Toolkit.getDefaultToolkit ().getScreenSize ());
    final Robot robot = new Robot ();
    int imageCount;
    final AtomicInteger state = new AtomicInteger (STATE0);
    IMediaWriter writer;
    private JPanel meinpanel;
    private JLabel label;
    private JTextField textfield;
    String filename;

    public ScreenVidCapture () throws Exception {
        stopButton.setEnabled (false);
        ToolFactory.setTurboCharged (true);

        startButton.addActionListener (e -> {
            System.out.println ("Start");
            stopButton.setEnabled (true);
            startButton.setEnabled (false);
            filename = textfield.getText () + File.separator +
                    System.currentTimeMillis () + ".mp4";
            System.out.println (filename);
            writer = ToolFactory.makeWriter (filename);
            writer.addVideoStream (0, 0,
                    /* ICodec.ID.CODEC_ID_MPEG4 */  ICodec.ID.CODEC_ID_H264,
                    halfbut2 (screenRect.width),
                    halfbut2 (screenRect.height));
            imageCount = 0;
            state.set (STATE1);
        });

        stopButton.addActionListener (e -> {
            System.out.println ("Stop");
            stopButton.setEnabled (false);
            state.set (STATE2);
        });

        new Timer ().scheduleAtFixedRate (new TimerTask () {
            @Override
            public void run () {
                try {
                    switch (state.get ()) {
                        case STATE1:
//                            long time = System.currentTimeMillis ();
                            BufferedImage image = robot.createScreenCapture (screenRect);
                            BufferedImage bgrScreen = convertBitmap (image, BufferedImage.TYPE_3BYTE_BGR);
                            imageCount++;
                            writer.encodeVideo (0, bgrScreen, 300L * imageCount, TimeUnit.MILLISECONDS);
//                            time = System.currentTimeMillis () - time;
//                            System.out.println (time);
                            label.setText ("" + imageCount);
                            break;

                        case STATE2:
                            state.set (STATE0);
                            writer.close ();
                            startButton.setEnabled (true);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace ();
                }
            }
        }, 0, 200);
    }

    public static BufferedImage convertBitmap (BufferedImage sourceImage, int targetType) {
        BufferedImage image;
        if (sourceImage.getType () == targetType) {
            image = sourceImage;
        } else {
            image = new BufferedImage (sourceImage.getWidth (),
                    sourceImage.getHeight (), targetType);
            Graphics2D g = (Graphics2D) image.getGraphics();
//            g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
//            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
//            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g.drawImage (sourceImage, 0, 0, null);
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

    int halfbut2 (int i) {
        int ret = i / 2;
        if (ret % 2 == 1)
            ret++;
        return ret;
    }

    private void createUIComponents () {
        // TODO: place custom component creation code here
    }
}
