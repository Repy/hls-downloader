package info.repy.m3u8java.gui;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.*;

import info.repy.m3u8java.core.AsyncListener;
import info.repy.m3u8java.core.Executer;
import info.repy.m3u8java.core.M3U8;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import info.repy.m3u8java.core.*;

/**
 * FXML Controller class
 *
 * @author repy
 */
public class FormController implements Initializable {

	@FXML
	private ProgressBar progressBar;
	@FXML
	private TextField urlUrl;
	@FXML
	private TextField urlFilename;
	@FXML
	private Label progressLabel;

	@Override
	public void initialize(URL url, ResourceBundle rb) {

	}

	private double progMax = 0;
	private double progCount = 0;

	//スレッドプール
	private final ExecutorService thread = new ThreadPoolExecutor(0, 5, 100, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

	private void addProgress(double count) {
		if (progMax == progCount) {
			progMax = 0;
			progCount = 0;
		}
		progMax += count;
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				progressBar.setProgress(1.0 * progCount / progMax);
				progressLabel.setText(progCount + "/" + progMax);
			}
		});
	}

	private void finishProgress(double count) {
		progCount += count;
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				progressBar.setProgress(1.0 * progCount / progMax);
				progressLabel.setText(progCount + "/" + progMax);
			}
		});
	}

	@FXML
	private void urlDownloadAction(ActionEvent event) {
		Executer m3u8 = new M3U8(urlUrl.getText(), urlFilename.getText() + ".ts");
		urlUrl.clear();
		urlFilename.clear();
		m3u8.start(new GUIM3U8Listener(this));
	}

	private static class GUIM3U8Listener implements AsyncListener {

		private boolean first = true;
		private double now = 0.0;
		private double max = 0.0;
		private final FormController c;

		private GUIM3U8Listener(FormController aThis) {
			c = aThis;
		}

		@Override
		public void progress(double next, double max) {
			if (first) {
				this.first = false;
				this.max = max;
				this.now = 0.0;
				c.addProgress(this.max);
				c.finishProgress(next - this.now);
			} else {
				c.finishProgress(next - this.now);
				this.now = next;
			}
		}

		@Override
		public void complete() {
			c.finishProgress(this.max - this.now);
		}

		@Override
		public void exception(Exception e) {

		}
	}

	private static class NameValue {

		private final String name;
		private final String value;

		public NameValue(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return name;
		}

		public String getValue() {
			return value;
		}
	}

}
