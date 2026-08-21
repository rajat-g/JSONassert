package io.smart.jsonassert;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderDifferenceTest {

    @Test
    void reorderedObjectArrayPassesAndReportsOrderDifference() {
        JSONCompareResult result = JSONCompare.compareJSON(
                "{\"a\": [{\"id\": 1}, {\"id\": 2}, {\"id\": 3}]}",
                "{\"a\": [{\"id\": 3}, {\"id\": 1}, {\"id\": 2}]}",
                JSONCompareMode.LENIENT);

        assertTrue(result.passed());
        assertTrue(result.isReorderedOnField());
        assertEquals(3, result.getFieldReordered().size());
        assertEquals("a[0]", result.getFieldReordered().get(0).getField());
        assertEquals(0, result.getFieldReordered().get(0).getExpected());
        assertEquals(1, result.getFieldReordered().get(0).getActual());
    }

    @Test
    void reorderSummaryAppearsInMessageWhenComparisonPasses() {
        JSONCompareResult result = JSONCompare.compareJSON(
                "[{\"id\": 1}, {\"id\": 2}]",
                "[{\"id\": 2}, {\"id\": 1}]",
                JSONCompareMode.LENIENT);

        assertTrue(result.passed());
        String message = result.getMessage();
        assertTrue(message.contains("2 element(s) matched in different order"));
        assertTrue(message.contains("[0]: expected at index 0, found at index 1"));
    }

    @Test
    void sameOrderProducesNoReorderEntries() {
        JSONCompareResult result = JSONCompare.compareJSON(
                "[{\"id\": 1}, {\"id\": 2}]",
                "[{\"id\": 1}, {\"id\": 2}]",
                JSONCompareMode.NON_EXTENSIBLE);

        assertTrue(result.passed());
        assertFalse(result.isReorderedOnField());
    }

    @Test
    void simpleValueReorderIsDetected() {
        JSONCompareResult result = JSONCompare.compareJSON(
                "{\"t\": [1, 2, 3]}",
                "{\"t\": [3, 1, 2]}",
                JSONCompareMode.LENIENT);

        assertTrue(result.passed());
        assertTrue(result.isReorderedOnField());
        assertEquals(3, result.getFieldReordered().size());
        assertEquals("t[1]", result.getFieldReordered().get(1).getField());
        assertEquals(1, result.getFieldReordered().get(1).getExpected());
        assertEquals(2, result.getFieldReordered().get(1).getActual());
    }

    @Test
    void strictModeStillFailsOnReorderWithoutReorderEntries() {
        JSONCompareResult result = JSONCompare.compareJSON(
                "[{\"id\": 1}, {\"id\": 2}]",
                "[{\"id\": 2}, {\"id\": 1}]",
                JSONCompareMode.STRICT);

        assertTrue(result.failed());
        assertFalse(result.isReorderedOnField());
    }

    @Test
    void reorderInfoAvailableViaGetterWhenOtherFailuresExist() {
        JSONCompareResult result = JSONCompare.compareJSON(
                "{\"x\": [{\"id\": 2}, {\"id\": 1}], \"y\": \"old\"}",
                "{\"x\": [{\"id\": 1}, {\"id\": 2}], \"y\": \"new\"}",
                JSONCompareMode.LENIENT);

        assertTrue(result.failed());
        assertTrue(result.isReorderedOnField());
        // failure message stays clean by default...
        assertFalse(result.getMessage().contains("matched in different order"));
        // ...but the summary can be requested explicitly
        assertTrue(result.getMessage(true).contains("matched in different order"));
    }
}
