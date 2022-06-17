package mycode;


import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

import static mycode.RecorderState.DO_RECORDING;
import static mycode.RecorderState.IDLE;

public class H264Recorder extends RecorderBase {
    private IMediaWriter writer;

    public H264Recorder (ScreenVidCapture svc) {
        super (svc);
    }

    @Override
    public void run () {
        lab:
        try {
            while (true) {
                switch (svc.state.get ()) {
                    case START_RECORDING:
                        writer = ToolFactory.makeWriter (svc.filename);
                        writer.addVideoStream (0, 0,
                                ICodec.ID.CODEC_ID_H264,
                                Tools.halfbut2 (svc.screenRect.width),
                                Tools.halfbut2 (svc.screenRect.height));
                        svc.state.set (DO_RECORDING);
                        break;

                    case DO_RECORDING:
                        BufferedImage image = Tools.robot.createScreenCapture (svc.screenRect);
                        CursorPainter cp = svc.getCursorPainter();
                        if (cp != null)
                            cp.paint (image);
                        BufferedImage bgrScreen = Tools.convertBitmap (image, BufferedImage.TYPE_3BYTE_BGR);
                        svc.imageCount++;
                        writer.encodeVideo (0, bgrScreen, 300L * svc.imageCount, TimeUnit.MILLISECONDS);
                        svc.label.setText ("" + svc.imageCount);
                        Tools.robot.delay (100);
                        //Thread.sleep (100);
                        break;

                    case FINISH_RECORDING:
                        writer.close ();
                        svc.state.set (IDLE);
                        break lab; // end thread
                }
            }
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }
}
