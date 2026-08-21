package io.smart.jsonassert;

/** A single field-level failure found during comparison. */
public final class FieldComparisonFailure {

    private final String path;
    private final String message;
    private final Object expected;
    private final Object actual;

    FieldComparisonFailure(String path, String message, Object expected, Object actual) {
        this.path = path;
        this.message = message;
        this.expected = expected;
        this.actual = actual;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }

    public Object getExpected() {
        return expected;
    }

    public Object getActual() {
        return actual;
    }

    @Override
    public String toString() {
        return path + ": " + message
                + " | expected=" + expected
                + " | actual=" + actual;
    }
}
