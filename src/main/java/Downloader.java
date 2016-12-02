import org.isomorphism.util.TokenBucket;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Downloader implements Runnable {

    protected List<Callable<Integer>> tasks = new ArrayList<>();

    protected Map<String, String> fileToDownload = new HashMap<>();

    protected final String destinationFolder;

    protected final int numThreads;

    protected final int downloadSpeed;

    protected final ExecutorService executor;

    protected Downloader(Map<String, String> fileToDownload, String destinationFolder, int downloadSpeed, int numThreads) {
        this.fileToDownload = fileToDownload;
        this.downloadSpeed = downloadSpeed;
        this.destinationFolder = destinationFolder;
        this.numThreads = numThreads;
        this.executor = Executors.newFixedThreadPool(numThreads);
    }

    public Map<String, String> getFileToDownload() {
        return fileToDownload;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public int getDownloadSpeed() {
        return downloadSpeed;
    }

    public String getDestinationFolder() {
        return destinationFolder;
    }

    public List<Callable<Integer>> getTasks() {
        return tasks;
    }

    protected abstract class DownloadThread implements Callable<Integer> {

        protected TokenBucket tokenBucket;

        protected Integer startByte;

        protected Integer endByte;

        protected int bufferSize;

        protected URL url;

        protected String outputFile;

        public DownloadThread(TokenBucket tokenBucket, Integer startByte, Integer endByte, URL url, String outputFile, int bufferSize) {
            this.tokenBucket = tokenBucket;
            this.startByte = startByte;
            this.endByte = endByte;
            this.url = url;
            this.bufferSize = bufferSize;
            this.outputFile = outputFile;
        }

        public TokenBucket getTokenBucket() {
            return tokenBucket;
        }

        public void setTokenBucket(TokenBucket tokenBucket) {
            this.tokenBucket = tokenBucket;
        }

        public Integer getStartByte() {
            return startByte;
        }

        public void setStartByte(Integer startByte) {
            this.startByte = startByte;
        }

        public Integer getEndByte() {
            return endByte;
        }

        public void setEndByte(Integer endByte) {
            this.endByte = endByte;
        }

        public int getBufferSize() {
            return bufferSize;
        }

        public void setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        public URL getUrl() {
            return url;
        }

        public void setUrl(URL url) {
            this.url = url;
        }
    }
}
