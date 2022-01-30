package mycode;

import monte.Format;
import monte.FormatKeys;
import monte.Rational;
import monte.ScreenRecorder;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

import static monte.VideoFormatKeys.*;

public class MOVRecorder extends RecorderBase {
    GraphicsConfiguration cfg = GraphicsEnvironment//
            .getLocalGraphicsEnvironment ()//
            .getDefaultScreenDevice ()//
            .getDefaultConfiguration ();
    ScreenRecorder screenRecorder;

    public MOVRecorder (ScreenVidCapture svc) {
        super (svc);
    }

    @Override
    public void run () {
        lab:
        while (true) {
            switch (svc.state.get ()) {
                case START_RECORDING:
                    cfg = new JFrame ().getGraphicsConfiguration ();
                    screenRecorder = new ScreenRecorder (cfg, svc.screenRect,
                            new Format (MediaTypeKey, FormatKeys.MediaType.FILE, MimeTypeKey, "video/quicktime"),
                            new Format (MediaTypeKey, FormatKeys.MediaType.VIDEO, EncodingKey, "tscc",
                                    CompressorNameKey, "Techsmith Screen Capture",
                                    WidthKey, svc.screenRect.width,
                                    HeightKey, svc.screenRect.height,
                                    DepthKey, 16, FrameRateKey, Rational.valueOf (10),
                                    QualityKey, 1.0f,
                                    KeyFrameIntervalKey, (10 * 60) // one keyframe per minute is enough
                            ), new File (svc.outputPath.getText ()));
                    screenRecorder.start ();
                    svc.state.set (ScreenVidCapture.STATE.DO_RECORDING);
                    break;

                case DO_RECORDING:
                    svc.imageCount++;
                    svc.label.setText ("" + svc.imageCount);
                    try {
                        Thread.sleep (200);
                    } catch (InterruptedException e) {
                        return;
                    }
                    break;

                case FINISH_RECORDING:
                    try {
                        screenRecorder.stop ();
                    } catch (IOException e) {
                        e.printStackTrace ();
                    }
                    svc.startButton.setEnabled (true);
                    svc.state.set (ScreenVidCapture.STATE.IDLE);
                    break lab;
            }
        }
    }
}
