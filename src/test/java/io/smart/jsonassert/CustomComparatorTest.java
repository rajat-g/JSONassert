package io.smart.jsonassert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomComparatorTest {

    private static String normalize(Object value) {
        if (!(value instanceof String s)) {
            return String.valueOf(value);
        }
        return s.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    @Test
    void whitespaceOnlyDifferencesAreEqualWithCustomMatcher() {
        JsonComparator comparator = new CustomComparator(
                JsonCompareMode.NON_EXTENSIBLE,
                Customization.customization("note", (e, a) -> normalize(e).equals(normalize(a))));

        assertDoesNotThrow(() -> SmartJsonAssert.assertEquals(
                "{\"note\": \"hello   world \"}",
                "{\"note\": \"hello world\"}", comparator));
    }

    @Test
    void blankStringsAreEqualRegardlessOfSpaces() {
        JsonComparator comparator = new CustomComparator(
                JsonCompareMode.NON_EXTENSIBLE,
                Customization.customization("note", (e, a) -> normalize(e).equals(normalize(a))));

        assertDoesNotThrow(() -> SmartJsonAssert.assertEquals(
                "{\"note\": \"\"}",
                "{\"note\": \"    \"}", comparator));
    }

    @Test
    void nonBlankDifferentStringsStillShowDiff() {
        JsonComparator comparator = new CustomComparator(
                JsonCompareMode.NON_EXTENSIBLE,
                Customization.customization("note", (e, a) -> normalize(e).equals(normalize(a))));

        JsonCompareResult result = SmartJsonAssert.compare(
                "{\"id\": 1, \"note\": \"abc\"}",
                "{\"id\": 1, \"note\": \"xyz\"}", comparator);

        assertTrue(result.failed());
        assertEquals(1, result.getFailureCount());
        FieldComparisonFailure failure = result.getFailures().get(0);
        assertEquals("note", failure.getPath());
        assertEquals("abc", failure.getExpected());
        assertEquals("xyz", failure.getActual());
        assertTrue(failure.getMessage().contains("custom comparator"));
    }

    @Test
    void wildcardPathAppliesMatcherToEveryArrayElement() {
        JsonComparator comparator = new CustomComparator(
                JsonCompareMode.NON_EXTENSIBLE,
                Customization.customization("items[*].label",
                        (e, a) -> normalize(e).equals(normalize(a))));

        String expected = "{\"items\": [{\"label\": \"a b\"}, {\"label\": \"c\"}]}";
        String actual = "{\"items\": [{\"label\": \"  a  b \"}, {\"label\": \"c\"}]}";

        assertDoesNotThrow(() -> SmartJsonAssert.assertEquals(expected, actual, comparator));
    }

    @Test
    void resultExposesAllMatchesAndCounts() {
        JsonCompareResult result = SmartJsonAssert.compare(
                "{\"user\": {\"name\": \"Alice\", \"age\": 30}}",
                "{\"user\": {\"name\": \"Alice\", \"age\": 31}}",
                JsonCompareMode.LENIENT);

        assertTrue(result.failed());
        assertEquals(2, result.getFieldCount());
        assertEquals(1, result.getMatchCount());
        assertEquals(1, result.getFailureCount());

        assertTrue(result.getMatches().stream()
                .anyMatch(m -> m.path().equals("user.name") && "Alice".equals(m.expected())));
        assertEquals("user.age", result.getFailures().get(0).getPath());
    }

    @Test
    void messageIsHumanReadableJsonAssertStyle() {
        AssertionError error = assertThrows(AssertionError.class, () ->
                SmartJsonAssert.assertEquals("{\"a\": 1}", "{\"a\": 2}", JsonCompareMode.STRICT));
        String message = error.getMessage();
        assertTrue(message.contains("a"));
        assertTrue(message.contains("Expected:") && message.contains("got:"));
    }

    @Test
    void customMatcherAlsoDrivesHungarianArrayPairing() {
        // Without the matcher these two elements would score < threshold and fail
        JsonComparator comparator = new CustomComparator(
                JsonCompareMode.NON_EXTENSIBLE,
                Customization.customization("logs[*]",
                        (e, a) -> normalize(e).equals(normalize(a))));

        JsonCompareResult result = SmartJsonAssert.compare(
                "{\"logs\": [\"job finished\", \"started\"]}",
                "{\"logs\": [\"started\", \"JOB FINISHED\"]}",
                comparator);

        assertTrue(result.succeeded(), result::toString);
    }
}
