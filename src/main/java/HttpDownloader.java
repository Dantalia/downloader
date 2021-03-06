import org.isomorphism.util.TokenBucket;
import org.isomorphism.util.TokenBuckets;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class HttpDownloader extends Downloader {

    public HttpDownloader(Map<String, String> fileToDownload, String destinationFolder, int downloadSpeed, int numThreads) {
        super(fileToDownload, destinationFolder, downloadSpeed, numThreads);
    }

    public void run() {
        // actually should be inside thread, and then total time have to be calculated
        long startTimeExecution = System.currentTimeMillis();

        // create output folder, if doesn't exist
        File directory = new File(String.valueOf(destinationFolder));
        if (!directory.exists()){
            directory.mkdir();
        }

        // create bucket according to the downloadSpeed
        TokenBucket bucket = TokenBuckets.builder()
                                         .withCapacity(downloadSpeed)
                                         .withFixedIntervalRefillStrategy(downloadSpeed, 1, TimeUnit.SECONDS)
                                         .build();

        // allocate appropriate buffer for each download thread (not sure that it was right decision)
        int bufferSize = (int) bucket.getCapacity() / numThreads;

        // create tasks for fixed thread executor
        for (Map.Entry<String, String> file : fileToDownload.entrySet()) {
            String path = file.getKey();
            String outputFile = file.getValue();
            HttpURLConnection connection = null;
            try {
                URL url = new URL(path);
                connection = (HttpURLConnection) url.openConnection();

                int partSize = Math.round(((float)connection.getContentLength() / numThreads));

                for (int i = 0; i < numThreads; i++) {
                    int startByte = partSize * i;
                    int endByte = partSize * (i + 1);
                    tasks.add(new HttpDownloadThread(bucket, startByte, endByte, url, outputFile, bufferSize));
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O exception with URL connection");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        int totalDownloadedBytes = 0;
        try {
            List<Future<Integer>> futures = executor.invokeAll(tasks);
            for (Future<Integer> future : futures) {
                Integer downloadedBytes = future.get();
                totalDownloadedBytes += downloadedBytes;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Thread execution has been interrupted");
        }
        executor.shutdown();

        long endTimeExecution = System.currentTimeMillis();
        System.out.println("Average time: " + (endTimeExecution - startTimeExecution) / 1000 + " sec");
        System.out.println("Downloaded bytes: " + totalDownloadedBytes);
    }

    protected class HttpDownloadThread extends DownloadThread {

        public HttpDownloadThread(TokenBucket tokenBucket, Integer startByte, Integer endByte, URL url, String outputFile, int bufferSize) {
            super(tokenBucket, startByte, endByte, url, outputFile, bufferSize);
        }

        public Integer call() {
            // this variable is initialized for calculation how many bytes were downloaded
            int startPosition = startByte;
            HttpURLConnection connection = null;
            try {
                 connection = (HttpURLConnection) url.openConnection();

                // set download range of bytes
                String rangeBytes = startByte + "-" + endByte;
                connection.setRequestProperty("Range", "bytes=" + rangeBytes);

                connection.connect();

                // check response code
                if (connection.getResponseCode() / 100 != 2) {
                    throw new RuntimeException("Invalid response from server");
                }

                try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                     RandomAccessFile raf = new RandomAccessFile(destinationFolder + File.separator + outputFile, "rw");) {
                    // seek for a start position
                    raf.seek(startByte);

                    // write data
                    int numRead;
                    byte data[] = new byte[bufferSize];
                    while ((numRead = readFully(in, data, 0, bufferSize)) != 0) {
                        tokenBucket.consume(bufferSize);
                        startByte += numRead;
                        raf.write(data, 0, numRead);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("I/O exception inside thread");
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O exception inside thread (URL connection problem)");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return startByte - startPosition;
        }
    }

    // since bufferInputStream can't completely fill inside buffer, this method helps to fill it fully and then write to file
    // cause of this method, can avoid situation when bufferInputStream read 1,2,3..bytes
    public final int readFully(InputStream in, byte b[], int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0) {
                return n;
            }
            n += count;
        }
        return n;
    }
}
