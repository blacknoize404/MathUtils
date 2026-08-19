package barnesinside.mathutils.cas.integral;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * Cuadratura numérica de doble exponencial (tanh-sinh, exp-sinh y sinh-sinh) para comprobar los
 * resultados exactos: aguanta singularidades integrables en los extremos e intervalos infinitos.
 * Si dos refinamientos no concuerdan, devuelve {@code NaN} en vez de un valor dudoso.
 */
final class Quadrature {

    private static final double HALF_PI = Math.PI / 2;

    private Quadrature() {
    }

    /** Integral de {@code f} en {@code [a, b]}, partida en los puntos interiores dados. */
    static double integrate(DoubleUnaryOperator f, double a, double b, List<Double> interior) {
        List<Double> bounds = new ArrayList<>();
        bounds.add(a);
        interior.stream().sorted().forEach(bounds::add);
        bounds.add(b);
        double total = 0;
        for (int i = 0; i + 1 < bounds.size(); i++) total += piece(f, bounds.get(i), bounds.get(i + 1));
        return total;
    }

    private static double piece(DoubleUnaryOperator f, double a, double b) {
        double coarse = rule(f, a, b, 1.0 / 16);
        double fine = rule(f, a, b, 1.0 / 32);
        if (!Double.isFinite(coarse) || !Double.isFinite(fine)) return Double.NaN;
        return Math.abs(coarse - fine) <= 1e-6 * (1 + Math.abs(fine)) ? fine : Double.NaN;
    }

    private static double rule(DoubleUnaryOperator f, double a, double b, double step) {
        if (Double.isInfinite(a) && Double.isInfinite(b)) return sinhSinh(f, step);
        if (Double.isInfinite(b)) return expSinh(f, a, step);
        if (Double.isInfinite(a)) return expSinh(x -> f.applyAsDouble(-x), -b, step);
        return tanhSinh(f, a, b, step);
    }

    private static double tanhSinh(DoubleUnaryOperator f, double a, double b, double step) {
        double half = (b - a) / 2;
        double sum = 0;
        for (double t = -3.3; t <= 3.3 + 1e-12; t += step) {
            double u = HALF_PI * Math.sinh(t);
            double weight = half * HALF_PI * Math.cosh(t) / Math.pow(Math.cosh(u), 2);
            double x = u >= 0 ? b - half * 2 / (1 + Math.exp(2 * u)) : a + half * 2 / (1 + Math.exp(-2 * u));
            sum += contribution(f, x, weight);
        }
        return sum * step;
    }

    private static double expSinh(DoubleUnaryOperator f, double a, double step) {
        double sum = 0;
        for (double t = -4.5; t <= 4.5 + 1e-12; t += step) {
            double e = Math.exp(HALF_PI * Math.sinh(t));
            sum += contribution(f, a + e, HALF_PI * Math.cosh(t) * e);
        }
        return sum * step;
    }

    private static double sinhSinh(DoubleUnaryOperator f, double step) {
        double sum = 0;
        for (double t = -4; t <= 4 + 1e-12; t += step) {
            double u = HALF_PI * Math.sinh(t);
            sum += contribution(f, Math.sinh(u), HALF_PI * Math.cosh(t) * Math.cosh(u));
        }
        return sum * step;
    }

    /** Aporte de un nodo; los nodos donde la función no es finita (los extremos) no cuentan. */
    private static double contribution(DoubleUnaryOperator f, double x, double weight) {
        if (!Double.isFinite(weight) || weight == 0) return 0;
        double y = f.applyAsDouble(x);
        return Double.isFinite(y) ? y * weight : Double.isNaN(y) ? Double.NaN : 0;
    }
}
