/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.skyscreamer.jsonassert.comparator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompareResult;
import io.smart.jsonassert.internal.HungarianAlgorithm;
import io.smart.jsonassert.internal.SimilarityScorer;

import java.util.*;

import static org.skyscreamer.jsonassert.comparator.JSONCompareUtil.*;

/**
 * This class provides a skeletal implementation of the {@link JSONComparator}
 * interface, to minimize the effort required to implement this interface.
 *
 *
 */
public abstract class AbstractComparator implements JSONComparator {

    /**
     * Minimum structural similarity for two array elements to be considered
     * a pairing candidate by the Hungarian matcher.
     */
    protected static final double MATCH_THRESHOLD = 0.5;

    /**
     * Default constructor
     */
    public AbstractComparator() {
    }

    /**
     * Compares JSONObject provided to the expected JSONObject, and returns the results of the comparison.
     *
     * @param expected Expected JSONObject
     * @param actual   JSONObject to compare
     */
    @Override
    public final JSONCompareResult compareJSON(JSONObject expected, JSONObject actual) {
        JSONCompareResult result = new JSONCompareResult();
        compareJSON("", expected, actual, result);
        return result;
    }

    /**
     * Compares JSONArray provided to the expected JSONArray, and returns the results of the comparison.
     *
     * @param expected Expected JSONArray
     * @param actual   JSONArray to compare
     */
    @Override
    public final JSONCompareResult compareJSON(JSONArray expected, JSONArray actual) {
        JSONCompareResult result = new JSONCompareResult();
        compareJSONArray("", expected, actual, result);
        return result;
    }

    /**
     * @param prefix
     * @param expected
     * @param actual
     * @param result
     */
    protected void checkJsonObjectKeysActualInExpected(String prefix, JSONObject expected, JSONObject actual, JSONCompareResult result) {
        Set<String> actualKeys = getKeys(actual);
        for (String key : actualKeys) {
            if (!expected.has(key)) {
                result.unexpected(prefix, key);
            }
        }
    }

    /**
     *
     * @param prefix
     * @param expected
     * @param actual
     * @param result
     */
    protected void checkJsonObjectKeysExpectedInActual(String prefix, JSONObject expected, JSONObject actual, JSONCompareResult result) {
        Set<String> expectedKeys = getKeys(expected);
        for (String key : expectedKeys) {
            Object expectedValue = expected.get(key);
            if (actual.has(key)) {
                Object actualValue = actual.get(key);
                compareValues(qualify(prefix, key), expectedValue, actualValue, result);
            } else {
                result.missing(prefix, key);
            }
        }
    }

    protected void compareJSONArrayOfJsonObjects(String key, JSONArray expected, JSONArray actual, JSONCompareResult result) {
        String uniqueKey = findUniqueKey(expected);
        if (uniqueKey == null || !isUsableAsUniqueKey(uniqueKey, actual)) {
            // An expensive last resort
            recursivelyCompareJSONArray(key, expected, actual, result);
            return;
        }
        Map<Object, JSONObject> expectedValueMap = arrayOfJsonObjectToMap(expected, uniqueKey);
        Map<Object, JSONObject> actualValueMap = arrayOfJsonObjectToMap(actual, uniqueKey);
        for (Object id : expectedValueMap.keySet()) {
            if (!actualValueMap.containsKey(id)) {
                result.missing(formatUniqueKey(key, uniqueKey, id), expectedValueMap.get(id));
                continue;
            }
            JSONObject expectedValue = expectedValueMap.get(id);
            JSONObject actualValue = actualValueMap.get(id);
            compareValues(formatUniqueKey(key, uniqueKey, id), expectedValue, actualValue, result);
        }
        for (Object id : actualValueMap.keySet()) {
            if (!expectedValueMap.containsKey(id)) {
                result.unexpected(formatUniqueKey(key, uniqueKey, id), actualValueMap.get(id));
            }
        }
    }

    protected void compareJSONArrayOfSimpleValues(String key, JSONArray expected, JSONArray actual, JSONCompareResult result) {
        Map<Object, Integer> expectedCount = JSONCompareUtil.getCardinalityMap(jsonArrayToList(expected));
        Map<Object, Integer> actualCount = JSONCompareUtil.getCardinalityMap(jsonArrayToList(actual));
        for (Object o : expectedCount.keySet()) {
            if (!actualCount.containsKey(o)) {
                result.missing(key + "[]", o);
            } else if (!actualCount.get(o).equals(expectedCount.get(o))) {
                result.fail(key + "[]: Expected " + expectedCount.get(o) + " occurrence(s) of " + o
                        + " but got " + actualCount.get(o) + " occurrence(s)");
            }
        }
        for (Object o : actualCount.keySet()) {
            if (!expectedCount.containsKey(o)) {
                result.unexpected(key + "[]", o);
            }
        }
    }

    protected void compareJSONArrayWithStrictOrder(String key, JSONArray expected, JSONArray actual, JSONCompareResult result) {
        for (int i = 0; i < expected.length(); ++i) {
            Object expectedValue = JSONCompareUtil.getObjectOrNull(expected, i);
            Object actualValue = JSONCompareUtil.getObjectOrNull(actual, i);
            compareValues(key + "[" + i + "]", expectedValue, actualValue, result);
        }
    }

    /**
     * Matches array elements with loose ordering using the Hungarian algorithm
     * (Kuhn-Munkres) over a structural-similarity matrix, so the globally
     * optimal expected-to-actual pairing is found without any assumptions
     * about unique keys or element identity.
     *
     * <p>Paired elements are then compared field-by-field so detailed
     * differences are recorded. An expected element whose best available
     * counterpart is too dissimilar is reported with a standard
     * "Could not find match" failure.</p>
     */
    protected void recursivelyCompareJSONArray(String key, JSONArray expected, JSONArray actual,
                                               JSONCompareResult result) {
        int n = expected.length();
        int m = actual.length();
        SimilarityScorer scorer = new SimilarityScorer();
        double[][] similarity = new double[n][m];
        for (int i = 0; i < n; i++) {
            Object expectedElement = JSONCompareUtil.getObjectOrNull(expected, i);
            for (int j = 0; j < m; j++) {
                similarity[i][j] = scorer.similarity(
                        expectedElement, JSONCompareUtil.getObjectOrNull(actual, j), key + "[" + i + "]");
            }
        }

        HungarianAlgorithm hungarian = new HungarianAlgorithm(negate(similarity));
        int[] assignment = hungarian.getResult();

        boolean[] usedActual = new boolean[m];
        for (int i = 0; i < n; i++) {
            int j = assignment[i];
            if (j >= 0 && !usedActual[j] && similarity[i][j] >= MATCH_THRESHOLD) {
                usedActual[j] = true;
                compareValues(key + "[" + i + "]",
                        JSONCompareUtil.getObjectOrNull(expected, i),
                        JSONCompareUtil.getObjectOrNull(actual, j), result);
            } else {
                result.fail(key + "[" + i + "] Could not find match for element "
                        + JSONCompareUtil.getObjectOrNull(expected, i));
                return;
            }
        }
    }

    private static double[][] negate(double[][] matrix) {
        double[][] negated = new double[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            negated[i] = new double[matrix[i].length];
            for (int j = 0; j < matrix[i].length; j++) {
                negated[i][j] = -matrix[i][j];
            }
        }
        return negated;
    }
}
