package mycode;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Tools {

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
