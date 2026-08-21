package io.smart.jsonassert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartJsonAssertTest {

    @Test
    void nonExtensibleRejectsExtraFieldsInActual() {
        String expected = "{\"name\":\"Alice\",\"age\":30}";
        String actual = "{\"name\":\"Alice\",\"age\":30,\"city\":\"Berlin\"}";
        var result = SmartJsonAssert.compare(expected, actual, JsonCompareMode.NON_EXTENSIBLE);
        assertFalse(result.succeeded());
        assertTrue(result.getFailures().stream()
                .anyMatch(f -> f.getMessage().contains("Unexpected field")));
    }

    @Test
    void nonExtensiblePassesWhenAllFieldsMatchExactly() {
        String expected = "{\"name\":\"Alice\",\"age\":30}";
        String actual = "{\"name\":\"Alice\",\"age\":30}";
        assertDoesNotThrow(() -> SmartJsonAssert.assertEquals(expected, actual, JsonCompareMode.NON_EXTENSIBLE));
    }

    @Test
    void nonExtensibleFailsWhenExpectedFieldMissing() {
        String expected = "{\"name\":\"Alice\",\"age\":30}";
        String actual = "{\"name\":\"Alice\"}";
        var result = SmartJsonAssert.compare(expected, actual, JsonCompareMode.NON_EXTENSIBLE);
        assertFalse(result.succeeded());
    }

    @Test
    void strictReportsExtraFields() {
        String expected = "{\"a\":1}";
        String actual = "{\"a\":1,\"b\":2}";
        var result = SmartJsonAssert.compare(expected, actual, JsonCompareMode.STRICT);
        assertFalse(result.succeeded());
    }

    @Test
    void lenientIgnoresExtraFields() {
        String expected = "{\"a\":1}";
        String actual = "{\"a\":1,\"b\":2}";
        assertDoesNotThrow(() -> SmartJsonAssert.assertEquals(expected, actual, JsonCompareMode.LENIENT));
    }

    @Test
    void hungarianMatchesReorderedArrayElementsOptimally() {
        String expected = """
                [
                  {"id": 3, "score": 91},
                  {"id": 1, "score": 95},
                  {"id": 2, "score": 88}
                ]
                """;
        String actual = """
                [
                  {"id": 2, "score": 88},
                  {"id": 1, "score": 95},
                  {"id": 3, "score": 91}
                ]
                """;
        var result = SmartJsonAssert.compare(expected, actual, JsonCompareMode.NON_EXTENSIBLE);
        assertTrue(result.succeeded(), result::toString);
    }

    @Test
    void strictEnforcesArrayElementOrder() {
        String expected = "[{\"k\":\"x\",\"v\":10},{\"k\":\"y\",\"v\":20}]";
        String actual = "[{\"k\":\"y\",\"v\":20},{\"k\":\"x\",\"v\":10}]";
        var result = SmartJsonAssert.compare(expected, actual, JsonCompareMode.STRICT);
        assertFalse(result.succeeded());
    }

    @Test
    void hungarianPicksGloballyBestPairingNotGreedy() {
        // Greedy would match e0->a0; optimal is e0->a1, e1->a0
        String expected = "[{\"k\":\"x\",\"v\":10},{\"k\":\"y\",\"v\":20}]";
        String actual = "[{\"k\":\"y\",\"v\":20},{\"k\":\"x\",\"v\":10}]";
        var result = SmartJsonAssert.compare(expected, actual, JsonCompareMode.NON_EXTENSIBLE);
        assertTrue(result.succeeded(), result::toString);
    }

    @Test
    void missingArrayElementIsReported() {
        String expected = "[{\"id\":1},{\"id\":2},{\"id\":3}]";
        String actual = "[{\"id\":1},{\"id\":2}]";
        var result = SmartJsonAssert.compare(expected, actual, JsonCompareMode.NON_EXTENSIBLE);
        assertFalse(result.succeeded());
        assertTrue(result.getFailures().stream()
                .anyMatch(f -> f.getMessage().contains("No matching element")));
    }

    @Test
    void nestedArraysAndObjectsComparedRecursively() {
        String expected = """
                {"team": {"members": [{"role":"dev","skills":["java","sql"]}]}}
                """;
        String actual = """
                {"team": {"members": [{"skills":["java","sql"],"role":"dev"}]}}
                """;
        var result = SmartJsonAssert.compare(expected, actual, JsonCompareMode.NON_EXTENSIBLE);
        assertTrue(result.succeeded(), result::toString);
    }

    @Test
    void primitiveMismatchReportedAtCorrectPath() {
        String expected = "{\"user\":{\"age\":30}}";
        String actual = "{\"user\":{\"age\":31}}";
        var result = SmartJsonAssert.compare(expected, actual, JsonCompareMode.LENIENT);
        assertFalse(result.succeeded());
        assertEquals("user.age", result.getFailures().get(0).getPath());
    }

    private void assertEquals(String a, String b) {
        org.junit.jupiter.api.Assertions.assertEquals(a, b);
    }
}
