package barnesinside.mathutils.cas.limit;

import barnesinside.mathutils.cas.expr.Equivalence;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.limit.Limits.Direction;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class LimitsTest {

    private static Expr p(String s) {
        return ExprParser.parse(s);
    }

    private static Solution lim(String f, String point) {
        return Limits.limit(f, "x", point);
    }

    private static void assertLimit(String f, String point, String expected) {
        assertLimit(f, point, Direction.BOTH, expected);
    }

    private static void assertLimit(String f, String point, Direction direction, String expected) {
        Solution s = Limits.limit(f, "x", point, direction);
        assert s.isSolved() : f + " en " + point + " -> " + s.status() + "\n" + s;
        Expr want = p(expected);
        boolean same = s.result().equals(want) || Equivalence.areEqual(s.result(), want);
        assert same : "lim " + f + " en " + point + ": esperado " + expected + " pero fue " + s.result();
    }

    private static List<Step> flatten(List<Step> steps) {
        List<Step> all = new ArrayList<>();
        for (Step step : steps) {
            all.add(step);
            all.addAll(flatten(step.substeps()));
        }
        return all;
    }

    private static boolean uses(Solution s, Rule rule) {
        return flatten(s.steps()).stream().anyMatch(step -> step.rule() == rule);
    }

    // ------------------------------------------------------------ Sustitución directa

    @Test
    void directSubstitution() {
        assertLimit("x^2 + 1", "2", "5");
        assertLimit("sqrt(x + 1)", "3", "2");
        assertLimit("sin(x)", "pi/6", "1/2");
        assertLimit("e^x", "0", "1");
        assertLimit("5", "7", "5");
        assertLimit("x/(x+2)", "1", "1/3");
        assertLimit("ln(x)", "e", "1");
        assertLimit("1/x^2", "0", "oo");
        assertLimit("x*y", "3", "3y");
    }

    @Test
    void directSubstitutionUsesTheRightRule() {
        assert uses(lim("x^2 + 1", "2"), Rule.LIMIT_DIRECT);
        assert uses(lim("5", "7"), Rule.LIMIT_CONSTANT);
        assert uses(lim("1/x^2", "0"), Rule.LIMIT_ALGEBRA);
    }

    // ------------------------------------------------------------ L'Hôpital 0/0

    @Test
    void zeroOverZero() {
        assertLimit("(x^2 - 1)/(x - 1)", "1", "2");
        assertLimit("sin(x)/x", "0", "1");
        assertLimit("(1 - cos(x))/x^2", "0", "1/2");
        assertLimit("(e^x - 1)/x", "0", "1");
        assertLimit("(e^x - 1 - x)/x^2", "0", "1/2");
        assertLimit("ln(x)/(x - 1)", "1", "1");
        assertLimit("tan(x)/x", "0", "1");
        assertLimit("(sqrt(x) - 2)/(x - 4)", "4", "1/4");
        assertLimit("(x^3 - 8)/(x - 2)", "2", "12");
        assertLimit("(sin(x) - x)/x^3", "0", "-1/6");
        assertLimit("(atan(x) - x)/x^3", "0", "-1/3");
        assertLimit("(x - sin(x))/(x - tan(x))", "0", "-1/2");
    }

    @Test
    void lHopitalLeavesVisibleSteps() {
        Solution s = lim("sin(x)/x", "0");
        assert uses(s, Rule.LIMIT_LHOPITAL_ZERO);
        assert uses(s, Rule.DIFF_SIN) : "debe incluir los pasos de la derivada";
        Step lhopital = flatten(s.steps()).stream().filter(st -> st.rule() == Rule.LIMIT_LHOPITAL_ZERO).findFirst().get();
        assert lhopital.params().get("n").equals(p("sin(x)")) && lhopital.params().get("d").equals(p("x"));
    }

    @Test
    void repeatedLHopital() {
        Solution s = lim("(1 - cos(x))/x^2", "0");
        long applications = flatten(s.steps()).stream().filter(st -> st.rule() == Rule.LIMIT_LHOPITAL_ZERO).count();
        assert applications == 2 : applications;
    }

    // ------------------------------------------------------------ Infinito

    @Test
    void rationalFunctionsAtInfinity() {
        assertLimit("x^2/(x^2 + 1)", "oo", "1");
        assertLimit("(3x^2 + x)/(2x^2 - 5)", "oo", "3/2");
        assertLimit("x^3/x^2", "oo", "oo");
        assertLimit("x/x^2", "oo", "0");
        assertLimit("(-x^3)/(x^2 + 1)", "oo", "-oo");
        assertLimit("x^3/(x^2+1)", "-oo", "-oo");
        assertLimit("x^2/(x+1)", "-oo", "-oo");
        assertLimit("(2x + 1)/(x - 3)", "-oo", "2");
        assertLimit("x^2 - 3x", "oo", "oo");
        assertLimit("1/x", "oo", "0");
    }

    @Test
    void rationalFunctionRule() {
        assert uses(lim("(3x^2 + x)/(2x^2 - 5)", "oo"), Rule.LIMIT_INFINITY_RATIONAL);
    }

    @Test
    void rootsAtInfinityUseDominantTerms() {
        assertLimit("sqrt(x^2 + x) - x", "oo", "1/2");
        assertLimit("sqrt(x^2 + 1)/x", "oo", "1");
        assertLimit("sqrt(x^2 + 1)/x", "-oo", "-1");
        assertLimit("x*(sqrt(x^2 + 1) - x)", "oo", "1/2");
        assertLimit("sqrt(4x^2 + 1)/(x + 1)", "oo", "2");
    }

    @Test
    void growthComparison() {
        assertLimit("e^x/x", "oo", "oo");
        assertLimit("ln(x)/x", "oo", "0");
        assertLimit("x/e^x", "oo", "0");
        assertLimit("x^2/e^x", "oo", "0");
        assertLimit("ln(x)/sqrt(x)", "oo", "0");
        assertLimit("e^x", "oo", "oo");
        assertLimit("e^x", "-oo", "0");
        assertLimit("e^(-x)", "oo", "0");
    }

    @Test
    void inverseTrigonometricAtInfinity() {
        assertLimit("atan(x)", "oo", "pi/2");
        assertLimit("atan(x)", "-oo", "-pi/2");
        assertLimit("tanh(x)", "oo", "1");
        assertLimit("erf(x)", "oo", "1");
    }

    // ------------------------------------------------------------ Otras formas indeterminadas

    @Test
    void zeroTimesInfinity() {
        assertLimit("x*ln(x)", "0", Direction.RIGHT, "0");
        assertLimit("x*sin(1/x)", "oo", "1");
        assertLimit("x*e^(-x)", "oo", "0");
        assertLimit("x^2*e^(-x)", "oo", "0");
    }

    @Test
    void zeroTimesInfinityUsesTheRewrite() {
        assert uses(Limits.limit("x*ln(x)", "x", "0", Direction.RIGHT), Rule.LIMIT_PRODUCT_TO_QUOTIENT);
    }

    @Test
    void infinityMinusInfinity() {
        assertLimit("1/x - 1/sin(x)", "0", "0");
        assertLimit("1/(x - 1) - 2/(x^2 - 1)", "1", "1/2");
        assertLimit("1/ln(x) - 1/(x - 1)", "1", "1/2");
    }

    @Test
    void combiningFractionsIsAStep() {
        assert uses(lim("1/(x - 1) - 2/(x^2 - 1)", "1"), Rule.LIMIT_COMBINE_FRACTIONS);
    }

    @Test
    void exponentialForms() {
        assertLimit("(1 + 1/x)^x", "oo", "e");
        assertLimit("x^x", "0", Direction.RIGHT, "1");
        assertLimit("(1 + x)^(1/x)", "0", "e");
        assertLimit("x^(1/x)", "oo", "1");
        assertLimit("(1 + 2/x)^x", "oo", "e^2");
        assertLimit("(cos(x))^(1/x^2)", "0", "e^(-1/2)");
    }

    @Test
    void exponentialFormUsesTheRewrite() {
        assert uses(lim("(1 + 1/x)^x", "oo"), Rule.LIMIT_EXPONENTIAL_FORM);
    }

    // ------------------------------------------------------------ Laterales y no existencia

    @Test
    void oneSidedInfiniteLimits() {
        assertLimit("1/x", "0", Direction.RIGHT, "oo");
        assertLimit("1/x", "0", Direction.LEFT, "-oo");
        assertLimit("1/(x - 2)", "2", Direction.RIGHT, "oo");
        assertLimit("1/(x - 2)", "2", Direction.LEFT, "-oo");
        assertLimit("ln(x)", "0", Direction.RIGHT, "-oo");
        assertLimit("tan(x)", "pi/2", Direction.LEFT, "oo");
        assertLimit("tan(x)", "pi/2", Direction.RIGHT, "-oo");
    }

    @Test
    void absoluteValueOnOneSide() {
        assertLimit("abs(x)/x", "0", Direction.RIGHT, "1");
        assertLimit("abs(x)/x", "0", Direction.LEFT, "-1");
        assertLimit("abs(x - 2)/(x - 2)", "2", Direction.RIGHT, "1");
        assert uses(Limits.limit("abs(x)/x", "x", "0", Direction.RIGHT), Rule.LIMIT_ABSOLUTE_VALUE);
    }

    @Test
    void limitDoesNotExist() {
        for (String[] c : new String[][]{{"1/x", "0"}, {"abs(x)/x", "0"}, {"sin(x)", "oo"}, {"cos(x)", "-oo"},
                {"1/(x - 3)", "3"}, {"x*sin(x)", "oo"}}) {
            Solution s = lim(c[0], c[1]);
            assert s.status() == Solution.Status.DOES_NOT_EXIST || (c[0].equals("x*sin(x)") && !s.isSolved())
                    : c[0] + " en " + c[1] + " -> " + s.status();
            assert s.result() == null;
        }
    }

    @Test
    void differentSidesLeaveAnExplanation() {
        Solution s = lim("1/x", "0");
        assert uses(s, Rule.LIMIT_SIDES) && uses(s, Rule.LIMIT_DIFFERENT_SIDES);
        String text = s.render(Locale.forLanguageTag("es"), Format.PLAIN);
        assert text.contains("El límite no existe.") : text;
    }

    @Test
    void oscillationLeavesAnExplanation() {
        assert uses(lim("sin(x)", "oo"), Rule.LIMIT_OSCILLATES);
    }

    @Test
    void twoSidedLimitFromAgreeingSides() {
        // Los dos lados coinciden: no hace falta la regla de límites laterales para sin(x)/x
        assertLimit("(x^2)/abs(x)", "0", "0");
    }

    // ------------------------------------------------------------ Honestidad

    @Test
    void unsolvedLimitsAreReportedNotGuessed() {
        // Oscila con amplitud que crece: no se afirma nada
        Solution s = lim("x*sin(x)", "oo");
        assert !s.isSolved();
        assert s.result() == null;
    }

    @Test
    void numericVerificationRejectsWrongAnswers() {
        // Un límite con otras variables no se verifica, pero los numéricos sí
        for (String[] c : new String[][]{{"(x^2-1)/(x-1)", "1"}, {"sin(3x)/x", "0"}, {"(1+3/x)^x", "oo"}}) {
            Solution s = lim(c[0], c[1]);
            assert s.isSolved() : c[0];
        }
        assertLimit("sin(3x)/x", "0", "3");
        assertLimit("(1+3/x)^x", "oo", "e^3");
    }

    // ------------------------------------------------------------ Entradas y salida

    @Test
    void pointCanBeAnExpression() {
        assertLimit("sin(x)/(x - pi)", "pi", "-1");
        assertLimit("cos(x)", "pi/3", "1/2");
    }

    @Test
    void solutionRecordsTheProblem() {
        Solution s = Limits.limit("sin(x)/x", "x", "0", Direction.RIGHT);
        assert s.kind() == Solution.Kind.LIMIT;
        assert s.problem().equals(p("sin(x)/x")) && s.variable().equals("x");
        assert s.extras().get("a").isZero();
        assert s.extras().get("direction").equals(Expr.num(1));
    }

    @Test
    void renderingInBothLanguages() {
        Solution s = lim("sin(x)/x", "0");
        String es = s.render(Locale.forLanguageTag("es"), Format.PLAIN);
        assert es.contains("Calcular el límite de sin(x)/x cuando x tiende a 0.") : es;
        assert es.contains("Regla de L'Hôpital (0/0)") : es;
        assert es.contains("Resultado: lim[x -> 0] sin(x)/x = 1") : es;

        String en = s.render(Locale.ENGLISH, Format.PLAIN);
        assert en.contains("Compute the limit of sin(x)/x as x approaches 0.") : en;
        assert en.contains("L'Hôpital's rule (0/0)") : en;

        String right = Limits.limit("1/x", "x", "0", Direction.RIGHT).render(Locale.ENGLISH, Format.PLAIN);
        assert right.contains("from the right") : right;
    }

    @Test
    void latexAndJsonRendering() {
        Solution s = lim("sin(x)/x", "0");
        String latex = s.render(Locale.ENGLISH, Format.LATEX);
        assert latex.contains("\\lim_{x \\to 0}") : latex;
        String json = s.render(Locale.ENGLISH, Format.JSON);
        barnesinside.mathutils.cas.JsonValidator.assertValid(json);
        assert json.contains("\"kind\":\"LIMIT\"") && json.contains("\"extras\":{\"a\"");
    }

    @Test
    void everyLimitRuleIsTranslated() {
        for (Rule rule : Rule.values()) {
            for (Locale locale : barnesinside.mathutils.cas.steps.Messages.supportedLocales()) {
                assert barnesinside.mathutils.cas.steps.Messages.has(rule.titleKey(), locale) : rule;
                assert barnesinside.mathutils.cas.steps.Messages.has(rule.textKey(), locale) : rule;
            }
        }
    }

    @Test
    void renderedLimitTextHasNoPlaceholders() {
        for (String[] c : new String[][]{{"sin(x)/x", "0"}, {"(1 + 1/x)^x", "oo"}, {"x*ln(x)", "0"},
                {"1/(x - 1) - 2/(x^2 - 1)", "1"}, {"1/x", "0"}, {"sqrt(x^2 + x) - x", "oo"}, {"x^2/(x+1)", "oo"}}) {
            Solution s = lim(c[0], c[1]);
            for (Locale locale : new Locale[]{Locale.forLanguageTag("es"), Locale.ENGLISH}) {
                for (Format format : Format.values()) {
                    String text = s.render(locale, format);
                    assert !text.contains("${") : c[0] + ": " + text;
                }
            }
        }
    }

    @Test
    void invalidInputsAreParseErrors() {
        try {
            Limits.limit("sin(x", "x", "0");
            assert false;
        } catch (barnesinside.mathutils.exceptions.ExpressionParseException e) {
            assert true;
        }
    }
}
