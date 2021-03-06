/*
 * @(#)ScaleImageCodec.java  1.0  2012-04-24
 * 
 * Copyright (c) 2012 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package monte;

// import org.monte.media.AbstractVideoCodec;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import static monte.BufferFlag.DISCARD;
import static monte.FormatKeys.*;
import static monte.VideoFormatKeys.DepthKey;
import static monte.VideoFormatKeys.ENCODING_BUFFERED_IMAGE;
import static monte.VideoFormatKeys.HeightKey;
import static monte.VideoFormatKeys.WidthKey;
/*
import static org.monte.media.BufferFlag.*;
import static org.monte.media.BufferFlag.DISCARD;
import static org.monte.media.FormatKeys.*;
import static org.monte.media.VideoFormatKeys.*;
import static org.monte.media.VideoFormatKeys.DepthKey;
import static org.monte.media.VideoFormatKeys.ENCODING_BUFFERED_IMAGE;
import static org.monte.media.VideoFormatKeys.HeightKey;
import static org.monte.media.VideoFormatKeys.WidthKey;
*/

/**
 * Scales a buffered image.
 *
 * @author Werner Randelshofer
 * @version 1.0 2012-04-24 Created.
 */
public class ScaleImageCodec extends AbstractVideoCodec {

    public ScaleImageCodec () {
        super(new Format[]{
                    new Format(MediaTypeKey, FormatKeys.MediaType.VIDEO, MimeTypeKey, MIME_JAVA,
                    EncodingKey, ENCODING_BUFFERED_IMAGE), //
                },
                new Format[]{
                    new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA,
                    EncodingKey, ENCODING_BUFFERED_IMAGE), //
                }//
                );
        name = "Scale Image";
    }

    @Override
    public Format setOutputFormat(Format f) {
        if (!f.containsKey(WidthKey) || !f.containsKey(HeightKey)) {
            throw new IllegalArgumentException("Output format must specify width and height.");
        }
        Format fNew = super.setOutputFormat(f.prepend(DepthKey, 24));
        return fNew;
    }

    @Override
    public int process(Buffer in, Buffer out) {
        out.setMetaTo(in);
        out.format = outputFormat;

        if (in.isFlag(DISCARD)) {
            return CODEC_OK;
        }
        BufferedImage imgIn = (BufferedImage) in.data;
        if (imgIn == null) {
            out.setFlag(DISCARD);
            return CODEC_FAILED;
        }

        BufferedImage imgOut = null;
        if (out.data instanceof BufferedImage) {
            imgOut = (BufferedImage) out.data;
            if (imgOut.getWidth() != outputFormat.get(WidthKey)
                    || imgOut.getHeight() != outputFormat.get(HeightKey)//
                    || imgOut.getType() != imgIn.getType()) {
                imgOut = null;
            }
        }
        if (imgOut == null) {
            if (imgIn.getColorModel() instanceof IndexColorModel) {
                imgOut = new BufferedImage(outputFormat.get(WidthKey), outputFormat.get(HeightKey), imgIn.getType(), (IndexColorModel) imgIn.getColorModel());
            } else {
                imgOut = new BufferedImage(outputFormat.get(WidthKey), outputFormat.get(HeightKey), imgIn.getType());
            }

        }
        Graphics2D g = imgOut.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(imgIn, 0, 0, imgOut.getWidth() - 1, imgOut.getHeight() - 1, 0, 0, imgIn.getWidth() - 1, imgIn.getHeight() - 1, null);
        g.dispose();

        out.data = imgOut;

        return CODEC_OK;
    }
}
