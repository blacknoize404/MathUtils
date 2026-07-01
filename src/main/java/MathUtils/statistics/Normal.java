package MathUtils.statistics;

import MathUtils.special.Special;

import java.util.Random;

/**
 * Distribución normal {@code N(μ, σ²)}.
 */
public final class Normal implements Distribution {

    private static final double[] A = {
            -3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02,
            1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00};
    private static final double[] B = {
            -5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02,
            6.680131188771972e+01, -1.328068155288572e+01};
    private static final double[] C = {
            -7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00,
            -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00};
    private static final double[] D = {
            7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00};
    private static final double P_LOW = 0.02425;

    private final double mu;
    private final double sigma;

    /**
     * @param mu media
     * @param sigma desviación estándar (> 0)
     */
    public Normal(double mu, double sigma) {
        if (!(sigma > 0)) throw new IllegalArgumentException("sigma debe ser positiva");
        this.mu = mu;
        this.sigma = sigma;
    }

    /** Normal estándar {@code N(0, 1)}. */
    public Normal() {
        this(0, 1);
    }

    @Override
    public double pdf(double x) {
        double z = (x - mu) / sigma;
        return Math.exp(-z * z / 2) / (sigma * Math.sqrt(2 * Math.PI));
    }

    @Override
    public double cdf(double x) {
        return 0.5 * (1 + Special.erf((x - mu) / (sigma * Math.sqrt(2))));
    }

    /**
     * Aproximación racional de Acklam refinada con un paso de Halley (error relativo ~1e-15).
     */
    @Override
    public double quantile(double p) {
        Distribution.requireProbability(p);
        if (p == 0) return Double.NEGATIVE_INFINITY;
        if (p == 1) return Double.POSITIVE_INFINITY;
        return mu + sigma * standardQuantile(p);
    }

    private static double standardQuantile(double p) {
        double x;
        if (p < P_LOW) {
            double q = Math.sqrt(-2 * Math.log(p));
            x = (((((C[0] * q + C[1]) * q + C[2]) * q + C[3]) * q + C[4]) * q + C[5])
                    / ((((D[0] * q + D[1]) * q + D[2]) * q + D[3]) * q + 1);
        } else if (p <= 1 - P_LOW) {
            double q = p - 0.5;
            double r = q * q;
            x = (((((A[0] * r + A[1]) * r + A[2]) * r + A[3]) * r + A[4]) * r + A[5]) * q
                    / (((((B[0] * r + B[1]) * r + B[2]) * r + B[3]) * r + B[4]) * r + 1);
        } else {
            double q = Math.sqrt(-2 * Math.log(1 - p));
            x = -(((((C[0] * q + C[1]) * q + C[2]) * q + C[3]) * q + C[4]) * q + C[5])
                    / ((((D[0] * q + D[1]) * q + D[2]) * q + D[3]) * q + 1);
        }

        // Refinamiento de Halley
        double e = 0.5 * (1 + Special.erf(x / Math.sqrt(2))) - p;
        double u = e * Math.sqrt(2 * Math.PI) * Math.exp(x * x / 2);
        return x - u / (1 + x * u / 2);
    }

    @Override
    public double mean() {
        return mu;
    }

    @Override
    public double variance() {
        return sigma * sigma;
    }

    /** Muestreo por el método de Box-Muller. */
    @Override
    public double sample(Random random) {
        double u1 = 1 - random.nextDouble(); // en (0, 1]
        double u2 = random.nextDouble();
        return mu + sigma * Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
    }
}
