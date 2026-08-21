package io.smart.jsonassert.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class HungarianAlgorithmTest {

    @Test
    void squareMatrixMinimizesTotalCost() {
        double[][] cost = {
                {4, 1, 3},
                {2, 0, 5},
                {3, 2, 2}
        };
        // Optimal: row0->col1(1), row1->col0(2), row2->col2(2) = 5
        int[] expected = {1, 0, 2};
        assertArrayEquals(expected, new HungarianAlgorithm(cost).getResult());
    }

    @Test
    void rectangularMoreColumnsThanRows() {
        double[][] cost = {
                {10, 19, 8, 15},
                {10, 18, 7, 17}
        };
        // Optimal: row0->col0 (10), row1->col2 (7) = 17
        int[] result = new HungarianAlgorithm(cost).getResult();
        assertArrayEquals(new int[] {0, 2}, result);
    }

    @Test
    void maximizesSimilarityWhenNegated() {
        double[][] sim = {
                {0.9, 0.4},
                {0.5, 0.8}
        };
        double[][] cost = negate(sim);
        // Best total similarity = 0.9 + 0.8 = 1.7 -> row0->col0, row1->col1
        assertArrayEquals(new int[] {0, 1}, new HungarianAlgorithm(cost).getResult());
    }

    @Test
    void greedyTrapCase() {
        // Greedy: row0 takes col0 (cost 1), leaving row1 with col1 (cost 100) => 101
        // Optimal: row0 takes col1 (cost 2), row1 takes col0 (cost 1) => 3
        double[][] cost = {
                {1, 2},
                {1, 100}
        };
        assertArrayEquals(new int[] {1, 0}, new HungarianAlgorithm(cost).getResult());
    }

    private static double[][] negate(double[][] m) {
        double[][] r = new double[m.length][m[0].length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[i].length; j++)
                r[i][j] = -m[i][j];
        return r;
    }
}
