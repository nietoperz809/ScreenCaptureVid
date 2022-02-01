package mycode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class Tools {

    public static BufferedImage getBitmap (String name)
    {
        try {
            return ImageIO.read(Objects.requireNonNull (getResource("small.png")));
        } catch (IOException e) {
            e.printStackTrace ();
            return null;
        }
    }

    public static InputStream getResource (String name)
    {
        InputStream is = ClassLoader.getSystemResourceAsStream (name);
        if (is == null)
        {
            System.out.println ("could not load: "+name);
            return null;
        }
        return new BufferedInputStream (is);
    }

    public static BufferedImage convertBitmap (BufferedImage sourceImage, int targetType) {
        BufferedImage image;
        if (sourceImage.getType () == targetType) {
            image = sourceImage;
        } else {
            image = new BufferedImage (sourceImage.getWidth (),
                    sourceImage.getHeight (), targetType);
            Graphics2D g = (Graphics2D) image.getGraphics ();
            g.drawImage (sourceImage, 0, 0, null);
        }
        return image;
    }

    static int halfbut2 (int i) {
        int ret = i / 2;
        return ret - (ret % 2);
    }

}
