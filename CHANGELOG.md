# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [0.1.2] - 2026-11-13

### Added
- Path-specific detailed comparators that return field-level differences, allowing a selected array
  to use unordered matching while other arrays follow the outer comparison mode.
- A regression test covering detailed comparison of one unordered array within strict comparison.

### Added
- Order-difference reporting: when unordered arrays match with elements in a different
  position than expected, `JSONCompareResult` now records each reordering. Inspect via
  `isReorderedOnField()`, `getFieldReordered()`, and a summary appended to `getMessage()`
  for otherwise-passing comparisons (`getMessage(true)` to always include it). Reorder
  entries never affect pass/fail.

## [0.1.0] - 2026-08-22

### Added
- Full JSONassert-compatible API (`org.skyscreamer.jsonassert.*`): `JSONAssert`, `JSONCompare`,
  `JSONCompareMode` (STRICT / LENIENT / NON_EXTENSIBLE / STRICT_ORDER), `JSONCompareResult`,
  `Customization`, `CustomComparator`, `DefaultComparator`, `ArraySizeComparator`,
  `ArrayValueMatcher`, `RegularExpressionValueMatcher`.
- **Hungarian-algorithm array matching**: unordered arrays are paired via a globally optimal
  Kuhn–Munkres assignment (`io.smart.jsonassert.internal.HungarianAlgorithm`) instead of
  upstream's unique-key/greedy heuristics.
- Similarity-based pairing: near-miss array elements are paired with their best counterpart and
  produce precise field-level diffs (e.g. `friends[2].age Expected: 30, got: 31`).
- Extended API (`io.smart.jsonassert.*`): `SmartJsonAssert` with match tracking
  (`JsonCompareResult.getMatches()`), wildcard customizations, and comparator interop.

### Changed
- Removed upstream's unique-key pairing logic for arrays of objects; all loose-order arrays use
  optimal Hungarian matching. Failure messages for object arrays therefore use index-based paths
  with detailed field diffs rather than `[id=...]`-style messages.

### Compatibility
- The complete upstream JSONassert test suite (154 tests) passes against this library, with one
  test updated to reflect the improved array failure messages described above.
