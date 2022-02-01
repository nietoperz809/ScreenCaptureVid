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
        try {
            while (true) {
                switch (svc.state.get ()) {
                    case START_RECORDING:
                        cfg = new JFrame ().getGraphicsConfiguration ();
                        screenRecorder = new ScreenRecorder (cfg, svc.screenRect,
                                new Format (MediaTypeKey, MediaType.FILE, MimeTypeKey, "video/quicktime"),
                                new Format (MediaTypeKey, MediaType.VIDEO, EncodingKey, "tscc",
                                        CompressorNameKey, "Techsmith Screen Capture",
                                        WidthKey, svc.screenRect.width,
                                        HeightKey, svc.screenRect.height,
                                        DepthKey, 16, FrameRateKey, Rational.valueOf (10),
                                        QualityKey, 1.0f,
                                        KeyFrameIntervalKey, (10 * 60) ),
                                new File (svc.outputPath.getText ()),
                                svc.getCursorPainter ());
                        screenRecorder.start ();
                        svc.state.set (RecorderState.DO_RECORDING);
                        break;

                    case DO_RECORDING:
                        svc.label.setText ("" + (++svc.imageCount));
                        Tools.robot.delay(200);
                        //Thread.sleep (200);
                        break;

                    case FINISH_RECORDING:
                        screenRecorder.stop ();
                        svc.state.set (RecorderState.IDLE);
                        break lab;
                }
            }
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }
}
