/*
 * @(#)QuickTimeMultiplexer.java  1.0  2011-02-20
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */

package monte.quicktime;

import org.monte.media.Multiplexer;

import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * {@code QuickTimeMultiplexer}.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-02-20 Created.
 */
public class QuickTimeMultiplexer extends QuickTimeWriter implements Multiplexer {
 public QuickTimeMultiplexer(File file) throws IOException {

super(file);

    }

    /**
     * Creates a new QuickTime writer.
     *
     * @param out the underlying output stream.
     */
    public QuickTimeMultiplexer(ImageOutputStream out) throws IOException {
        super(out);
    }


}
