package barnesinside.mathutils.statistics;

import barnesinside.mathutils.special.Special;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

class StatisticsTest {

    private static final double[] DATA = {2, 4, 4, 4, 5, 5, 7, 9};

    private static void assertClose(double expected, double actual, double eps) {
        assert Math.abs(expected - actual) < eps : "expected " + expected + " but got " + actual;
    }

    @Test
    void descriptive() {
        assertClose(5, Descriptive.mean(DATA), 1e-12);
        assertClose(4.5, Descriptive.median(DATA), 1e-12);
        assertClose(4, Descriptive.variance(DATA, false), 1e-12);
        assertClose(2, Descriptive.standardDeviation(DATA, false), 1e-12);
        assertClose(32.0 / 7, Descriptive.variance(DATA), 1e-12);
        assert Descriptive.min(DATA) == 2 && Descriptive.max(DATA) == 9;
    }

    @Test
    void modes() {
        assert Arrays.equals(Descriptive.modes(DATA), new double[]{4});
        assert Arrays.equals(Descriptive.modes(new double[]{3, 1, 1, 3, 2}), new double[]{1, 3});
        assert Arrays.equals(Descriptive.modes(new double[]{5}), new double[]{5});
    }

    @Test
    void percentiles() {
        double[] d = {1, 2, 3, 4, 5};
        assertClose(1, Descriptive.percentile(d, 0), 1e-12);
        assertClose(3, Descriptive.percentile(d, 50), 1e-12);
        assertClose(2, Descriptive.percentile(d, 25), 1e-12);
        assertClose(5, Descriptive.percentile(d, 100), 1e-12);
        assertClose(1.4, Descriptive.percentile(d, 10), 1e-12);
    }

    @Test
    void percentileDoesNotReorderInput() {
        double[] d = {5, 1, 3};
        Descriptive.percentile(d, 50);
        assert Arrays.equals(d, new double[]{5, 1, 3});
    }

    @Test
    void skewnessAndKurtosis() {
        assertClose(0, Descriptive.skewness(new double[]{1, 2, 3, 4, 5}), 1e-12);
        assert Descriptive.skewness(new double[]{1, 1, 1, 2, 10}) > 0;
        assertClose(-1.3, Descriptive.kurtosis(new double[]{1, 2, 3, 4, 5}), 1e-12);
    }

    @Test
    void covarianceAndCorrelation() {
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {2, 4, 6, 8, 10};
        double[] z = {5, 4, 3, 2, 1};
        assertClose(5, Descriptive.covariance(x, y), 1e-12);
        assertClose(1, Descriptive.correlation(x, y), 1e-12);
        assertClose(-1, Descriptive.correlation(x, z), 1e-12);
    }

    @Test
    void invalidDataThrows() {
        for (Runnable r : new Runnable[]{
                () -> Descriptive.mean(new double[0]),
                () -> Descriptive.variance(new double[]{1}),
                () -> Descriptive.percentile(DATA, 101),
                () -> Descriptive.covariance(new double[]{1, 2}, new double[]{1})}) {
            try {
                r.run();
                assert false;
            } catch (IllegalArgumentException e) {
                assert true;
            }
        }
        try {
            Descriptive.skewness(new double[]{3, 3, 3});
            assert false;
        } catch (ArithmeticException e) {
            assert true;
        }
    }

    @Test
    void erf() {
        assertClose(0, Special.erf(0), 1e-15);
        assertClose(0.8427007929497149, Special.erf(1), 1e-13);
        assertClose(0.9953222650189527, Special.erf(2), 1e-13);
        assertClose(-0.8427007929497149, Special.erf(-1), 1e-13);
        assertClose(1, Special.erf(10), 1e-15);
    }

    @Test
    void normal() {
        Normal n = new Normal();
        assertClose(0.3989422804014327, n.pdf(0), 1e-13);
        assertClose(0.5, n.cdf(0), 1e-13);
        assertClose(0.9750021048517795, n.cdf(1.96), 1e-12);
        assertClose(1.959963984540054, n.quantile(0.975), 1e-9);
        assertClose(0, n.quantile(0.5), 1e-12);
        assert n.quantile(0) == Double.NEGATIVE_INFINITY && n.quantile(1) == Double.POSITIVE_INFINITY;
    }

