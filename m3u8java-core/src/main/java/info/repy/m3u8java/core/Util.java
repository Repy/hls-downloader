package info.repy.m3u8java.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * @author gamec
 */
public class Util {

    public static String filename(String str) {
        str = str.replace('\\', '-');
        str = str.replace('/', '-');
        str = str.replace('?', ' ');
        str = str.replace(':', ' ');
        str = str.replace('*', ' ');
        str = str.replace('"', ' ');
        str = str.replace('>', ' ');
        str = str.replace('<', ' ');
        str = str.replace('|', ' ');
        return str;
    }

    private static final HttpClient client = HttpClient.newBuilder().build();
    public static InputStream http(URL url, int timeout, String referer) throws IOException, URISyntaxException, InterruptedException {
        String origin = null;
        if (referer != null) {
            try {
                URL u = new URL(referer);
                origin = u.getProtocol() + "://" + u.getHost();
            } catch (MalformedURLException e) {
            }
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(url.toURI());
        builder = builder.header("User-Agent", "Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53");
        builder = builder.header("Accept", "*/*");
        if (origin != null) builder = builder.header("Origin", origin);
        if (referer != null) builder = builder.header("Referer", referer);
        HttpRequest request = builder.timeout(Duration.ofSeconds(timeout)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
    }

}
