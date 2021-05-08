package info.repy.m3u8java.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTP extends AbstractExecuter {

    public HTTP(String url, String savefilename) {
        super(url, savefilename);
    }

    public HTTP(String url, OutputStream stream) {
        super(url, stream);
    }

    @Override
    protected void run(AsyncListener listener) {
        if (status != Status.NONE) return;
        status = Status.RUNNING;
        HttpURLConnection uc = null;
        try {
            uc = (HttpURLConnection) new URL(url).openConnection();
            int count = 0;
            int allsize = uc.getContentLength();
            try (InputStream fis = uc.getInputStream(); BufferedInputStream is = new BufferedInputStream(fis); OutputStream fileStream = this.output;) {
                byte[] buf = new byte[1024 * 1024];
                int bytesRead;
                listener.progress((count / 1024 / 1024), (allsize / 1024 / 1024));
                while ((bytesRead = is.read(buf)) >= 0) {
                    fileStream.write(buf, 0, bytesRead);
                    count += bytesRead;
                    listener.progress((count / 1024 / 1024), (allsize / 1024 / 1024));
                }
                listener.progress((count / 1024 / 1024), (allsize / 1024 / 1024));
                status = Status.COMPLETE;
            } catch (IOException ex) {
                status = Status.ERROR;
            }
        } catch (IOException ex) {
            status = Status.ERROR;
        } finally {
            if (uc != null) uc.disconnect();
        }
        listener.complete();
    }

}
