package io.smart.jsonassert;

/**
 * A successful leaf-level comparison: the value at {@code path} matched
 * (either by default rules or a custom comparator).
 */
public record FieldMatch(String path, Object expected, Object actual) {
}
