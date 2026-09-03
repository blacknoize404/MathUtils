package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

class InequalitiesTest {

    private static Solution solve(String text) {
        return Equations.solve(text);
    }

    private static void assertAnswer(String expected, String text) {
        Solution s = solve(text);
        assert s.isSolved() : text + " sin resolver: " + s.steps().stream().map(Step::rule).toList();
        assert expected.equals(s.answer().toString()) : text + " -> esperaba " + expected + " pero fue " + s.answer();
    }

    private static boolean uses(Solution s, Rule rule) {
        return uses(s.steps(), rule);
    }

    private static boolean uses(List<Step> steps, Rule rule) {
        for (Step step : steps) if (step.rule() == rule || uses(step.substeps(), rule)) return true;
        return false;
    }

    private static List<SolveMethod> methods(String text) {
        Relation r = Relation.parse(text).orElseThrow();
        return Equations.mainVariable(r).isEmpty() ? List.of() : Inequalities.applicableMethods(r, Equations.mainVariable(r));
    }

    @Test
    void linearInequalities() {
        assertAnswer("(-∞, 2)", "2x + 3 < 7");
        assertAnswer("[5, ∞)", "3x - 1 >= 2x + 4");
        assertAnswer("(-∞, 1/2]", "2x <= 1");
        assertAnswer("ℝ", "x + 1 > x");
        assertAnswer("∅", "x + 1 < x");
    }

    @Test
    void dividingByANegativeNumberReversesTheInequality() {
        assertAnswer("(-∞, -3)", "-2x > 6");
        assertAnswer("[-2, ∞)", "-3x <= 6");
        assert uses(solve("-2x > 6"), Rule.ALG_DIVIDE_BOTH_NEGATIVE);
        assert !uses(solve("2x > 6"), Rule.ALG_DIVIDE_BOTH_NEGATIVE);
    }

    @Test
    void quadraticInequalitiesUseTheSignChart() {
        assertAnswer("(-2, 2)", "x^2 - 4 < 0");
        assertAnswer("(-∞, -2] ∪ [2, ∞)", "x^2 - 4 >= 0");
        assertAnswer("ℝ", "x^2 + 1 > 0");
        assertAnswer("∅", "x^2 + 1 < 0");
        assertAnswer("{0}", "x^2 <= 0");
        assertAnswer("(-∞, 1) ∪ (1, ∞)", "x^2 - 2x + 1 > 0");
        assertAnswer("[2, 3]", "x^2 - 5x + 6 <= 0");
        Solution s = solve("x^2 - 5x + 6 < 0");
        assert uses(s, Rule.INEQ_CRITICAL_POINTS) && uses(s, Rule.INEQ_SIGN_TEST) && uses(s, Rule.INEQ_ENDPOINTS_EXCLUDED) && uses(s, Rule.INEQ_COLLECT);
        assert uses(solve("x^2 - 5x + 6 <= 0"), Rule.INEQ_ENDPOINTS_INCLUDED);
    }

    @Test
    void polynomialInequalities() {
        assertAnswer("(-∞, -2] ∪ [1, 3]", "(x - 1)*(x + 2)*(x - 3) <= 0");
        assertAnswer("(-2, 1) ∪ (3, ∞)", "(x - 1)*(x + 2)*(x - 3) > 0");
        assertAnswer("[-1, 0] ∪ [1, ∞)", "x^3 - x >= 0");
    }

    @Test
    void rationalInequalities() {
        assertAnswer("(-∞, -2) ∪ [1, ∞)", "(x - 1)/(x + 2) >= 0");
        assertAnswer("(0, ∞)", "1/x > 0");
        assertAnswer("(-∞, 0)", "1/x < 0");
        assertAnswer("(-2, 1]", "(x - 1)/(x + 2) <= 0");
        assertAnswer("(-∞, 1) ∪ (2, ∞)", "(x - 2)/(x - 1) > 0");
    }

