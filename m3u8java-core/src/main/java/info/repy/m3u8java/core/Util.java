package info.repy.m3u8java.core;

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
}
