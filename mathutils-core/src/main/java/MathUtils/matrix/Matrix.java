package MathUtils.matrix;

import java.util.Arrays;

/**
 * Matriz real inmutable de dimensión {@code rows × cols}.
 * <p>Las operaciones que no son posibles por las dimensiones lanzan
 * {@link IllegalArgumentException}; las que requieren una matriz cuadrada lanzan
 * {@link IllegalStateException}, y las que requieren una matriz invertible
 * lanzan {@link ArithmeticException}.
 */
public final class Matrix {

    private static final double EPSILON = 1e-12;

    private final double[][] data;
    private final int rows;
    private final int cols;

    /** Toma posesión del arreglo recibido. */
    private Matrix(double[][] data) {
        this.data = data;
        this.rows = data.length;
        this.cols = data[0].length;
    }

    /**
     * Crea una matriz copiando los valores recibidos.
     * @param values arreglo rectangular no vacío
     */
    public static Matrix of(double[][] values) {
        if (values.length == 0 || values[0].length == 0) {
            throw new IllegalArgumentException("La matriz no puede estar vacía");
        }
        double[][] copy = new double[values.length][];
        for (int i = 0; i < values.length; i++) {
            if (values[i].length != values[0].length) {
                throw new IllegalArgumentException("Todas las filas deben tener la misma longitud");
            }
            copy[i] = values[i].clone();
        }
        return new Matrix(copy);
    }

    public static Matrix zeros(int rows, int cols) {
        if (rows < 1 || cols < 1) throw new IllegalArgumentException("Las dimensiones deben ser positivas");
        return new Matrix(new double[rows][cols]);
    }

    public static Matrix identity(int n) {
        Matrix m = zeros(n, n);
        for (int i = 0; i < n; i++) m.data[i][i] = 1;
        return m;
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public boolean isSquare() {
        return rows == cols;
    }

    public double get(int row, int col) {
        return data[row][col];
    }

    /**
     * @return una copia de los valores de la matriz
     */
    public double[][] toArray() {
        double[][] copy = new double[rows][];
        for (int i = 0; i < rows; i++) copy[i] = data[i].clone();
        return copy;
    }

    public Matrix add(Matrix o) {
        requireSameShape(o);
        Matrix result = zeros(rows, cols);
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                result.data[i][j] = data[i][j] + o.data[i][j];
        return result;
    }

    public Matrix subtract(Matrix o) {
        requireSameShape(o);
        Matrix result = zeros(rows, cols);
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                result.data[i][j] = data[i][j] - o.data[i][j];
        return result;
    }

    public Matrix multiply(double scalar) {
        Matrix result = zeros(rows, cols);
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                result.data[i][j] = data[i][j] * scalar;
        return result;
    }

    public Matrix multiply(Matrix o) {
        if (cols != o.rows) {
            throw new IllegalArgumentException(
                    "No se puede multiplicar " + rows + "x" + cols + " por " + o.rows + "x" + o.cols);
        }
        Matrix result = zeros(rows, o.cols);
        for (int i = 0; i < rows; i++)
            for (int k = 0; k < cols; k++) {
                double a = data[i][k];
                for (int j = 0; j < o.cols; j++)
                    result.data[i][j] += a * o.data[k][j];
            }
        return result;
    }

    public Matrix transpose() {
        Matrix result = zeros(cols, rows);
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                result.data[j][i] = data[i][j];
        return result;
    }

    public double trace() {
        requireSquare();
        double sum = 0;
        for (int i = 0; i < rows; i++) sum += data[i][i];
        return sum;
    }

    /**
     * Determinante calculado con la descomposición LU con pivoteo parcial.
     */
    public double determinant() {
        Decomposition d = decompose();
        if (d.singular()) return 0;
        double det = d.sign();
        for (int i = 0; i < rows; i++) det *= d.lu()[i][i];
        return det;
    }

    /**
     * Rango: número de filas linealmente independientes (eliminación gaussiana con tolerancia).
     */
    public int rank() {
        double[][] a = toArray();
        double tolerance = EPSILON * scale();
        int rank = 0;
        for (int col = 0; col < cols && rank < rows; col++) {
            int pivot = rank;
            for (int i = rank + 1; i < rows; i++)
                if (Math.abs(a[i][col]) > Math.abs(a[pivot][col])) pivot = i;
            if (Math.abs(a[pivot][col]) <= tolerance) continue;

            double[] tmp = a[pivot];
            a[pivot] = a[rank];
            a[rank] = tmp;

            for (int i = rank + 1; i < rows; i++) {
                double f = a[i][col] / a[rank][col];
                for (int j = col; j < cols; j++) a[i][j] -= f * a[rank][j];
            }
            rank++;
        }
        return rank;
    }

    /**
     * Resuelve {@code A·x = b}.
     * @throws ArithmeticException si la matriz es singular
     */
    public double[] solve(double[] b) {
        Decomposition d = decompose();
        if (b.length != rows) throw new IllegalArgumentException("b debe tener " + rows + " elementos");
        if (d.singular()) throw new ArithmeticException("La matriz es singular");

        int n = rows;
        double[][] lu = d.lu();
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = b[d.perm()[i]];

        // Sustitución hacia adelante (L tiene diagonal unitaria)
        for (int i = 1; i < n; i++)
            for (int j = 0; j < i; j++)
                x[i] -= lu[i][j] * x[j];

        // Sustitución hacia atrás
        for (int i = n - 1; i >= 0; i--) {
            for (int j = i + 1; j < n; j++) x[i] -= lu[i][j] * x[j];
            x[i] /= lu[i][i];
        }
        return x;
    }

    /**
     * Resuelve {@code A·X = B} columna a columna.
     */
    public Matrix solve(Matrix b) {
        if (b.rows != rows) throw new IllegalArgumentException("B debe tener " + rows + " filas");
        Matrix result = zeros(rows, b.cols);
        for (int j = 0; j < b.cols; j++) {
            double[] column = new double[rows];
            for (int i = 0; i < rows; i++) column[i] = b.data[i][j];
            double[] x = solve(column);
            for (int i = 0; i < rows; i++) result.data[i][j] = x[i];
        }
        return result;
    }

    /**
     * @throws ArithmeticException si la matriz es singular
     */
    public Matrix inverse() {
        requireSquare();
        return solve(identity(rows));
    }

    /**
     * Descomposición LU con pivoteo parcial tal que {@code P·A = L·U}.
     * @throws ArithmeticException si la matriz es singular
     */
    public LU lu() {
        Decomposition d = decompose();
        if (d.singular()) throw new ArithmeticException("La matriz es singular");

        int n = rows;
        Matrix l = identity(n);
        Matrix u = zeros(n, n);
        Matrix p = zeros(n, n);
        for (int i = 0; i < n; i++) {
            p.data[i][d.perm()[i]] = 1;
            for (int j = 0; j < n; j++) {
                if (j < i) l.data[i][j] = d.lu()[i][j];
                else u.data[i][j] = d.lu()[i][j];
            }
        }
        return new LU(l, u, p);
    }

    /**
     * Resultado de {@link #lu()}: {@code P·A = L·U}.
     */
    public record LU(Matrix l, Matrix u, Matrix p) {
    }

    /**
     * Compara con una tolerancia absoluta elemento a elemento.
     */
    public boolean isCloseTo(Matrix o, double epsilon) {
        if (rows != o.rows || cols != o.cols) return false;
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                if (Math.abs(data[i][j] - o.data[i][j]) > epsilon) return false;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Matrix m && Arrays.deepEquals(data, m.data);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            if (i > 0) sb.append('\n');
            sb.append(Arrays.toString(data[i]));
        }
        return sb.toString();
    }

