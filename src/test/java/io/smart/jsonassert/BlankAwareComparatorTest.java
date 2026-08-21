package io.smart.jsonassert;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.BlankAwareComparator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlankAwareComparatorTest {

    private final BlankAwareComparator comparator = new BlankAwareComparator(JSONCompareMode.NON_EXTENSIBLE);

    @Test
    void blankVersusWhitespaceOnlyStringMatches() {
        assertDoesNotThrow(() -> JSONAssert.assertEquals(
                "{\"note\": \"\"}", "{\"note\": \"   \"}", comparator));
    }

    @Test
    void emptyVersusEmptyStringMatches() {
        assertDoesNotThrow(() -> JSONAssert.assertEquals(
                "{\"a\": \"\", \"b\": \"  \t \"}", "{\"a\": \"   \", \"b\": \"\"}", comparator));
    }

    @Test
    void nonBlankStringsStillComparedNormally() {
        JSONCompareResult result = JSONCompare.compareJSON(
                "{\"note\": \"abc\"}", "{\"note\": \"xyz\"}", comparator);
        assertTrue(result.failed());
        assertTrue(result.getMessage().contains("Expected: abc"));
    }

    @Test
    void equalNonBlankStringsMatch() {
        assertDoesNotThrow(() -> JSONAssert.assertEquals(
                "{\"note\": \"abc\"}", "{\"note\": \"abc\"}", comparator));
    }

    @Test
    void nonStringValuesFallThroughToDefaultRules() {
        // numbers compared as usual
        assertDoesNotThrow(() -> JSONAssert.assertEquals(
                "{\"n\": 5, \"flag\": true}", "{\"n\": 5.0, \"flag\": true}", comparator));
        // unequal numbers still fail
        JSONCompareResult result = JSONCompare.compareJSON(
                "{\"n\": 5}", "{\"n\": 6}", comparator);
        assertTrue(result.failed());
    }

    @Test
    void worksInsideArraysAndNestedStructures() {
        // 'y' is the unique key used to pair elements ('tag' repeats so it
        // cannot be a key); 'tag' values are then compared field-by-field
        // with blank tolerance, in any order.
        assertDoesNotThrow(() -> JSONAssert.assertEquals(
                "{\"items\": [{\"tag\": \"\", \"y\": 1}, {\"tag\": \"\", \"y\": 2}]}",
                "{\"items\": [{\"tag\": \" \", \"y\": 2}, {\"tag\": \"   \", \"y\": 1}]}", comparator));
    }
}
