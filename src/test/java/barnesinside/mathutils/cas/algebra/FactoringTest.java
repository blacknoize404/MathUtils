package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

class FactoringTest {

    private static Expr p(String source) {
        return ExprParser.parse(source);
    }

    private static boolean uses(Solution s, Rule rule) {
        return s.steps().stream().anyMatch(step -> step.rule() == rule);
    }

    private static void assertFactors(String source, FactorMethod method, String expected) {
        Solution s = Factoring.factor(p(source), method);
        assert s.isSolved() : source + " con " + method + " no se resolvió: " + s.steps().stream().map(Step::rule).toList();
        assert s.result().equals(p(expected)) : source + " con " + method + " -> esperaba " + p(expected) + " pero fue " + s.result();
        assert Expand.expand(s.result()).equals(Expand.expand(p(source))) : "el resultado no se desarrolla a la expresión";
        assert !s.steps().isEmpty() : "sin pasos";
    }

    @Test
    void commonFactor() {
        assertFactors("6x^2 + 9x", FactorMethod.COMMON_FACTOR, "3*x*(2x + 3)");
        Solution s = Factoring.factor(p("6x^2 + 9x"), FactorMethod.COMMON_FACTOR);
        assert uses(s, Rule.POLY_COMMON_FACTOR);
    }

    @Test
    void differenceOfSquares() {
        assertFactors("4x^2 - 9", FactorMethod.DIFFERENCE_OF_SQUARES, "(2x - 3)*(2x + 3)");
        assert uses(Factoring.factor(p("x^2 - 25"), FactorMethod.DIFFERENCE_OF_SQUARES), Rule.POLY_DIFF_SQUARES);
    }

    @Test
    void perfectSquareTrinomial() {
        assertFactors("x^2 + 6x + 9", FactorMethod.PERFECT_SQUARE, "(x + 3)^2");
        assertFactors("4a^2 - 12a*b + 9b^2", FactorMethod.PERFECT_SQUARE, "(2a - 3b)^2");
    }

    @Test
    void sumAndDifferenceOfCubes() {
        assertFactors("x^3 + 8", FactorMethod.SUM_DIFFERENCE_OF_CUBES, "(x + 2)*(x^2 - 2x + 4)");
        assertFactors("27x^3 - 1", FactorMethod.SUM_DIFFERENCE_OF_CUBES, "(3x - 1)*(9x^2 + 3x + 1)");
        assert uses(Factoring.factor(p("x^3 + 8"), FactorMethod.SUM_DIFFERENCE_OF_CUBES), Rule.POLY_SUM_CUBES);
        assert uses(Factoring.factor(p("x^3 - 8"), FactorMethod.SUM_DIFFERENCE_OF_CUBES), Rule.POLY_DIFF_CUBES);
    }

    @Test
    void trinomialWithTwoNumbers() {
        assertFactors("x^2 + 5x + 6", FactorMethod.TRINOMIAL, "(x + 2)*(x + 3)");
        assertFactors("x^2 - x - 12", FactorMethod.TRINOMIAL, "(x - 4)*(x + 3)");
        Solution s = Factoring.factor(p("x^2 + 5x + 6"), FactorMethod.TRINOMIAL);
        assert uses(s, Rule.POLY_TRINOMIAL_NUMBERS);
    }

    @Test
    void trinomialWithALeadingCoefficientSplitsTheMiddleTerm() {
        assertFactors("2x^2 + 7x + 3", FactorMethod.TRINOMIAL, "(2x + 1)*(x + 3)");
        assertFactors("6x^2 - 7x - 3", FactorMethod.TRINOMIAL, "(3x + 1)*(2x - 3)");
        Solution s = Factoring.factor(p("2x^2 + 7x + 3"), FactorMethod.TRINOMIAL);
        assert uses(s, Rule.POLY_TRINOMIAL_SPLIT) && uses(s, Rule.POLY_GROUPING) && uses(s, Rule.POLY_COMMON_BINOMIAL);
    }

    @Test
    void grouping() {
        assertFactors("a*x + a*y + b*x + b*y", FactorMethod.GROUPING, "(a + b)*(x + y)");
        assertFactors("x^3 + x^2 + x + 1", FactorMethod.GROUPING, "(x + 1)*(x^2 + 1)");
    }

    @Test
    void rationalRootsWithSyntheticDivision() {
        assertFactors("x^3 - 6x^2 + 11x - 6", FactorMethod.RATIONAL_ROOTS, "(x - 1)*(x - 2)*(x - 3)");
        Solution s = Factoring.factor(p("x^3 - 6x^2 + 11x - 6"), FactorMethod.RATIONAL_ROOTS);
        assert uses(s, Rule.ALG_RATIONAL_ROOT_CANDIDATES) && uses(s, Rule.ALG_ROOT_FOUND) && uses(s, Rule.ALG_SYNTHETIC_DIVISION);
        assertFactors("2x^3 - 3x^2 - 3x + 2", FactorMethod.RATIONAL_ROOTS, "(x + 1)*(x - 2)*(2x - 1)");
    }

