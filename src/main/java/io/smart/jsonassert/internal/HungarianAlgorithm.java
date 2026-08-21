package io.smart.jsonassert.internal;

import java.util.Arrays;

/**
 * Hungarian (Kuhn-Munkres) algorithm for the rectangular assignment problem,
 * implemented with potentials via shortest augmenting paths in O(n^2 * m).
 *
 * Finds a one-to-one assignment of rows to columns that MINIMIZES total cost.
 * To maximize similarity instead, pass cost = -similarity.
 */
public final class HungarianAlgorithm {

    private static final double INF = Double.POSITIVE_INFINITY;

    private final int rows;
    private final int cols;
    private final int[] resultByRow;

    public HungarianAlgorithm(double[][] costMatrix) {
        this.rows = costMatrix.length;
        this.cols = rows > 0 ? costMatrix[0].length : 0;
        this.resultByRow = solve(costMatrix);
    }

    private int[] solve(double[][] a) {
        int n = rows;
        int m = cols;
        double[] u = new double[n + 1];
        double[] v = new double[m + 1];
        // p[j] = row assigned to column j (1-based internally; index 0 is virtual)
        int[] p = new int[m + 1];
        int[] way = new int[m + 1];

        if (n == 0 || m == 0) {
            return new int[rows];
        }

        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            double[] minv = new double[m + 1];
            boolean[] used = new boolean[m + 1];
            Arrays.fill(minv, INF);

            do {
                used[j0] = true;
                int i0 = p[j0];
                double delta = INF;
                int j1 = -1;
                for (int j = 1; j <= m; j++) {
                    if (!used[j]) {
                        double cur = a[i0 - 1][j - 1] - u[i0] - v[j];
                        if (cur < minv[j]) {
                            minv[j] = cur;
                            way[j] = j0;
                        }
                        if (minv[j] < delta) {
                            delta = minv[j];
                            j1 = j;
                        }
                    }
                }
                if (j1 == -1) {
                    break;
                }
                for (int j = 0; j <= m; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }
                j0 = j1;
            } while (p[j0] != 0);

            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] result = new int[rows];
        Arrays.fill(result, -1);
        for (int col = 1; col <= m; col++) {
            if (p[col] >= 1) {
                result[p[col] - 1] = col - 1;
            }
        }
        return result;
    }

    /** Returns array where result[row] = assigned column, or -1 when unassigned. */
    public int[] getResult() {
        return resultByRow.clone();
    }

    /** Returns the column assigned to the given row, or -1. */
    public int getAssignedColumn(int row) {
        return resultByRow[row];
    }
}
