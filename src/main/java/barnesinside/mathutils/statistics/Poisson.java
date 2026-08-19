package barnesinside.mathutils.statistics;

import barnesinside.mathutils.special.Special;

import java.util.Random;

/**
 * Distribución de Poisson de media {@code λ}.
 */
public final class Poisson implements DiscreteDistribution {

    private static final double SAMPLING_CHUNK = 500;

    private final double lambda;

    /**
     * @param lambda media (> 0)
     */
    public Poisson(double lambda) {
        if (!(lambda > 0)) throw new IllegalArgumentException("lambda debe ser positiva");
        this.lambda = lambda;
    }

    @Override
    public double pmf(int k) {
        if (k < 0) return 0;
        return Math.exp(-lambda + k * Math.log(lambda) - Special.logFactorial(k));
    }

    @Override
    public double cdf(int k) {
        if (k < 0) return 0;
        double sum = 0;
        for (int i = 0; i <= k; i++) sum += pmf(i);
        return Math.min(1, sum);
    }

    /**
     * @throws IllegalArgumentException si {@code q} no está en [0, 1) (el soporte no tiene máximo)
     */
    @Override
    public int quantile(double q) {
        if (!(q >= 0 && q < 1)) throw new IllegalArgumentException("La probabilidad debe estar en [0, 1)");
        double cumulative = 0;
        for (int k = 0; ; k++) {
            cumulative += pmf(k);
            if (cumulative >= q) return k;
        }
    }

    @Override
    public double mean() {
        return lambda;
    }

    @Override
    public double variance() {
        return lambda;
    }

    /** Algoritmo de Knuth, dividiendo la media en tramos para evitar el desbordamiento de {@code e^-λ}. */
    @Override
    public int sample(Random random) {
        int total = 0;
        double remaining = lambda;
        while (remaining > 0) {
            double chunk = Math.min(remaining, SAMPLING_CHUNK);
            remaining -= chunk;

            double limit = Math.exp(-chunk);
            double product = random.nextDouble();
            while (product > limit) {
                total++;
                product *= random.nextDouble();
            }
        }
        return total;
    }
}
