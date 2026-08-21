package io.smart.jsonassert;

/**
 * Comparison modes for JSON comparison.
 */
public enum JsonCompareMode {

    /**
     * All fields in the expected document must exist in the actual document and match.
     * Extra fields in the actual object are reported as failures.
     * Arrays are matched optimally via the Hungarian algorithm (order-insensitive).
     */
    NON_EXTENSIBLE(true, false),

    /**
     * Same field rules as {@link #NON_EXTENSIBLE}, but arrays are compared
     * positionally, so element order must match exactly.
     */
    STRICT(true, true),

    /**
     * Only checks that every field present in expected exists with a matching value in actual.
     * Extra fields/elements in actual are ignored.
     */
    LENIENT(false, false);

    private final boolean nonExtensible;
    private final boolean strictOrder;

    JsonCompareMode(boolean nonExtensible, boolean strictOrder) {
        this.nonExtensible = nonExtensible;
        this.strictOrder = strictOrder;
    }

    /** If true, extra keys in the actual object are treated as failures. */
    public boolean isNonExtensible() {
        return nonExtensible;
    }

    /** If true, array elements must match positionally (order-sensitive). */
    public boolean isStrictOrder() {
        return strictOrder;
    }
}
