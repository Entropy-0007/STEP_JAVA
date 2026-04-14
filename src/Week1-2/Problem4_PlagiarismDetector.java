import java.util.*;

/**
 * Problem 4: Plagiarism Detection System
 * Concepts: String hashing, frequency counting with hashmaps, n-gram extraction, similarity scoring
 */
public class Problem4_PlagiarismDetector {

    private static final int N_GRAM_SIZE = 5; // 5-grams for accuracy
    private static final double SUSPICIOUS_THRESHOLD = 10.0;
    private static final double PLAGIARISM_THRESHOLD = 50.0;

    // HashMap<ngramHash, Set<documentId>> — maps each n-gram to all docs containing it
    private final HashMap<String, Set<String>> ngramIndex = new HashMap<>();

    // Store each document's n-gram set for similarity computation
    private final HashMap<String, Set<String>> documentNgrams = new HashMap<>();

    // Add a document to the index
    public void indexDocument(String docId, String content) {
        Set<String> ngrams = extractNgrams(content);
        documentNgrams.put(docId, ngrams);

        for (String ngram : ngrams) {
            ngramIndex.computeIfAbsent(ngram, k -> new HashSet<>()).add(docId);
        }

        System.out.println("Indexed \"" + docId + "\" → " + ngrams.size() + " n-grams extracted");
    }

    // Extract n-grams (sequences of N words)
    private Set<String> extractNgrams(String content) {
        Set<String> ngrams = new HashSet<>();
        String[] words = content.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "") // remove punctuation
                .trim()
                .split("\\s+");

        for (int i = 0; i <= words.length - N_GRAM_SIZE; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < i + N_GRAM_SIZE; j++) {
                if (j > i) sb.append(' ');
                sb.append(words[j]);
            }
            ngrams.add(sb.toString());
        }
        return ngrams;
    }

    // Analyze a new document against all indexed ones
    public void analyzeDocument(String docId, String content) {
        Set<String> newNgrams = extractNgrams(content);
        System.out.println("\nanalyzeDocument(\"" + docId + "\")");
        System.out.println("→ Extracted " + newNgrams.size() + " n-grams");

        // Count matching n-grams per existing document
        Map<String, Integer> matchCounts = new HashMap<>();
        for (String ngram : newNgrams) {
            Set<String> matchingDocs = ngramIndex.getOrDefault(ngram, Collections.emptySet());
            for (String existingDoc : matchingDocs) {
                if (!existingDoc.equals(docId)) {
                    matchCounts.merge(existingDoc, 1, Integer::sum);
                }
            }
        }

        // Sort by match count descending
        matchCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    String existingDoc = entry.getKey();
                    int matches = entry.getValue();
                    double similarity = (matches * 100.0) / newNgrams.size();
                    String verdict = similarity >= PLAGIARISM_THRESHOLD ? "PLAGIARISM DETECTED"
                            : similarity >= SUSPICIOUS_THRESHOLD ? "suspicious" : "OK";

                    System.out.printf("→ Found %d matching n-grams with \"%s\"%n", matches, existingDoc);
                    System.out.printf("→ Similarity: %.1f%% (%s)%n", similarity, verdict);
                });

        if (matchCounts.isEmpty()) {
            System.out.println("→ No matching documents found. Original content.");
        }
    }

    // Direct similarity between two indexed documents
    public double computeSimilarity(String docId1, String docId2) {
        Set<String> ngrams1 = documentNgrams.getOrDefault(docId1, new HashSet<>());
        Set<String> ngrams2 = documentNgrams.getOrDefault(docId2, new HashSet<>());

        Set<String> intersection = new HashSet<>(ngrams1);
        intersection.retainAll(ngrams2);
        Set<String> union = new HashSet<>(ngrams1);
        union.addAll(ngrams2);

        return union.isEmpty() ? 0.0 : (intersection.size() * 100.0 / union.size()); // Jaccard similarity
    }

    public static void main(String[] args) {
        System.out.println("=== Problem 4: Plagiarism Detection System ===\n");

        Problem4_PlagiarismDetector detector = new Problem4_PlagiarismDetector();

        String essay089 = "The water cycle is a fundamental process in earth science. "
                + "Water evaporates from oceans and lakes, rises into the atmosphere as vapor, "
                + "condenses into clouds, and eventually falls as precipitation. "
                + "This process is essential for distributing fresh water around the planet "
                + "and regulating global temperatures. The cycle also plays a key role in "
                + "shaping weather patterns and supporting all forms of life on earth.";

        String essay092 = "Photosynthesis is a critical biological process performed by plants. "
                + "During photosynthesis, plants absorb sunlight using chlorophyll, "
                + "combine it with carbon dioxide and water to produce glucose, "
                + "and release oxygen as a byproduct. This process is the foundation of "
                + "most food chains on earth and is responsible for the oxygen we breathe. "
                + "Without photosynthesis, life as we know it would not be possible.";

        // essay_123 heavily copies from essay_092
        String essay123 = "Photosynthesis is a critical biological process performed by plants. "
                + "During photosynthesis, plants absorb sunlight using chlorophyll, "
                + "combine it with carbon dioxide and water to produce glucose, "
                + "and release oxygen as a byproduct. This process is the foundation of "
                + "most food chains on earth. The water cycle is also important for life. "
                + "Water evaporates from oceans and lakes and rises into the atmosphere. "
                + "Students must understand both cycles for their biology exam next week.";

        detector.indexDocument("essay_089.txt", essay089);
        detector.indexDocument("essay_092.txt", essay092);

        detector.analyzeDocument("essay_123.txt", essay123);

        System.out.printf("%nJaccard Similarity (essay_089 vs essay_092): %.1f%%%n",
                detector.computeSimilarity("essay_089.txt", "essay_092.txt"));
    }
}
