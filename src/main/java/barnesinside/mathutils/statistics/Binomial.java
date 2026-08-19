package barnesinside.mathutils.statistics;

import barnesinside.mathutils.special.Special;

import java.util.Random;

/**
 * Distribución binomial {@code B(n, p)}: éxitos en {@code n} ensayos independientes.
 */
public final class Binomial implements DiscreteDistribution {

    private final int n;
    private final double p;

    /**
     * @param n cantidad de ensayos (≥ 0)
     * @param p probabilidad de éxito en [0, 1]
     */
    public Binomial(int n, double p) {
        if (n < 0) throw new IllegalArgumentException("n debe ser mayor o igual que 0");
        Distribution.requireProbability(p);
        this.n = n;
        this.p = p;
    }

    @Override
    public double pmf(int k) {
        if (k < 0 || k > n) return 0;
        if (p == 0) return k == 0 ? 1 : 0;
        if (p == 1) return k == n ? 1 : 0;
        return Math.exp(Special.logBinomial(n, k) + k * Math.log(p) + (n - k) * Math.log1p(-p));
    }

    @Override
    public double cdf(int k) {
        if (k < 0) return 0;
        if (k >= n) return 1;
        double sum = 0;
        for (int i = 0; i <= k; i++) sum += pmf(i);
        return Math.min(1, sum);
    }

    @Override
    public int quantile(double q) {
        Distribution.requireProbability(q);
        double cumulative = 0;
        for (int k = 0; k < n; k++) {
            cumulative += pmf(k);
            if (cumulative >= q) return k;
        }
        return n;
    }

    @Override
    public double mean() {
        return n * p;
    }

    @Override
    public double variance() {
        return n * p * (1 - p);
    }

    @Override
    public int sample(Random random) {
        int successes = 0;
        for (int i = 0; i < n; i++) if (random.nextDouble() < p) successes++;
        return successes;
    }
}
