import java.util.*;
import java.util.stream.Collectors;

/**
 * Problem 7: Autocomplete System for Search Engine
 * Concepts: Hash table for query frequency, Trie+HashMap hybrid for prefix matching,
 *           min-heap for top K, string hashing, space optimization
 */
public class Problem7_Autocomplete {

    // Global frequency map: query → frequency
    private final HashMap<String, Integer> queryFrequency = new HashMap<>();

    // Trie node for prefix matching
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord = false;
        // Cache top-10 suggestions at each node for fast retrieval
        List<String> cachedTopSuggestions = null;
    }

    private final TrieNode trieRoot = new TrieNode();

    // Prefix → cached results (speeds up repeated prefix searches)
    private final HashMap<String, List<String>> prefixCache = new HashMap<>();

    // Insert a query into the Trie with frequency
    public void insertQuery(String query, int frequency) {
        String lower = query.toLowerCase().trim();
        queryFrequency.merge(lower, frequency, Integer::sum);

        // Insert into Trie
        TrieNode current = trieRoot;
        for (char c : lower.toCharArray()) {
            current.children.putIfAbsent(c, new TrieNode());
            current = current.children.get(c);
            current.cachedTopSuggestions = null; // invalidate cache
        }
        current.isEndOfWord = true;

        // Invalidate prefix cache for all prefixes of this query
        for (int i = 1; i <= lower.length(); i++) {
            prefixCache.remove(lower.substring(0, i));
        }
    }

    // Update frequency when a new search occurs
    public void updateFrequency(String query) {
        String lower = query.toLowerCase().trim();
        int oldFreq = queryFrequency.getOrDefault(lower, 0);
        int newFreq = oldFreq + 1;
        System.out.printf("updateFrequency(\"%s\") → Frequency: %d → %d → %d %s%n",
                query, oldFreq, oldFreq, newFreq, newFreq > oldFreq ? "(trending)" : "");
        insertQuery(lower, 1);
    }

    // Get all words with a given prefix using Trie traversal
    private List<String> getAllWithPrefix(String prefix) {
        if (prefixCache.containsKey(prefix)) return prefixCache.get(prefix);

        TrieNode current = trieRoot;
        for (char c : prefix.toCharArray()) {
            if (!current.children.containsKey(c)) return Collections.emptyList();
            current = current.children.get(c);
        }

        List<String> results = new ArrayList<>();
        collectWords(current, new StringBuilder(prefix), results);
        prefixCache.put(prefix, results);
        return results;
    }

    // DFS to collect all words from a Trie node
    private void collectWords(TrieNode node, StringBuilder current, List<String> results) {
        if (node.isEndOfWord) results.add(current.toString());
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            current.append(entry.getKey());
            collectWords(entry.getValue(), current, results);
            current.deleteCharAt(current.length() - 1);
        }
    }

    // Main search — returns top K suggestions for prefix in <50ms
    public List<String> search(String prefix, int topK) {
        long start = System.currentTimeMillis();
        String lower = prefix.toLowerCase().trim();

        List<String> candidates = getAllWithPrefix(lower);

        // Use min-heap to efficiently get top K by frequency — O(n log k)
        PriorityQueue<Map.Entry<String, Integer>> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));

        for (String candidate : candidates) {
            int freq = queryFrequency.getOrDefault(candidate, 0);
            minHeap.offer(Map.entry(candidate, freq));
            if (minHeap.size() > topK) minHeap.poll();
        }

        // Extract from heap and reverse (highest first)
        List<Map.Entry<String, Integer>> topResults = new ArrayList<>(minHeap);
        topResults.sort((a, b) -> b.getValue() - a.getValue());

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("%nsearch(\"%s\") [%dms] →%n", prefix, elapsed);
        for (int i = 0; i < topResults.size(); i++) {
            Map.Entry<String, Integer> e = topResults.get(i);
            System.out.printf("  %d. \"%s\" (%,d searches)%n", i + 1, e.getKey(), e.getValue());
        }

        return topResults.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    // Suggest corrections for possible typos using edit distance
    public List<String> suggestCorrections(String query, int maxDistance) {
        String lower = query.toLowerCase().trim();
        return queryFrequency.keySet().stream()
                .filter(q -> editDistance(lower, q) <= maxDistance)
                .sorted((a, b) -> queryFrequency.get(b) - queryFrequency.get(a))
                .limit(5)
                .collect(Collectors.toList());
    }

    // Levenshtein edit distance for typo correction
    private int editDistance(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = s1.charAt(i - 1) == s2.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
        return dp[m][n];
    }

    public static void main(String[] args) {
        System.out.println("=== Problem 7: Autocomplete System for Search Engine ===");

        Problem7_Autocomplete ac = new Problem7_Autocomplete();

        // Pre-populate with Java-related search queries
        ac.insertQuery("java tutorial", 1_234_567);
        ac.insertQuery("java script", 987_654);
        ac.insertQuery("java download", 456_789);
        ac.insertQuery("java interview questions", 234_567);
        ac.insertQuery("java collections", 198_432);
        ac.insertQuery("java stream api", 176_543);
        ac.insertQuery("java 21 features", 1);
        ac.insertQuery("javascript frameworks", 654_321);
        ac.insertQuery("javascript react", 543_210);

        // Search for prefix "jav"
        ac.search("jav", 10);

        // Update frequency (simulating a new trending search)
        ac.updateFrequency("java 21 features");
        ac.updateFrequency("java 21 features");

        // Typo correction
        System.out.println("\nTypo correction for \"jaav tutorial\":");
        ac.suggestCorrections("jaav tutorial", 2).forEach(s ->
                System.out.println("  → " + s + " (" + ac.queryFrequency.get(s) + " searches)"));
    }
}
