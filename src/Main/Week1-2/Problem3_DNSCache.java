import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Problem 3: DNS Cache with TTL (Time To Live)
 * Concepts: Hash table with custom Entry class, chaining, time-based ops, LRU eviction
 */
public class Problem3_DNSCache {

    // DNS Entry stores domain, IP, timestamps
    private static class DNSEntry {
        String domain;
        String ipAddress;
        long insertedAt;   // ms
        long expiryTime;   // ms

        DNSEntry(String domain, String ipAddress, long ttlSeconds) {
            this.domain = domain;
            this.ipAddress = ipAddress;
            this.insertedAt = System.currentTimeMillis();
            this.expiryTime = insertedAt + ttlSeconds * 1000;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private final int maxCapacity;
    // LinkedHashMap in access-order for LRU eviction
    private final LinkedHashMap<String, DNSEntry> cache;
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong totalLookupTimeNs = new AtomicLong(0);
    private final AtomicLong lookupCount = new AtomicLong(0);

    // Simulated upstream DNS (domain → IP)
    private final Map<String, String> upstreamDNS = new HashMap<>();

    public Problem3_DNSCache(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        // Access-order LinkedHashMap for LRU eviction
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
                return size() > maxCapacity;
            }
        };
    }

    // Register upstream DNS mapping
    public void registerUpstream(String domain, String ip) {
        upstreamDNS.put(domain, ip);
    }

    // Main resolve method
    public String resolve(String domain) {
        long start = System.nanoTime();

        synchronized (cache) {
            DNSEntry entry = cache.get(domain);

            if (entry != null && !entry.isExpired()) {
                hits.incrementAndGet();
                long elapsed = System.nanoTime() - start;
                totalLookupTimeNs.addAndGet(elapsed);
                lookupCount.incrementAndGet();
                System.out.printf("resolve(\"%s\") → Cache HIT → %s (%.2fms)%n",
                        domain, entry.ipAddress, elapsed / 1_000_000.0);
                return entry.ipAddress;
            }

            // Expired or missing
            boolean expired = (entry != null);
            misses.incrementAndGet();

            // Query upstream DNS
            String ip = upstreamDNS.getOrDefault(domain, "0.0.0.0");
            long defaultTTL = 300L; // 300 seconds default TTL
            DNSEntry newEntry = new DNSEntry(domain, ip, defaultTTL);
            cache.put(domain, newEntry);

            long elapsed = System.nanoTime() - start;
            totalLookupTimeNs.addAndGet(elapsed);
            lookupCount.incrementAndGet();

            System.out.printf("resolve(\"%s\") → Cache %s → Query upstream → %s (TTL: %ds, %.2fms)%n",
                    domain, expired ? "EXPIRED" : "MISS", ip, defaultTTL, elapsed / 1_000_000.0);
            return ip;
        }
    }

    // Manually expire an entry (for testing)
    public void forceExpire(String domain) {
        synchronized (cache) {
            DNSEntry entry = cache.get(domain);
            if (entry != null) entry.expiryTime = 0;
        }
    }

    // Background cleanup of expired entries
    public void startCleanupDaemon(long intervalSeconds) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dns-cleanup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (cache) {
                int before = cache.size();
                cache.entrySet().removeIf(e -> e.getValue().isExpired());
                int removed = before - cache.size();
                if (removed > 0) System.out.println("[Cleanup] Removed " + removed + " expired entries.");
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void getCacheStats() {
        long total = hits.get() + misses.get();
        double hitRate = total == 0 ? 0 : (hits.get() * 100.0 / total);
        double avgLookup = lookupCount.get() == 0 ? 0 :
                (totalLookupTimeNs.get() / 1_000_000.0 / lookupCount.get());

        System.out.printf("%ngetCacheStats() → Hit Rate: %.1f%%, Avg Lookup Time: %.2fms%n",
                hitRate, avgLookup);
        System.out.println("  Hits: " + hits.get() + ", Misses: " + misses.get() +
                ", Cache size: " + cache.size() + "/" + maxCapacity);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Problem 3: DNS Cache with TTL ===\n");

        Problem3_DNSCache dns = new Problem3_DNSCache(1000);
        dns.startCleanupDaemon(60);

        // Register upstream DNS
        dns.registerUpstream("google.com", "172.217.14.206");
        dns.registerUpstream("github.com", "140.82.121.4");
        dns.registerUpstream("openai.com", "104.18.32.89");

        // First resolve — cache miss
        dns.resolve("google.com");
        // Second resolve — cache hit
        dns.resolve("google.com");
        dns.resolve("github.com");
        dns.resolve("github.com");
        dns.resolve("openai.com");

        // Simulate TTL expiry
        dns.forceExpire("google.com");
        dns.registerUpstream("google.com", "172.217.14.207"); // IP changed upstream
        dns.resolve("google.com"); // Should show EXPIRED and re-query

        dns.getCacheStats();
    }
}
