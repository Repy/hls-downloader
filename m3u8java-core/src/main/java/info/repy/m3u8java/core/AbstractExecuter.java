package info.repy.m3u8java.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

public abstract class AbstractExecuter implements Executer {

	protected final String url;
	protected final OutputStream output;
	protected Status status = Status.NONE;

	public AbstractExecuter(String url, String savefilename) {
		try {
			this.url = url;
			File file = new File(savefilename).getAbsoluteFile();
			file.getParentFile().mkdirs();
			this.output =  new FileOutputStream(file);
		} catch (FileNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

	public AbstractExecuter(String url, OutputStream stream) {
		this.url = url;
		this.output = stream;
	}

	@Override
	public void start(AsyncListener listener) {
		if (status != Status.NONE) {
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				AbstractExecuter.this.run(listener);
			}
		}).start();
	}

	@Override
	public void start() {
		this.run(new AsyncListenerImpl());
	}

	@Override
	public void cancel() {
		this.status = Status.CANCEL;
	}

	protected abstract void run(AsyncListener listener);
}
