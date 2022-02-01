package mycode;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CursorPainter {
    BufferedImage cursor;
    public CursorPainter (BufferedImage cursor) {
        this.cursor = cursor;
    }

    public void paint (BufferedImage target) {
        Graphics2D g = (Graphics2D) target.getGraphics ();
        Point p = MouseInfo.getPointerInfo ().getLocation ();
        g.drawImage (cursor, p.x, p.y, null);
    }
}
