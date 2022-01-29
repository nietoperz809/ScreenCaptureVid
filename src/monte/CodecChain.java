/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package monte;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * {@code CodecChain}.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-09-07 Created.
 */
public class CodecChain implements Codec {

    private Codec first;
    private Codec second;
    private Buffer tmpBuf;
    private int firstState;
    private int secondState;
    private long firstElapsed;
    private long secondElapsed;

    public CodecChain (Codec first, Codec second) {
        if (first==null||second==null)throw new IllegalArgumentException("first and second must not be null");
        this.first = first;
        this.second = second;
    }

    @Override
    public Format[] getOutputFormats(Format input) {
        ArrayList<Format> secondOuts = new ArrayList<Format>();
        for (Format firstOut : first.getOutputFormats(input)) {
            secondOuts.addAll(Arrays.asList(second.getOutputFormats(firstOut)));
        }

        return secondOuts.toArray(new Format[secondOuts.size()]);
    }

    @Override
    public Format setInputFormat(Format input) {
        return second.setInputFormat(first.setInputFormat(input));
    }

    @Override
    public Format setOutputFormat(Format output) {
        return second.setOutputFormat(output);
    }

    @Override
    public Format getOutputFormat() {
        return second.getOutputFormat();
    }

    @Override
    public int process(Buffer in, Buffer out) {
        if (tmpBuf == null) {
            tmpBuf = new Buffer();
        }


        if (CODEC_INPUT_NOT_CONSUMED == (secondState & CODEC_INPUT_NOT_CONSUMED)) {
            // => second codec needs to process tmpBuffer again
            long start = System.currentTimeMillis();
            secondState = second.process(tmpBuf, out);
            secondElapsed += System.currentTimeMillis() - start;
            return secondState;
        }


        long start = System.currentTimeMillis();
        firstState = first.process(in, tmpBuf);
        firstElapsed += System.currentTimeMillis() - start;
        if (firstState == CODEC_FAILED) {
            return firstState;
        }
        if (CODEC_OUTPUT_NOT_FILLED == (firstState & CODEC_OUTPUT_NOT_FILLED)) {
            // => first codec needs to process tmpBuffer again
            return firstState;
        }

        start = System.currentTimeMillis();
        secondState = second.process(tmpBuf, out);
        secondElapsed += System.currentTimeMillis() - start;
        if (secondState == CODEC_FAILED) {
            return secondState;
        }

        return (secondState & (-1 ^ CODEC_INPUT_NOT_CONSUMED)) | (firstState & (-1 ^ CODEC_OUTPUT_NOT_FILLED));
    }

    @Override
    public String getName() {
        return first.getName() + ", " + second.getName();
    }

    @Override
    public String toString() {
        return "CodecChain{" + first + "," + second + "}";
    }

}
