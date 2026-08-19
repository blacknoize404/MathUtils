package barnesinside.mathutils.calculus;

import java.util.function.DoubleUnaryOperator;

/**
 * Diferenciación numérica por diferencias finitas.
 */
public final class NumericDifferentiation {

    private NumericDifferentiation() {
    }

    /** Diferencia hacia adelante: {@code (f(x+h) - f(x)) / h}, error O(h). */
    public static double forward(DoubleUnaryOperator f, double x, double h) {
        requireStep(h);
        return (f.applyAsDouble(x + h) - f.applyAsDouble(x)) / h;
    }

    /** Diferencia hacia atrás: {@code (f(x) - f(x-h)) / h}, error O(h). */
    public static double backward(DoubleUnaryOperator f, double x, double h) {
        requireStep(h);
        return (f.applyAsDouble(x) - f.applyAsDouble(x - h)) / h;
    }

    /** Diferencia central: {@code (f(x+h) - f(x-h)) / 2h}, error O(h²). */
    public static double central(DoubleUnaryOperator f, double x, double h) {
        requireStep(h);
        return (f.applyAsDouble(x + h) - f.applyAsDouble(x - h)) / (2 * h);
    }

    /** Segunda derivada central: {@code (f(x+h) - 2f(x) + f(x-h)) / h²}, error O(h²). */
    public static double second(DoubleUnaryOperator f, double x, double h) {
        requireStep(h);
        return (f.applyAsDouble(x + h) - 2 * f.applyAsDouble(x) + f.applyAsDouble(x - h)) / (h * h);
    }

    /**
     * Extrapolación de Richardson sobre la diferencia central, con error O(h⁴):
     * {@code (4·D(h/2) - D(h)) / 3}.
     */
    public static double richardson(DoubleUnaryOperator f, double x, double h) {
        return (4 * central(f, x, h / 2) - central(f, x, h)) / 3;
    }

    private static void requireStep(double h) {
        if (h == 0 || !Double.isFinite(h)) throw new IllegalArgumentException("El paso h debe ser finito y distinto de 0");
    }
}
