import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Problem 5: Real-Time Analytics Dashboard for Website Traffic
 * Concepts: Frequency counting, multiple hash tables, load factor under high throughput
 */
public class Problem5_AnalyticsDashboard {

    // page URL → total visit count
    private final ConcurrentHashMap<String, Long> pageViewCounts = new ConcurrentHashMap<>();

    // page URL → unique visitor set
    private final ConcurrentHashMap<String, Set<String>> uniqueVisitors = new ConcurrentHashMap<>();

    // traffic source → count
    private final ConcurrentHashMap<String, Long> sourceCount = new ConcurrentHashMap<>();

    // Event buffer for batch processing
    private final List<PageViewEvent> eventBuffer = Collections.synchronizedList(new ArrayList<>());

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private long totalEvents = 0;

    // Page view event
    record PageViewEvent(String url, String userId, String source, long timestamp) {}

    public Problem5_AnalyticsDashboard() {
        // Process buffered events every 5 seconds
        scheduler.scheduleAtFixedRate(this::flushEvents, 5, 5, TimeUnit.SECONDS);
    }

    // Process a single event immediately (or buffer it)
    public void processEvent(String url, String userId, String source) {
        eventBuffer.add(new PageViewEvent(url, userId, source, System.currentTimeMillis()));
        totalEvents++;
    }

    // Flush and process buffered events
    private void flushEvents() {
        List<PageViewEvent> batch;
        synchronized (eventBuffer) {
            if (eventBuffer.isEmpty()) return;
            batch = new ArrayList<>(eventBuffer);
            eventBuffer.clear();
        }

        for (PageViewEvent event : batch) {
            // Update page view count
            pageViewCounts.merge(event.url(), 1L, Long::sum);

            // Update unique visitors (using a ConcurrentHashMap-backed Set)
            uniqueVisitors.computeIfAbsent(event.url(),
                    k -> ConcurrentHashMap.newKeySet()).add(event.userId());

            // Update source count
            sourceCount.merge(event.source(), 1L, Long::sum);
        }

        System.out.println("[Dashboard] Flushed " + batch.size() + " events.");
    }

    // Force flush for testing purposes
    public void forceFlush() {
        flushEvents();
    }

    // Get top N most visited pages
    public List<Map.Entry<String, Long>> getTopPages(int n) {
        return pageViewCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    // Get unique visitor count for a page
    public int getUniqueVisitors(String url) {
        return uniqueVisitors.getOrDefault(url, Collections.emptySet()).size();
    }

    // Get source distribution as percentages
    public Map<String, Double> getSourceDistribution() {
        long total = sourceCount.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Double> distribution = new LinkedHashMap<>();
        sourceCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> distribution.put(e.getKey(), e.getValue() * 100.0 / total));
        return distribution;
    }

    // Print full dashboard
    public void getDashboard() {
        System.out.println("\n=== Live Dashboard ===");
        System.out.println("Total Events Processed: " + totalEvents);

        System.out.println("\nTop Pages:");
        List<Map.Entry<String, Long>> topPages = getTopPages(10);
        for (int i = 0; i < topPages.size(); i++) {
            Map.Entry<String, Long> entry = topPages.get(i);
            int unique = getUniqueVisitors(entry.getKey());
            System.out.printf("  %d. %s - %,d views (%,d unique)%n",
                    i + 1, entry.getKey(), entry.getValue(), unique);
        }

        System.out.println("\nTraffic Sources:");
        getSourceDistribution().forEach((source, pct) ->
                System.out.printf("  %s: %.1f%%%n", source, pct));
    }

    public void shutdown() {
        forceFlush();
        scheduler.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Problem 5: Real-Time Analytics Dashboard ===\n");

        Problem5_AnalyticsDashboard dashboard = new Problem5_AnalyticsDashboard();

        String[] pages = {
                "/article/breaking-news", "/sports/championship", "/tech/java-tutorial",
                "/politics/election", "/health/nutrition", "/business/stocks"
        };
        String[] sources = {"google", "direct", "facebook", "twitter", "other"};
        String[] sourceWeights = {"google", "google", "google", "direct", "direct",
                "facebook", "twitter", "other"}; // weighted distribution

        Random rand = new Random(42);

        // Simulate 1 million-scale events (we'll do 50,000 for demo speed)
        System.out.println("Simulating 50,000 page view events...");
        for (int i = 0; i < 50000; i++) {
            String page = pages[rand.nextInt(pages.length)];
            String userId = "user_" + rand.nextInt(8000); // ~8000 unique users
            String source = sourceWeights[rand.nextInt(sourceWeights.length)];
            dashboard.processEvent(page, userId, source);
        }

        // Explicit events matching sample output
        dashboard.processEvent("/article/breaking-news", "user_123", "google");
        dashboard.processEvent("/article/breaking-news", "user_456", "facebook");

        dashboard.forceFlush();
        dashboard.getDashboard();
        dashboard.shutdown();
    }
}
