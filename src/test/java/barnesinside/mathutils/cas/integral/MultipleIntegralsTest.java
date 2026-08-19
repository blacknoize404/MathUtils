package barnesinside.mathutils.cas.integral;

import barnesinside.mathutils.cas.JsonValidator;
import barnesinside.mathutils.cas.expr.Equivalence;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.integral.MultipleIntegrals.Dimension;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static barnesinside.mathutils.cas.integral.IntegralChecks.uses;

class MultipleIntegralsTest {

    private static Solution solved(String f, List<Dimension> dims, double expected) {
        Solution s = MultipleIntegrals.iterated(f, dims);
        assert s.isSolved() : f + " -> " + s.status() + "\n" + s;
        double value = Eval.value(s.result());
        assert Math.abs(value - expected) <= 1e-9 * (1 + Math.abs(expected)) : f + ": " + s.result() + " = " + value + ", esperado " + expected;
        return s;
    }

    @Test
    void rectangularDoubleIntegrals() {
        solved("x*y", List.of(new Dimension("x", "0", "1"), new Dimension("y", "0", "2")), 1);
        solved("x^2 + y^2", List.of(new Dimension("x", "0", "1"), new Dimension("y", "0", "1")), 2.0 / 3);
        solved("sin(x)*cos(y)", List.of(new Dimension("x", "0", "pi"), new Dimension("y", "0", "pi/2")), 2);
        solved("e^(x+y)", List.of(new Dimension("x", "0", "1"), new Dimension("y", "0", "1")), Math.pow(Math.E - 1, 2));
    }

    @Test
    void innerLimitsMayDependOnTheOuterVariable() {
        solved("x*y", List.of(new Dimension("x", "0", "y"), new Dimension("y", "0", "1")), 1.0 / 8);
        solved("x + y", List.of(new Dimension("y", "x^2", "x"), new Dimension("x", "0", "1")), 3.0 / 20);
        solved("1", List.of(new Dimension("y", "0", "sqrt(1-x^2)"), new Dimension("x", "-1", "1")), Math.PI / 2);
    }

    @Test
    void tripleIntegrals() {
        solved("x*y*z", List.of(new Dimension("x", "0", "1"), new Dimension("y", "0", "1"), new Dimension("z", "0", "1")), 1.0 / 8);
        solved("x + y + z", List.of(new Dimension("z", "0", "1"), new Dimension("y", "0", "1"), new Dimension("x", "0", "1")), 1.5);
    }

    @Test
    void indefiniteAndMixedIterations() {
        Solution indefinite = MultipleIntegrals.iterated("x*y", List.of(new Dimension("x"), new Dimension("y")));
        assert indefinite.isSolved();
        assert Equivalence.areEqual(indefinite.result(), ExprParser.parse("x^2*y^2/4"));
        assert indefinite.render(Locale.ENGLISH, Format.PLAIN).contains("+ C");

        Solution mixed = MultipleIntegrals.iterated("x*y", List.of(new Dimension("x", "0", "1"), new Dimension("y")));
        assert mixed.isSolved() && Equivalence.areEqual(mixed.result(), ExprParser.parse("y^2/4"));
    }

    @Test
    void stepsAreOneBlockPerIntegral() {
        Solution s = solved("x*y", List.of(new Dimension("x", "0", "y"), new Dimension("y", "0", "1")), 1.0 / 8);
        assert s.steps().size() == 2;
        assert s.steps().get(0).rule() == Rule.INT_ITERATED_DEFINITE && !s.steps().get(0).substeps().isEmpty();
        assert uses(s, Rule.INT_BARROW) && uses(s, Rule.INT_DEFINITE_ANTIDERIVATIVE);
        assert s.extras().get("approximation") instanceof Expr.Num;
    }

    @Test
    void renderingShowsTheIteratedSigns() {
        Solution s = solved("x*y", List.of(new Dimension("x", "0", "y"), new Dimension("y", "0", "1")), 1.0 / 8);
        String plain = s.render(Locale.forLanguageTag("es"), Format.PLAIN);
        assert plain.contains("∫[0, 1] ∫[0, y]") && plain.contains("d" + "x d" + "y") && !plain.contains("+ C") : plain;
        assert plain.contains("integral iterada") : plain;
        String latex = s.render(Locale.ENGLISH, Format.LATEX);
        assert latex.contains("\\int_{0}^{1} \\int_{0}^{y}") && !latex.contains("${") : latex;
        JsonValidator.assertValid(s.render(Locale.ENGLISH, Format.JSON));
        for (Locale locale : new Locale[]{Locale.forLanguageTag("es"), Locale.ENGLISH}) {
            for (Format format : Format.values()) assert !s.render(locale, format).contains("${");
        }
    }

    @Test
    void failuresAreHonest() {
        Solution divergent = MultipleIntegrals.iterated("1/x", List.of(new Dimension("x", "0", "1"), new Dimension("y", "0", "1")));
        assert divergent.status() == Solution.Status.DOES_NOT_EXIST : divergent.status();
        Solution unknown = MultipleIntegrals.iterated("sqrt(1+x^3)*y", List.of(new Dimension("x", "0", "1"), new Dimension("y", "0", "1")));
        assert !unknown.isSolved() && unknown.result() == null;
        try {
            new Dimension("x", "0", "");
            assert false;
        } catch (IllegalArgumentException expected) {
            assert !expected.getMessage().isBlank();
        }
    }
}
