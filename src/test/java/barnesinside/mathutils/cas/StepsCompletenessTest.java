package barnesinside.mathutils.cas;

import barnesinside.mathutils.cas.diff.Derivatives;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.integral.Integrals;
import barnesinside.mathutils.cas.integral.MultipleIntegrals;
import barnesinside.mathutils.cas.limit.Limits;
import barnesinside.mathutils.cas.limit.MultiLimits;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Los procedimientos no se saltan pasos: lo que antes salía de golpe lleva ahora su explicación. */
class StepsCompletenessTest {

    private static List<Rule> rules(List<Step> steps) {
        List<Rule> all = new ArrayList<>();
        for (Step s : steps) {
            all.add(s.rule());
            all.addAll(rules(s.substeps()));
        }
        return all;
    }

    private static void uses(Solution s, Rule... expected) {
        List<Rule> all = rules(s.steps());
        for (Rule r : expected) assert all.contains(r) : r + " falta en " + all;
    }

    private static Solution limit(String f, String point) {
        return Limits.limit(f, "x", point);
    }

    @Test
    void theClassicZeroTimesInfinityAndItsPowerFormAreSolved() {
        Solution s = limit("x*ln(x)", "0");
        assert s.isSolved() && s.result().isZero() : s.status();
        uses(s, Rule.LIMIT_DOMAIN_ONE_SIDE, Rule.LIMIT_LHOPITAL_INFINITY);
        Solution power = limit("x^x", "0");
        assert power.isSolved() && power.result().isOne() : power.status() + " " + power.result();
    }

    @Test
    void quotientsOfExponentialsAndBoundedTermsAreSolvedWithTheirSteps() {
        Solution bases = limit("2^x/3^x", "oo");
        assert bases.isSolved() && bases.result().isZero();
        uses(bases, Rule.LIMIT_COMMON_BASE);
        Solution bounded = limit("(x+sin(x))/(x-sin(x))", "oo");
        assert bounded.isSolved() && bounded.result().isOne() : bounded.status();
        uses(bounded, Rule.LIMIT_DIVIDE_DOMINANT);
    }

    @Test
    void anOscillatingLimitSaysSo() {
        Solution s = limit("sin(1/x)", "0");
        assert s.status() == Solution.Status.DOES_NOT_EXIST : s.status();
        uses(s, Rule.LIMIT_OSCILLATES);
    }

    @Test
    void knownValuesAtInfinityAreExplained() {
        uses(limit("atan(x)", "oo"), Rule.LIMIT_KNOWN_VALUE);
        uses(limit("exp(-x)", "oo"), Rule.LIMIT_KNOWN_VALUE);
    }

    @Test
    void theConjugateShowsItsNumeratorAndTheLeadingTerms() {
        Solution s = limit("sqrt(x^2+x)-x", "oo");
        assert s.isSolved() && Math.abs(Eval.value(s.result()) - 0.5) < 1e-12;
        uses(s, Rule.LIMIT_CONJUGATE, Rule.LIMIT_CONJUGATE_NUMERATOR, Rule.LIMIT_LEADING_TERM, Rule.LIMIT_DOMINANT_TERMS);
    }

    @Test
    void derivativesShowTheSimplificationOfWhatTheRuleGives() {
        uses(Derivatives.derivative("x*exp(-x)", "x"), Rule.DIFF_PRODUCT, Rule.SIMPLIFY);
        uses(Derivatives.derivative("sqrt(1+x^2)", "x"), Rule.DIFF_CHAIN, Rule.SIMPLIFY);
        // Lo que ya sale como lo da la regla no lleva un paso de más
        assert !rules(Derivatives.derivative("x^2", "x").steps()).contains(Rule.SIMPLIFY);
    }

    @Test
    void tableFormsThatAreNotOneLineCarryTheirDerivation() {
        uses(Integrals.indefinite("1/(x^2-1)", "x"), Rule.INT_LOG_FORM, Rule.INT_FORM_PARTIAL_FRACTIONS, Rule.INT_FORM_COMBINE_LOGS);
        uses(Integrals.indefinite("sqrt(1-x^2)", "x"), Rule.INT_FORM_TRIG_SUBSTITUTION, Rule.INT_FORM_COS_SQUARED, Rule.INT_FORM_BACK_SUBSTITUTION);
        uses(Integrals.indefinite("1/sqrt(x^2+1)", "x"), Rule.INT_FORM_HYPERBOLIC_SUBSTITUTION, Rule.INT_FORM_HYPERBOLIC_BACK);
        uses(Integrals.indefinite("sqrt(x^2+4)", "x"), Rule.INT_FORM_HYPERBOLIC_SQUARED);
        for (String f : new String[]{"1/(x^2-1)", "sqrt(1-x^2)", "1/sqrt(x^2+1)"}) {
            Solution s = Integrals.indefinite(f, "x");
            assert s.isSolved() && Integrals.indefinite(f, "x").render(Locale.ENGLISH, Format.PLAIN).length() > 100;
        }
    }

