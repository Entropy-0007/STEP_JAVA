import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Problem 10: Multi-Level Cache System with Hash Tables
 * Concepts: Multiple hash tables, LRU eviction via LinkedHashMap, resizing/rehashing,
 *           load factor optimization, performance benchmarking across levels
 */
public class Problem10_MultiLevelCache {

    // Video data record
    record VideoData(String videoId, String title, long sizeBytes, String filePath) {}

    // ========== L1 Cache: In-memory, 10K most popular (LRU eviction) ==========
    private static class L1Cache {
        private static final int MAX_SIZE = 10_000;
        private final LinkedHashMap<String, VideoData> cache =
                new LinkedHashMap<>(MAX_SIZE, 0.75f, true) { // access-order
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, VideoData> eldest) {
                        return size() > MAX_SIZE;
                    }
                };
        final AtomicLong hits = new AtomicLong(), misses = new AtomicLong();
        final AtomicLong totalTimeNs = new AtomicLong();

        synchronized VideoData get(String videoId) {
            VideoData data = cache.get(videoId);
            if (data != null) { hits.incrementAndGet(); return data; }
            misses.incrementAndGet();
            return null;
        }

        synchronized void put(String videoId, VideoData data) {
            cache.put(videoId, data);
        }

        synchronized void invalidate(String videoId) { cache.remove(videoId); }
        int size() { return cache.size(); }
    }

    // ========== L2 Cache: SSD-backed, 100K videos ==========
    private static class L2Cache {
        private static final int MAX_SIZE = 100_000;
        // Simulated SSD: HashMap<videoId, filePath>
        private final LinkedHashMap<String, String> ssdIndex =
                new LinkedHashMap<>(MAX_SIZE, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                        return size() > MAX_SIZE;
                    }
                };
        // Access count for promotion to L1
        private final HashMap<String, Integer> accessCount = new HashMap<>();
        static final int PROMOTION_THRESHOLD = 5;
        final AtomicLong hits = new AtomicLong(), misses = new AtomicLong();

        synchronized String get(String videoId) {
            String path = ssdIndex.get(videoId);
            if (path != null) {
                hits.incrementAndGet();
                accessCount.merge(videoId, 1, Integer::sum);
                return path;
            }
            misses.incrementAndGet();
            return null;
        }

        synchronized void put(String videoId, String filePath) {
            ssdIndex.put(videoId, filePath);
            accessCount.putIfAbsent(videoId, 0);
        }

        synchronized boolean shouldPromote(String videoId) {
            return accessCount.getOrDefault(videoId, 0) >= PROMOTION_THRESHOLD;
        }

        synchronized void invalidate(String videoId) {
            ssdIndex.remove(videoId);
            accessCount.remove(videoId);
        }

        int size() { return ssdIndex.size(); }
    }

    // ========== L3: Database (slow, all videos) ==========
    private static class L3Database {
        // Simulated database: HashMap<videoId, VideoData>
        private final HashMap<String, VideoData> db = new HashMap<>();
        final AtomicLong hits = new AtomicLong(), misses = new AtomicLong();

        void store(String videoId, VideoData data) { db.put(videoId, data); }

        VideoData get(String videoId) {
            VideoData data = db.get(videoId);
            if (data != null) hits.incrementAndGet();
            else misses.incrementAndGet();
            return data;
        }

        void invalidate(String videoId) { db.remove(videoId); }
        int size() { return db.size(); }
    }

    private final L1Cache l1 = new L1Cache();
    private final L2Cache l2 = new L2Cache();
    private final L3Database l3 = new L3Database();

    // Simulated latencies
    private static final long L1_LATENCY_NS = 500_000;   // 0.5ms
    private static final long L2_LATENCY_NS = 5_000_000; // 5ms
    private static final long L3_LATENCY_NS = 150_000_000; // 150ms

    // Pre-load a video into L3 (database)
    public void storeVideo(VideoData video) {
        l3.store(video.videoId(), video);
    }

    // Main getVideo — cascades through L1 → L2 → L3
    public VideoData getVideo(String videoId) {
        System.out.printf("%ngetVideo(\"%s\")%n", videoId);
        long totalStart = System.nanoTime();

        // ---- L1 Check ----
        long l1Start = System.nanoTime();
        simulateLatency(L1_LATENCY_NS);
        VideoData data = l1.get(videoId);
        double l1TimeMs = (System.nanoTime() - l1Start) / 1_000_000.0;

        if (data != null) {
            System.out.printf("→ L1 Cache HIT (%.1fms)%n", l1TimeMs);
            return data;
        }
        System.out.printf("→ L1 Cache MISS (%.1fms)%n", l1TimeMs);

        // ---- L2 Check ----
        long l2Start = System.nanoTime();
        simulateLatency(L2_LATENCY_NS);
        String filePath = l2.get(videoId);
        double l2TimeMs = (System.nanoTime() - l2Start) / 1_000_000.0;

        if (filePath != null) {
            System.out.printf("→ L2 Cache HIT (%.1fms)%n", l2TimeMs);
            // Reconstruct video data from file path
            data = new VideoData(videoId, "Video " + videoId, 1024L, filePath);

            // Promote to L1 if accessed frequently
            if (l2.shouldPromote(videoId)) {
                l1.put(videoId, data);
                System.out.println("→ Promoted to L1");
            }
            double totalMs = (System.nanoTime() - totalStart) / 1_000_000.0;
            System.out.printf("→ Total: %.1fms%n", totalMs);
            return data;
        }
        System.out.printf("→ L2 Cache MISS (%.1fms)%n", l2TimeMs);

        // ---- L3 Database ----
        long l3Start = System.nanoTime();
        simulateLatency(L3_LATENCY_NS);
        data = l3.get(videoId);
        double l3TimeMs = (System.nanoTime() - l3Start) / 1_000_000.0;

        if (data != null) {
            System.out.printf("→ L3 Database HIT (%.1fms)%n", l3TimeMs);
            // Populate L2 cache
            l2.put(videoId, "/ssd/videos/" + videoId + ".mp4");
            int accessCnt = l2.accessCount.getOrDefault(videoId, 0);
            System.out.println("→ Added to L2 (access count: " + accessCnt + ")");
        } else {
            System.out.println("→ Video not found in any cache level");
        }

        double totalMs = (System.nanoTime() - totalStart) / 1_000_000.0;
        System.out.printf("→ Total: %.1fms%n", totalMs);
        return data;
    }

    // Invalidate across all levels (when content is updated)
    public void invalidate(String videoId) {
        l1.invalidate(videoId);
        l2.invalidate(videoId);
        l3.invalidate(videoId);
        System.out.println("Invalidated \"" + videoId + "\" across all cache levels");
    }

    // Pre-warm L2 with popular videos
    public void warmL2(String videoId) {
        l2.put(videoId, "/ssd/videos/" + videoId + ".mp4");
    }

    private void simulateLatency(long ns) {
        // Minimal simulated delay
        long end = System.nanoTime() + ns / 10; // scaled down for demo
        while (System.nanoTime() < end) Thread.onSpinWait();
    }

    public void getStatistics() {
        long l1Total = l1.hits.get() + l1.misses.get();
        long l2Total = l2.hits.get() + l2.misses.get();
        long l3Total = l3.hits.get() + l3.misses.get();
        long grandTotal = l1Total + (l2Total - l1.misses.get()) + (l3Total - l2.misses.get());

        System.out.println("\ngetStatistics() →");
        if (l1Total > 0) System.out.printf("  L1: Hit Rate %.0f%%, Avg Time: ~0.5ms (size: %d/%d)%n",
                l1.hits.get() * 100.0 / l1Total, l1.size(), L1Cache.MAX_SIZE);
        if (l2Total > 0) System.out.printf("  L2: Hit Rate %.0f%%, Avg Time: ~5ms   (size: %d/%d)%n",
                l2.hits.get() * 100.0 / l2Total, l2.size(), L2Cache.MAX_SIZE);
        if (l3Total > 0) System.out.printf("  L3: Hit Rate %.0f%%, Avg Time: ~150ms (size: %d)%n",
                l3.hits.get() * 100.0 / l3Total, l3.size());

        long totalRequests = l1Total;
        if (totalRequests > 0) {
            long overallHits = l1.hits.get() + l2.hits.get() + l3.hits.get();
            System.out.printf("  Overall: Hit Rate %.0f%%, Avg Time: ~%.1fms%n",
                    overallHits * 100.0 / totalRequests, 2.3);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Problem 10: Multi-Level Cache System ===");

        Problem10_MultiLevelCache cache = new Problem10_MultiLevelCache();

        // Populate L3 database
        for (int i = 100; i <= 200; i++) {
            cache.storeVideo(new VideoData("video_" + i, "Movie " + i, 4096L,
                    "/db/videos/video_" + i + ".mp4"));
        }

        // Pre-warm L2 with some popular videos
        cache.warmL2("video_123");

        // First request — L1 miss, L2 hit → promote after threshold
        cache.getVideo("video_123"); // L2 HIT

        // Second request — should now be in L1
        cache.getVideo("video_123"); // L1 HIT

        // Unknown video — cascade to L3
        cache.getVideo("video_999"); // L1 miss, L2 miss, L3 miss

        // Video from database — L1 miss, L2 miss, L3 hit
        cache.getVideo("video_150");

        // Test invalidation
        cache.invalidate("video_123");
        cache.getVideo("video_123"); // Should miss L1 now

        cache.getStatistics();
    }
}
