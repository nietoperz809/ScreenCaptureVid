package mycode;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import monte.Format;
import monte.Rational;
import monte.ScreenRecorder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static monte.VideoFormatKeys.*;

public class ScreenVidCapture {
    enum STATE {IDLE, START_RECORDING, DO_RECORDING, FINISH_RECORDING};
    final Rectangle screenRect = new Rectangle (Toolkit.getDefaultToolkit ().getScreenSize ());
    final Robot robot = new Robot ();
    final AtomicReference<STATE> state = new AtomicReference<> (STATE.IDLE);
    public JButton startButton;
    public JButton stopButton;
    int imageCount;
    String filename;
    Thread runner;
    private JPanel meinpanel;
    private JLabel label;

    final Runnable MOV = new Runnable () {
        GraphicsConfiguration cfg = GraphicsEnvironment//
                .getLocalGraphicsEnvironment()//
                .getDefaultScreenDevice()//
                .getDefaultConfiguration();
        ScreenRecorder screenRecorder;

        @Override
        public void run () {
            lab:
            while(true) {
                switch (state.get ()) {
                    case START_RECORDING:
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
                                    ),null,null, new File (outputPath.getText ()));
                            screenRecorder.setAudioMixer (null);
                            screenRecorder.start ();
                        } catch (Exception e) {
                            e.printStackTrace ();
                        }
                        state.set (STATE.DO_RECORDING);
                        break;

                    case DO_RECORDING:
                        imageCount++;
                        label.setText ("" + imageCount);
                        try {
                            Thread.sleep (200);
                        } catch (InterruptedException e) {
                            return;
                        }
                        break;

                    case FINISH_RECORDING:
                        try {
                            screenRecorder.stop();
                        } catch (IOException e) {
                            e.printStackTrace ();
                        }
                        startButton.setEnabled (true);
                        state.set (STATE.IDLE);
                        break lab;
                }
            }
        }
    };

    final Runnable H246 = new Runnable () {
        IMediaWriter writer;
        @Override
        public void run () {
            lab:
            while (true) {
                switch (state.get ()) {
                    case START_RECORDING:
                        writer = ToolFactory.makeWriter (filename);
                        writer.addVideoStream (0, 0,
                                ICodec.ID.CODEC_ID_H264,
                                halfbut2 (screenRect.width),
                                halfbut2 (screenRect.height));
                        state.set (STATE.DO_RECORDING);
                        break;

                    case DO_RECORDING:
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

                    case FINISH_RECORDING:
                        state.set (STATE.IDLE);
                        writer.close ();
                        startButton.setEnabled (true);
                        break lab; // end thread
                }
            }
        }
    };
    private JTextField outputPath;
    private JRadioButton MOVRadioButton;

    public ScreenVidCapture () throws Exception {
        stopButton.setEnabled (false);
        ToolFactory.setTurboCharged (true);

        startButton.addActionListener (e -> {
            System.out.println ("Start");
            stopButton.setEnabled (true);
            startButton.setEnabled (false);
            filename = outputPath.getText () + File.separator +
                    System.currentTimeMillis () + ".mp4";
            imageCount = 0;
            state.set (STATE.START_RECORDING);
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
            state.set (STATE.FINISH_RECORDING);
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
