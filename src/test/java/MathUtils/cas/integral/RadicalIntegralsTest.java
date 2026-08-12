package MathUtils.cas.integral;

import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

import static MathUtils.cas.integral.IntegralChecks.check;
import static MathUtils.cas.integral.IntegralChecks.uses;

class RadicalIntegralsTest {

    @Test
    void rootsOfLinearExpressions() {
        check("x*sqrt(x+1)", 0.2, 2);
        check("sqrt(2x+3)", 0.2, 2);
        check("x/sqrt(x-1)", 1.5, 3);
        check("1/(1+sqrt(x))", 0.2, 3);
        check("sqrt(x)/(1+x)", 0.2, 3);
        check("x*(x+1)^(1/3)", 0.2, 2);
        check("1/(sqrt(x)+x^(1/3))", 0.3, 3);
        check("x^2*sqrt(3x+1)", 0.2, 2);
    }

    @Test
    void sineSubstitution() {
        check("sqrt(1-x^2)", -0.5, 0.8);
        check("x^2*sqrt(1-x^2)", -0.5, 0.8);
        check("sqrt(4-x^2)", -1, 1.5);
        check("1/(x^2*sqrt(1-x^2))", 0.2, 0.9);
        check("sqrt(1-x^2)/x^2", 0.2, 0.9);
        check("1/(1-x^2)^(3/2)", -0.5, 0.8);
        check("sqrt(9-4x^2)", -1, 1.2);
    }

    @Test
    void tangentSubstitution() {
        check("sqrt(x^2+1)");
        check("x^2/sqrt(x^2+4)");
        check("sqrt(4x^2+9)");
        check("1/(x^2*sqrt(x^2+1))", 0.3, 2);
        check("1/(x^2+1)^(3/2)", -1, 2);
        check("x^2*sqrt(x^2+1)", -1, 1.5);
    }

    @Test
    void secantSubstitution() {
        check("sqrt(x^2-1)", 1.5, 3);
        check("1/(x*sqrt(x^2-1))", 1.5, 3);
        check("sqrt(x^2-4)/x", 2.5, 4);
        check("1/(x^2*sqrt(x^2-1))", 1.5, 3);
    }

    @Test
    void completingTheSquare() {
        check("1/sqrt(x^2+2x+5)", -1, 2);
        check("sqrt(x^2+2x+2)", -1, 2);
        check("sqrt(3-2x-x^2)", -2.5, 0.5);
        check("1/sqrt(3-2x-x^2)", -2.5, 0.5);
        check("x/sqrt(x^2+4x+5)", -1, 2);
        check("sqrt(-x^2+2x)", 0.2, 1.8);
    }

    @Test
    void stepsNameTheTechnique() {
        assert uses(check("1/(sqrt(x)+x^(1/3))", 0.3, 3), Rule.INT_RADICAL_SUBSTITUTION);
        assert uses(check("x^2*sqrt(1-x^2)", -0.5, 0.8), Rule.INT_TRIG_SUBSTITUTION_SIN);
        assert uses(check("1/(x^2*sqrt(x^2+1))", 0.3, 2), Rule.INT_TRIG_SUBSTITUTION_TAN);
        assert uses(check("1/(x^2*sqrt(x^2-1))", 1.5, 3), Rule.INT_TRIG_SUBSTITUTION_SEC);
        assert uses(check("sqrt(x^2+2x+2)", -1, 2), Rule.INT_RADICAL_COMPLETE_SQUARE);
        assert uses(check("sqrt(1-x^2)", -0.5, 0.8), Rule.INT_ROOT_DIFFERENCE_FORM);
        assert uses(check("sqrt(x^2+1)"), Rule.INT_ROOT_SUM_FORM);
    }

    @Test
    void unsupportedRadicalsAreUnsolved() {
        for (String f : new String[]{"sqrt(x^3+1)", "sqrt(x^4+1)", "1/sqrt(sin(x))"}) {
            long start = System.nanoTime();
            Solution s = Integrals.indefinite(f, "x");
            assert !s.isSolved() : f + " no debe resolverse: " + s.result();
            assert (System.nanoTime() - start) < 8_000_000_000L : f + " tardó demasiado";
        }
    }
}
