package barnesinside.mathutils.calculus;

import barnesinside.mathutils.algebra.primitives.MathElement;

import java.util.function.DoubleUnaryOperator;

/**
 * Integración numérica definida: trapecio, Simpson 1/3, Simpson 3/8 y cuadratura de Gauss-Legendre.
 * Cada método tiene una versión sobre {@link DoubleUnaryOperator} y otra sobre una expresión
 * y su variable.
 */
public final class Integration {

    private static final double[] GAUSS_NODES = {
            0,
            -0.5384693101056831, 0.5384693101056831,
            -0.9061798459386640, 0.9061798459386640
    };
    private static final double[] GAUSS_WEIGHTS = {
            0.5688888888888889,
            0.47862867049936647, 0.47862867049936647,
            0.23692688505618908, 0.23692688505618908
    };

    private Integration() {
    }

    /**
     * Regla del trapecio compuesta.
     * @param n cantidad de subintervalos (≥ 1)
     */
    public static double trapezoid(DoubleUnaryOperator f, double a, double b, int n) {
        if (n < 1) throw new IllegalArgumentException("n debe ser al menos 1");
        double h = (b - a) / n;
        double sum = (f.applyAsDouble(a) + f.applyAsDouble(b)) / 2;
        for (int i = 1; i < n; i++) sum += f.applyAsDouble(a + i * h);
        return h * sum;
    }

    /**
     * Regla de Simpson 1/3 compuesta.
     * @param n cantidad de subintervalos (par y ≥ 2)
     */
    public static double simpson13(DoubleUnaryOperator f, double a, double b, int n) {
        if (n < 2 || n % 2 != 0) throw new IllegalArgumentException("n debe ser par y al menos 2");
        double h = (b - a) / n;
        double sum = f.applyAsDouble(a) + f.applyAsDouble(b);
        for (int i = 1; i < n; i++) sum += (i % 2 == 1 ? 4 : 2) * f.applyAsDouble(a + i * h);
        return h / 3 * sum;
    }

    /**
     * Regla de Simpson 3/8 compuesta.
     * @param n cantidad de subintervalos (múltiplo de 3 y ≥ 3)
     */
    public static double simpson38(DoubleUnaryOperator f, double a, double b, int n) {
        if (n < 3 || n % 3 != 0) throw new IllegalArgumentException("n debe ser múltiplo de 3 y al menos 3");
        double h = (b - a) / n;
        double sum = f.applyAsDouble(a) + f.applyAsDouble(b);
        for (int i = 1; i < n; i++) sum += (i % 3 == 0 ? 2 : 3) * f.applyAsDouble(a + i * h);
        return 3 * h / 8 * sum;
    }

    /**
     * Cuadratura de Gauss-Legendre de 5 puntos aplicada en cada uno de los paneles
     * en que se divide el intervalo (exacta para polinomios de grado ≤ 9 por panel).
     * @param panels cantidad de paneles (≥ 1)
     */
    public static double gaussLegendre(DoubleUnaryOperator f, double a, double b, int panels) {
        if (panels < 1) throw new IllegalArgumentException("panels debe ser al menos 1");
        double width = (b - a) / panels;
        double total = 0;
        for (int p = 0; p < panels; p++) {
            double mid = a + (p + 0.5) * width;
            double half = width / 2;
            double sum = 0;
            for (int i = 0; i < GAUSS_NODES.length; i++) {
                sum += GAUSS_WEIGHTS[i] * f.applyAsDouble(mid + half * GAUSS_NODES[i]);
            }
            total += half * sum;
        }
        return total;
    }

    public static double trapezoid(MathElement e, String variable, double a, double b, int n) {
        return trapezoid(Functions.of(e, variable), a, b, n);
    }

    public static double simpson13(MathElement e, String variable, double a, double b, int n) {
        return simpson13(Functions.of(e, variable), a, b, n);
    }

    public static double simpson38(MathElement e, String variable, double a, double b, int n) {
        return simpson38(Functions.of(e, variable), a, b, n);
    }

    public static double gaussLegendre(MathElement e, String variable, double a, double b, int panels) {
        return gaussLegendre(Functions.of(e, variable), a, b, panels);
    }
}
