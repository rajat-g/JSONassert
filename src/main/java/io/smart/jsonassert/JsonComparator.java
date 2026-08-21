package io.smart.jsonassert;

/**
 * Strategy interface for comparing two JSON documents, mirroring
 * JSONAssert's JSONComparator. Implementations receive an already-created
 * {@link JsonCompareResult} and record matches/failures into it.
 */
@FunctionalInterface
public interface JsonComparator {

    void compare(Object expected, Object actual, JsonCompareResult result);
}
