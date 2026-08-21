# Contributing

Thanks for your interest in improving this library!

## Development setup

- Java 17+ (JDK 17 or 21)
- Maven 3.9+ — or just use the included wrapper (`./mvnw` / `mvnw.cmd`)

```bash
./mvnw test        # run the full suite (upstream JSONassert tests + our tests)
./mvnw javadoc:javadoc   # build the Javadoc
```

## Before opening a PR

1. **Add a test.** Behavior changes need coverage. For comparison-behavior changes, include the
   exact expected/actual JSON pair and the expected `JSONCompareResult` message where relevant.
2. **Keep upstream compatibility.** The ported JSONassert test suite
   (`src/test/java/org/skyscreamer/jsonassert/...`) is our compatibility contract. If your change
   intentionally alters behavior, update those assertions *and* document the divergence in
   `CHANGELOG.md`.
3. **Array matching changes** must respect the core design: no unique-key or identity heuristics;
   pairing decisions come from the Hungarian assignment over the similarity matrix
   (`io.smart.jsonassert.internal.HungarianAlgorithm` / `SimilarityScorer`).
4. Run `./mvnw test` and make sure everything passes.

## Code style

- Java 17, no comments unless explaining non-obvious logic
- Public API classes in `org.skyscreamer.jsonassert` mirror upstream naming/signatures; new
  convenience APIs belong in `io.smart.jsonassert`
- Keep failure message formats stable — users assert on them

## Reporting bugs

Open an issue using the bug report template and include the minimal expected/actual JSON pair.
