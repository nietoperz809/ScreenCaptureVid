/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xuggletest;

import java.io.File;


import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaViewer;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;

import static java.lang.System.out;
import static java.lang.System.exit;

/** 
 * A very simple media transcoder which uses {@link IMediaReader}, {@link
 * IMediaWriter} and {@link IMediaViewer}.
 */

public class TranscodeAudioAndVideo
{
  /**
   * Transcodes a media file into a new media file, guessing parameters 
   * and codecs
   * based on the file names.
   * @param args 2 strings; an input file and an output file.
   */
  public static void main(String[] args)
  {
    if (args.length < 2)
    {
      out.println("To perform a simple media transcode.  The destination " +
        "format will be guessed from the file extention.");
      out.println("");
      out.println("   TranscodeAudioAndVideo <source-file> <destination-file>");
      out.println("");
      out.println(
        "The destination type will be guess from the supplied file extsion.");
      exit(0);
    }

    File source = new File(args[0]);
    if (!source.exists())
    {
      out.println("Source file does not exist: " + source);
      exit(0);
    }

    transcode(args[0], args[1]);
  }

  /**
   * Transcode a source url to a destination url.  Really.  That's
   * all this does.
   */

  public static void transcode(String sourceUrl, String destinationUrl)
  {
    out.printf("transcode %s -> %s\n", sourceUrl, destinationUrl);

    // create the media reader, not that no BufferedImages need to be
    // created because the video is not going to be manipulated

    IMediaReader reader = ToolFactory.makeReader(sourceUrl);

    // add a viewer to the reader, to see progress as the media is
    // transcoded

    reader.addListener(ToolFactory.makeViewer(true));

    // create the media writer
    reader.addListener(ToolFactory.makeWriter(destinationUrl, reader));

    // read packets from the source file, which dispatch events to the
    // writer, this will continue until 

    while (reader.readPacket() == null)
      do {} while(false);
  }
}