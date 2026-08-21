package io.smart.jsonassert;

import org.json.JSONArray;
import org.json.JSONObject;

import io.smart.jsonassert.internal.HungarianAlgorithm;
import io.smart.jsonassert.internal.SimilarityScorer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Default {@link JsonComparator} implementation.
 *
 * Objects are compared key-by-key according to the compare mode.
 * Arrays are matched optimally using the Hungarian algorithm over a
 * similarity matrix, so elements are paired with their best counterparts
 * regardless of ordering (unless the mode requires strict ordering).
 *
 * Customizations registered for a path take precedence over default rules:
 * if a custom matcher says two values are equal they match; otherwise a
 * diff is recorded showing both values.
 */
public class DefaultJsonComparator implements JsonComparator {

    /** Minimum similarity for an array element pairing to count as a match. */
    private static final double MATCH_THRESHOLD = 0.5;

    protected final JsonCompareMode mode;
    private final List<Customization> customizations;
    private final SimilarityScorer scorer;

    public DefaultJsonComparator(JsonCompareMode mode) {
        this(mode, java.util.Collections.emptyList());
    }

    public DefaultJsonComparator(JsonCompareMode mode, Customization... customizations) {
        this(mode, Arrays.asList(customizations));
    }

    private DefaultJsonComparator(JsonCompareMode mode, List<Customization> customizations) {
        this.mode = mode;
        this.customizations = customizations == null ? List.of() : List.copyOf(customizations);
        this.scorer = new SimilarityScorer(this.customizations);
    }

    @Override
    public void compare(Object expected, Object actual, JsonCompareResult result) {
        compareValues(expected, actual, "", result);
    }

    private void compareValues(Object expected, Object actual, String path, JsonCompareResult result) {
        Optional<JsonValueMatcher> custom = Customization.findMatcher(customizations, path);
        if (custom.isPresent()) {
            applyCustomMatcher(custom.get(), expected, actual, path, result);
            return;
        }
        if (expected instanceof JSONObject e && actual instanceof JSONObject a) {
            compareObjects(e, a, path, result);
        } else if (expected instanceof JSONArray e && actual instanceof JSONArray a) {
            compareArrays(e, a, path, result);
        } else {
            comparePrimitives(expected, actual, path, result);
        }
    }

    private void applyCustomMatcher(JsonValueMatcher matcher, Object expected, Object actual,
                                    String path, JsonCompareResult result) {
        if (matcher.equal(expected, actual)) {
            result.recordMatch(path, expected, actual);
        } else {
            result.addFailure(path, "Values differ according to custom comparator", expected, actual);
        }
    }

    private void compareObjects(JSONObject expected, JSONObject actual,
                                String path, JsonCompareResult result) {
        for (String key : expected.keySet()) {
            String childPath = childPath(path, key);
            if (!actual.has(key)) {
                result.addFailure(childPath, "Field is missing", expected.get(key), null);
            } else {
                compareValues(expected.get(key), actual.get(key), childPath, result);
            }
        }
        if (mode.isNonExtensible()) {
            for (String key : actual.keySet()) {
                if (!expected.has(key)) {
                    result.addFailure(childPath(path, key), "Unexpected field in actual", null, actual.get(key));
                }
            }
        }
    }

    private static String childPath(String parent, String key) {
        return parent.isEmpty() ? key : parent + "." + key;
    }

    private void compareArrays(JSONArray expected, JSONArray actual,
                               String path, JsonCompareResult result) {
        int n = expected.length();
        int m = actual.length();

        if (mode.isStrictOrder()) {
            compareArraysPositionally(expected, actual, path, result);
            return;
        }

        double[][] similarity = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                similarity[i][j] = scorer.similarity(expected.get(i), actual.get(j), arrayElementPath(path, i));
            }
        }

        int[] assignment;
        if (n == 0 || m == 0) {
            assignment = new int[n];
            Arrays.fill(assignment, -1);
        } else {
            assignment = new HungarianAlgorithm(negate(similarity)).getResult();
        }

        boolean[] usedActual = new boolean[m];
        for (int i = 0; i < n; i++) {
            int j = assignment[i];
            if (j >= 0 && similarity[i][j] >= MATCH_THRESHOLD) {
                usedActual[j] = true;
                compareValues(expected.get(i), actual.get(j), arrayElementPath(path, i), result);
            } else {
                result.addFailure(arrayElementPath(path, i), "No matching element found", expected.get(i), null);
            }
        }

        if (mode.isNonExtensible()) {
            for (int j = 0; j < m; j++) {
                if (!usedActual[j]) {
                    result.addFailure(arrayElementPath(path, j), "Unexpected extra element", null, actual.get(j));
                }
            }
        }
    }

    private void compareArraysPositionally(JSONArray expected, JSONArray actual,
                                           String path, JsonCompareResult result) {
        if (expected.length() != actual.length()) {
            result.addFailure(path, "Array lengths differ", expected.length(), actual.length());
            return;
        }
        for (int i = 0; i < expected.length(); i++) {
            compareValues(expected.get(i), actual.get(i), arrayElementPath(path, i), result);
        }
    }

    private static String arrayElementPath(String parent, int index) {
        return parent + "[" + index + "]";
    }

    private void comparePrimitives(Object expected, Object actual, String path, JsonCompareResult result) {
        if (valuesEqual(expected, actual)) {
            result.recordMatch(path, expected, actual);
        } else {
            result.addFailure(path, "Values differ", expected, actual);
        }
    }

    private boolean valuesEqual(Object expected, Object actual) {
        if (expected == null || actual == null) {
            return expected == actual;
        }
        if (expected instanceof Number en && actual instanceof Number an) {
            return Math.abs(en.doubleValue() - an.doubleValue()) < 1e-9;
        }
        return expected.equals(actual);
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