    private record Decomposition(double[][] lu, int[] perm, int sign, boolean singular) {
    }

    private Decomposition decompose() {
        requireSquare();
        int n = rows;
        double[][] lu = toArray();
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) perm[i] = i;

        int sign = 1;
        boolean singular = false;
        double tolerance = EPSILON * scale();

        for (int k = 0; k < n; k++) {
            int pivot = k;
            for (int i = k + 1; i < n; i++)
                if (Math.abs(lu[i][k]) > Math.abs(lu[pivot][k])) pivot = i;

            if (Math.abs(lu[pivot][k]) <= tolerance) {
                singular = true;
                continue;
            }

            if (pivot != k) {
                double[] tmp = lu[pivot];
                lu[pivot] = lu[k];
                lu[k] = tmp;
                int t = perm[pivot];
                perm[pivot] = perm[k];
                perm[k] = t;
                sign = -sign;
            }

            for (int i = k + 1; i < n; i++) {
                lu[i][k] /= lu[k][k];
                double f = lu[i][k];
                for (int j = k + 1; j < n; j++) lu[i][j] -= f * lu[k][j];
            }
        }
        return new Decomposition(lu, perm, sign, singular);
    }

    /** Magnitud máxima de la matriz, usada para escalar la tolerancia de singularidad. */
    private double scale() {
        double max = 1;
        for (double[] row : data)
            for (double v : row) max = Math.max(max, Math.abs(v));
        return max;
    }

    private void requireSquare() {
        if (!isSquare()) throw new IllegalStateException("La matriz debe ser cuadrada");
    }

    private void requireSameShape(Matrix o) {
        if (rows != o.rows || cols != o.cols) {
            throw new IllegalArgumentException(
                    "Dimensiones distintas: " + rows + "x" + cols + " y " + o.rows + "x" + o.cols);
        }
    }
}
