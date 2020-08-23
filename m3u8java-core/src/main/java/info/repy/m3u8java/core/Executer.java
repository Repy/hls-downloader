package info.repy.m3u8java.core;

/**
 *
 * @author Repy
 */
public interface Executer {

	public void start(AsyncListener listener);

	public void start();

	public void cancel();

}
