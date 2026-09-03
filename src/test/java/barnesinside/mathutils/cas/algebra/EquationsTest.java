package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.expr.SolutionSet;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

class EquationsTest {

    private static Solution solve(String text) {
        return Equations.solve(text);
    }

    private static Solution solve(String text, SolveMethod method) {
        Relation r = Relation.parse(text).orElseThrow();
        return Equations.solve(r, Equations.mainVariable(r), method);
    }

    private static void assertAnswer(String expected, Solution s) {
        assert s.isSolved() : "sin resolver: " + s.statement() + " " + s.steps().stream().map(st -> st.rule()).toList();
        assert expected.equals(s.answer().toString()) : s.statement() + " -> esperaba " + expected + " pero fue " + s.answer();
    }

    private static boolean uses(Solution s, Rule rule) {
        return uses(s.steps(), rule);
    }

    private static boolean uses(List<barnesinside.mathutils.cas.steps.Step> steps, Rule rule) {
        for (var step : steps) {
            if (step.rule() == rule || uses(step.substeps(), rule)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- Primer grado

    @Test
    void linearEquations() {
        assertAnswer("{4}", solve("2x + 3 = 11"));
        assertAnswer("{4}", solve("3x - 2 = x + 6"));
        assertAnswer("{28/3}", solve("x/2 + 1/3 = 5"));
        assertAnswer("{-1}", solve("-3x = 3"));
        assertAnswer("{3/2}", solve("2(x + 1) = 5"));
        Solution s = solve("2x + 3 = 11");
        assert uses(s, Rule.ALG_ADD_TO_BOTH) && uses(s, Rule.ALG_DIVIDE_BOTH);
    }

    @Test
    void linearIdentitiesAndContradictions() {
        assertAnswer("ℝ", solve("2(x + 1) = 2x + 2"));
        assertAnswer("∅", solve("x + 1 = x + 2"));
        assert uses(solve("x + 1 = x + 2"), Rule.ALG_CONTRADICTION);
        assert uses(solve("2(x + 1) = 2x + 2"), Rule.ALG_IDENTITY);
    }

    // ---------------------------------------------------------------- Segundo grado

    @Test
    void quadraticEquationsWithEachMethod() {
        for (SolveMethod m : List.of(SolveMethod.AUTO, SolveMethod.FACTORING, SolveMethod.QUADRATIC_FORMULA, SolveMethod.COMPLETE_SQUARE)) {
            assertAnswer("{2, 3}", solve("x^2 - 5x + 6 = 0", m));
        }
        assertAnswer("{-2, 1/2}", solve("2x^2 + 3x - 2 = 0", SolveMethod.QUADRATIC_FORMULA));
        assertAnswer("{-2, 1/2}", solve("2x^2 + 3x - 2 = 0", SolveMethod.FACTORING));
        assertAnswer("{-2, 1/2}", solve("2x^2 + 3x - 2 = 0", SolveMethod.COMPLETE_SQUARE));
    }

    @Test
    void eachQuadraticMethodExplainsItself() {
        assert uses(solve("x^2 - 5x + 6 = 0", SolveMethod.FACTORING), Rule.ALG_ZERO_PRODUCT);
        Solution formula = solve("x^2 - 5x + 6 = 0", SolveMethod.QUADRATIC_FORMULA);
        assert uses(formula, Rule.ALG_QUADRATIC_FORMULA) && uses(formula, Rule.ALG_DISCRIMINANT) && uses(formula, Rule.ALG_DISCRIMINANT_POSITIVE);
        Solution square = solve("x^2 - 5x + 6 = 0", SolveMethod.COMPLETE_SQUARE);
        assert uses(square, Rule.ALG_COMPLETE_SQUARE) && uses(square, Rule.ALG_PERFECT_SQUARE_FORM) && uses(square, Rule.ALG_SQUARE_ROOT);
    }

    @Test
    void quadraticsWithIrrationalOrNoRealSolutions() {
        Solution irrational = solve("x^2 - 2 = 0");
        assert irrational.answer().contains(Math.sqrt(2)) && irrational.answer().contains(-Math.sqrt(2)) : irrational.answer();
        Solution formula = solve("x^2 + x - 1 = 0");
        assert formula.answer().contains((-1 + Math.sqrt(5)) / 2) && formula.answer().contains((-1 - Math.sqrt(5)) / 2) : formula.answer();
        assertAnswer("∅", solve("x^2 + 1 = 0"));
        assert uses(solve("x^2 + x + 1 = 0"), Rule.ALG_DISCRIMINANT_NEGATIVE);
        assertAnswer("{1}", solve("x^2 - 2x + 1 = 0"));
        assertAnswer("{0, 3}", solve("x^2 = 3x"));
    }

    @Test
    void squareRootMethod() {
        assertAnswer("{-3, 3}", solve("x^2 = 9", SolveMethod.SQUARE_ROOT));
        assertAnswer("{-3, 3}", solve("2x^2 - 18 = 0"));
        assertAnswer("∅", solve("x^2 = -4", SolveMethod.SQUARE_ROOT));
        assert uses(solve("x^2 = 9", SolveMethod.SQUARE_ROOT), Rule.ALG_SQUARE_ROOT);
    }

    @Test
    void methodsAreListedByApplicability() {
        assert methods("x^2 - 5x + 6 = 0").equals(List.of(SolveMethod.AUTO, SolveMethod.FACTORING, SolveMethod.COMPLETE_SQUARE, SolveMethod.QUADRATIC_FORMULA))
                : methods("x^2 - 5x + 6 = 0");
        assert methods("x^2 = 9").contains(SolveMethod.SQUARE_ROOT) && methods("x^2 = 9").contains(SolveMethod.FACTORING);
        assert !methods("x^2 - 2 = 0").contains(SolveMethod.FACTORING) && methods("x^2 - 2 = 0").contains(SolveMethod.QUADRATIC_FORMULA);
        assert methods("x + 1 = 3").equals(List.of(SolveMethod.AUTO, SolveMethod.ISOLATE));
        assert methods("x^3 - 6x^2 + 11x - 6 = 0").containsAll(List.of(SolveMethod.FACTORING, SolveMethod.RATIONAL_ROOTS));
        assert methods("x^4 - 5x^2 + 4 = 0").contains(SolveMethod.SUBSTITUTION);
        assert methods("1/x = 2").contains(SolveMethod.ZERO_NUMERATOR);
        assert methods("sqrt(x) = 2").contains(SolveMethod.ISOLATE_RADICAL);
        assert methods("abs(x) = 2").contains(SolveMethod.ABSOLUTE_CASES);
        assert methods("ln(x) = 2").contains(SolveMethod.LOGARITHMS);
        assert methods("sin(x) = 1/2").contains(SolveMethod.INVERSE_TRIG);
        assert methods("3 = 3").isEmpty();
    }

    private static List<SolveMethod> methods(String text) {
        Relation r = Relation.parse(text).orElseThrow();
        return Equations.applicableMethods(r, Equations.mainVariable(r));
    }

    @Test
    void aMethodThatDoesNotApplyIsReported() {
        Solution s = solve("x^2 - 5x + 6 = 0", SolveMethod.SQUARE_ROOT);
        assert !s.isSolved() && uses(s, Rule.POLY_NOT_APPLICABLE);
    }

    // ---------------------------------------------------------------- Polinomios

    @Test
    void polynomialsOfHigherDegree() {
        assertAnswer("{1, 2, 3}", solve("x^3 - 6x^2 + 11x - 6 = 0"));
        assertAnswer("{1, 2, 3}", solve("x^3 - 6x^2 + 11x - 6 = 0", SolveMethod.RATIONAL_ROOTS));
        assertAnswer("{1, 2, 3}", solve("x^3 - 6x^2 + 11x - 6 = 0", SolveMethod.FACTORING));
        assertAnswer("{-1, 0, 1}", solve("x^3 - x = 0"));
        assertAnswer("{-2, -1, 1, 2}", solve("x^4 - 5x^2 + 4 = 0", SolveMethod.SUBSTITUTION));
        assertAnswer("{-2, -1, 1, 2}", solve("x^4 - 5x^2 + 4 = 0"));
        assertAnswer("{-1, 1/2, 2}", solve("2x^3 - 3x^2 - 3x + 2 = 0"));
        assert uses(solve("x^3 - 6x^2 + 11x - 6 = 0", SolveMethod.RATIONAL_ROOTS), Rule.ALG_SYNTHETIC_DIVISION);
        assert uses(solve("x^4 - 5x^2 + 4 = 0", SolveMethod.SUBSTITUTION), Rule.ALG_BACK_SUBSTITUTION);
    }

    @Test
    void aBinomialPowerHasAnExactRoot() {
        Solution s = solve("x^3 - 2 = 0");
        assert !Equations.isApproximate(s) && s.answer().contains(Math.cbrt(2)) : s.answer();
        assert uses(s, Rule.ALG_SUBSTITUTION);
    }

    @Test
    void polynomialsWithoutExactRootsAreApproximated() {
        Solution s = solve("x^3 - x - 1 = 0");
        assert Equations.isApproximate(s) && s.answer().contains(1.32471796) : s.answer();
        assert uses(s, Rule.ALG_NUMERIC_ROOT);
        assert s.answer().toString().equals("{1.32472}") : s.answer();
    }

    // ---------------------------------------------------------------- Fracciones y radicales

    @Test
    void rationalEquations() {
        assertAnswer("{1/2}", solve("1/x = 2"));
        assertAnswer("{-1}", solve("(x^2 - 1)/(x - 1) = 0"));
        assertAnswer("{3}", solve("x/(x - 2) = 3/(x - 2)"));
        assert uses(solve("1/x = 2"), Rule.ALG_NUMERATOR_ZERO);
    }

    @Test
    void radicalEquationsDiscardFalseSolutions() {
        Solution s = solve("sqrt(x + 2) = x");
        assertAnswer("{2}", s);
        assert uses(s, Rule.ALG_SQUARE_BOTH_SIDES) && uses(s, Rule.ALG_CHECK_EXTRANEOUS) && uses(s, Rule.ALG_CHECK_VALID);
        assertAnswer("{3}", solve("sqrt(x + 1) + sqrt(x - 2) = 3"));
        assertAnswer("{9}", solve("sqrt(x) = 3"));
        assertAnswer("∅", solve("sqrt(x) = -2"));
    }

    // ---------------------------------------------------------------- Valor absoluto

    @Test
    void absoluteValueEquations() {
        assertAnswer("{-1, 4}", solve("abs(2x - 3) = 5"));
        assertAnswer("∅", solve("abs(x) = -2"));
        assert uses(solve("abs(x) = -2"), Rule.ALG_ABS_NEGATIVE);
        Solution both = solve("abs(x - 1) = abs(2x + 3)");
        assert both.answer().contains(-4) && both.answer().contains(-2.0 / 3) : both.answer();
        assert uses(solve("abs(2x - 3) = 5"), Rule.ALG_ABS_CASES);
    }

    // ---------------------------------------------------------------- Exponenciales y logaritmos

    @Test
    void exponentialEquations() {
        Solution s = solve("2^x = 8");
        assert s.answer().contains(3) : s.answer();
        assert s.answer().contains(3) && uses(s, Rule.ALG_TAKE_LOG);
        Solution e = solve("e^x = 5");
        assert e.answer().contains(Math.log(5)) : e.answer();
        Solution quadratic = solve("4^x - 5*2^x + 4 = 0");
        assert quadratic.answer().contains(0) && quadratic.answer().contains(2) : quadratic.answer();
        assert uses(quadratic, Rule.ALG_SUBSTITUTION);
        assertAnswer("∅", solve("2^x = -3"));
    }

    @Test
    void logarithmicEquations() {
        Solution s = solve("ln(x) = 2");
        assert s.answer().contains(Math.exp(2)) && uses(s, Rule.ALG_TO_EXPONENTIAL) : s.answer();
        Solution two = solve("ln(x) + ln(x - 1) = ln(2)");
        assert two.answer().contains(2) && !two.answer().contains(-1) : two.answer();
        assert uses(solve("ln(x) = 2"), Rule.ALG_LOG_DOMAIN);
    }

    // ---------------------------------------------------------------- Trigonometría

    @Test
    void trigonometricEquationsHavePeriodicSolutions() {
        Solution sin = solve("sin(x) = 1/2");
        assert sin.answer() instanceof SolutionSet.Periodic : sin.answer();
        assert sin.answer().contains(Math.PI / 6) && sin.answer().contains(5 * Math.PI / 6 + 2 * Math.PI) && !sin.answer().contains(Math.PI / 3);
        Solution cos = solve("2cos(x) - 1 = 0");
        assert cos.answer().contains(Math.PI / 3) && cos.answer().contains(-Math.PI / 3);
        Solution tan = solve("tan(x) = 1");
        assert tan.answer().contains(Math.PI / 4) && tan.answer().contains(Math.PI / 4 + 3 * Math.PI) : tan.answer();
        Solution quadratic = solve("2sin(x)^2 - sin(x) - 1 = 0");
        assert quadratic.answer().contains(Math.PI / 2) && quadratic.answer().contains(7 * Math.PI / 6) : quadratic.answer();
        assertAnswer("∅", solve("sin(x) = 2"));
        assert uses(sin, Rule.ALG_TRIG_GENERAL);
        Solution scaled = solve("sin(2x) = 0");
        assert scaled.answer().contains(0) && scaled.answer().contains(Math.PI / 2) : scaled.answer();
    }

    @Test
    void whenThereIsNoFormulaTheRootIsFoundNumerically() {
        Solution s = solve("x = cos(x)");
        assert Equations.isApproximate(s) && s.answer().contains(0.73908513) : s.answer();
        assert uses(s, Rule.ALG_NUMERIC_ROOT);
    }

    // ---------------------------------------------------------------- Varios

    @Test
    void anEquationWithOtherVariablesIsNotSolved() {
        Relation r = Relation.parse("a*x = 3").orElseThrow();
        assert !Equations.solve(r, "x").isSolved();
    }

    @Test
    void theStatementAndTheVariableAreKept() {
        Solution s = solve("t^2 = 4");
        assert s.kind() == Solution.Kind.EQUATION && s.variable().equals("t") && s.statement().toString().equals("t^2 = 4");
    }

    @Test
    void stepsRenderInBothLanguagesAndAllFormats() {
        String[] equations = {"2x + 3 = 11", "x^2 - 5x + 6 = 0", "x^3 - 6x^2 + 11x - 6 = 0", "1/x = 2", "sqrt(x + 2) = x",
                "abs(2x - 3) = 5", "ln(x) = 2", "sin(x) = 1/2", "x^4 - 5x^2 + 4 = 0", "x = cos(x)", "2^x = 8"};
        for (String text : equations) {
            Solution s = solve(text);
            for (Locale locale : List.of(Locale.forLanguageTag("es"), Locale.ENGLISH)) {
                for (Format format : Format.values()) {
                    String rendered = s.render(locale, format);
                    assert !rendered.isBlank() && !rendered.contains("${") : text + " " + format + " " + locale + ": " + rendered;
                }
            }
        }
        assert solve("x^2 - 5x + 6 = 0").render(Locale.ENGLISH, Format.PLAIN).contains("Solve x^2 - 5*x + 6 = 0 for x");
        assert solve("x^2 - 5x + 6 = 0").render(Locale.ENGLISH, Format.PLAIN).contains("x ∈ {2, 3}");
        assert solve("x^2 - 5x + 6 = 0").render(Locale.ENGLISH, Format.JSON).contains("\"answer\"");
    }

    @Test
    void methodTextsExistInBothLanguages() {
        for (SolveMethod m : SolveMethod.values()) {
            assert !m.title(Locale.ENGLISH).isBlank() && !m.description(Locale.forLanguageTag("es")).isBlank() : m;
        }
        assert Expr.ONE.isOne();
    }
}
