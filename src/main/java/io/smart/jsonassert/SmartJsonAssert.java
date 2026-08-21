package io.smart.jsonassert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Entry point API, mirroring JSONAssert. Usage:
 * <pre>
 *     SmartJsonAssert.assertEquals(expectedJson, actualJson, JsonCompareMode.NON_EXTENSIBLE);
 *
 *     JsonComparator comparator = new CustomComparator(
 *             JsonCompareMode.NON_EXTENSIBLE,
 *             Customization.customization("note", (e, a) -&gt; normalize(e).equals(normalize(a))));
 *     SmartJsonAssert.assertEquals(expectedJson, actualJson, comparator);
 * </pre>
 */
public final class SmartJsonAssert {

    private SmartJsonAssert() {
    }

    public static JsonCompareResult compare(String expected, String actual, JsonCompareMode mode) {
        return compare(expected, actual, new DefaultJsonComparator(mode));
    }

    public static JsonCompareResult compare(String expected, String actual, JsonComparator comparator) {
        JsonCompareResult result = new JsonCompareResult();
        comparator.compare(parse(expected), parse(actual), result);
        return result;
    }

    public static void assertEquals(String expected, String actual, JsonCompareMode mode) {
        assertSuccess(compare(expected, actual, mode));
    }

    public static void assertEquals(String expected, String actual, JsonComparator comparator) {
        assertSuccess(compare(expected, actual, comparator));
    }

    public static void assertEquals(Object expected, Object actual, JsonCompareMode mode) {
        assertSuccess(compareObjects(expected, actual, mode));
    }

    public static void assertEquals(Object expected, Object actual, JsonComparator comparator) {
        JsonCompareResult result = new JsonCompareResult();
        comparator.compare(expected, actual, result);
        assertSuccess(result);
    }

    private static JsonCompareResult compareObjects(Object expected, Object actual, JsonCompareMode mode) {
        JsonCompareResult result = new JsonCompareResult();
        new DefaultJsonComparator(mode).compare(expected, actual, result);
        return result;
    }

    public static JsonCompareResult compare(Object expected, Object actual, JsonCompareMode mode) {
        return compareObjects(expected, actual, mode);
    }

    private static void assertSuccess(JsonCompareResult result) {
        if (result.failed()) {
            throw new AssertionError(result.getMessage());
        }
    }

    private static Object parse(String json) {
        JSONTokener tokener = new JSONTokener(json);
        char first = skipWhitespace(json);
        try {
            return (first == '[')
                    ? new JSONArray(tokener)
                    : new JSONObject(tokener);
        } catch (JSONException e) {
            throw new JSONException("Invalid JSON input: " + json, e);
        }
    }

    private static char skipWhitespace(String json) {
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }
        throw new JSONException("Empty or blank JSON input");
    }
}
