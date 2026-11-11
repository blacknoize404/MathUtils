package com.barnesinside.mathutils.cas.series;

import com.barnesinside.mathutils.cas.expr.Eval;
import com.barnesinside.mathutils.cas.expr.Expr;
import com.barnesinside.mathutils.cas.steps.Format;
import com.barnesinside.mathutils.cas.steps.Messages;
import com.barnesinside.mathutils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

/** Los desarrollos en serie coinciden con la función cerca del punto y explican cada coeficiente. */
class ExpansionsTest {

    private static double at(Expr e, double x) {
        return Eval.value(e, Map.of("x", x));
    }

    @Test
    void taylorOfTheExponentialHasTheFactorialCoefficients() {
        Expansions.Expansion e = Expansions.taylor("exp(x)", "x", "0", 4);
        assert e.solution().isSolved() && e.solution().kind() == Solution.Kind.TAYLOR;
        assert e.terms().size() == 5;
        double[] expected = {1, 1, 0.5, 1.0 / 6, 1.0 / 24};
        for (int k = 0; k < 5; k++) {
            double coefficient = at(e.terms().get(k), 1) ;
            assert Math.abs(coefficient - expected[k]) < 1e-12 : k + " " + e.terms().get(k);
        }
        assert Math.abs(at(e.partial(3), 2) - 5) < 1e-12 : e.partial(3);
        assert Math.abs(at(e.solution().result(), 0.1) - Math.exp(0.1)) < 1e-6;
    }

    @Test
    void taylorAroundAnotherPoint() {
        Expansions.Expansion e = Expansions.taylor("ln(x)", "x", "1", 4);
        assert e.solution().isSolved();
        assert Math.abs(at(e.solution().result(), 1.1) - Math.log(1.1)) < 1e-5 : e.solution().result();
        assert Math.abs(at(e.solution().result(), 1)) < 1e-12;
    }

    @Test
    void taylorKeepsGoingWhereTheFunctionIsNotDefinedBySubstitution() {
        Expansions.Expansion e = Expansions.taylor("sin(x)/x", "x", "0", 6);
        assert e.solution().isSolved() : e.solution().status();
        assert Math.abs(at(e.solution().result(), 0.5) - Math.sin(0.5) / 0.5) < 1e-5 : e.solution().result();
        assert Math.abs(at(e.solution().result(), 0) - 1) < 1e-12;
    }

    @Test
    void laurentHasTheSingularPartOfAPole() {
        Expansions.Expansion e = Expansions.laurent("1/(x*(x-1))", "x", "0", 3);
        assert e.solution().isSolved() && e.solution().kind() == Solution.Kind.LAURENT : e.solution().status();
        assert Eval.value(e.solution().extras().get("order")) == 1;
        // 1/(x(x-1)) = -1/x - 1 - x - x^2 - x^3 …
        assert Math.abs(at(e.solution().result(), 0.1) - 1 / (0.1 * (0.1 - 1))) < 1e-3 : e.solution().result();
        Expansions.Expansion higher = Expansions.laurent("exp(x)/x^3", "x", "0", 2);
        assert Eval.value(higher.solution().extras().get("order")) == 3;
        assert Math.abs(at(higher.solution().result(), 0.2) - Math.exp(0.2) / 0.008) < 1e-3 : higher.solution().result();
        Expansions.Expansion around = Expansions.laurent("1/(x^2-1)", "x", "1", 3);
        assert around.solution().isSolved();
        assert Math.abs(at(around.solution().result(), 1.1) - 1 / (1.1 * 1.1 - 1)) < 1e-3 : around.solution().result();
    }

    @Test
    void laurentAtARegularPointIsTaylor() {
        Expansions.Expansion e = Expansions.laurent("1/(1-x)", "x", "0", 4);
        assert e.solution().isSolved() && Eval.value(e.solution().extras().get("order")) == 0;
        assert Math.abs(at(e.solution().result(), 0.1) - 1 / 0.9) < 1e-4;
    }

