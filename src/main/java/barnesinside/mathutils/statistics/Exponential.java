package barnesinside.mathutils.statistics;

import java.util.Random;

/**
 * Distribución exponencial de tasa {@code λ}.
 */
public final class Exponential implements Distribution {

    private final double lambda;

    /**
     * @param lambda tasa (> 0)
     */
    public Exponential(double lambda) {
        if (!(lambda > 0)) throw new IllegalArgumentException("lambda debe ser positiva");
        this.lambda = lambda;
    }

    @Override
    public double pdf(double x) {
        return x < 0 ? 0 : lambda * Math.exp(-lambda * x);
    }

    @Override
    public double cdf(double x) {
        return x < 0 ? 0 : -Math.expm1(-lambda * x);
    }

    @Override
    public double quantile(double p) {
        Distribution.requireProbability(p);
        if (p == 1) return Double.POSITIVE_INFINITY;
        return -Math.log1p(-p) / lambda;
    }

    @Override
    public double mean() {
        return 1 / lambda;
    }

    @Override
    public double variance() {
        return 1 / (lambda * lambda);
    }

    /** Muestreo por transformada inversa. */
    @Override
    public double sample(Random random) {
        return quantile(random.nextDouble());
    }
}
