/*
 * @(#)ScreenRecorder.java 
 * 
 * Copyright (c) 2011-2012 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package monte;
import monte.quicktime.QuickTimeWriter;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

import static java.lang.Math.max;
import static monte.BufferFlag.SAME_DATA;
import static monte.VideoFormatKeys.*;

/**
 * A screen recorder written in pure Java. <p> Captures the screen, the mouse
 * cursor and audio. <p> This class records mouse clicks occurring on other Java
 * Windows running in the same JVM. Mouse clicks occurring in other JVM's and
 * other processes are not recorded. This ability is useful for performing
 * in-JVM recordings of an application that is being tested. <p> This recorder
 * uses four threads. Three capture threads for screen, mouse cursor and audio,
 * and one output thread for the movie writer. <p> FIXME - This class is a
 * horrible mess.
 *
 * @author Werner Randelshofer
 * @version $Id: ScreenRecorder.java 303 2013-01-03 07:43:37Z werner $
 */
public class ScreenRecorder /*extends AbstractStateModel*/ {

    public enum State {

        DONE, FAILED, RECORDING
    }
    private State state = State.DONE;
    //    public final static String ENCODING_BLACK_CURSOR = "black";
    /**
     * The file format. "AVI" or "QuickTime"
     */
    private final Format fileFormat;
    /**
     * The input video format for screen capture.
     */
    private final Format screenFormat;
    /**
     * The bounds of the graphics device that we capture with AWT Robot.
     */
    private final Rectangle captureArea;
    /**
     * The writer for the movie file.
     */
    private MovieWriter w;
    /**
     * The start time of the recording.
     */
    protected long recordingStartTime;
    /**
     * The stop time of the recording.
     */
    protected volatile long recordingStopTime;
    /**
     * The start time of the current movie file.
     */
    private long fileStartTime;
//    /**
//     * Holds the mouse captures made with {@code MouseInfo}.
//     */
//    private ArrayBlockingQueue<Buffer> mouseCaptures;
    /**
     * Timer for screen captures.
     */
    private ScheduledThreadPoolExecutor screenCaptureTimer;
//    /**
//     * Timer for mouse captures.
//     */
//    protected ScheduledThreadPoolExecutor mouseCaptureTimer;
    /**
     * Thread for file writing.
     */
    private volatile Thread writerThread;
    /**
     * Object for thread synchronization.
     */
    //private final Object sync = new Object();
    private ArrayBlockingQueue<Buffer> writerQueue;
    /**
     * This codec encodes a video frame.
     */
    private Codec frameEncoder;
    /**
     * outputTime and ffrDuration are needed for conversion of the video stream
     * from variable frame rate to fixed frame rate. FIXME - Do this with a
     * CodecChain.
     */
    private Rational outputTime;
    private Rational ffrDuration;
    //private final ArrayList<File> recordedFiles;
    /**
     * Id of the video track.
     */
    protected int videoTrack = 0;
    /**
     * The device from which screen captures are generated.
     */
    private final GraphicsDevice captureDevice;
    private ScreenGrabber screenGrabber;
    private ScheduledFuture screenFuture;
    /**
     * Where to store the movie.
     */
    protected File movieFolder;

