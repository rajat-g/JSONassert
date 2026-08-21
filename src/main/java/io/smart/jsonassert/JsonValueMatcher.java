package io.smart.jsonassert;

/**
 * A user-supplied matcher deciding whether two raw values are considered equal.
 * Typically used via {@link Customization#customization(String, JsonValueMatcher)}.
 */
@FunctionalInterface
public interface JsonValueMatcher {

    /**
     * @param expected the value from the expected document
     * @param actual   the value from the actual document
     * @return true if these two values should be treated as equal
     */
    boolean equal(Object expected, Object actual);
}
