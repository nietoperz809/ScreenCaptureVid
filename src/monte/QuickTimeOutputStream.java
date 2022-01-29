/*
 * @(#)QuickTimeOutputStream.java  1.0  2011-08-19
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package monte;

//import org.monte.media.Format;
//import org.monte.media.io.ImageOutputStreamAdapter;
//import org.monte.media.math.Rational;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.*;
import java.nio.ByteOrder;
import java.util.Date;

import static java.lang.Math.max;
import static monte.AudioFormatKeys.*;
import static monte.VideoFormatKeys.*;
//import static org.monte.media.AudioFormatKeys.*;
//import static org.monte.media.VideoFormatKeys.*;

/**
 * This class provides low-level support for writing already encoded audio and
 * video samples into a QuickTime file.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-08-19 Created.
 */
public class QuickTimeOutputStream extends AbstractQuickTimeStream {

    /**
     * Creates a new instance.
     *
     * @param file the output file
     */
    public QuickTimeOutputStream(File file) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        this.out = new FileImageOutputStream(file);
        this.streamOffset = 0;
        init();
    }

    /**
     * Creates a new instance.
     *
     * @param out the output stream.
     */
    public QuickTimeOutputStream(ImageOutputStream out) throws IOException {
        this.out = out;
        this.streamOffset = out.getStreamPosition();
        init();
    }

    private void init() {
        creationTime = new Date();
        modificationTime = new Date();
    }

    /**
     * Returns the total duration of the movie in the movie's time scale.
     *
     * @return media duration
     */
    public long getMovieDuration() {
        long duration = 0;
        for (Track t : tracks) {
            duration = Math.max(duration, t.getTrackDuration(movieTimeScale));
        }
        return duration;
    }

    /**
     * Sets the color table for videos with indexed color models.
     *
     * @param track The track number.
     * @param icm IndexColorModel. Specify null to use the standard Macintosh
     * color table. 
     */
    public void setVideoColorTable(int track, ColorModel icm) {
        if (icm instanceof IndexColorModel) {
            VideoTrack t = (VideoTrack) tracks.get(track);
            t.videoColorTable = (IndexColorModel) icm;
        }
    }

    /**
     * Adds a video track.
     *
     * @param compressionType The QuickTime "image compression format"
     * 4-Character code. A list of supported 4-Character codes is given in qtff,
     * table 3-1, page 96.
     * @param compressorName The QuickTime compressor name. Can be up to 32
     * characters long. 
     * @param timeScale The media time scale between 1 and 2^32.
     * @param width The width of a video frame.
     * @param height The height of a video frame.
     * @param depth The number of bits per pixel.
     * @param syncInterval Interval for sync-samples. 0=automatic. 1=all frames
     * are keyframes. Values larger than 1 specify that for every n-th frame is
     * a keyframe. Apple's QuickTime will not work properly if there is not at
     * least one keyframe every second.
     *
     * @return Returns the track index.
     *
     * @throws IllegalArgumentException if {@code width} or {@code height} is
     * smaller than 1, if the length of {@code compressionType} is not equal to
     * 4, if the length of the {@code compressorName} is not between 1 and 32,
     * if the tiimeScale is not between 1 and 2^32.
     */
    public int addVideoTrack(String compressionType, String compressorName, long timeScale, int width, int height, int depth, int syncInterval) throws IOException {
        ensureStarted();
        if (compressionType == null || compressionType.length() != 4) {
            throw new IllegalArgumentException("compressionType must be 4 characters long:" + compressionType);
        }
        if (compressorName == null||compressorName.length() < 1 || compressorName.length() > 32) {
            throw new IllegalArgumentException("compressorName must be between 1 and 32 characters long:" + (compressorName == null ? "null" : "\"" + compressorName + "\""));
        }
        if (timeScale < 1 || timeScale > (2L << 32)) {
            throw new IllegalArgumentException("timeScale must be between 1 and 2^32:" + timeScale);
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Width and height must be greater than 0, width:" + width + " height:" + height);
        }

        VideoTrack t = new VideoTrack();
        t.mediaCompressionType = compressionType;
        t.mediaCompressorName = compressorName;
        t.mediaTimeScale = timeScale;
        t.width = width;
        t.height = height;
        t.videoDepth = depth;
        t.syncInterval = syncInterval;
        t.format = new Format (
                MediaTypeKey, FormatKeys.MediaType.VIDEO,
                MimeTypeKey, MIME_QUICKTIME,
                EncodingKey, compressionType,
                CompressorNameKey, compressorName,
                DataClassKey, byte[].class,
                WidthKey, width, HeightKey, height, DepthKey, depth,
                FrameRateKey, new Rational (timeScale, 1));
        tracks.add(t);
        return tracks.size() - 1;
    }

    /**
     * Adds an audio track.
     *
     * @param compressionType The QuickTime 4-character code. A list of
     * supported 4-Character codes is given in qtff, table 3-7, page 113.
     * @param timeScale The media time scale between 1 and 2^32.
     * @param sampleRate The sample rate. The integer portion must match the
     * {@code timeScale}.
     * @param numberOfChannels The number of channels: 1 for mono, 2 for stereo.
     * @param sampleSizeInBits The number of bits in a sample: 8 or 16.
     * @param isCompressed Whether the sound is compressed.
     * @param frameDuration The frame duration, expressed in the media’s
     * timescale, where the timescale is equal to the sample rate. For
     * uncompressed formats, this field is always 1.
     * @param frameSize For uncompressed audio, the number of bytes in a sample
     * for a single channel (sampleSize divided by 8). For compressed audio, the
     * number of bytes in a frame.
     *
     * @throws IllegalArgumentException if the audioFormat is not 4 characters
     * long, if the time scale is not between 1 and 2^32, if the integer portion
     * of the sampleRate is not equal to the timeScale, if numberOfChannels is
     * not 1 or 2.
     * @return Returns the track index.
     */
    public int addAudioTrack(String compressionType, //
            long timeScale, double sampleRate, //
            int numberOfChannels, int sampleSizeInBits, //
            boolean isCompressed, //
            int frameDuration, int frameSize, boolean signed, ByteOrder byteOrder) throws IOException {
        ensureStarted();
        if (compressionType == null || compressionType.length() != 4) {
            throw new IllegalArgumentException("audioFormat must be 4 characters long:" + compressionType);
        }
        if (timeScale < 1 || timeScale > (2L << 32)) {
            throw new IllegalArgumentException("timeScale must be between 1 and 2^32:" + timeScale);
        }
        if (timeScale != (int) Math.floor(sampleRate)) {
            throw new IllegalArgumentException("timeScale: " + timeScale + " must match integer portion of sampleRate: " + sampleRate);
        }
        if (numberOfChannels != 1 && numberOfChannels != 2) {
            throw new IllegalArgumentException("numberOfChannels must be 1 or 2: " + numberOfChannels);
        }
        if (sampleSizeInBits != 8 && sampleSizeInBits != 16) {
            throw new IllegalArgumentException("sampleSize must be 8 or 16: " + numberOfChannels);
        }

        AudioTrack t = new AudioTrack();
        t.mediaCompressionType = compressionType;
        t.mediaTimeScale = timeScale;
        t.soundSampleRate = sampleRate;
        t.soundCompressionId = isCompressed ? -2 : -1;
        t.soundNumberOfChannels = numberOfChannels;
        t.soundSampleSize = sampleSizeInBits;
        t.soundSamplesPerPacket = frameDuration;
        if (isCompressed) {
            t.soundBytesPerPacket = frameSize;
            t.soundBytesPerFrame = frameSize * numberOfChannels;
        } else {
            t.soundBytesPerPacket = frameSize / numberOfChannels;
            t.soundBytesPerFrame = frameSize;
        }
        t.soundBytesPerSample = sampleSizeInBits / 8;

        t.format = new Format(
                MediaTypeKey, MediaType.AUDIO,
                MimeTypeKey, MIME_QUICKTIME,
                EncodingKey, compressionType,
                SampleRateKey, Rational.valueOf(sampleRate),
                SampleSizeInBitsKey, sampleSizeInBits,
                ChannelsKey, numberOfChannels,
                FrameSizeKey, frameSize,
                SampleRateKey, Rational.valueOf(sampleRate),
                SignedKey, signed,
                ByteOrderKey, byteOrder);
        tracks.add(t);
        return tracks.size() - 1;
    }

    /**
     * Sets the compression quality of a track. <p> A value of 0 stands for
     * "high compression is important" a value of 1 for "high image quality is
     * important". <p> Changing this value affects the encoding of video frames
     * which are subsequently written into the track. Frames which have already
     * been written are not changed. <p> This value has no effect on videos
     * encoded with lossless encoders such as the PNG format. <p> The default
     * value is 0.97.
     *
     * @param newValue
     */
    public void setCompressionQuality(int track, float newValue) {
        VideoTrack vt = (VideoTrack) tracks.get(track);
        vt.videoQuality = newValue;
    }

    /**
     * Returns the compression quality of a track.
     *
     * @return compression quality
     */
    public float getCompressionQuality(int track) {
        return ((VideoTrack) tracks.get(track)).videoQuality;
    }

    /**
     * Sets the state of the QuickTimeWriter to started. <p> If the state is
     * changed by this method, the prolog is written.
     */
    protected void ensureStarted() throws IOException {
        ensureOpen();
        if (state == States.FINISHED) {
            throw new IOException("Can not write into finished movie.");
        }
        if (state != States.STARTED) {
            writeProlog();
            mdatAtom = new WideDataAtom("mdat");
            state = States.STARTED;
        }
    }

    /**
     * Writes an already encoded sample from a byte array into a track. <p> This
     * method does not inspect the contents of the samples. The contents has to
     * match the format and dimensions of the media in this track.
     *
     * @param track The track index.
     * @param data The encoded sample data.
     * @param off The start offset in the data.
     * @param len The number of bytes to write.
     * @param duration The duration of the sample in media time scale units.
     * @param isSync Whether the sample is a sync sample (keyframe).
     *
     * @throws IllegalArgumentException if the duration is less than 1.
     * @throws IOException if writing the sample data failed.
     */
    public void writeSample(int track, byte[] data, int off, int len, long duration, boolean isSync) throws IOException {
        ensureStarted();
        if (duration <= 0) {
            throw new IllegalArgumentException("duration must be greater 0");
        }
        Track t = tracks.get(track); // throws index out of bounds exception if illegal track index
        ensureOpen();
        ensureStarted();
        long offset = getRelativeStreamPosition();
        OutputStream mdatOut = mdatAtom.getOutputStream();
        mdatOut.write(data, off, len);
        t.addSample(new Sample(duration, offset, len), 1, isSync);
    }

    /**
     * Writes multiple samples from a byte array into a track. <p> This method
     * does not inspect the contents of the data. The contents has to match the
     * format and dimensions of the media in this track.
     *
     * @param track The track index.
     * @param sampleCount The number of samples.
     * @param data The encoded sample data.
     * @param off The start offset in the data.
     * @param len The number of bytes to write. Must be dividable by
     * sampleCount.
     * @param sampleDuration The duration of a sample. All samples must have the
     * same duration.
     * @param isSync Whether the samples are sync samples. All samples must
     * either be sync samples or non-sync samples.
     *
     * @throws IllegalArgumentException if the duration is less than 1.
     * @throws IOException if writing the sample data failed.
     */
    public void writeSamples(int track, int sampleCount, byte[] data, int off, int len, long sampleDuration, boolean isSync) throws IOException {
        ensureStarted();
        if (sampleDuration <= 0) {
            throw new IllegalArgumentException("sampleDuration must be greater 0, sampleDuration=" + sampleDuration + " track=" + track);
        }
        if (sampleCount <= 0) {
            throw new IllegalArgumentException("sampleCount must be greater 0, sampleCount=" + sampleCount + " track=" + track);
        }
        if (len % sampleCount != 0) {
            throw new IllegalArgumentException("len must be divisable by sampleCount len=" + len + " sampleCount=" + sampleCount + " track=" + track);
        }
        Track t = tracks.get(track); // throws index out of bounds exception if illegal track index
        ensureOpen();
        ensureStarted();
        long offset = getRelativeStreamPosition();
        OutputStream mdatOut = mdatAtom.getOutputStream();
        mdatOut.write(data, off, len);


        int sampleLength = len / sampleCount;
        Sample first = new Sample(sampleDuration, offset, sampleLength);
        Sample last = new Sample(sampleDuration, offset + sampleLength * (sampleCount - 1), sampleLength);
        t.addChunk(new Chunk(first, last, sampleCount, 1), isSync);
    }

    /**
     * Returns true if the limit for media samples has been reached. If this
     * limit is reached, no more samples should be added to the movie. <p>
     * QuickTime files can be up to 64 TB long, but there are other values that
     * may overflow before this size is reached. This method returns true when
     * the files size exceeds 2^60 or when the media duration value of a track
     * exceeds 2^61.
     */
    public boolean isDataLimitReached() {
        try {
            long maxMediaDuration = 0;
            for (Track t : tracks) {
                maxMediaDuration = max(t.mediaDuration, maxMediaDuration);
            }

            return getRelativeStreamPosition() > (long) (1L << 61) //
                    || maxMediaDuration > 1L << 61;
        } catch (IOException ex) {
            return true;
        }
    }

    /**
     * Check to make sure that this stream has not been closed
     */
    protected void ensureOpen() throws IOException {
        if (state == States.CLOSED) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * Writes the stream prolog.
     */
    private void writeProlog() throws IOException {
        /* File type atom
         *
         typedef struct {
         magic brand;
         bcd4 versionYear;
         bcd2 versionMonth;
         bcd2 versionMinor;
         magic[4] compatibleBrands;
         } ftypAtom;
         */
        DataAtom ftypAtom = new DataAtom("ftyp");
        DataAtomOutputStream d = ftypAtom.getOutputStream();
        d.writeType("qt  "); // brand
        d.writeBCD4(2005); // versionYear
        d.writeBCD2(3); // versionMonth
        d.writeBCD2(0); // versionMinor
        d.writeType("qt  "); // compatibleBrands
        d.writeInt(0); // compatibleBrands (0 is used to denote no value)
        d.writeInt(0); // compatibleBrands (0 is used to denote no value)
        d.writeInt(0); // compatibleBrands (0 is used to denote no value)
        ftypAtom.finish();
    }

    private void writeEpilog() throws IOException {
        long duration = getMovieDuration();

        DataAtom leaf;

        /* Movie Atom ========= */
        moovAtom = new CompositeAtom("moov");

        /* Movie Header Atom -------------
         * The data contained in this atom defines characteristics of the entire
         * QuickTime movie, such as time scale and duration. It has an atom type
         * value of 'mvhd'.
         *
         * typedef struct {
         byte version;
         byte[3] flags;
         mactimestamp creationTime;
         mactimestamp modificationTime;
         int timeScale;
         int duration;
         fixed16d16 preferredRate;
         fixed8d8 preferredVolume;
         byte[10] reserved;
         fixed16d16 matrixA;
         fixed16d16 matrixB;
         fixed2d30 matrixU;
         fixed16d16 matrixC;
         fixed16d16 matrixD;
         fixed2d30 matrixV;
         fixed16d16 matrixX;
         fixed16d16 matrixY;
         fixed2d30 matrixW;
         int previewTime;
         int previewDuration;
         int posterTime;
         int selectionTime;
         int selectionDuration;
         int currentTime;
         int nextTrackId;
         } movieHeaderAtom;
         */
        leaf = new DataAtom("mvhd");
        moovAtom.add(leaf);
        DataAtomOutputStream d = leaf.getOutputStream();
        d.writeByte(0); // version
        // A 1-byte specification of the version of this movie header atom.

        d.writeByte(0); // flags[0]
        d.writeByte(0); // flags[1]
        d.writeByte(0); // flags[2]
        // Three bytes of space for future movie header flags.

        d.writeMacTimestamp(creationTime); // creationTime
        // A 32-bit integer that specifies the calendar date and time (in
        // seconds since midnight, January 1, 1904) when the movie atom was
        // created. It is strongly recommended that this value should be
        // specified using coordinated universal time (UTC).

        d.writeMacTimestamp(modificationTime); // modificationTime
        // A 32-bit integer that specifies the calendar date and time (in
        // seconds since midnight, January 1, 1904) when the movie atom was
        // changed. BooleanIt is strongly recommended that this value should be
        // specified using coordinated universal time (UTC).

        d.writeUInt(movieTimeScale); // timeScale
        // A time value that indicates the time scale for this movie—that is,
        // the number of time units that pass per second in its time coordinate
        // system. A time coordinate system that measures time in sixtieths of a
        // second, for example, has a time scale of 60.

        d.writeUInt(duration); // duration
        // A time value that indicates the duration of the movie in time scale
        // units. Note that this property is derived from the movie’s tracks.
        // The value of this field corresponds to the duration of the longest
        // track in the movie.

        d.writeFixed16D16(preferredRate); // preferredRate
        // A 32-bit fixed-point number that specifies the rate at which to play
        // this movie. A value of 1.0 indicates normal rate.

        d.writeFixed8D8(preferredVolume); // preferredVolume
        // A 16-bit fixed-point number that specifies how loud to play this
        // movie’s sound. A value of 1.0 indicates full volume.

        d.write(new byte[10]); // reserved;
        // Ten bytes reserved for use by Apple. Set to 0.

        d.writeFixed16D16(movieMatrix[0]); // matrix[0]
        d.writeFixed16D16(movieMatrix[1]); // matrix[1]
        d.writeFixed2D30(movieMatrix[2]); // matrix[2]
        d.writeFixed16D16(movieMatrix[3]); // matrix[3]
        d.writeFixed16D16(movieMatrix[4]); // matrix[4]
        d.writeFixed2D30(movieMatrix[5]); // matrix[5]
        d.writeFixed16D16(movieMatrix[6]); // matrix[6]
        d.writeFixed16D16(movieMatrix[7]); // matrix[7]
        d.writeFixed2D30(movieMatrix[8]); // matrix[8]

        // The matrix structure associated with this movie. A matrix shows how
        // to map points from one coordinate space into another. See “Matrices”
        // for a discussion of how display matrices are used in QuickTime:
        // http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap4/chapter_5_section_4.html#//apple_ref/doc/uid/TP40000939-CH206-18737

        d.writeUInt(previewTime); // previewTime
        // The time value in the movie at which the preview begins.

        d.writeUInt(previewDuration); // previewDuration
        // The duration of the movie preview in movie time scale units.

        d.writeUInt(posterTime); // posterTime
        // The time value of the time of the movie poster.

        d.writeUInt(selectionTime); // selectionTime
        // The time value for the start time of the current selection.

        d.writeUInt(selectionDuration); // selectionDuration
        // The duration of the current selection in movie time scale units.

        d.writeUInt(currentTime); // currentTime;
        // The time value for current time position within the movie.

        d.writeUInt(tracks.size() + 1); // nextTrackId
        // A 32-bit integer that indicates a value to use for the track ID
        // number of the next track added to this movie. Note that 0 is not a
        // valid track ID value.

        for (int i = 0, n = tracks.size(); i < n; i++) {
            Track t = tracks.get(i);
            /* Track Atom ======== */
            t.writeTrackAtoms(i, moovAtom, modificationTime);
        }
        //
        moovAtom.finish();
    }

}