    /**
     * Creates a screen recorder.
     *
     * @param cfg Graphics configuration of the capture screen.
     * @param captureArea Defines the area of the screen that shall be captured.
     * @param fileFormat The file format "AVI" or "QuickTime".
     * @param screenFormat The video format for screen capture.
     * @param movieFolder Where to store the movie
     */
    public ScreenRecorder (GraphicsConfiguration cfg,
                           Rectangle captureArea,
                           Format fileFormat,
                           Format screenFormat,
                           File movieFolder) {

        this.fileFormat = fileFormat;
        this.screenFormat = screenFormat;
        this.captureDevice = cfg.getDevice();
        this.captureArea = (captureArea == null) ? cfg.getBounds() : captureArea;
//        if (mouseFormat != null && mouseFormat.get(FrameRateKey).intValue() > 0) {
//            mouseCaptures = new ArrayBlockingQueue<Buffer>(mouseFormat.get(FrameRateKey).intValue() * 2);
//        }
        this.movieFolder = movieFolder;
        if (this.movieFolder == null) {
            if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                this.movieFolder = new File(System.getProperty("user.home") + File.separator + "Videos");
            } else {
                this.movieFolder = new File(System.getProperty("user.home") + File.separator + "Movies");
            }
        }

    }

    protected MovieWriter createMovieWriter() throws IOException {
        File f = createMovieFile(fileFormat);
        //recordedFiles.add(f);

        MovieWriter mw = w = new QuickTimeWriter (f); //Registry.getInstance().getWriter(fileFormat, f);


        // Create the video encoder
        Rational videoRate = screenFormat.get(FrameRateKey); //Rational.max(screenFormat.get(FrameRateKey), mouseFormat.get(FrameRateKey));
        ffrDuration = videoRate.inverse();
        Format videoInputFormat = screenFormat.prepend(MediaTypeKey, MediaType.VIDEO,
                EncodingKey, ENCODING_BUFFERED_IMAGE,
                WidthKey, captureArea.width,
                HeightKey, captureArea.height,
                FrameRateKey, videoRate);
        Format videoOutputFormat = screenFormat.prepend(
                FrameRateKey, videoRate,
                MimeTypeKey, fileFormat.get(MimeTypeKey))//
                //
                .append(//
                WidthKey, captureArea.width,
                HeightKey, captureArea.height);

        videoTrack = w.addTrack(videoOutputFormat);

        frameEncoder = new TechSmithCodec ();
        frameEncoder.setInputFormat(videoInputFormat);
        frameEncoder.setOutputFormat(videoOutputFormat);
        if (frameEncoder.getOutputFormat() == null) {
            throw new IOException("Unable to encode video frames in this output format:\n" + videoOutputFormat);
        }

        // If the capture area does not have the same dimensions as the
        // video format, create a codec chain which scales the image before
        // performing the frame encoding.
        if (!videoInputFormat.intersectKeys(WidthKey, HeightKey).matches(
                videoOutputFormat.intersectKeys(WidthKey, HeightKey))) {
            ScaleImageCodec sic = new ScaleImageCodec();
            sic.setInputFormat(videoInputFormat);
            sic.setOutputFormat(videoOutputFormat.intersectKeys(WidthKey, HeightKey).append(videoInputFormat));
            frameEncoder = new CodecChain(sic, frameEncoder);
        }


        // FIXME - There should be no need for format-specific code.
        if (screenFormat.get(DepthKey) == 8) {
            /*if (w instanceof AVIWriter) {
                AVIWriter aviw = (AVIWriter) w;
                aviw.setPalette(videoTrack, Colors.createMacColors());
            } else*/ if (w instanceof QuickTimeWriter) {
                QuickTimeWriter qtw = (QuickTimeWriter) w;
                qtw.setVideoColorTable(videoTrack, Colors.createMacColors());
            }
        }

        fileStartTime = System.currentTimeMillis();
        return mw;
    }

    /**
     * Creates a file for recording the movie. <p> This implementation creates a
     * file in the users "Video" folder on Windows, or in the users "Movies"
     * folders on Mac OS X. <p> You can override this method, if you would like
     * to create a movie file at a different location.
     *
     * @param fileFormat
     * @return the file
     * @throws IOException
     */
    protected File createMovieFile(Format fileFormat) throws IOException {
        if (!movieFolder.exists()) {
            movieFolder.mkdirs();
        } else if (!movieFolder.isDirectory()) {
            throw new IOException("\"" + movieFolder + "\" is not a directory.");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd 'at' HH.mm.ss");

        return new File(movieFolder,//
                "ScreenRecording " + dateFormat.format(new Date()) + "." + Registry.getInstance().getExtension(fileFormat));
    }

    //    /**
//     * Returns the state of the recorder.
//     */
//    public String getStateMessage() {
//        return stateMessage;
//    }

    /**
     * Sets the state of the recorder and fires a ChangeEvent.
     */
    private void setState(State newValue, String msg) {
        state = newValue;
        //fireStateChanged();
    }

    /**
     * Starts the screen recorder.
     */
    public void start() throws IOException {
        stop();
        //recordedFiles.clear();
        createMovieWriter();
        try {
            recordingStartTime = System.currentTimeMillis();
            recordingStopTime = Long.MAX_VALUE;

            outputTime = new Rational(0, 0);
            startWriter();
            try {
                startScreenCapture();
            } catch (AWTException e) {
                IOException ioe = new IOException("Start screen capture failed", e);
                stop();
                throw ioe;
            } catch (IOException ioe) {
                stop();
                throw ioe;
            }
            setState(State.RECORDING, null);
        } catch (IOException e) {
            stop();
            throw e;
        }
    }

    /**
     * Starts screen capture.
     */
    private void startScreenCapture() throws AWTException, IOException {
        screenCaptureTimer = new ScheduledThreadPoolExecutor(1);
        int delay = max(1, (int) (1000 / screenFormat.get(FrameRateKey).doubleValue()));
        screenGrabber = new ScreenGrabber(this, recordingStartTime);
        screenFuture = screenCaptureTimer.scheduleAtFixedRate(screenGrabber, delay, delay, TimeUnit.MILLISECONDS);
        screenGrabber.setFuture(screenFuture);
    }

    private static class ScreenGrabber implements Runnable {

        /**
         * Previously draw mouse location. This is used to have the last mouse
         * location at hand, when a new screen capture has been created, but the
         * mouse has not been moved.
         */
        private final Point prevDrawnMouseLocation = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
        /**
         * Holds the screen capture made with AWT Robot.
         */
        private BufferedImage screenCapture;
        private final ScreenRecorder recorder;
        //private ScheduledThreadPoolExecutor screenTimer;
        /**
         * The AWT Robot which we use for capturing the screen.
         */
        private final Robot robot;
        private final Rectangle captureArea;
        /**
         * Holds the composed image (screen capture and super-imposed mouse
         * cursor). This is the image that is written into the video track of
         * the file.
         */
        private final BufferedImage videoImg;
        /**
         * Graphics object for drawing into {@code videoImg}.
         */
        private final Graphics2D videoGraphics;
        //private final Format mouseFormat;
//        private final ArrayBlockingQueue<Buffer> mouseCaptures;
        /**
         * The time the previous screen frame was captured.
         */
        private Rational prevScreenCaptureTime;
        private final int videoTrack;
        private final long startTime;
        private volatile long stopTime = Long.MAX_VALUE;
        private ScheduledFuture future;
        private long sequenceNumber;

        public void setFuture(ScheduledFuture future) {
            this.future = future;
        }

        public synchronized void setStopTime(long newValue) {
            this.stopTime = newValue;
        }

        public synchronized long getStopTime() {
            return this.stopTime;
        }

        public ScreenGrabber(ScreenRecorder recorder, long startTime) throws AWTException, IOException {
            this.recorder = recorder;
            this.captureArea = recorder.captureArea;
            this.robot = new Robot(recorder.captureDevice);
//            this.mouseFormat = recorder.mouseFormat;
//            this.mouseCaptures = recorder.mouseCaptures;
            //Object sync = recorder.sync;
            this.videoTrack = recorder.videoTrack;
            this.prevScreenCaptureTime = new Rational(startTime, 1000);
            this.startTime = startTime;

            Format screenFormat = recorder.screenFormat;
            if (screenFormat.get(DepthKey, 24) == 24) {
                videoImg = new BufferedImage(this.captureArea.width, this.captureArea.height, BufferedImage.TYPE_INT_RGB);
            } else if (screenFormat.get(DepthKey) == 16) {
                videoImg = new BufferedImage(this.captureArea.width, this.captureArea.height, BufferedImage.TYPE_USHORT_555_RGB);
            } else if (screenFormat.get(DepthKey) == 8) {
                videoImg = new BufferedImage(this.captureArea.width, this.captureArea.height, BufferedImage.TYPE_BYTE_INDEXED, Colors.createMacColors());
            } else {
                throw new IOException("Unsupported color depth " + screenFormat.get(DepthKey));
            }
            videoGraphics = videoImg.createGraphics();
            videoGraphics.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            videoGraphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            videoGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        }

        @Override
        public void run() {
            try {
                grabScreen();
            } catch (Throwable ex) {
                ex.printStackTrace();
                //screenTimer.shutdown();
                recorder.recordingFailed(ex.getMessage());
            }
        }

        /**
         * Grabs a screen, generates video images with pending mouse captures
         * and writes them into the movie file.
         */
        private void grabScreen() throws IOException, InterruptedException {
            // Capture the screen
            BufferedImage previousScreenCapture = screenCapture;
            long timeBeforeCapture = System.currentTimeMillis();
            try {
                screenCapture = robot.createScreenCapture(captureArea);
            } catch (IllegalMonitorStateException e) {
                //IOException ioe= new IOException("Could not grab screen");
                //ioe.initCause(e);
                //throw ioe;
                // Screen capture failed due to a synchronization error
                return;
            }
            long timeAfterCapture = System.currentTimeMillis();
            if (previousScreenCapture == null) {
                previousScreenCapture = screenCapture;
            }
            videoGraphics.drawImage(previousScreenCapture, 0, 0, null);

            Buffer buf = new Buffer();
            buf.format = new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, ENCODING_BUFFERED_IMAGE);
            // Generate video frames with mouse cursor painted on them
            boolean hasMouseCapture = false;
            if (false) {
//                while (!mouseCaptures.isEmpty() && mouseCaptures.peek().timeStamp.compareTo(new Rational(timeAfterCapture, 1000)) < 0) {
//                    Buffer mouseCapture = mouseCaptures.poll();
//                    if (mouseCapture.timeStamp.compareTo(prevScreenCaptureTime) > 0) {
//                        if (mouseCapture.timeStamp.compareTo(new Rational(timeBeforeCapture, 1000)) < 0) {
//                            previousScreenCapture = screenCapture;
//                            videoGraphics.drawImage(previousScreenCapture, 0, 0, null);
//                        }
//
//                        Point mcp = (Point) mouseCapture.data;
//                        boolean prevMousePressed = (Boolean) mouseCapture.header;
//                        prevDrawnMouseLocation.setLocation(mcp.x - captureArea.x, mcp.y - captureArea.y);
//                        Point p = prevDrawnMouseLocation;
//
//                        long localStopTime = getStopTime();
//                        if (mouseCapture.timeStamp.compareTo(new Rational(localStopTime, 1000)) > 0) {
//                            break;
//                        }
//
//                    }
//                }

                if (!hasMouseCapture && prevScreenCaptureTime.compareTo(new Rational(getStopTime(), 1000)) < 0) {
                    Point p = prevDrawnMouseLocation;

                    buf.data = videoImg;
                    buf.sampleDuration = new Rational(timeAfterCapture, 1000).subtract(prevScreenCaptureTime);
                    buf.timeStamp = prevScreenCaptureTime.subtract(new Rational(startTime, 1000));
                    buf.track = videoTrack;
                    buf.sequenceNumber = sequenceNumber++;
                    buf.header = p.x == Integer.MAX_VALUE ? null : p;
                    recorder.write(buf);
                    prevScreenCaptureTime = new Rational(timeAfterCapture, 1000);
                }
            } else if (prevScreenCaptureTime.compareTo(new Rational(getStopTime(), 1000)) < 0) {
                buf.data = videoImg;
                buf.sampleDuration = new Rational(timeAfterCapture, 1000).subtract(prevScreenCaptureTime);
                buf.timeStamp = prevScreenCaptureTime.subtract(new Rational(startTime, 1000));
                buf.track = videoTrack;
                buf.sequenceNumber = sequenceNumber++;
                buf.header = null; // no mouse position has been recorded for this frame
                recorder.write(buf);
                prevScreenCaptureTime = new Rational(timeAfterCapture, 1000);
            }

            if (timeBeforeCapture > getStopTime()) {
                future.cancel(false);
            }
        }

        public void close() {
            videoGraphics.dispose();
            videoImg.flush();
        }
    }

    /**
     * Starts file writing.
     */
    private void startWriter() {
        writerQueue = new ArrayBlockingQueue<Buffer>(
                screenFormat.get(FrameRateKey).intValue()); //max(screenFormat.get(FrameRateKey).intValue(), mouseFormat.get(FrameRateKey).intValue()) + 1);
        writerThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (writerThread == this || !writerQueue.isEmpty()) {
                        try {
                            Buffer buf = writerQueue.take();
                            doWrite(buf);
                        } catch (InterruptedException ex) {
                            // We have been interrupted, terminate
                            break;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    recordingFailed(e.getMessage()==null?e.toString():e.getMessage());
                }
            }
        };
        writerThread.start();
    }

    private void recordingFailed(final String msg) {
        SwingUtilities.invokeLater(() -> {
            try {
                stop();
                setState(State.FAILED, msg);
            } catch (IOException ex2) {
                ex2.printStackTrace();
            }
        });
    }

    /**
     * Stops the screen recorder. <p> Stopping the screen recorder may take
     * several seconds, because audio capture uses a large capture buffer. Also,
     * the MovieWriter has to finish up a movie file which may take some time
     * depending on the amount of meta-data that needs to be written.
     */
    public void stop() throws IOException {
        if (state == State.RECORDING) {
            recordingStopTime = System.currentTimeMillis();
            //stopMouseCapture();
            if (screenCaptureTimer != null) {
                screenGrabber.setStopTime(recordingStopTime);
            }
            try {
//                waitUntilMouseCaptureStopped();
                if (screenCaptureTimer != null) {
                    try {
                        screenFuture.get();
                    } catch (Exception ignored) {
                    }
                    screenCaptureTimer.shutdown();
                    screenCaptureTimer.awaitTermination(5000, TimeUnit.MILLISECONDS);
                    screenCaptureTimer = null;
                    screenGrabber.close();
                    screenGrabber = null;
                }
            } catch (InterruptedException ex) {
                // nothing to do
            }
            stopWriter();
            setState(State.DONE, null);
        }
    }

    private void stopWriter() throws IOException {
        Thread pendingWriterThread = writerThread;
        writerThread = null;

        try {
            if (pendingWriterThread != null) {
                pendingWriterThread.interrupt();
                pendingWriterThread.join();
            }
        } catch (InterruptedException ex) {
            // nothing to do
            ex.printStackTrace();
        }
        if (w != null) {
            w.close();
            w = null;
        }
    }
    long counter = 0;

    /**
     * Writes a buffer into the movie. Since the file system may not be
     * immediately available at all times, we do this asynchronously. <p> The
     * buffer is copied and passed to the writer queue, which is consumed by the
     * writer thread. See method startWriter(). <p> AVI does not support a
     * variable frame rate for the video track. Since we can not capture frames
     * at a fixed frame rate we have to resend the same captured screen multiple
     * times to the writer. <p> This method is called asynchronously from
     * different threads. <p> You can override this method if you wish to
     * process the media data.
     *
     *
     * @param buf A buffer with un-encoded media data. If
     * {@code buf.track==videoTrack}, then the buffer contains a
     * {@code BufferedImage} in {@code buffer.data} and a {@code Point} in
     * {@code buffer.header} with the recorded mouse location. The header is
     * null if the mouse is outside the capture area, or mouse recording has not
     * been enabled.
     *
     * @throws IOException
     */
    protected void write(Buffer buf) throws IOException, InterruptedException {
        MovieWriter writer = this.w;
        if (writer == null) {
            return;
        }
        if (buf.track == videoTrack) {
            if (writer.getFormat(videoTrack).get(FixedFrameRateKey, false) == false) {
                // variable frame rate is supported => easy
                Buffer wbuf = new Buffer();
                frameEncoder.process(buf, wbuf);
                writerQueue.put(wbuf);
            } else {// variable frame rate not supported => convert to fixed frame rate

                // FIXME - Use CodecChain for this

                Rational inputTime = buf.timeStamp.add(buf.sampleDuration);
                boolean isFirst = true;
                while (outputTime.compareTo(inputTime) < 0) {
                    buf.timeStamp = outputTime;
                    buf.sampleDuration = ffrDuration;
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        buf.setFlag(SAME_DATA);
                    }
                    Buffer wbuf = new Buffer();
                    if (frameEncoder.process(buf, wbuf) != Codec.CODEC_OK) {
                        throw new IOException("Codec failed or could not process frame in a single step.");
                    }
                    writerQueue.put(wbuf);
                    outputTime = outputTime.add(ffrDuration);
                }
            }
        } else {
            Buffer wbuf = new Buffer();
            wbuf.setMetaTo(buf);
            wbuf.data = ((byte[]) buf.data).clone();
            wbuf.length = buf.length;
            wbuf.offset = buf.offset;
            writerQueue.put(wbuf);
        }
    }

    /**
     * The actual writing of the buffer happens here. <p> This method is called
     * exclusively from the writer thread in startWriter().
     *
     * @param buf
     * @throws IOException
     */
    private void doWrite(Buffer buf) throws IOException {
        MovieWriter mw = w;
        // Close file on a separate thread if file is full or an hour
        // has passed.
        // The if-statement must ensure that we only start a new video file
        // at a key-frame.
        // FIXME - this assumes that all audio frames are key-frames
        // FIXME - this does not guarantee that audio and video track have
        //         the same duration
        long now = System.currentTimeMillis();
        //    private AWTEventListener awtEventListener;
        long maxRecordingTime = 60 * 60 * 1000;
        if (buf.track == videoTrack && buf.isFlag(BufferFlag.KEYFRAME)
                && (mw.isDataLimitReached() || now - fileStartTime > maxRecordingTime)) {
            final MovieWriter closingWriter = mw;
            new Thread (() -> {
                try {
                    closingWriter.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            }).start();
            mw = createMovieWriter();

        }
        //}
        mw.write(buf.track, buf);
    }


}
