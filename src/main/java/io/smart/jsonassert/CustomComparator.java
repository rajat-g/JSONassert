package io.smart.jsonassert;

/**
 * A comparator combining a {@link JsonCompareMode} with user-supplied
 * {@link Customization}s, mirroring JSONAssert's CustomComparator.
 *
 * <pre>
 * JsonComparator comparator = new CustomComparator(
 *         JsonCompareMode.NON_EXTENSIBLE,
 *         Customization.customization("note", (e, a) -&gt; normalize(e).equals(normalize(a))));
 * SmartJsonAssert.assertEquals(expected, actual, comparator);
 * </pre>
 */
public class CustomComparator extends DefaultJsonComparator {

    public CustomComparator(JsonCompareMode mode, Customization... customizations) {
        super(mode, customizations);
    }
}
