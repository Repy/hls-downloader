package info.repy.m3u8java.core;

/**
 * @author Repy
 */
public interface AsyncListener {

    public void progress(double now, double max);

    public void complete();

    public void exception(Exception e);
}
