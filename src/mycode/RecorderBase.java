package mycode;

abstract class RecorderBase implements Runnable {
    protected final ScreenVidCapture svc;

    public RecorderBase (ScreenVidCapture svc) {
        this.svc = svc;
        new Thread(this).start();
    }
}
