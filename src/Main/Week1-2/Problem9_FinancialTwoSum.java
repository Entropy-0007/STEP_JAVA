import java.util.*;
import java.util.stream.Collectors;

/**
 * Problem 9: Two-Sum Problem Variants for Financial Transactions
 * Concepts: Hash table for complement lookup, O(1) performance, multiple hash tables,
 *           time window filtering, K-sum with memoization
 */
public class Problem9_FinancialTwoSum {

    // Transaction record
    record Transaction(int id, double amount, String merchant, String time, String account) {}

    // ========== Classic Two-Sum ==========
    // Find all pairs summing to target — O(n)
    public List<int[]> findTwoSum(List<Transaction> transactions, double target) {
        List<int[]> result = new ArrayList<>();
        // HashMap<complement, transaction_index>
        HashMap<Double, Integer> complementMap = new HashMap<>();

        for (Transaction tx : transactions) {
            double complement = roundTo2(target - tx.amount());
            if (complementMap.containsKey(tx.amount())) {
                int pairedIdx = complementMap.get(tx.amount());
                result.add(new int[]{transactions.get(pairedIdx).id(), tx.id()});
            }
            complementMap.put(complement, transactions.indexOf(tx));
        }
        return result;
    }

    // ========== Two-Sum with Time Window (1 hour) ==========
    // Pairs must occur within 60 minutes of each other
    public List<int[]> findTwoSumWithTimeWindow(List<Transaction> transactions,
                                                 double target, int windowMinutes) {
        List<int[]> result = new ArrayList<>();

        // Sort by time (HH:MM format)
        List<Transaction> sorted = transactions.stream()
                .sorted(Comparator.comparing(Transaction::time))
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            HashMap<Double, Integer> windowMap = new HashMap<>();
            int startMinutes = parseMinutes(sorted.get(i).time());

            for (int j = i; j < sorted.size(); j++) {
                int endMinutes = parseMinutes(sorted.get(j).time());
                if (endMinutes - startMinutes > windowMinutes) break;

                double complement = roundTo2(target - sorted.get(j).amount());
                if (windowMap.containsKey(sorted.get(j).amount())) {
                    result.add(new int[]{windowMap.get(sorted.get(j).amount()), sorted.get(j).id()});
                }
                windowMap.put(complement, sorted.get(j).id());
            }
        }
        return result;
    }

    private int parseMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    // ========== K-Sum with Hash Table Memoization ==========
    // Find K transactions that sum to target — O(n^(k-1)) with memoization
    public List<List<Integer>> findKSum(List<Transaction> transactions, int k, double target) {
        List<double[]> amounts = transactions.stream()
                .map(t -> new double[]{t.amount(), t.id()})
                .collect(Collectors.toList());
        List<List<Integer>> result = new ArrayList<>();
        kSumHelper(amounts, k, target, 0, new ArrayList<>(), result);
        return result;
    }

    private void kSumHelper(List<double[]> amounts, int k, double target, int start,
                              List<Integer> current, List<List<Integer>> result) {
        if (k == 2) {
            // Use hash table for the base case
            HashMap<Double, Integer> map = new HashMap<>();
            for (int i = start; i < amounts.size(); i++) {
                double complement = roundTo2(target - amounts.get(i)[0]);
                if (map.containsKey(amounts.get(i)[0])) {
                    List<Integer> combo = new ArrayList<>(current);
                    combo.add(map.get(amounts.get(i)[0]));
                    combo.add((int) amounts.get(i)[1]);
                    result.add(combo);
                }
                map.put(complement, (int) amounts.get(i)[1]);
            }
            return;
        }

        for (int i = start; i < amounts.size() - k + 1; i++) {
            current.add((int) amounts.get(i)[1]);
            kSumHelper(amounts, k - 1, roundTo2(target - amounts.get(i)[0]), i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    // ========== Duplicate Detection ==========
    // Same amount + same merchant + different accounts = potential fraud
    public List<Map<String, Object>> detectDuplicates(List<Transaction> transactions) {
        // HashMap<amount_merchant_key, List<Transaction>>
        HashMap<String, List<Transaction>> groupMap = new HashMap<>();

        for (Transaction tx : transactions) {
            String key = tx.amount() + "_" + tx.merchant();
            groupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(tx);
        }

        List<Map<String, Object>> duplicates = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> entry : groupMap.entrySet()) {
            List<Transaction> group = entry.getValue();
            if (group.size() > 1) {
                // Check if different accounts are involved
                Set<String> accounts = group.stream().map(Transaction::account).collect(Collectors.toSet());
                if (accounts.size() > 1) {
                    Map<String, Object> fraudAlert = new LinkedHashMap<>();
                    fraudAlert.put("amount", group.get(0).amount());
                    fraudAlert.put("merchant", group.get(0).merchant());
                    fraudAlert.put("accounts", accounts);
                    fraudAlert.put("transactionIds",
                            group.stream().map(Transaction::id).collect(Collectors.toList()));
                    duplicates.add(fraudAlert);
                }
            }
        }
        return duplicates;
    }

    private double roundTo2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    public static void main(String[] args) {
        System.out.println("=== Problem 9: Two-Sum Variants for Financial Transactions ===\n");

        Problem9_FinancialTwoSum solver = new Problem9_FinancialTwoSum();

        // Sample transactions from the problem
        List<Transaction> transactions = List.of(
                new Transaction(1, 500, "Store A", "10:00", "acc1"),
                new Transaction(2, 300, "Store B", "10:15", "acc2"),
                new Transaction(3, 200, "Store C", "10:30", "acc3"),
                new Transaction(4, 500, "Store A", "10:45", "acc2"), // duplicate amount+merchant
                new Transaction(5, 700, "Store D", "11:00", "acc4"),
                new Transaction(6, 100, "Store E", "11:30", "acc5")
        );

        // Classic Two-Sum: target = 500 (300 + 200)
        System.out.println("findTwoSum(target=500):");
        solver.findTwoSum(transactions, 500).forEach(pair ->
                System.out.println("  → (id:" + pair[0] + ", id:" + pair[1] + ")  // "
                        + transactions.get(pair[0]-1).amount() + " + "
                        + transactions.get(pair[1]-1).amount()));

        // Two-Sum with 60-minute window
        System.out.println("\nfindTwoSumWithTimeWindow(target=500, window=60min):");
        solver.findTwoSumWithTimeWindow(transactions, 500, 60).forEach(pair ->
                System.out.println("  → (id:" + pair[0] + ", id:" + pair[1] + ")"));

        // K-Sum: k=3, target=1000
        System.out.println("\nfindKSum(k=3, target=1000):");
        solver.findKSum(transactions, 3, 1000).forEach(combo ->
                System.out.println("  → " + combo + " // "
                        + combo.stream().mapToDouble(id -> transactions.get(id-1).amount()).sum()));

        // Duplicate detection
        System.out.println("\ndetectDuplicates():");
        solver.detectDuplicates(transactions).forEach(alert ->
                System.out.println("  → " + alert));
    }
}
