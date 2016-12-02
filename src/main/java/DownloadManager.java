import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DownloadManager {

    private static Options options = new Options();

    static {
        options.addOption(OptionBuilder
                                    .withArgName("numThreads")
                                    .withDescription("number of threads")
                                    .isRequired()
                                    .withType(Number.class)
                                    .hasArgs(1)
                                    .create("n"));
        options.addOption(OptionBuilder
                                    .withArgName("limit")
                                    .withDescription("limit of download speed")
                                    .isRequired()
                                    .hasArgs(1)
                                    .create("l"));
        options.addOption(OptionBuilder
                                    .withArgName("links")
                                    .withDescription("file with list of links")
                                    .isRequired()
                                    .hasArgs(1)
                                    .create("f"));
        options.addOption(OptionBuilder
                                    .withArgName("outputFolder")
                                    .withDescription("name of folder")
                                    .isRequired()
                                    .hasArgs(1)
                                    .create("o"));
    }

    public static void main(String[] args) throws ParseException {
        List<String> linksFromFile;
        int downloadSpeed = 0;
        int numThreads = 0;

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("help", options );

        CommandLine cmd;
        CommandLineParser parser = new BasicParser();
            cmd = parser.parse(options, args);

            try {
                numThreads = Integer.parseInt(cmd.getOptionValue("n"));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid argument for -n (number of download threads)");
            }

            String bandwidth = cmd.getOptionValue("l");
            try {
                if (bandwidth.endsWith("k")) {
                    downloadSpeed = Integer.parseInt(bandwidth.substring(0, bandwidth.length() - 1)) * Bandwidth.kiB.getBandwidth();
                } else if (bandwidth.endsWith("m")) {
                    downloadSpeed = Integer.parseInt(bandwidth.substring(0, bandwidth.length() - 1)) * Bandwidth.miB.getBandwidth();
                } else {
                    throw new RuntimeException("Please specify suffix for download speed (k,m)");
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid argument for -l (limit of download speed, should be a number)");
            }

            String fileWithLinksPath = cmd.getOptionValue("f");
            String destinationFolder = cmd.getOptionValue("o");
            Path fileWithLinks = Paths.get(fileWithLinksPath);
            try {
                linksFromFile = Files.readAllLines(fileWithLinks);
            } catch (IOException  e) {
                throw new RuntimeException("Probably file doesn't exist");
            }
            Map<String, String> fileToDownload = linksFromFile.stream()
                                                        .map(path -> path.split(" "))
                                                        .collect(Collectors.toMap(path -> path[0],
                                                                                  path -> path[1],
                                                                                  (path1, path2) -> path1));

            HttpDownloader httpDownloader = new HttpDownloader(fileToDownload, destinationFolder, downloadSpeed, numThreads);
            Thread thread = new Thread(httpDownloader);
            thread.start();
    }

    private enum Bandwidth {

        kiB(1024), miB(1024*1024);

        int bandwidth;

        Bandwidth(int bandwidth) {
            this.bandwidth = bandwidth;
        }

        public int getBandwidth() {
            return bandwidth;
        }

        public void setBandwidth(int bandwidth) {
            this.bandwidth = bandwidth;
        }
    }
}
