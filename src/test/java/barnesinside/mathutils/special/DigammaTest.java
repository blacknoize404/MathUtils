package barnesinside.mathutils.special;

import org.junit.jupiter.api.Test;

class DigammaTest {

    private static void near(double actual, double expected) {
        assert Math.abs(actual - expected) <= 1e-12 * (1 + Math.abs(expected)) : actual + " != " + expected;
    }

    @Test
    void knownValues() {
        near(Special.digamma(1), -Special.EULER_GAMMA);
        near(Special.digamma(2), 1 - Special.EULER_GAMMA);
        near(Special.digamma(0.5), -Special.EULER_GAMMA - 2 * Math.log(2));
        near(Special.digamma(10), 2.251752589066721);
    }

    @Test
    void recurrenceAndReflection() {
        for (double x : new double[]{0.3, 1.7, 4.2, 25.5}) {
            near(Special.digamma(x + 1), Special.digamma(x) + 1 / x);
        }
        // ψ(1 − x) − ψ(x) = π cot(πx)
        double x = 0.3;
        near(Special.digamma(1 - x) - Special.digamma(x), Math.PI / Math.tan(Math.PI * x));
        near(Special.digamma(-0.5), 0.03648997397857652);
    }

    @Test
    void notDefinedAtNonPositiveIntegers() {
        assert Double.isNaN(Special.digamma(0));
        assert Double.isNaN(Special.digamma(-3));
    }
}
