import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

/**
 * Runnable tour of the library. Run with:
 *
 * <pre>
 *   mvn -q package
 *   java -cp target/classes;examples;"~/.m2/repository/org/json/json/20240303/json-20240303.jar" Examples
 * </pre>
 * (use ":" instead of ";" as the classpath separator on Linux/macOS)
 */
public final class Examples {

    private Examples() {
    }

    public static void main(String[] args) throws Exception {
        modes();
        detailedResults();
        optimalArrayMatching();
    }

    /** The four compare modes. */
    static void modes() throws Exception {
        String expected = "{\"id\": 1, \"name\": \"Alice\", \"tags\": [\"admin\", \"user\"]}";
        String actual = "{\"id\": 1, \"name\": \"Alice\", \"extra\": true, \"tags\": [\"user\", \"admin\"]}";

        // LENIENT ignores extra fields and array order -> passes
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT);

        // NON_EXTENSIBLE rejects the extra field, but still matches array order-insensitively
        JSONCompareResult result = JSONCompare.compareJSON(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        System.out.println("[modes] NON_EXTENSIBLE failed = " + result.failed());

        // STRICT also requires exact array order (fails here)
        result = JSONCompare.compareJSON(expected, actual, JSONCompareMode.STRICT);
        System.out.println("[modes] STRICT failed = " + result.failed());
        System.out.println();
    }

    /** Inspecting the diff programmatically instead of asserting. */
    static void detailedResults() {
        JSONCompareResult result = JSONCompare.compareJSON(
                "{\"user\": {\"name\": \"Bob\", \"age\": 30}}",
                "{\"user\": {\"name\": \"Alice\", \"age\": 30}}",
                JSONCompareMode.LENIENT);

        System.out.println("[result] message:");
        System.out.println(result.getMessage());
        result.getFieldFailures().forEach(f ->
                System.out.println("[result] failure at '" + f.getField() + "'"));
        System.out.println();
    }

    /**
     * Unordered arrays are paired by globally optimal assignment - no unique-key
     * or identity assumptions, and near-miss elements produce precise diffs.
     */
    static void optimalArrayMatching() {
        String expected = "{\"friends\": [{\"id\": 2, \"age\": 31}, {\"id\": 3}]}";
        String actual = "{\"friends\": [{\"id\": 3}, {\"id\": 2, \"age\": 30}]}";

        JSONCompareResult result = JSONCompare.compareJSON(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        System.out.println("[hungarian] message:");
        System.out.println(result.getMessage());
    }
}