    @Test
    void normalQuantileIsInverseOfCdf() {
        Normal n = new Normal(10, 3);
        for (double p : new double[]{1e-6, 0.01, 0.1, 0.5, 0.9, 0.99, 1 - 1e-6}) {
            assertClose(p, n.cdf(n.quantile(p)), 1e-10);
        }
    }

    @Test
    void normalMoments() {
        Normal n = new Normal(5, 2);
        assert n.mean() == 5 && n.variance() == 4 && n.standardDeviation() == 2;
    }

    @Test
    void uniform() {
        Uniform u = new Uniform(2, 6);
        assertClose(0.25, u.pdf(3), 1e-12);
        assert u.pdf(7) == 0 && u.cdf(1) == 0 && u.cdf(7) == 1;
        assertClose(0.5, u.cdf(4), 1e-12);
        assertClose(5, u.quantile(0.75), 1e-12);
        assertClose(4, u.mean(), 1e-12);
        assertClose(16.0 / 12, u.variance(), 1e-12);
    }

    @Test
    void exponential() {
        Exponential e = new Exponential(2);
        assertClose(2, e.pdf(0), 1e-12);
        assertClose(1 - Math.exp(-2), e.cdf(1), 1e-12);
        assertClose(Math.log(2) / 2, e.quantile(0.5), 1e-12);
        assertClose(0.5, e.mean(), 1e-12);
        assertClose(0.25, e.variance(), 1e-12);
        assert e.pdf(-1) == 0 && e.cdf(-1) == 0;
    }

    @Test
    void binomial() {
        Binomial b = new Binomial(10, 0.3);
        assertClose(0.2668279320, b.pmf(3), 1e-9);
        assertClose(0.6496107184, b.cdf(3), 1e-9);
        assertClose(3, b.mean(), 1e-12);
        assertClose(2.1, b.variance(), 1e-12);
        assert b.quantile(0.5) == 3;
        assert b.pmf(-1) == 0 && b.pmf(11) == 0 && b.cdf(10) == 1;
        double total = 0;
        for (int k = 0; k <= 10; k++) total += b.pmf(k);
        assertClose(1, total, 1e-12);
    }

    @Test
    void binomialEdgeProbabilities() {
        assert new Binomial(5, 0).pmf(0) == 1 && new Binomial(5, 1).pmf(5) == 1;
    }

    @Test
    void poisson() {
        Poisson p = new Poisson(4);
        assertClose(Math.exp(-4) * 4 * 4 * 4 / 6, p.pmf(3), 1e-12);
        assertClose(0.4334701203667089, p.cdf(3), 1e-9);
        assert p.quantile(0.5) == 4;
        assertClose(4, p.mean(), 1e-12);
    }

    @Test
    void samplingMatchesMoments() {
        Random random = new Random(42);
        int n = 50_000;

        double[] normal = new double[n];
        double[] uniform = new double[n];
        double[] exponential = new double[n];
        double[] poisson = new double[n];
        double[] binomial = new double[n];
        Normal nd = new Normal(3, 2);
        Uniform ud = new Uniform(0, 4);
        Exponential ed = new Exponential(0.5);
        Poisson pd = new Poisson(6);
        Binomial bd = new Binomial(20, 0.4);
        for (int i = 0; i < n; i++) {
            normal[i] = nd.sample(random);
            uniform[i] = ud.sample(random);
            exponential[i] = ed.sample(random);
            poisson[i] = pd.sample(random);
            binomial[i] = bd.sample(random);
        }

        assertClose(3, Descriptive.mean(normal), 0.05);
        assertClose(4, Descriptive.variance(normal), 0.15);
        assertClose(2, Descriptive.mean(uniform), 0.05);
        assertClose(2, Descriptive.mean(exponential), 0.08);
        assertClose(6, Descriptive.mean(poisson), 0.08);
        assertClose(6, Descriptive.variance(poisson), 0.25);
        assertClose(8, Descriptive.mean(binomial), 0.05);
    }

    @Test
    void invalidParametersThrow() {
        for (Runnable r : new Runnable[]{
                () -> new Normal(0, 0),
                () -> new Uniform(1, 1),
                () -> new Exponential(-1),
                () -> new Binomial(-1, 0.5),
                () -> new Binomial(5, 1.5),
                () -> new Poisson(0),
                () -> new Normal().quantile(1.5)}) {
            try {
                r.run();
                assert false;
            } catch (IllegalArgumentException e) {
                assert true;
            }
        }
    }
}
