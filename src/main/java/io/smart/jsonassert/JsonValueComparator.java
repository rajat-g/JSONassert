package io.smart.jsonassert;

/** Compares two JSON values and returns detailed matches and differences. */
@FunctionalInterface
public interface JsonValueComparator {
    JsonCompareResult compare(Object expected, Object actual);
}