    @Test
    void quadraticFormulaAndCompletingTheSquareGiveFactorsWithRoots() {
        Solution s = Factoring.factor(p("x^2 - 2"), FactorMethod.QUADRATIC_FORMULA);
        assert s.isSolved() && Expand.expand(s.result()).equals(p("x^2 - 2")) : s.result();
        Solution c = Factoring.factor(p("x^2 - 2"), FactorMethod.COMPLETE_SQUARE);
        assert c.isSolved() && Expand.expand(c.result()).equals(p("x^2 - 2")) : c.result();
        assert uses(c, Rule.POLY_COMPLETE_SQUARE);
        assertFactors("x^2 + 2x - 8", FactorMethod.QUADRATIC_FORMULA, "(x - 2)*(x + 4)");
        assert !Factoring.factor(p("x^2 + 1"), FactorMethod.QUADRATIC_FORMULA).isSolved();
    }

    @Test
    void substitutionForPolynomialsInAPower() {
        assertFactors("x^4 - 5x^2 + 4", FactorMethod.SUBSTITUTION, "(x - 1)*(x + 1)*(x - 2)*(x + 2)");
        assert uses(Factoring.factor(p("x^4 - 5x^2 + 4"), FactorMethod.SUBSTITUTION), Rule.POLY_SUBSTITUTION);
    }

    @Test
    void aMethodThatDoesNotApplyReportsIt() {
        Solution s = Factoring.factor(p("x^2 + x + 1"), FactorMethod.DIFFERENCE_OF_SQUARES);
        assert !s.isSolved() && uses(s, Rule.POLY_NOT_APPLICABLE);
    }

    @Test
    void autoFactorsCompletely() {
        Solution s = Factoring.factor(p("2x^3 - 8x"));
        assert s.isSolved() && s.result().equals(p("2*x*(x - 2)*(x + 2)")) : s.result();
        Solution t = Factoring.factor(p("x^4 - 1"));
        assert t.result().equals(p("(x - 1)*(x + 1)*(x^2 + 1)")) : t.result();
        Solution u = Factoring.factor(p("(x + 1)^2 - 4"));
        assert u.result().equals(p("(x - 1)*(x + 3)")) : u.result();
    }

    @Test
    void autoReportsIrreducibleExpressions() {
        Solution s = Factoring.factor(p("x^2 + 1"));
        assert s.isSolved() && s.result().equals(p("x^2 + 1")) && uses(s, Rule.POLY_IRREDUCIBLE);
    }

    @Test
    void applicableMethodsAreTheOnesThatReallyWork() {
        List<FactorMethod> trinomial = Factoring.applicableMethods(p("x^2 + 5x + 6"));
        assert trinomial.contains(FactorMethod.AUTO) && trinomial.contains(FactorMethod.TRINOMIAL)
                && trinomial.contains(FactorMethod.RATIONAL_ROOTS) && trinomial.contains(FactorMethod.QUADRATIC_FORMULA)
                && trinomial.contains(FactorMethod.COMPLETE_SQUARE) : trinomial;
        assert !trinomial.contains(FactorMethod.DIFFERENCE_OF_SQUARES) && !trinomial.contains(FactorMethod.COMMON_FACTOR) : trinomial;

        List<FactorMethod> squares = Factoring.applicableMethods(p("x^2 - 16"));
        assert squares.contains(FactorMethod.DIFFERENCE_OF_SQUARES) && squares.contains(FactorMethod.RATIONAL_ROOTS) : squares;

        assert Factoring.applicableMethods(p("x^2 + 1")).isEmpty();
        List<FactorMethod> grouped = Factoring.applicableMethods(p("a*x + a*y + b*x + b*y"));
        assert grouped.contains(FactorMethod.GROUPING) && !grouped.contains(FactorMethod.TRINOMIAL) : grouped;
    }

    @Test
    void expandUsesTheRightTheorem() {
        Solution distributive = Factoring.expand(p("(x + 1)*(x + 2)"));
        assert distributive.result().equals(p("x^2 + 3x + 2")) && uses(distributive, Rule.POLY_DISTRIBUTE);
        Solution binomial = Factoring.expand(p("(x + 2)^3"));
        assert binomial.result().equals(p("x^3 + 6x^2 + 12x + 8")) && uses(binomial, Rule.POLY_BINOMIAL);
        Solution multinomial = Factoring.expand(p("(a + b + c)^2"));
        assert uses(multinomial, Rule.POLY_MULTINOMIAL);
    }

    @Test
    void theStepsRenderInBothLanguages() {
        Solution s = Factoring.factor(p("x^2 + 5x + 6"), FactorMethod.TRINOMIAL);
        for (Locale locale : List.of(Locale.forLanguageTag("es"), Locale.ENGLISH)) {
            for (Format format : Format.values()) {
                String text = s.render(locale, format);
                assert !text.isBlank() && !text.contains("${") : format + " " + locale + ": " + text;
            }
        }
        assert s.render(Locale.ENGLISH, Format.PLAIN).contains("Factor x^2");
        for (FactorMethod m : FactorMethod.values()) {
            assert !m.title(Locale.ENGLISH).isBlank() && !m.description(Locale.forLanguageTag("es")).isBlank();
        }
    }
}
