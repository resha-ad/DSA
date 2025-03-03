// import javax.swing.*;
// import java.awt.*;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
// import java.util.*;
// import java.util.concurrent.*;
// import java.io.IOException;
// import org.jsoup.Jsoup;
// import org.jsoup.nodes.Document;
// import org.jsoup.nodes.Element;
// import org.jsoup.select.Elements;

// public class WebCrawlerGUI extends JFrame {

//     // Thread-safe queue to store URLs to be crawled
//     private final Queue<String> urlQueue = new ConcurrentLinkedQueue<>();
//     // Thread-safe set to store visited URLs
//     private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
//     // Thread pool for managing multiple threads
//     private final ExecutorService executorService;

//     // GUI Components
//     private JTextArea outputArea;
//     private JTextField urlField;
//     private JButton startButton;

//     public WebCrawlerGUI(int threadPoolSize) {
//         this.executorService = Executors.newFixedThreadPool(threadPoolSize);

//         // Set up the GUI
//         setTitle("Multithreaded Web Crawler");
//         setSize(600, 400);
//         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//         setLayout(new BorderLayout());

//         // Input panel
//         JPanel inputPanel = new JPanel();
//         inputPanel.setLayout(new FlowLayout());
//         inputPanel.add(new JLabel("Enter Seed URL:"));
//         urlField = new JTextField(30);
//         inputPanel.add(urlField);
//         startButton = new JButton("Start Crawling");
//         inputPanel.add(startButton);

//         // Output area
//         outputArea = new JTextArea();
//         outputArea.setEditable(false);
//         JScrollPane scrollPane = new JScrollPane(outputArea);

//         // Add components to the frame
//         add(inputPanel, BorderLayout.NORTH);
//         add(scrollPane, BorderLayout.CENTER);

//         // Add action listener to the start button
//         startButton.addActionListener(new ActionListener() {
//             @Override
//             public void actionPerformed(ActionEvent e) {
//                 String seedUrl = urlField.getText().trim();
//                 if (!seedUrl.isEmpty()) {
//                     startCrawling(Arrays.asList(seedUrl));
//                 } else {
//                     outputArea.append("Please enter a valid seed URL.\n");
//                 }
//             }
//         });
//     }

//     // Method to start crawling
//     public void startCrawling(List<String> seedUrls) {
//         // Add seed URLs to the queue
//         urlQueue.addAll(seedUrls);

//         // Submit tasks to the thread pool
//         while (!urlQueue.isEmpty()) {
//             String url = urlQueue.poll();
//             if (!visitedUrls.contains(url)) {
//                 visitedUrls.add(url);
//                 executorService.submit(() -> crawlPage(url));
//             }
//         }

//         // Shutdown the thread pool after all tasks are completed
//         executorService.shutdown();
//         try {
//             executorService.awaitTermination(1, TimeUnit.MINUTES);
//         } catch (InterruptedException e) {
//             outputArea.append("Thread pool interrupted: " + e.getMessage() + "\n");
//         }
//     }

//     // Method to crawl a single web page
//     private void crawlPage(String url) {
//         try {
//             outputArea.append("Crawling: " + url + "\n");
//             Document document = Jsoup.connect(url).get();

//             // Process the page content (e.g., extract data or index content)
//             processPageContent(document);

//             // Extract links from the page and add them to the queue
//             Elements links = document.select("a[href]");
//             for (Element link : links) {
//                 String nextUrl = link.absUrl("href");
//                 if (!visitedUrls.contains(nextUrl)) {
//                     urlQueue.add(nextUrl);
//                 }
//             }
//         } catch (IOException e) {
//             outputArea.append("Error crawling " + url + ": " + e.getMessage() + "\n");
//         }
//     }

//     // Method to process the content of a web page
//     private void processPageContent(Document document) {
//         // Example: Extract and print the title of the page
//         String title = document.title();
//         outputArea.append("Title: " + title + "\n");

//         // Example: Extract and print all paragraphs
//         Elements paragraphs = document.select("p");
//         for (Element paragraph : paragraphs) {
//             outputArea.append("Paragraph: " + paragraph.text() + "\n");
//         }
//     }

//     public static void main(String[] args) {
//         // Run the GUI on the Event Dispatch Thread
//         SwingUtilities.invokeLater(() -> {
//             WebCrawlerGUI crawlerGUI = new WebCrawlerGUI(5); // Thread pool size of 5
//             crawlerGUI.setVisible(true);
//         });
//     }
// }