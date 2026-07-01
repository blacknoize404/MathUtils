package MathUtils.statistics;

import java.util.Random;

/**
 * Distribución uniforme continua en {@code [a, b]}.
 */
public final class Uniform implements Distribution {

    private final double a;
    private final double b;

    /**
     * @throws IllegalArgumentException si {@code a >= b}
     */
    public Uniform(double a, double b) {
        if (!(a < b)) throw new IllegalArgumentException("Se requiere a < b");
        this.a = a;
        this.b = b;
    }

    @Override
    public double pdf(double x) {
        return x >= a && x <= b ? 1 / (b - a) : 0;
    }

    @Override
    public double cdf(double x) {
        if (x <= a) return 0;
        if (x >= b) return 1;
        return (x - a) / (b - a);
    }

    @Override
    public double quantile(double p) {
        Distribution.requireProbability(p);
        return a + p * (b - a);
    }

    @Override
    public double mean() {
        return (a + b) / 2;
    }

    @Override
    public double variance() {
        return (b - a) * (b - a) / 12;
    }

    @Override
    public double sample(Random random) {
        return a + random.nextDouble() * (b - a);
    }
}
