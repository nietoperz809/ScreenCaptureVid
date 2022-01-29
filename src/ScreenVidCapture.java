import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import monte.Format;
import monte.FormatKeys;
import monte.Rational;
import monte.ScreenRecorder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static monte.AudioFormatKeys.EncodingKey;
import static monte.AudioFormatKeys.FrameRateKey;
import static monte.AudioFormatKeys.KeyFrameIntervalKey;
import static monte.AudioFormatKeys.MediaTypeKey;
import static monte.AudioFormatKeys.MimeTypeKey;
import static monte.VideoFormatKeys.*;

public class ScreenVidCapture {
    static final int STATE_IDLE = 0;
    static final int STATE_START_RECORDING = 3;
    static final int STATE_DO_RECORDING = 1;
    static final int STATE_FINISH_RECORDING = 2;
    final Rectangle screenRect = new Rectangle (Toolkit.getDefaultToolkit ().getScreenSize ());
    final Robot robot = new Robot ();
    final AtomicInteger state = new AtomicInteger (STATE_IDLE);
    public JButton startButton;
    public JButton stopButton;
    int imageCount;
    IMediaWriter writer;
    String filename;
    Thread runner;
    private JPanel meinpanel;
    private JLabel label;

    Runnable MOV = new Runnable () {
        GraphicsConfiguration cfg;
        ScreenRecorder screenRecorder;

        @Override
        public void run () {
            lab:
            while(true) {
                switch (state.get ()) {
                    case STATE_START_RECORDING:
                        cfg = new JFrame().getGraphicsConfiguration();
                        try {
                            screenRecorder = new ScreenRecorder(cfg, screenRect,
                                    // the file format:
                                    new Format (MediaTypeKey, MediaType.FILE, MimeTypeKey, "video/quicktime"),
                                    //
                                    // the output format for screen capture:
                                    new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, "tscc",
                                            CompressorNameKey, "Techsmith Screen Capture",
                                            WidthKey, screenRect.width,
                                            HeightKey, screenRect.height,
                                            DepthKey, 16, FrameRateKey, Rational.valueOf(10),
                                            QualityKey, 1.0f,
                                            KeyFrameIntervalKey, (int) (10 * 60) // one keyframe per minute is enough
                                    ),
                                    //
                                    // the output format for mouse capture:
                                    null,
                                    //
                                    // the output format for audio capture:
                                    null,
                                    //
                                    // the storage location of the movie
                                    new File (textfield.getText ()));
                        } catch (IOException e) {
                            e.printStackTrace ();
                        } catch (AWTException e) {
                            e.printStackTrace ();
                        }
                        screenRecorder.setAudioMixer (null);
                        try {
                            screenRecorder.start ();
                        } catch (IOException e) {
                            e.printStackTrace ();
                        }
                        state.set (STATE_DO_RECORDING);
                        break;

                    case STATE_DO_RECORDING:
                        break;

                    case STATE_FINISH_RECORDING:
                        try {
                            screenRecorder.stop();
                        } catch (IOException e) {
                            e.printStackTrace ();
                        }
                        state.set (STATE_IDLE);
                        break lab;
                }
            }
        }
    };

    Runnable H246 = new Runnable () {
        @Override
        public void run () {
            writer = ToolFactory.makeWriter (filename);
            writer.addVideoStream (0, 0,
                    ICodec.ID.CODEC_ID_H264,
                    halfbut2 (screenRect.width),
                    halfbut2 (screenRect.height));
            lab:
            while (true) {
                switch (state.get ()) {
                    case STATE_START_RECORDING:
                        state.set (STATE_DO_RECORDING);
                        break;

                    case STATE_DO_RECORDING:
                        BufferedImage image = robot.createScreenCapture (screenRect);
                        BufferedImage bgrScreen = convertBitmap (image, BufferedImage.TYPE_3BYTE_BGR);
                        imageCount++;
                        writer.encodeVideo (0, bgrScreen, 300L * imageCount, TimeUnit.MILLISECONDS);
                        label.setText ("" + imageCount);
                        try {
                            Thread.sleep (100);
                        } catch (InterruptedException e) {
                            return;
                        }
                        break;

                    case STATE_FINISH_RECORDING:
                        state.set (STATE_IDLE);
                        writer.close ();
                        startButton.setEnabled (true);
                        break lab; // end thread
                }
            }
        }
    };
    private JTextField textfield;
    private JRadioButton MOVRadioButton;

    public ScreenVidCapture () throws Exception {
        stopButton.setEnabled (false);
        ToolFactory.setTurboCharged (true);

        startButton.addActionListener (e -> {
            System.out.println ("Start");
            stopButton.setEnabled (true);
            startButton.setEnabled (false);
            filename = textfield.getText () + File.separator +
                    System.currentTimeMillis () + ".mp4";
            imageCount = 0;
            state.set (STATE_START_RECORDING);
            if (MOVRadioButton.isSelected ()) {
                runner = new Thread (MOV);
                runner.start ();
            } else {
                runner = new Thread (H246);
                runner.start ();
            }
        });

        stopButton.addActionListener (e -> {
            System.out.println ("Stop");
            stopButton.setEnabled (false);
            state.set (STATE_FINISH_RECORDING);
        });
    }

    public static BufferedImage convertBitmap (BufferedImage sourceImage, int targetType) {
        BufferedImage image;
        if (sourceImage.getType () == targetType) {
            image = sourceImage;
        } else {
            image = new BufferedImage (sourceImage.getWidth (),
                    sourceImage.getHeight (), targetType);
            Graphics2D g = (Graphics2D) image.getGraphics ();
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
            return ret - 1;
        return ret;
    }

    private void createUIComponents () {
        // TODO: place custom component creation code here
    }
}
