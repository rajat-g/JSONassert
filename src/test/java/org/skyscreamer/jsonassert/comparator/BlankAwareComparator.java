package org.skyscreamer.jsonassert.comparator;

import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

/**
 * Test-only example: a {@link DefaultComparator} variant with blank-tolerant
 * string semantics. Lives in the test sources to demonstrate how to write
 * your own comparator; it is not part of the shipped library jar.
 *
 * <p>When both expected and actual values are {@link String}s and both are
 * empty/blank after trimming (e.g. {@code ""} vs {@code "   "}), they are
 * treated as a match. Everything else — non-blank strings, numbers, objects,
 * arrays — falls through to the default comparison rules.</p>
 *
 * <pre>
 * JSONAssert.assertEquals(
 *         "{\"note\": \"\"}",
 *         "{\"note\": \"   \"}",
 *         new BlankAwareComparator(JSONCompareMode.NON_EXTENSIBLE));
 * </pre>
 */
public class BlankAwareComparator extends DefaultComparator {

    public BlankAwareComparator(JSONCompareMode mode) {
        super(mode);
    }

    @Override
    public void compareValues(String prefix, Object expectedValue, Object actualValue,
                              JSONCompareResult result) {
        if (expectedValue instanceof String && actualValue instanceof String) {
            String expectedTrimmed = ((String) expectedValue).trim();
            String actualTrimmed = ((String) actualValue).trim();
            if (expectedTrimmed.isEmpty() && actualTrimmed.isEmpty()) {
                return; // both blank -> match, nothing recorded
            }
        }
        // strings that are not both blank, and all non-string values: compare as usual
        super.compareValues(prefix, expectedValue, actualValue, result);
    }
}
