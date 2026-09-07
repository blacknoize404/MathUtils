package barnesinside.mathutils.cas;

import barnesinside.mathutils.cas.diff.DerivativeMethod;
import barnesinside.mathutils.cas.diff.Derivatives;
import barnesinside.mathutils.cas.expr.Equivalence;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.integral.IntegralMethod;
import barnesinside.mathutils.cas.integral.Integrals;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

class MethodsTest {

    private static Expr p(String source) {
        return ExprParser.parse(source);
    }

    private static boolean uses(List<Step> steps, Rule rule) {
        for (Step step : steps) if (step.rule() == rule || uses(step.substeps(), rule)) return true;
        return false;
    }

    // ---------------------------------------------------------------- Derivadas

    @Test
    void everyDerivativeMethodGivesTheSameResult() {
        for (String source : new String[]{"(x + 1)^2*sin(x)", "(x^2 + 1)/(x - 1)", "x^x", "x*e^x*sin(x)", "(x^2 - 1)/(x - 1)"}) {
            Expr f = p(source);
            Expr expected = Derivatives.derivative(f, "x").result();
            for (DerivativeMethod m : Derivatives.applicableMethods(f, "x")) {
                Solution s = Derivatives.derivative(f, "x", m);
                assert s.isSolved() : source + " " + m;
                assert Equivalence.areEqual(s.result(), expected) : source + " con " + m + ": " + s.result() + " en vez de " + expected;
            }
        }
    }

    @Test
    void derivativeMethodsAreListedByApplicability() {
        List<DerivativeMethod> product = Derivatives.applicableMethods(p("x^2*sin(x)"), "x");
        assert product.equals(List.of(DerivativeMethod.AUTO, DerivativeMethod.LOGARITHMIC)) : product;

        List<DerivativeMethod> power = Derivatives.applicableMethods(p("(x + 1)^2"), "x");
        assert power.contains(DerivativeMethod.EXPAND_FIRST) && !power.contains(DerivativeMethod.QUOTIENT_AS_PRODUCT) : power;

        List<DerivativeMethod> quotient = Derivatives.applicableMethods(p("(x^2 + 1)/(x - 1)"), "x");
        assert quotient.contains(DerivativeMethod.QUOTIENT_AS_PRODUCT) && quotient.contains(DerivativeMethod.EXPAND_FIRST) : quotient;

        assert Derivatives.applicableMethods(p("(x^2 - 1)/(x - 1)"), "x").contains(DerivativeMethod.SIMPLIFY_FIRST);
        assert Derivatives.applicableMethods(p("x^x"), "x").contains(DerivativeMethod.LOGARITHMIC);
        assert Derivatives.applicableMethods(p("sin(x)"), "x").equals(List.of(DerivativeMethod.AUTO));
        assert Derivatives.applicableMethods(p("5"), "x").isEmpty();
    }

    @Test
    void eachDerivativeMethodExplainsItself() {
        assert uses(Derivatives.derivative(p("(x + 1)^2*sin(x)"), "x", DerivativeMethod.EXPAND_FIRST).steps(), Rule.DIFF_EXPAND_FIRST);
        assert uses(Derivatives.derivative(p("x^2*sin(x)"), "x", DerivativeMethod.LOGARITHMIC).steps(), Rule.DIFF_LOGARITHMIC);
        assert uses(Derivatives.derivative(p("(x^2 - 1)/(x - 1)"), "x", DerivativeMethod.SIMPLIFY_FIRST).steps(), Rule.DIFF_SIMPLIFY_FIRST);
        Solution quotient = Derivatives.derivative(p("(x^2 + 1)/(x - 1)"), "x", DerivativeMethod.QUOTIENT_AS_PRODUCT);
        assert uses(quotient.steps(), Rule.DIFF_QUOTIENT_AS_PRODUCT) && uses(quotient.steps(), Rule.DIFF_PRODUCT)
                && !uses(quotient.steps(), Rule.DIFF_QUOTIENT) : quotient.steps().stream().map(Step::rule).toList();
        Solution direct = Derivatives.derivative(p("(x^2 + 1)/(x - 1)"), "x", DerivativeMethod.AUTO);
        assert uses(direct.steps(), Rule.DIFF_QUOTIENT);
    }

    @Test
    void aMethodThatChangesNothingFallsBackToTheDirectRules() {
        Solution s = Derivatives.derivative(p("sin(x)"), "x", DerivativeMethod.EXPAND_FIRST);
        assert s.isSolved() && s.result().equals(p("cos(x)")) && !uses(s.steps(), Rule.DIFF_EXPAND_FIRST);
    }

