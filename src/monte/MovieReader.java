/*
 * @(#)MovieReader.java  
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package monte;

import java.io.IOException;

/**
 * A simple API for reading movie data (audio and video) from a file.
 * 
 * <p>
 * FIXME - MovieReader should extend Demultiplexer
 *
 * @author Werner Randelshofer
 * @version $Id: MovieReader.java 299 2013-01-03 07:40:18Z werner $
 */
public interface MovieReader {

    /** Reads the next sample chunk from the next track in playback sequence.
     * The variable buffer.track contains the track number.
     *
     * @param buf The buffer into which to store the sample data.
     */
    //public void read(Buffer buffer) throws IOException;

}
