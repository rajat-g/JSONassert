package io.smart.jsonassert.internal;

import org.json.JSONArray;
import org.json.JSONObject;

import io.smart.jsonassert.Customization;
import io.smart.jsonassert.JsonValueMatcher;

import java.util.List;
import java.util.Optional;

/**
 * Computes a structural/value similarity score in [0.0, 1.0] between two JSON values.
 * Used to build the cost matrix for the Hungarian algorithm.
 *
 * If a {@link Customization} applies at the given path, its matcher decides
 * similarity (1.0 for equal, 0.0 otherwise).
 */
public final class SimilarityScorer {

    private final List<Customization> customizations;

    public SimilarityScorer() {
        this(List.of());
    }

    public SimilarityScorer(List<Customization> customizations) {
        this.customizations = customizations == null ? List.of() : customizations;
    }

    public double similarity(Object expected, Object actual, String path) {
        if (expected == null || actual == null) {
            return (expected == null && actual == null) ? 1.0 : 0.0;
        }
        Optional<JsonValueMatcher> custom = Customization.findMatcher(customizations, path);
        if (custom.isPresent()) {
            return custom.get().equal(expected, actual) ? 1.0 : 0.0;
        }

        if (expected instanceof JSONObject e && actual instanceof JSONObject a) {
            return objectSimilarity(e, a, path);
        }
        if (expected instanceof JSONArray e && actual instanceof JSONArray a) {
            return arraySimilarity(e, a, path);
        }
        return primitiveSimilarity(expected, actual);
    }

    private double objectSimilarity(JSONObject expected, JSONObject actual, String path) {
        int totalFields = Math.max(expected.length(), actual.length());
        if (totalFields == 0) {
            return 1.0;
        }
        double score = 0.0;
        for (String key : expected.keySet()) {
            if (actual.has(key)) {
                String childPath = path.isEmpty() ? key : path + "." + key;
                score += similarity(expected.get(key), actual.get(key), childPath);
            }
        }
        // Missing keys in actual contribute 0; extra keys in actual dilute the score
        return score / totalFields;
    }

    private double arraySimilarity(JSONArray expected, JSONArray actual, String path) {
        if (expected.isEmpty() && actual.isEmpty()) {
            return 1.0;
        }
        int n = expected.length();
        int m = actual.length();
        double sizeRatio = (double) Math.min(n, m) / Math.max(n, m);
        if (n == 0 || m == 0) {
            return 0.0;
        }

        double[][] sim = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                sim[i][j] = similarity(expected.get(i), actual.get(j), elementPath(path, i));
            }
        }
        HungarianAlgorithm hungarian = new HungarianAlgorithm(negate(sim));
        int[] assignment = hungarian.getResult();

        double sum = 0.0;
        int matchedPairs = 0;
        for (int i = 0; i < n && i < m; i++) {
            if (assignment[i] >= 0) {
                sum += Math.max(0.0, sim[i][assignment[i]]);
                matchedPairs++;
            }
        }
        return sizeRatio * (matchedPairs == 0 ? 0.0 : sum / matchedPairs);
    }

    private static String elementPath(String parent, int index) {
        return parent + "[" + index + "]";
    }

    private double primitiveSimilarity(Object expected, Object actual) {
        if (expected.equals(actual)) {
            return 1.0;
        }
        if (expected instanceof Number en && actual instanceof Number an) {
            return numericSimilarity(en.doubleValue(), an.doubleValue());
        }
        if (expected instanceof String es && actual instanceof String as) {
            return levenshteinRatio(es, as);
        }
        return 0.0;
    }

    private static double numericSimilarity(double a, double b) {
        double max = Math.max(Math.abs(a), Math.abs(b));
        if (max == 0.0) {
            return 1.0;
        }
        return Math.max(0.0, 1.0 - (Math.abs(a - b) / max));
    }

    static double levenshteinRatio(String s1, String s2) {
        int distance = levenshtein(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return maxLen == 0 ? 1.0 : 1.0 - ((double) distance / maxLen);
    }

    private static int levenshtein(String s1, String s2) {
        int[] prev = new int[s2.length() + 1];
        int[] curr = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= s1.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[s2.length()];
    }

    private static double[][] negate(double[][] matrix) {
        double[][] negated = new double[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            negated[i] = new double[matrix[i].length];
            for (int j = 0; j < matrix[i].length; j++) {
                negated[i][j] = -matrix[i][j];
            }
        }
        return negated;
    }
}
