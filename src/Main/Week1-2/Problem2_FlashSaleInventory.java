import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Problem 2: E-commerce Flash Sale Inventory Manager
 * Concepts: Hash table, collision resolution, load factor, concurrent requests, waiting list
 */
public class Problem2_FlashSaleInventory {

    // Thread-safe stock tracking: productId → remaining stock (AtomicInteger for thread safety)
    private final ConcurrentHashMap<String, AtomicInteger> inventory = new ConcurrentHashMap<>();

    // Waiting list per product (FIFO using LinkedList, synchronized)
    private final ConcurrentHashMap<String, Queue<Integer>> waitingLists = new ConcurrentHashMap<>();

    // Purchase history: productId → list of userIds who purchased
    private final ConcurrentHashMap<String, List<Integer>> purchaseHistory = new ConcurrentHashMap<>();

    // Add product to inventory
    public void addProduct(String productId, int stockCount) {
        inventory.put(productId, new AtomicInteger(stockCount));
        waitingLists.put(productId, new ConcurrentLinkedQueue<>());
        purchaseHistory.put(productId, Collections.synchronizedList(new ArrayList<>()));
    }

    // O(1) stock check
    public int checkStock(String productId) {
        AtomicInteger stock = inventory.get(productId);
        return (stock == null) ? -1 : stock.get();
    }

    // Atomic purchase with waiting list fallback
    public String purchaseItem(String productId, int userId) {
        AtomicInteger stock = inventory.get(productId);
        if (stock == null) return "Product not found";

        // Atomic decrement — prevents overselling
        int remaining = stock.decrementAndGet();
        if (remaining >= 0) {
            purchaseHistory.get(productId).add(userId);
            return "Success, " + remaining + " units remaining";
        } else {
            // Undo the decrement and add to waiting list
            stock.incrementAndGet();
            Queue<Integer> waitList = waitingLists.get(productId);
            waitList.offer(userId);
            return "Added to waiting list, position #" + waitList.size();
        }
    }

    // Process next person on waiting list when stock is restocked
    public String restock(String productId, int quantity) {
        AtomicInteger stock = inventory.get(productId);
        if (stock == null) return "Product not found";

        Queue<Integer> waitList = waitingLists.get(productId);
        StringBuilder result = new StringBuilder("Restocked " + quantity + " units.\n");

        for (int i = 0; i < quantity; i++) {
            Integer nextUser = waitList.poll();
            if (nextUser != null) {
                purchaseHistory.get(productId).add(nextUser);
                result.append("  Auto-purchased for userId: ").append(nextUser).append("\n");
            } else {
                stock.incrementAndGet();
            }
        }
        return result.toString().trim();
    }

    public int getWaitingListSize(String productId) {
        Queue<Integer> wl = waitingLists.get(productId);
        return (wl == null) ? 0 : wl.size();
    }

    // Concurrent load test
    public void runConcurrentTest(String productId, int numThreads) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int userId = 10000 + i;
            executor.submit(() -> {
                String result = purchaseItem(productId, userId);
                System.out.println("User " + userId + ": " + result);
                latch.countDown();
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {
        Problem2_FlashSaleInventory inventory = new Problem2_FlashSaleInventory();

        System.out.println("=== Problem 2: Flash Sale Inventory Manager ===\n");

        // Setup
        inventory.addProduct("IPHONE15_256GB", 100);

        System.out.println("checkStock(\"IPHONE15_256GB\") → " + inventory.checkStock("IPHONE15_256GB") + " units available");
        System.out.println(inventory.purchaseItem("IPHONE15_256GB", 12345));
        System.out.println(inventory.purchaseItem("IPHONE15_256GB", 67890));

        // Simulate 100 purchases to exhaust stock
        for (int i = 2; i < 100; i++) inventory.purchaseItem("IPHONE15_256GB", 20000 + i);

        System.out.println("After 100 purchases, stock: " + inventory.checkStock("IPHONE15_256GB"));
        System.out.println(inventory.purchaseItem("IPHONE15_256GB", 99999));
        System.out.println(inventory.purchaseItem("IPHONE15_256GB", 99998));
        System.out.println("Waiting list size: " + inventory.getWaitingListSize("IPHONE15_256GB"));

        System.out.println("\n--- Concurrent Load Test (10 threads, 3 units left) ---");
        inventory.addProduct("LIMITED_ITEM", 3);
        inventory.runConcurrentTest("LIMITED_ITEM", 10);
    }
}
