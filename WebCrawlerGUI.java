//Q.no.6 b

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawlerGUI {

    // Pattern to extract URLs from HTML content
    private static final Pattern URL_PATTERN = Pattern.compile(
            "href=\"(http[s]?://[^\"]+)\"", Pattern.CASE_INSENSITIVE);

    // Queue to store URLs with their depth
    private final ConcurrentLinkedQueue<UrlDepthPair> urlsToCrawl;

    // Set to keep track of visited URLs to avoid duplicates
    private final Set<String> visitedUrls;

    // Map to store crawled data (URL -> content)
    private final ConcurrentHashMap<String, String> crawledData;

    // Maximum depth to crawl
    private final int maxDepth;

    // Number of threads in the pool
    private final int numThreads;

    // Static inner class to pair URL with its depth
    private static class UrlDepthPair {
        final String url;
        final int depth;

        UrlDepthPair(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }

    public WebCrawlerGUI(int maxDepth, int numThreads) {
        this.urlsToCrawl = new ConcurrentLinkedQueue<>();
        this.visitedUrls = ConcurrentHashMap.newKeySet();
        this.crawledData = new ConcurrentHashMap<>();
        this.maxDepth = maxDepth;
        this.numThreads = numThreads;
    }

    public void startCrawling(String seedUrl) {
        // Add the seed URL to the queue with depth 0
        urlsToCrawl.add(new UrlDepthPair(seedUrl, 0));

        // Create a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        System.out.println("Starting crawler with seed URL: " + seedUrl);

        // Continue crawling until there are no more URLs
        while (!urlsToCrawl.isEmpty()) {
            UrlDepthPair pair = urlsToCrawl.poll();

            if (pair != null && !visitedUrls.contains(pair.url)) {
                visitedUrls.add(pair.url);

                // Submit a task to crawl this URL
                final int currentDepth = pair.depth;
                final String url = pair.url;

                executor.submit(() -> crawlUrl(url, currentDepth));
            }
        }

        // Shutdown the executor and wait for all tasks to complete
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Crawling completed. Visited " + visitedUrls.size() + " URLs");
    }

    private void crawlUrl(String url, int currentDepth) {
        try {
            System.out.println("Crawling URL: " + url + " at depth: " + currentDepth);

            // Fetch the web page content
            String content = fetchWebPage(url);

            // Store the crawled data
            crawledData.put(url, content);

            // Extract URLs from the content if we haven't reached the maximum depth
            if (currentDepth < maxDepth - 1) {
                Set<String> extractedUrls = extractUrls(content);

                // Add new URLs to the queue with incremented depth
                for (String extractedUrl : extractedUrls) {
                    if (!visitedUrls.contains(extractedUrl)) {
                        urlsToCrawl.add(new UrlDepthPair(extractedUrl, currentDepth + 1));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error crawling URL: " + url + " - " + e.getMessage());
        }
    }

    private String fetchWebPage(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Set user agent to avoid being blocked by some servers
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        // Set reasonable timeouts
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString();
        } else {
            throw new IOException("HTTP error code: " + responseCode);
        }
    }

    private Set<String> extractUrls(String content) {
        Set<String> urls = new HashSet<>();
        Matcher matcher = URL_PATTERN.matcher(content);

        while (matcher.find()) {
            String url = matcher.group(1);
            urls.add(url);
        }

        return urls;
    }

    public ConcurrentHashMap<String, String> getCrawledData() {
        return crawledData;
    }

    public static void main(String[] args) {
        // Set the maximum depth and number of threads
        int maxDepth = 2;
        int numThreads = 10;
        String seedUrl = "https://example.com";

        WebCrawlerGUI crawler = new WebCrawlerGUI(maxDepth, numThreads);
        crawler.startCrawling(seedUrl);

        // Print statistics
        ConcurrentHashMap<String, String> crawledData = crawler.getCrawledData();
        System.out.println("Total pages crawled: " + crawledData.size());

        // Print the first few characters of each page (for demo purposes)
        crawledData.forEach((url, content) -> {
            int previewLength = Math.min(content.length(), 100);
            String preview = content.substring(0, previewLength).replace("\n", " ");
            System.out.println("URL: " + url + "\nPreview: " + preview + "...\n");
        });
    }
}