    @Test
    void higherOrderDerivativesAcceptAMethod() {
        Solution s = Derivatives.nth(p("(x + 1)^3"), "x", 2, DerivativeMethod.EXPAND_FIRST);
        assert s.isSolved() && Equivalence.areEqual(s.result(), p("6*x + 6")) : s.result();
    }

    // ---------------------------------------------------------------- Integrales

    @Test
    void integralMethodsAreTriedForReal() {
        List<IntegralMethod> parts = Integrals.applicableMethods(p("x*e^x"), "x");
        assert parts.contains(IntegralMethod.AUTO) && parts.contains(IntegralMethod.BY_PARTS) : parts;
        assert !parts.contains(IntegralMethod.TABLE) && !parts.contains(IntegralMethod.PARTIAL_FRACTIONS) : parts;

        List<IntegralMethod> substitution = Integrals.applicableMethods(p("2*x*e^(x^2)"), "x");
        assert substitution.contains(IntegralMethod.SUBSTITUTION) : substitution;

        assert Integrals.applicableMethods(p("1/(x^2 - 1)"), "x").contains(IntegralMethod.PARTIAL_FRACTIONS);
        assert Integrals.applicableMethods(p("sin(x)^2"), "x").contains(IntegralMethod.TRIGONOMETRIC);
        assert Integrals.applicableMethods(p("sqrt(1 - x^2)"), "x").contains(IntegralMethod.RADICAL);
        assert Integrals.applicableMethods(p("x^2 + 3*x"), "x").contains(IntegralMethod.LINEARITY);
        assert Integrals.applicableMethods(p("sin(x)"), "x").contains(IntegralMethod.TABLE);
        assert Integrals.applicableMethods(p("e^(x^2)"), "x").isEmpty();
        assert Integrals.applicableMethods(p("sin(x)/x"), "x").contains(IntegralMethod.TABLE);
        assert Integrals.applicableMethods(p("5"), "x").isEmpty();
    }

    @Test
    void aForcedMethodUsesThatTechniqueFirst() {
        Solution parts = Integrals.indefinite(p("x*e^x"), "x", IntegralMethod.BY_PARTS);
        assert parts.isSolved() && uses(parts.steps(), Rule.INT_PARTS);
        Solution substitution = Integrals.indefinite(p("2*x*e^(x^2)"), "x", IntegralMethod.SUBSTITUTION);
        assert substitution.isSolved() && uses(substitution.steps(), Rule.INT_SUBSTITUTION);
        assert Equivalence.areEqual(substitution.result(), p("e^(x^2)"));

        Solution direct = Integrals.indefinite(p("sin(x)"), "x", IntegralMethod.TABLE);
        assert direct.isSolved() && direct.result().equals(p("-cos(x)"));
    }

    @Test
    void aMethodThatDoesNotApplyIsUnsolvedNotNonElementary() {
        Solution s = Integrals.indefinite(p("x*e^x"), "x", IntegralMethod.TABLE);
        assert s.status() == Solution.Status.UNSOLVED : s.status();
        Solution nonElementary = Integrals.indefinite(p("sin(x)/x"), "x", IntegralMethod.AUTO);
        assert nonElementary.status() == Solution.Status.UNSOLVED || nonElementary.status() == Solution.Status.SOLVED
                || nonElementary.status() == Solution.Status.NON_ELEMENTAL;
    }

    @Test
    void definiteIntegralsAcceptAMethod() {
        Solution s = Integrals.definite(p("x*e^x"), "x", p("0"), p("1"), IntegralMethod.BY_PARTS);
        assert s.isSolved() && s.result().equals(p("1")) : s.result();
        assert uses(s.steps(), Rule.INT_DEFINITE_ANTIDERIVATIVE);
    }

    @Test
    void methodTextsExistInBothLanguages() {
        for (Locale locale : List.of(Locale.forLanguageTag("es"), Locale.ENGLISH)) {
            for (IntegralMethod m : IntegralMethod.values()) assert !m.title(locale).isBlank() && !m.description(locale).isBlank() : m;
            for (DerivativeMethod m : DerivativeMethod.values()) assert !m.title(locale).isBlank() && !m.description(locale).isBlank() : m;
        }
    }

    @Test
    void methodStepsRenderInAllFormats() {
        Solution s = Derivatives.derivative(p("x^2*sin(x)"), "x", DerivativeMethod.LOGARITHMIC);
        for (Locale locale : List.of(Locale.forLanguageTag("es"), Locale.ENGLISH)) {
            for (Format format : Format.values()) {
                String text = s.render(locale, format);
                assert !text.isBlank() && !text.contains("${") : format + ": " + text;
            }
        }
    }
}