    @Test
    void absoluteValuesAreSplitWhereTheyChangeSign() {
        Solution s = Integrals.definite("abs(x)", "x", "-1", "2");
        assert s.isSolved() && Math.abs(Eval.value(s.result()) - 2.5) < 1e-12 : s.status();
        uses(s, Rule.INT_SPLIT_ABSOLUTE, Rule.INT_ABSOLUTE_PIECE, Rule.INT_ADD_PIECES);
        Solution quadratic = Integrals.definite("abs(x^2-1)", "x", "0", "2");
        assert quadratic.isSolved() && Math.abs(Eval.value(quadratic.result()) - 2) < 1e-12 : quadratic.status();
        Solution infinite = Integrals.definite("abs(x)*exp(-abs(x))", "x", "-oo", "oo");
        assert infinite.isSolved() && Math.abs(Eval.value(infinite.result()) - 2) < 1e-9 : infinite.status();
        Solution plain = Integrals.definite("abs(x)", "x", "1", "3");
        assert plain.isSolved() && Math.abs(Eval.value(plain.result()) - 4) < 1e-12;
    }

    @Test
    void theGammaIntegralsCarryTheirDerivation() {
        uses(Integrals.definite("exp(-2*x)*x^z*ln(x)", "x", "0", "oo"), Rule.INT_GAMMA_LOG, Rule.INT_GAMMA_SCALE, Rule.INT_GAMMA_PARAMETER, Rule.INT_GAMMA_DIGAMMA);
        uses(Integrals.definite("exp(-x)*x^z", "x", "0", "oo"), Rule.INT_GAMMA, Rule.INT_GAMMA_DEFINITION);
    }

    private static Solution multi(String f, String... point) {
        return MultiLimits.limit(f, List.of("x", "y"), List.of(point));
    }

    @Test
    void aPathWhereTheFunctionDoesNotExistIsNotACounterexample() {
        Solution s = multi("(x^2-y^2)/(x-y)", "1", "1");
        assert s.isSolved() && Math.abs(Eval.value(s.result()) - 2) < 1e-12 : s.status() + " " + s.result();
        uses(s, Rule.LIMIT_MULTI_CANCEL);
    }

    @Test
    void multivariableLimitsWithBoundedFactorsInfinitesimalsAndChangesOfVariable() {
        uses(multi("x*sin(1/y)", "0", "0"), Rule.LIMIT_MULTI_SQUEEZE);
        uses(multi("sin(x*y)/y", "0", "0"), Rule.LIMIT_MULTI_INFINITESIMAL);
        Solution infinite = multi("1/(x^2+y^2)", "0", "0");
        assert infinite.isSolved() && infinite.result().toString().equals("oo") : infinite.result();
        uses(infinite, Rule.LIMIT_MULTI_NEW_VARIABLE);
        Solution cosine = multi("(1-cos(x*y))/(x*y)", "0", "0");
        assert cosine.isSolved() && cosine.result().isZero() : cosine.status();
        Solution root = multi("(x*y)/sqrt(x^2+y^2)", "0", "0");
        assert root.isSolved() && root.result().isZero() : root.status();
        uses(root, Rule.LIMIT_MULTI_POLAR_ZERO);
        // Los que no tienen límite siguen sin tenerlo
        assert multi("x*y/(x^2+y^2)", "0", "0").status() == Solution.Status.DOES_NOT_EXIST;
        assert multi("(x^2*y^2)/(x^4+y^4)", "0", "0").status() == Solution.Status.DOES_NOT_EXIST;
    }

    @Test
    void improperIteratedIntegralsAreSolved() {
        Solution plain = MultipleIntegrals.iterated("exp(-x-y)", List.of(new MultipleIntegrals.Dimension("x", "0", "oo"), new MultipleIntegrals.Dimension("y", "0", "oo")));
        assert plain.isSolved() && plain.result().isOne() : plain.status();
        Solution gauss = MultipleIntegrals.iterated("exp(-x^2-y^2)", List.of(new MultipleIntegrals.Dimension("x", "-oo", "oo"), new MultipleIntegrals.Dimension("y", "-oo", "oo")));
        assert gauss.isSolved() && Math.abs(Eval.value(gauss.result()) - Math.PI) < 1e-12 : gauss.status();
        uses(gauss, Rule.INT_SPLIT_EXPONENT);
    }

    @Test
    void integralResultsAreLeftInTheirShortestFormWithAStepThatSaysSo() {
        Solution s = Integrals.indefinite("x^2*exp(-x)", "x");
        assert barnesinside.mathutils.cas.expr.Equivalence.areEqual(s.result(), barnesinside.mathutils.cas.expr.ExprParser.parse("-e^(-x)*(x^2+2*x+2)")) : s.result();
        assert s.result().toString().length() < "-(x^2*e^(-x) - 2*(-x*e^(-x) - e^(-x)))".length() : "más corta que la que da el método: " + s.result();
        uses(s, Rule.SIMPLIFY);
        Solution parts = Integrals.indefinite("x*exp(x)", "x");
        assert parts.result().toString().length() <= "x*e^x - e^x".length();
        // Lo que ya está en su forma más corta no lleva un paso de más
        assert !rules(Integrals.indefinite("x^2", "x").steps()).contains(Rule.SIMPLIFY);
        Solution mixed = barnesinside.mathutils.cas.diff.MultiDerivatives.mixed("exp(x*y)/x", List.of("x", "y"));
        assert mixed.result().equals(barnesinside.mathutils.cas.expr.ExprParser.parse("y*e^(x*y)")) : mixed.result();
    }
}
