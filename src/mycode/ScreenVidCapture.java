package mycode;

import com.xuggle.mediatool.ToolFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class ScreenVidCapture {
    final Rectangle screenRect = new Rectangle (Toolkit.getDefaultToolkit ().getScreenSize ());
    final Robot robot = new Robot ();
    final AtomicReference<STATE> state = new AtomicReference<> (STATE.IDLE);
    public JButton startButton;
    public JButton stopButton;
    int imageCount;
    String filename;
    private JPanel mainPanel;
    protected JLabel label;
    protected JTextField outputPath;
    private JRadioButton MOVRadioButton;

    /**
     * Constructor
     * @throws Exception ifs smth gone wrong
     */
    public ScreenVidCapture () throws Exception {
        stopButton.setEnabled (false);
        ToolFactory.setTurboCharged (true);
        outputPath.setText (System.getProperty ("user.home") + File.separator + "Videos");
        outputPath.setToolTipText ("Drop path here ...");
        outputPath.setDropTarget (new DropTarget () {
            public synchronized void drop (DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop (DnDConstants.ACTION_COPY);
                    String dropped = evt.getTransferable ().
                            getTransferData (DataFlavor.javaFileListFlavor).
                            toString ();
                    dropped = dropped.substring(1, dropped.length() - 1);
                    outputPath.setText (dropped);
                } catch (Exception ex) {
                    ex.printStackTrace ();
                }
            }
        });

        startButton.addActionListener (e -> {
            System.out.println ("Start");
            stopButton.setEnabled (true);
            startButton.setEnabled (false);
            filename = outputPath.getText () + File.separator +
                    System.currentTimeMillis () + ".mp4";
            imageCount = 0;
            state.set (STATE.START_RECORDING);
            if (MOVRadioButton.isSelected ()) {
                new MOVRecorder (this);
            } else {
                new H264Recorder (this);
            }
        });

        stopButton.addActionListener (e -> {
            System.out.println ("Stop");
            stopButton.setEnabled (false);
            state.set (STATE.FINISH_RECORDING);
        });
    }

    public static void main (String[] args) throws Exception {
        JFrame frame = new JFrame ("Tester");
        ScreenVidCapture svc = new ScreenVidCapture ();
        frame.addWindowListener (new WindowAdapter () {
            @Override
            public void windowClosing (WindowEvent e) {
                if (svc.state.get () == STATE.DO_RECORDING) {
                    svc.state.set (STATE.FINISH_RECORDING);
                    while (svc.state.get () != STATE.IDLE) {
                        try {
                            Thread.sleep (100);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace ();
                        }
                    }
                }
            }
        });
        frame.setContentPane (svc.mainPanel);
        frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
        frame.pack ();
        frame.setResizable (false);
        frame.setVisible (true);
    }

    private void createUIComponents () {
        // TODO: place custom component creation code here
    }

    enum STATE {IDLE, START_RECORDING, DO_RECORDING, FINISH_RECORDING}
}
