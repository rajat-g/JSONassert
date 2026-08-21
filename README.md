# hungarian-json-assert

A Java library for comparing JSON documents in tests — **API-compatible with [JSONassert](https://github.com/skyscreamer/JSONassert)** — with one key difference: when arrays need to be matched without strict ordering, elements are paired using the **Hungarian algorithm (Kuhn–Munkres)** to find the *globally optimal* expected-to-actual assignment instead of relying on greedy first-match heuristics.

The full upstream JSONassert test suite (154 tests) runs against this library **unchanged**, so it is a drop-in replacement.

## Features

- **Drop-in compatible** — same `org.skyscreamer.jsonassert` packages, classes, and failure message formats as JSONassert
- **All four compare modes** — `STRICT`, `LENIENT`, `NON_EXTENSIBLE`, `STRICT_ORDER`
- **Optimal array matching** — unordered arrays are paired via the Hungarian algorithm, avoiding false failures that greedy matching can produce
- **Custom value comparators** — plug your own matchers into specific JSON paths, including `**` and `*` wildcards
- **Rich results** — `JSONCompareResult` reports every field failure, missing/unexpected fields, and human-readable messages
- **Extended API** (`io.smart.jsonassert`) — additional conveniences such as match tracking (`getMatches()`) and wildcard customizations

## Requirements

- Java 17+
- Maven 3.9+ (to build)

## Installation

Build and install into your local Maven repository:

```bash
mvn clean install
```

Then add the dependency:

```xml
<dependency>
    <groupId>io.smart</groupId>
    <artifactId>hungarian-json-assert</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

## Quick start

```java
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class MyTest {
    @Test
    public void verifyJson() throws Exception {
        String actual = "{\"id\": 1, \"name\": \"Alice\", \"tags\": [\"admin\", \"user\"]}";

        // Strict: no extra fields, array order must match exactly
        JSONAssert.assertEquals("{\"id\": 1, \"name\": \"Alice\", \"tags\": [\"admin\", \"user\"]}", actual, JSONCompareMode.STRICT);

        // Lenient: extra fields ignored, array order does not matter
        JSONAssert.assertEquals("{\"id\": 1}", actual, JSONCompareMode.LENIENT);

        // Boolean shorthand: true = STRICT, false = LENIENT
        JSONAssert.assertEquals("{\"name\": \"Alice\"}", actual, false);
    }
}
```

## Compare modes

| Mode            | Extensible | Strict array order | Behavior                                                                 |
|-----------------|------------|--------------------|--------------------------------------------------------------------------|
| `STRICT`        | no         | yes                | No extra fields; arrays compared element-by-element in order            |
| `LENIENT`       | yes        | no                 | Extra fields allowed; unordered arrays matched optimally                 |
| `NON_EXTENSIBLE`| no         | no                 | No extra fields; unordered arrays matched optimally                      |
| `STRICT_ORDER`  | yes        | yes                | Extra fields allowed; array order must match                             |

```java
// Convert between modes programmatically
JSONCompareMode.STRICT.withStrictOrdering(false);  // -> NON_EXTENSIBLE
JSONCompareMode.LENIENT.withExtensible(false);     // -> NON_EXTENSIBLE
```

## Getting detailed results

Use `JSONCompare` directly to get a `JSONCompareResult` instead of throwing an assertion error:

```java
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareResult;

JSONCompareResult result = JSONCompare.compareJSON(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

if (result.failed()) {
    System.out.println(result.getMessage());   // human-readable diff, JSONAssert format
    result.getFieldFailures().forEach(f ->
        System.out.println(f.getField() + ": expected " + f.getExpected() + ", got " + f.getActual()));
    System.out.println("Missing fields: "    + result.getFieldMissing().size());
    System.out.println("Unexpected fields: " + result.getFieldUnexpected().size());
}
```

Example failure message:

```
id
Expected: 3
     got: 5
```

## Custom value comparators

Associate your own matcher with a JSON path via `Customization` + `CustomComparator`:

```java
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.ValueMatcher;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

ValueMatcher<Object> whitespaceInsensitive = (expected, actual) ->
    normalize(expected).equals(normalize(actual));

JSONAssert.assertEquals(
    "{\"note\": \"hello   world \"}",
    "{\"note\": \"hello world\"}",
    new CustomComparator(JSONCompareMode.NON_EXTENSIBLE,
        Customization.customization("note", whitespaceInsensitive)));
```

### Path wildcards

| Pattern              | Matches                                              |
|----------------------|------------------------------------------------------|
| `note`               | The field `note` anywhere at that exact path         |
| `items[*].label`     | The `label` field of **every** element of `items`    |
| `**.createdAt`       | A `createdAt` field at any depth                     |
| `data.**.id`         | Any number of path segments between `data` and `id`  |

### Built-in matchers

- `RegularExpressionValueMatcher` — match values against a regex (constant or dynamic from the expected value):

```java
// Every 'id' must be one or more digits
JSONAssert.assertEquals("{a:[{id:X}]}", arrayOfObjects,
    new CustomComparator(JSONCompareMode.STRICT_ORDER,
        new Customization("a[*].id", new RegularExpressionValueMatcher<Object>("\\d+"))));
```

- `ArraySizeComparator` — assert only on array sizes:

```java
// 'a' contains between 2 and 6 elements
JSONAssert.assertEquals("{a:[2,6]}", json, new ArraySizeComparator(JSONCompareMode.LENIENT));

// 'a' contains exactly 3 elements
JSONAssert.assertEquals("{a:[3]}", json, new ArraySizeComparator(JSONCompareMode.STRICT));
```

- `ArrayValueMatcher` — apply another comparator to specific array element(s), cycling through the expected array:

```java
// Verify every element of 'a' has type=row
JSONComparator comparator = new DefaultComparator(JSONCompareMode.LENIENT);
JSONAssert.assertEquals("{a:[{type:row}]}", json,
    new CustomComparator(JSONCompareMode.LENIENT,
        new Customization("a", new ArrayValueMatcher<>(comparator))));
```

### Writing your own comparator

For full control, extend `DefaultComparator` and override `compareValues`. The example below (available as a test-only class in `src/test/java/.../BlankAwareComparator.java`) treats two strings that are both blank after trimming (`""` vs `"   "`) as equal, while everything else follows the default rules:

```java
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;

public class BlankAwareComparator extends DefaultComparator {

    public BlankAwareComparator(JSONCompareMode mode) {
        super(mode);
    }

    @Override
    public void compareValues(String prefix, Object expectedValue, Object actualValue,
                              JSONCompareResult result) {
        if (expectedValue instanceof String && actualValue instanceof String
                && ((String) expectedValue).trim().isEmpty()
                && ((String) actualValue).trim().isEmpty()) {
            return; // both blank -> match
        }
        // non-blank strings and all non-string values: compare as usual
        super.compareValues(prefix, expectedValue, actualValue, result);
    }
}

// Usage:
JSONAssert.assertEquals(
    "{\"note\": \"\"}", "{\"note\": \"   \"}",
    new BlankAwareComparator(JSONCompareMode.NON_EXTENSIBLE));
```

> **Note:** inside *arrays of objects*, JSONassert first tries to pair elements by a unique key field (e.g. `id`) before any value comparison runs. Fields used as the pairing key therefore bypass custom comparators; make sure your objects carry a proper identity field when relying on custom value semantics inside arrays.

## How the Hungarian matching works

When a mode allows loose array ordering and elements cannot be matched by a unique key, this library:

1. Builds an n×m compatibility matrix where each cell is `1.0` if the expected element `i` deeply equals the actual element `j`, `0.0` otherwise.
2. Runs the **Hungarian algorithm** (`io.smart.jsonassert.internal.HungarianAlgorithm`, O(n²·m) shortest-augmenting-path implementation) to find the pairing that maximizes total matches.
3. Reports the first unmatched expected element using the standard JSONassert message.

Unlike greedy matching, which pairs elements in encounter order and can consume an actual element needed by a later expected one, Hungarian matching always finds a perfect pairing if one exists:

```
Expected: [{k: x}, {k: y}]      Actual: [{k: y}, {k: x}]
Greedy:   e0→a0 fails, then e0 unmatched ✗
Hungarian: e0→a1, e1→a0 ✓
```

## Extended API (`io.smart.jsonassert`)

Beyond the JSONassert-compatible surface, this library offers:

```java
import io.smart.jsonassert.SmartJsonAssert;
import io.smart.jsonassert.JsonCompareResult;

JsonCompareResult result = SmartJsonAssert.compare(expected, actual, JsonCompareMode.NON_EXTENSIBLE);

result.getMatches();    // every successfully matched leaf (path + expected + actual)
result.getMatchCount();
result.getFailureCount();
SmartJsonAssert.assertEquals(expected, actual, comparator); // works with both comparator families
```

Customizations here additionally support single-segment `*` wildcards (`team.members[*].role`) and custom matchers also feed the similarity matrix used during array pairing.

## Project structure

```
src/main/java/
├── io/smart/jsonassert/                  # extended API
│   ├── SmartJsonAssert.java
│   ├── JsonCompareMode.java
│   ├── JsonCompareResult.java
│   ├── DefaultJsonComparator.java / CustomComparator.java / Customization.java
│   └── internal/
│       ├── HungarianAlgorithm.java       # Kuhn–Munkres assignment (O(n²·m))
│       └── SimilarityScorer.java
└── org/skyscreamer/jsonassert/           # JSONassert-compatible API
    ├── JSONAssert.java                   # static assertEquals/assertNotEquals entry points
    ├── JSONCompare.java                  # programmatic comparison API
    ├── JSONCompareMode.java              # STRICT / LENIENT / NON_EXTENSIBLE / STRICT_ORDER
    ├── JSONCompareResult.java            # detailed results & messages
    ├── Customization.java / ValueMatcher.java / ...
    └── comparator/
        ├── AbstractComparator.java       # contains Hungarian-based array matching
        ├── DefaultComparator.java
        ├── CustomComparator.java / ArraySizeComparator.java
        └── JSONCompareUtil.java
src/test/java/
├── org/skyscreamer/jsonassert/           # full upstream JSONassert test suite (unchanged)
└── io/smart/jsonassert/                  # tests for the extended API & engine
```

## Building and testing

```bash
mvn test      # runs 176 tests: upstream suite + extended API tests
mvn install   # build and install locally
```

## Credits and license

This project implements the API of, and includes test cases from, [JSONassert](https://github.com/skyscreamer/JSONassert) by SkyScreamer. The comparison engine's unordered-array matching is replaced with a Hungarian-algorithm-based optimal assignment.

Licensed under the [Apache License 2.0](LICENSE).
