package barnesinside.mathutils.complex;

import org.junit.jupiter.api.Test;

class ComplexTest {

    private static final double EPS = 1e-12;

    @Test
    void arithmetic() {
        Complex a = Complex.of(1, 2);
        Complex b = Complex.of(3, -1);
        assert a.add(b).equals(Complex.of(4, 1));
        assert a.subtract(b).equals(Complex.of(-2, 3));
        assert a.multiply(b).equals(Complex.of(5, 5));
        assert a.divide(b).isCloseTo(Complex.of(0.1, 0.7), EPS);
    }

    @Test
    void iSquaredIsMinusOne() {
        assert Complex.I.multiply(Complex.I).equals(Complex.real(-1));
        assert Complex.I.pow(2).equals(Complex.real(-1));
    }

    @Test
    void divideByZeroThrows() {
        try {
            Complex.ONE.divide(Complex.ZERO);
            assert false;
        } catch (ArithmeticException e) {
            assert true;
        }
    }

    @Test
    void polarRoundTrip() {
        Complex z = Complex.polar(2, Math.PI / 3);
        assert Math.abs(z.abs() - 2) < EPS;
        assert Math.abs(z.arg() - Math.PI / 3) < EPS;
    }

    @Test
    void eulerIdentity() {
        Complex z = Complex.of(0, Math.PI).exp();
        assert z.isCloseTo(Complex.real(-1), EPS);
    }

    @Test
    void logOfMinusOne() {
        Complex z = Complex.real(-1).log();
        assert z.isCloseTo(Complex.of(0, Math.PI), EPS);
    }

    @Test
    void integerPowers() {
        Complex z = Complex.of(1, 1);
        assert z.pow(4).isCloseTo(Complex.real(-4), EPS);
        assert z.pow(0).equals(Complex.ONE);
        assert z.pow(-1).isCloseTo(Complex.of(0.5, -0.5), EPS);
    }

    @Test
    void complexPower() {
        // i^i = e^(-pi/2)
        Complex z = Complex.I.pow(Complex.I);
        assert z.isCloseTo(Complex.real(Math.exp(-Math.PI / 2)), EPS);
    }

    @Test
    void sqrtOfMinusOne() {
        assert Complex.real(-1).sqrt().isCloseTo(Complex.I, EPS);
    }

    @Test
    void nthRoots() {
        Complex[] roots = Complex.ONE.roots(4);
        assert roots.length == 4;
        for (Complex r : roots) assert r.pow(4).isCloseTo(Complex.ONE, 1e-10);
    }

    @Test
    void trigonometric() {
        Complex z = Complex.of(0.5, 0.3);
        // sin^2 + cos^2 = 1 y cosh^2 - sinh^2 = 1
        assert z.sin().pow(2).add(z.cos().pow(2)).isCloseTo(Complex.ONE, EPS);
        assert z.cosh().pow(2).subtract(z.sinh().pow(2)).isCloseTo(Complex.ONE, EPS);
        assert z.tan().isCloseTo(z.sin().divide(z.cos()), EPS);
    }

    @Test
    void toStringShowsSign() {
        assert "1.0 + 2.0i".equals(Complex.of(1, 2).toString());
        assert "1.0 - 2.0i".equals(Complex.of(1, -2).toString());
    }
}