    @Test
    void radicalExponentialAndLogarithmicInequalities() {
        assertAnswer("[2, 11)", "sqrt(x - 2) < 3");
        assertAnswer("(1, ∞)", "ln(x) > 0");
        assertAnswer("(0, ∞)", "e^x > 1");
        assertAnswer("[0, ∞)", "sqrt(x) >= 0");
    }

    @Test
    void absoluteValueInequalitiesAreSplitIntoCases() {
        assertAnswer("(-2, 4)", "abs(x - 1) < 3");
        assertAnswer("(-∞, -2] ∪ [4, ∞)", "abs(x - 1) >= 3");
        assertAnswer("[-1, 2]", "abs(2x - 1) <= 3");
        assertAnswer("∅", "abs(2x + 1) < -1");
        assertAnswer("ℝ", "abs(x) > -1");
        assertAnswer("(-∞, 0) ∪ (0, ∞)", "abs(x) > 0");
        assertAnswer("{0}", "abs(x) <= 0");
        assert uses(solve("abs(x - 1) < 3"), Rule.INEQ_ABS_CASES);
        assertAnswer("(-∞, -3) ∪ (3, ∞)", "3 < abs(x)");
    }

    @Test
    void notEqualRemovesTheSolutionsOfTheEquation() {
        assertAnswer("(-∞, 3) ∪ (3, ∞)", "x != 3");
        assertAnswer("(-∞, -2) ∪ (-2, 2) ∪ (2, ∞)", "x^2 != 4");
        assertAnswer("ℝ", "x^2 + 1 != 0");
    }

    @Test
    void methodsAreListed() {
        assert methods("2x + 3 < 7").equals(List.of(SolveMethod.AUTO, SolveMethod.ISOLATE, SolveMethod.SIGN_CHART)) : methods("2x + 3 < 7");
        assert methods("x^2 - 4 < 0").equals(List.of(SolveMethod.AUTO, SolveMethod.SIGN_CHART));
        assert methods("abs(x) < 2").containsAll(List.of(SolveMethod.ABSOLUTE_CASES, SolveMethod.SIGN_CHART));
        assert methods("3 < 4").isEmpty();
    }

    @Test
    void theSignChartCanBeForcedOnALinearInequality() {
        Solution s = Equations.solve(Relation.parse("2x + 3 < 7").orElseThrow(), "x", SolveMethod.SIGN_CHART);
        assert s.isSolved() && s.answer().toString().equals("(-∞, 2)") && uses(s, Rule.INEQ_SIGN_TEST) : s.answer();
        Solution bad = Equations.solve(Relation.parse("x^2 < 4").orElseThrow(), "x", SolveMethod.ISOLATE);
        assert !bad.isSolved() && uses(bad, Rule.POLY_NOT_APPLICABLE);
    }

    @Test
    void theAnswerKnowsWhichNumbersBelongToIt() {
        Solution s = solve("(x - 1)/(x + 2) >= 0");
        assert s.answer().contains(1) && s.answer().contains(5) && !s.answer().contains(-2) && !s.answer().contains(0) && s.answer().contains(-3);
        assert s.kind() == Solution.Kind.INEQUALITY && s.statement().toString().equals("(x - 1)/(x + 2) >= 0");
    }

    @Test
    void stepsRenderInBothLanguagesAndAllFormats() {
        String[] texts = {"2x + 3 < 7", "-2x > 6", "x^2 - 4 >= 0", "(x - 1)/(x + 2) >= 0", "abs(x - 1) < 3", "sqrt(x - 2) < 3", "x != 3"};
        for (String text : texts) {
            Solution s = solve(text);
            for (Locale locale : List.of(Locale.forLanguageTag("es"), Locale.ENGLISH)) {
                for (Format format : Format.values()) {
                    String rendered = s.render(locale, format);
                    assert !rendered.isBlank() && !rendered.contains("${") : text + " " + format + " " + locale + ": " + rendered;
                }
            }
        }
        String plain = solve("x^2 - 4 >= 0").render(Locale.ENGLISH, Format.PLAIN);
        assert plain.contains("Solve the inequality") && plain.contains("x ∈ (-∞, -2] ∪ [2, ∞)") : plain;
    }
}