    @Test
    void anEssentialSingularityIsReportedNotInvented() {
        Expansions.Expansion e = Expansions.laurent("exp(1/x)", "x", "0", 2);
        assert e.solution().status() == Solution.Status.UNSOLVED : e.solution().status();
    }

    @Test
    void fourierOfAnOddFunctionHasOnlySines() {
        Expansions.Expansion e = Expansions.fourier("x", "x", "-pi", "pi", 4);
        assert e.solution().isSolved() && e.solution().kind() == Solution.Kind.FOURIER;
        String text = e.solution().result().toString();
        assert !text.contains("cos") : text;
        // x = 2 sin x − sin 2x + 2/3 sin 3x − 1/2 sin 4x …
        assert Math.abs(at(e.terms().get(1), Math.PI / 2) - 2) < 1e-9 : e.terms().get(1);
        assert Math.abs(at(e.terms().get(2), Math.PI / 4) + 1) < 1e-9 : e.terms().get(2);
    }

    @Test
    void fourierOfAnEvenFunctionHasOnlyCosinesAndExactCoefficients() {
        Expansions.Expansion e = Expansions.fourier("x^2", "x", "-pi", "pi", 3);
        assert e.solution().isSolved();
        String text = e.solution().result().toString();
        assert !text.contains("sin") : text;
        assert Math.abs(at(e.terms().get(0), 0) - Math.PI * Math.PI / 3) < 1e-9 : e.terms().get(0);
        assert !e.solution().extras().containsKey("approximate");
    }

    @Test
    void fourierOfTheAbsoluteValueIsExactToo() {
        Expansions.Expansion e = Expansions.fourier("abs(x)", "x", "-pi", "pi", 3);
        assert e.solution().isSolved() && !e.solution().extras().containsKey("approximate") : e.solution().extras();
        assert Math.abs(at(e.terms().get(0), 0) - Math.PI / 2) < 1e-9 : e.terms().get(0);
        // a1 = −4/π, a2 = 0, a3 = −4/(9π)
        assert Math.abs(at(e.terms().get(1), 0) + 4 / Math.PI) < 1e-9 : e.terms().get(1);
        assert e.terms().get(2).isZero() : e.terms().get(2);
    }

    @Test
    void fourierOnAnotherInterval() {
        Expansions.Expansion e = Expansions.fourier("x", "x", "0", "1", 3);
        assert e.solution().isSolved();
        assert Math.abs(at(e.terms().get(0), 0) - 0.5) < 1e-9;
        assert Math.abs(at(e.solution().result(), 0.25) - 0.25) < 0.2;
    }

    @Test
    void anInfinitePointOrIndexIsRejected() {
        int failures = 0;
        try {
            Expansions.taylor("exp(x)", "x", "oo", 3);
        } catch (IllegalArgumentException expected) {
            failures++;
        }
        try {
            Expansions.laurent("1/x", "x", "oo", 3);
        } catch (IllegalArgumentException expected) {
            failures++;
        }
        try {
            Convergence.test("1/n", "n", "oo");
        } catch (IllegalArgumentException expected) {
            failures++;
        }
        assert failures == 3 : failures;
    }

    @Test
    void anEmptyIntervalIsRejected() {
        boolean failed = false;
        try {
            Expansions.fourier("x", "x", "1", "0", 3);
        } catch (IllegalArgumentException expected) {
            failed = true;
        }
        assert failed;
    }

    @Test
    void everyExpansionRendersInBothLanguages() {
        Solution[] all = {
                Expansions.taylor("sin(x)", "x", "0", 5).solution(),
                Expansions.laurent("cos(x)/x^2", "x", "0", 2).solution(),
                Expansions.fourier("x^2", "x", "-pi", "pi", 2).solution()};
        for (Solution s : all) {
            for (Locale locale : Messages.supportedLocales()) {
                for (Format format : Format.values()) {
                    String text = s.render(locale, format);
                    assert !text.isBlank() && !text.contains("${") : s.kind() + " " + locale + " " + format;
                }
            }
        }
    }
}
