# Java downloader

1. Run maven phase "package"
2. Find jar-with-dependecies in the target directory
3. Execute jar with appropriate arguments, example: "java -jar utility.jar -n 5 -l 2000k -o output_folder -f links.txt"

# Disadvantages

1. Method bucket.consume(int amountBuckets) is synchronized, cause of that just 1 thread can access in certain period of time (it'd be great if all threads can access at the same time and if there're no free buckets they must be blocked)

2. ~~Method BufferedInputStream has inner buffer that can't be completely filled (I guess there's not enough time to fill it - need to more investigation)~~ solved

3. It's meaningless to make bufferSize > INTEGER.MAX_VALUE (need to fix it)

4. Verify if downloadable file has already exist (need to fix it)

5. May be it's better to use channels for file instead of RAF (investigate it)
