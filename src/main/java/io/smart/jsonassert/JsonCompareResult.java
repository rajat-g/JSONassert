package io.smart.jsonassert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a JSON comparison, in the spirit of JSONAssert's JSONCompareResult.
 * Collects every failure (diff) and every successful leaf match, plus counters.
 */
public final class JsonCompareResult {

    private final List<FieldComparisonFailure> failures = new ArrayList<>();
    private final List<FieldMatch> matches = new ArrayList<>();

    public void addFailure(String path, String message, Object expected, Object actual) {
        failures.add(new FieldComparisonFailure(path, message, expected, actual));
    }

    public void recordMatch(String path, Object expected, Object actual) {
        matches.add(new FieldMatch(path, expected, actual));
    }

    public void append(JsonCompareResult other) {
        failures.addAll(other.failures);
        matches.addAll(other.matches);
    }

    void append(JsonCompareResult other, String pathPrefix) {
        for (FieldComparisonFailure failure : other.failures) {
            failures.add(new FieldComparisonFailure(
                    prefix(pathPrefix, failure.getPath()), failure.getMessage(),
                    failure.getExpected(), failure.getActual()));
        }
        for (FieldMatch match : other.matches) {
            matches.add(new FieldMatch(prefix(pathPrefix, match.path()), match.expected(), match.actual()));
        }
    }

    private static String prefix(String prefix, String path) {
        if (prefix.isEmpty()) {
            return path;
        }
        if (path == null || path.isEmpty()) {
            return prefix;
        }
        return path.startsWith("[") ? prefix + path : prefix + "." + path;
    }

    /** True if the comparison found no differences. */
    public boolean succeeded() {
        return failures.isEmpty();
    }

    /** True if the comparison found any difference. */
    public boolean failed() {
        return !failures.isEmpty();
    }

    public List<FieldComparisonFailure> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    /** All successfully matched leaves (path + expected + actual values). */
    public List<FieldMatch> getMatches() {
        return Collections.unmodifiableList(matches);
    }

    /** Total number of leaf comparisons performed (matches + failures). */
    public int getFieldCount() {
        return matches.size() + failures.size();
    }

    public int getMatchCount() {
        return matches.size();
    }

    public int getFailureCount() {
        return failures.size();
    }

    /** Human-readable message in the style of JSONAssert. */
    public String getMessage() {
        if (succeeded()) {
            return "JSON structures match (" + getFieldCount() + " fields compared)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("JSON comparison failed with ").append(failures.size()).append(" difference(s):\n");
        for (FieldComparisonFailure f : failures) {
            sb.append('\n').append(f.getPath()).append(": ").append(f.getMessage())
              .append("\nExpected: ").append(f.getExpected())
              .append("\n     got: ").append(f.getActual());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        if (succeeded()) {
            return getMessage();
        }
        StringBuilder sb = new StringBuilder("JSON comparison failed with " + failures.size() + " difference(s):\n");
        for (FieldComparisonFailure f : failures) {
            sb.append(" - ").append(f).append('\n');
        }
        return sb.toString();
    }
}
