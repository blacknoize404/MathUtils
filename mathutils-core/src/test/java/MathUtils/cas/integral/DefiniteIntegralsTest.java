package MathUtils.cas.integral;

import MathUtils.cas.JsonValidator;
import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.steps.Format;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static MathUtils.cas.integral.IntegralChecks.uses;

class DefiniteIntegralsTest {

    private static Solution solved(String f, String a, String b, double expected) {
        Solution s = Integrals.definite(f, "x", a, b);
        assert s.isSolved() : f + " [" + a + ", " + b + "] -> " + s.status() + "\n" + s;
        double value = Eval.value(s.result());
        assert Math.abs(value - expected) <= 1e-9 * (1 + Math.abs(expected))
                : f + " [" + a + ", " + b + "]: " + s.result() + " = " + value + ", esperado " + expected;
        assert s.extras().get("approximation") instanceof Expr.Num;
        return s;
    }

    private static Solution diverges(String f, String a, String b) {
        Solution s = Integrals.definite(f, "x", a, b);
        assert s.status() == Solution.Status.DOES_NOT_EXIST : f + " [" + a + ", " + b + "] -> " + s.status() + " " + s.result();
        assert s.result() == null;
        assert uses(s, Rule.INT_DIVERGES);
        return s;
    }

    @Test
    void properIntegrals() {
        solved("x^2", "0", "1", 1.0 / 3);
        solved("sin(x)", "0", "pi", 2);
        solved("1/x", "1", "e", 1);
        solved("x*e^x", "0", "1", 1);
        solved("1/(1+x^2)", "0", "1", Math.PI / 4);
        solved("cos(x)^2", "0", "pi", Math.PI / 2);
        solved("1/(x^2-1)", "2", "3", 0.5 * Math.log(1.5));
    }

    @Test
    void reversedAndEmptyIntervals() {
        solved("x^2", "1", "0", -1.0 / 3);
        solved("x^2", "2", "2", 0);
        assert uses(Integrals.definite("x^2", "x", "1", "0"), Rule.INT_REVERSE_BOUNDS);
    }

    @Test
    void infiniteIntervals() {
        solved("1/x^2", "1", "oo", 1);
        solved("e^(-x)", "0", "oo", 1);
        solved("x*e^(-x)", "0", "oo", 1);
        solved("1/(1+x^2)", "-oo", "oo", Math.PI);
        solved("e^(-x^2)", "0", "oo", Math.sqrt(Math.PI) / 2);
        solved("ln(x)/x^2", "1", "oo", 1);
        solved("e^x", "-oo", "0", 1);
    }

    @Test
    void singularEndpoints() {
        solved("1/sqrt(x)", "0", "1", 2);
        solved("ln(x)", "0", "1", -1);
        solved("1/sqrt(1-x^2)", "0", "1", Math.PI / 2);
        solved("x*ln(x)", "0", "1", -0.25);
        solved("1/sqrt(x*(1-x))".replace("*(1-x)", "-x^2"), "0", "1", Math.PI);
    }

    @Test
    void divergentIntegrals() {
        diverges("1/x", "1", "oo");
        diverges("1/x", "0", "1");
        diverges("1/x^2", "0", "1");
        diverges("1/x^2", "-1", "1");
        diverges("x/(1+x^2)", "0", "oo");
        diverges("x/(1+x^2)", "-oo", "oo");
        diverges("1/(x-1)^2", "0", "2");
        diverges("e^x", "0", "oo");
        diverges("sin(x)", "0", "oo");
    }

    @Test
    void symmetry() {
        assert uses(solved("x^3", "-1", "1", 0), Rule.INT_ODD_SYMMETRY);
        assert uses(solved("x^2", "-1", "1", 2.0 / 3), Rule.INT_EVEN_SYMMETRY);
        solved("e^(-x^2)", "-oo", "oo", Math.sqrt(Math.PI));
        solved("sin(x)*x^2", "-2", "2", 0);
    }

    @Test
    void convergenceCriterion() {
        assert uses(solved("1/x^2", "1", "oo", 1), Rule.INT_P_TEST_INFINITY);
        assert uses(solved("1/sqrt(x)", "0", "1", 2), Rule.INT_P_TEST_ZERO);
    }

    @Test
    void stepsShowTheLimits() {
        Solution s = solved("e^(-x)", "0", "oo", 1);
        assert uses(s, Rule.INT_IMPROPER) && uses(s, Rule.INT_ENDPOINT_LIMIT) && uses(s, Rule.INT_BARROW);
        assert uses(solved("x^2", "0", "1", 1.0 / 3), Rule.INT_BARROW);
    }

    @Test
    void integralsWithoutAnExactAnswerKeepTheApproximation() {
        Solution s = Integrals.definite("e^(x^2)", "x", "0", "1");
        assert !s.isSolved() && s.result() == null;
        assert Math.abs(Eval.value(s.extras().get("approximation")) - 1.4626517459071816) < 1e-7;
    }

    @Test
    void neverReturnsAWrongValueEvenWhenTheAntiderivativeJumps() {
        Solution s = Integrals.definite("1/(2+sin(x))", "x", "0", "2*pi");
        if (s.isSolved()) {
            assert Math.abs(Eval.value(s.result()) - 2 * Math.PI / Math.sqrt(3)) < 1e-9 : s.result();
        } else {
            assert Math.abs(Eval.value(s.extras().get("approximation")) - 2 * Math.PI / Math.sqrt(3)) < 1e-6;
        }
    }

    @Test
    void rendersInEveryLanguageAndFormat() {
        for (String[] c : new String[][]{{"x^2", "0", "1"}, {"e^(-x)", "0", "oo"}, {"1/x", "0", "1"}, {"1/sqrt(x)", "0", "1"},
                {"x^3", "-1", "1"}}) {
            Solution s = Integrals.definite(c[0], "x", c[1], c[2]);
            for (Locale locale : new Locale[]{Locale.forLanguageTag("es"), Locale.ENGLISH}) {
                for (Format format : Format.values()) {
                    String text = s.render(locale, format);
                    assert !text.contains("${") : c[0] + ": " + text;
                    assert !text.contains("+ C") : c[0] + ": " + text;
                }
                JsonValidator.assertValid(s.render(locale, Format.JSON));
            }
            assert s.render(Locale.ENGLISH, Format.LATEX).contains("\\int");
        }
    }
}
