package MathUtils.calculus;

import MathUtils.parser.ExpressionParser;
import org.junit.jupiter.api.Test;

import java.util.function.DoubleUnaryOperator;

class NumericCalculusTest {

    private static final DoubleUnaryOperator SQUARE = x -> x * x;
    private static final DoubleUnaryOperator CUBE = x -> x * x * x;

    @Test
    void trapezoid() {
        assert Math.abs(Integration.trapezoid(SQUARE, 0, 3, 3000) - 9) < 1e-5;
        assert Math.abs(Integration.trapezoid(x -> 2 * x + 1, 0, 2, 1) - 6) < 1e-12;
    }

    @Test
    void simpson13IsExactForCubics() {
        assert Math.abs(Integration.simpson13(CUBE, 0, 2, 2) - 4) < 1e-12;
        assert Math.abs(Integration.simpson13(Math::sin, 0, Math.PI, 100) - 2) < 1e-6;
    }

    @Test
    void simpson38IsExactForCubics() {
        assert Math.abs(Integration.simpson38(CUBE, 0, 2, 3) - 4) < 1e-12;
        assert Math.abs(Integration.simpson38(Math::sin, 0, Math.PI, 99) - 2) < 1e-6;
    }

    @Test
    void gaussLegendre() {
        assert Math.abs(Integration.gaussLegendre(x -> Math.pow(x, 9), 0, 1, 1) - 0.1) < 1e-12;
        assert Math.abs(Integration.gaussLegendre(Math::exp, 0, 1, 2) - (Math.E - 1)) < 1e-12;
    }

    @Test
    void integratesExpressions() {
        var f = ExpressionParser.parse("x^2");
        assert Math.abs(Integration.simpson13(f, "x", 0, 3, 10) - 9) < 1e-9;
        assert Math.abs(Integration.gaussLegendre(f, "x", 0, 3, 1) - 9) < 1e-12;
    }

    @Test
    void invalidSubintervalsThrow() {
        for (Runnable r : new Runnable[]{
                () -> Integration.trapezoid(SQUARE, 0, 1, 0),
                () -> Integration.simpson13(SQUARE, 0, 1, 3),
                () -> Integration.simpson38(SQUARE, 0, 1, 4),
                () -> Integration.gaussLegendre(SQUARE, 0, 1, 0)}) {
            try {
                r.run();
                assert false;
            } catch (IllegalArgumentException e) {
                assert true;
            }
        }
    }

    @Test
    void finiteDifferences() {
        assert Math.abs(NumericDifferentiation.forward(SQUARE, 3, 1e-6) - 6) < 1e-4;
        assert Math.abs(NumericDifferentiation.backward(SQUARE, 3, 1e-6) - 6) < 1e-4;
        assert Math.abs(NumericDifferentiation.central(CUBE, 2, 1e-4) - 12) < 1e-6;
        assert Math.abs(NumericDifferentiation.second(CUBE, 2, 1e-3) - 12) < 1e-5;
        assert Math.abs(NumericDifferentiation.richardson(Math::sin, 1, 1e-2) - Math.cos(1)) < 1e-9;
    }

    @Test
    void zeroStepThrows() {
        try {
            NumericDifferentiation.central(SQUARE, 1, 0);
            assert false;
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void bisection() {
        double root = RootFinding.bisection(x -> x * x - 2, 0, 2, 1e-12, 200);
        assert Math.abs(root - Math.sqrt(2)) < 1e-10;
    }

    @Test
    void bisectionRequiresSignChange() {
        try {
            RootFinding.bisection(x -> x * x + 1, -1, 1, 1e-9, 100);
            assert false;
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void newtonWithDerivative() {
        double root = RootFinding.newton(x -> x * x - 2, x -> 2 * x, 1, 1e-12, 50);
        assert Math.abs(root - Math.sqrt(2)) < 1e-10;
    }

    @Test
    void newtonSymbolic() {
        var f = ExpressionParser.parse("x^3 - x - 2");
        double root = RootFinding.newton(f, "x", 1.5, 1e-12, 50);
        assert Math.abs(root - 1.5213797068045676) < 1e-9;
    }

    @Test
    void newtonFlatDerivativeThrows() {
        try {
            RootFinding.newton(x -> x * x + 1, x -> 2 * x, 0, 1e-12, 50);
            assert false;
        } catch (ArithmeticException e) {
            assert true;
        }
    }

    @Test
    void secant() {
        double root = RootFinding.secant(Math::cos, 1, 2, 1e-12, 50);
        assert Math.abs(root - Math.PI / 2) < 1e-9;
    }
}
