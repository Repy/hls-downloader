package info.repy.m3u8java.cui;

import info.repy.m3u8java.core.Executer;
import info.repy.m3u8java.core.M3U8;
import info.repy.m3u8java.core.Status;
import org.apache.commons.cli.*;

public class CUI {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder().argName("u").longOpt("url").hasArg().desc("HLS URL").required().build());
        options.addOption(Option.builder().argName("f").longOpt("file").hasArg().desc("file name").required().build());

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("m3u8java-cui", options);
            System.exit(1);
            return;
        }

        String url = cmd.getOptionValue("url");
        String file = cmd.getOptionValue("file");

        Executer m3u8 = new M3U8(url, file);
        m3u8.start();
        if (m3u8.getStatus() == Status.ERROR) System.exit(1);
    }
}
