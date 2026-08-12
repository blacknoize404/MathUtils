package MathUtils.calculus;

import MathUtils.algebra.Evaluation;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.parser.ExpressionParser;
import org.junit.jupiter.api.Test;

class DerivativeTest {

    private static MathElement parse(String s) {
        return ExpressionParser.parse(s);
    }

    private static String d(String source, String variable) {
        return Derivative.of(parse(source), variable).toString();
    }

    /** Compara la derivada simbólica con la numérica en varios puntos. */
    private static void assertMatchesNumeric(String source, double... points) throws Exception {
        MathElement f = parse(source);
        MathElement df = Derivative.of(f, "x");
        for (double x : points) {
            double symbolic = df.eval(new Evaluation("x", x));
            double numeric = NumericDifferentiation.richardson(Functions.of(f, "x"), x, 1e-3);
            assert Math.abs(symbolic - numeric) < 1e-6
                    : "d/dx " + source + " at " + x + ": symbolic " + symbolic + " vs numeric " + numeric;
        }
    }

    @Test
    void basicRules() {
        assert "0".equals(d("5", "x"));
        assert "1".equals(d("x", "x"));
        assert "0".equals(d("y", "x"));
        assert "(2 * x)".equals(d("x^2", "x"));
        assert "3".equals(d("3 * x", "x"));
    }

    @Test
    void sumAndDifference() {
        assert "(2 * x)".equals(d("x^2 + 5", "x"));
        assert "3".equals(d("3*x - 7", "x"));
    }

    @Test
    void trigonometric() {
        assert "cos(x)".equals(d("sin(x)", "x"));
        assert "(-1 * sin(x))".equals(d("cos(x)", "x"));
    }

    @Test
    void naturalLog() {
        assert "(1 / x)".equals(d("ln(x)", "x"));
    }

    @Test
    void numericalAgreement() throws Exception {
        assertMatchesNumeric("x^3 - 2*x^2 + x - 5", -2, 0.5, 3);
        assertMatchesNumeric("sin(x) * cos(x)", 0.3, 1.2);
        assertMatchesNumeric("sin(x^2)", 0.4, 1.1);
        assertMatchesNumeric("tan(x)", 0.2, 0.9);
        assertMatchesNumeric("cot(x)", 0.5, 1.3);
        assertMatchesNumeric("(x^2 + 1) / (x - 3)", 0, 5);
        assertMatchesNumeric("1 / x", 0.5, 2);
        assertMatchesNumeric("ln(x^2 + 1)", 0.5, 2);
        assertMatchesNumeric("log(x, 2)", 0.5, 4);
        assertMatchesNumeric("exp(2*x)", -1, 0.5);
        assertMatchesNumeric("2^x", -1, 1.5);
        assertMatchesNumeric("x^x", 0.5, 2);
        assertMatchesNumeric("sqrt(x)", 0.5, 4);
        assertMatchesNumeric("cbrt(x)", 1, 8);
        assertMatchesNumeric("x * x * x", 2);
        assertMatchesNumeric("x / 2 / 3", 2);
    }

    @Test
    void higherOrder() throws Exception {
        MathElement f = parse("x^4");
        MathElement d3 = Derivative.of(f, "x", 3);
        assert Math.abs(d3.eval(new Evaluation("x", 2)) - 48) < 1e-9;
        assert Derivative.of(f, "x", 0) == f;
    }

    @Test
    void gradient() throws Exception {
        MathElement f = parse("x^2 * y + y^3");
        MathElement[] g = Derivative.gradient(f, "x", "y");
        Evaluation[] at = {new Evaluation("x", 2), new Evaluation("y", 3)};
        assert Math.abs(g[0].eval(at) - 12) < 1e-9;
        assert Math.abs(g[1].eval(at) - 31) < 1e-9;
    }

    @Test
    void jacobian() throws Exception {
        MathElement[] fs = {parse("x*y"), parse("x + y^2")};
        MathElement[][] j = Derivative.jacobian(fs, "x", "y");
        Evaluation[] at = {new Evaluation("x", 2), new Evaluation("y", 3)};
        assert j[0][0].eval(at) == 3 && j[0][1].eval(at) == 2;
        assert j[1][0].eval(at) == 1 && j[1][1].eval(at) == 6;
    }

    @Test
    void hessian() throws Exception {
        MathElement f = parse("x^2 * y + y^3");
        MathElement[][] h = Derivative.hessian(f, "x", "y");
        Evaluation[] at = {new Evaluation("x", 2), new Evaluation("y", 3)};
        assert Math.abs(h[0][0].eval(at) - 6) < 1e-9;
        assert Math.abs(h[0][1].eval(at) - 4) < 1e-9;
        assert Math.abs(h[1][0].eval(at) - 4) < 1e-9;
        assert Math.abs(h[1][1].eval(at) - 18) < 1e-9;
    }

    @Test
    void dependsOn() {
        assert Derivative.dependsOn(parse("sin(x) + y"), "x");
        assert !Derivative.dependsOn(parse("sin(y) + 2"), "x");
        assert Derivative.dependsOn(parse("2^x"), "x");
        assert Derivative.dependsOn(parse("log(2, x)"), "x");
    }
}
