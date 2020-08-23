package info.repy.m3u8java.core;

public class AsyncListenerImpl implements AsyncListener {

	@Override
	public void progress(double now, double max) {
	}

	@Override
	public void complete() {
	}

	@Override
	public void exception(Exception e) {
		e.printStackTrace(System.err);
	}
}
