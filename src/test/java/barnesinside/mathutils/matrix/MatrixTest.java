package barnesinside.mathutils.matrix;

import org.junit.jupiter.api.Test;

class MatrixTest {

    private static final double EPS = 1e-10;

    private static Matrix m(double[][] values) {
        return Matrix.of(values);
    }

    @Test
    void ofCopiesInput() {
        double[][] values = {{1, 2}, {3, 4}};
        Matrix a = m(values);
        values[0][0] = 99;
        assert a.get(0, 0) == 1;
    }

    @Test
    void rejectsRaggedAndEmpty() {
        try {
            Matrix.of(new double[][]{{1, 2}, {3}});
            assert false;
        } catch (IllegalArgumentException e) {
            assert true;
        }
        try {
            Matrix.of(new double[0][0]);
            assert false;
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void addSubtractScale() {
        Matrix a = m(new double[][]{{1, 2}, {3, 4}});
        Matrix b = m(new double[][]{{4, 3}, {2, 1}});
        assert a.add(b).equals(m(new double[][]{{5, 5}, {5, 5}}));
        assert a.subtract(b).equals(m(new double[][]{{-3, -1}, {1, 3}}));
        assert a.multiply(2).equals(m(new double[][]{{2, 4}, {6, 8}}));
    }

    @Test
    void multiplyMatrices() {
        Matrix a = m(new double[][]{{1, 2, 3}, {4, 5, 6}});
        Matrix b = m(new double[][]{{7, 8}, {9, 10}, {11, 12}});
        assert a.multiply(b).equals(m(new double[][]{{58, 64}, {139, 154}}));
    }

    @Test
    void multiplyIncompatibleThrows() {
        try {
            m(new double[][]{{1, 2}}).multiply(m(new double[][]{{1, 2}}));
            assert false;
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void transposeAndTrace() {
        Matrix a = m(new double[][]{{1, 2, 3}, {4, 5, 6}});
        assert a.transpose().equals(m(new double[][]{{1, 4}, {2, 5}, {3, 6}}));
        assert m(new double[][]{{1, 2}, {3, 4}}).trace() == 5;
    }

    @Test
    void traceOfNonSquareThrows() {
        try {
            m(new double[][]{{1, 2, 3}}).trace();
            assert false;
        } catch (IllegalStateException e) {
            assert true;
        }
    }

    @Test
    void determinant() {
        assert Math.abs(m(new double[][]{{1, 2}, {3, 4}}).determinant() + 2) < EPS;
        // Requiere pivoteo: el primer pivote es 0
        assert Math.abs(m(new double[][]{{0, 1}, {1, 0}}).determinant() + 1) < EPS;
        assert Math.abs(Matrix.identity(4).determinant() - 1) < EPS;
    }

    @Test
    void determinantThreeByThree() {
        Matrix a = m(new double[][]{{6, 1, 1}, {4, -2, 5}, {2, 8, 7}});
        assert Math.abs(a.determinant() + 306) < 1e-9;
    }

    @Test
    void singularDeterminantIsZero() {
        assert m(new double[][]{{1, 2}, {2, 4}}).determinant() == 0;
    }

    @Test
    void inverse() {
        Matrix a = m(new double[][]{{4, 7}, {2, 6}});
        Matrix inv = a.inverse();
        assert inv.isCloseTo(m(new double[][]{{0.6, -0.7}, {-0.2, 0.4}}), EPS);
        assert a.multiply(inv).isCloseTo(Matrix.identity(2), EPS);
    }

    @Test
    void inverseOfSingularThrows() {
        try {
            m(new double[][]{{1, 2}, {2, 4}}).inverse();
            assert false;
        } catch (ArithmeticException e) {
            assert true;
        }
    }

    @Test
    void solveSystem() {
        // 2x + y - z = 8; -3x - y + 2z = -11; -2x + y + 2z = -3  ->  (2, 3, -1)
        Matrix a = m(new double[][]{{2, 1, -1}, {-3, -1, 2}, {-2, 1, 2}});
        double[] x = a.solve(new double[]{8, -11, -3});
        assert Math.abs(x[0] - 2) < EPS && Math.abs(x[1] - 3) < EPS && Math.abs(x[2] + 1) < EPS;
    }

    @Test
    void luReconstructs() {
        Matrix a = m(new double[][]{{0, 2, 1}, {1, 1, 0}, {3, 0, 1}});
        Matrix.LU lu = a.lu();
        assert lu.p().multiply(a).isCloseTo(lu.l().multiply(lu.u()), EPS);
    }

    @Test
    void rank() {
        assert m(new double[][]{{1, 2}, {2, 4}}).rank() == 1;
        assert m(new double[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}).rank() == 2;
        assert Matrix.identity(3).rank() == 3;
        assert Matrix.zeros(2, 3).rank() == 0;
    }
}
