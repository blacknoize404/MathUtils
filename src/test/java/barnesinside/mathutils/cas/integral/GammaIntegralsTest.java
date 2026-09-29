package barnesinside.mathutils.cas.integral;

import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.limit.Limits;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.special.Special;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static barnesinside.mathutils.cas.integral.IntegralChecks.uses;

class GammaIntegralsTest {

    private static double value(Solution s) {
        assert s.isSolved() : s.status() + "\n" + s;
        return Eval.value(s.result());
    }

    @Test
    void polynomialTimesExponentialToInfinity() {
        assert Math.abs(value(Integrals.definite("x^2*exp(-x)", "x", "0", "oo")) - 2) < 1e-12;
        assert Math.abs(value(Integrals.definite("x^3*exp(-2*x)", "x", "0", "oo")) - 6.0 / 16) < 1e-12;
    }

    @Test
    void symbolicExponentGivesTheFactorial() {
        Solution s = Integrals.definite("exp(-x)*x^z", "x", "0", "oo");
        assert s.isSolved() && s.result().equals(ExprParser.parse("z!")) : s.result();
        assert uses(s, Rule.INT_GAMMA);
        assert Math.abs(Eval.value(s.result(), Map.of("z", 2.5)) - Special.gamma(3.5)) < 1e-12;
    }

    @Test
    void logarithmBringsTheDigamma() {
        Solution s = Integrals.definite("exp(-x)*x^z*ln(x)", "x", "0", "oo");
        assert s.isSolved() && uses(s, Rule.INT_GAMMA_LOG) : s.status();
        double z = 0.7;
        double expected = Special.gamma(z + 1) * Special.digamma(z + 1);
        assert Math.abs(Eval.value(s.result(), Map.of("z", z)) - expected) < 1e-12;
    }

    @Test
    void integralOfLogTimesExponentialIsMinusEulerGamma() {
        Solution s = Integrals.definite("exp(-x)*ln(x)", "x", "0", "oo");
        assert s.isSolved();
        assert s.result().equals(Expr.neg(Expr.EULER_GAMMA)) : s.result();
        assert Math.abs(value(s) + Special.EULER_GAMMA) < 1e-15;
    }

    @Test
    void rateOtherThanOne() {
        Solution s = Integrals.definite("exp(-2*x)*x^3*ln(x)", "x", "0", "oo");
        double expected = 6.0 / 16 * (Special.digamma(4) - Math.log(2));
        assert Math.abs(value(s) - expected) < 1e-12;
    }

    @Test
    void stepsAreWrittenInBothLanguages() {
        Solution s = Integrals.definite("exp(-x)*x^z*ln(x)", "x", "0", "oo");
        String es = s.render(Locale.of("es"), Format.PLAIN);
        String en = s.render(Locale.ENGLISH, Format.PLAIN);
        assert es.contains("digamma") && en.contains("digamma");
        assert !es.equals(en);
    }

    @Test
    void theReciprocalLimitIsMinusOneOverGamma() {
        Solution s = Limits.limit("1/int(exp(-x) x^z ln(x), x, 0, oo)", "z", "0");
        assert s.isSolved() : s.status();
        assert s.result().equals(ExprParser.parse("-1/γ")) : s.result();
        assert Math.abs(Eval.value(s.result()) + 1 / Special.EULER_GAMMA) < 1e-12;
    }

    @Test
    void theReciprocalLimitShowsTheIntegralTheSpecialValuesAndTheSubstitution() {
        Solution s = Limits.limit("1/int(exp(-x) x^z ln(x), x, 0, oo)", "z", "0");
        java.util.List<Rule> top = s.steps().stream().map(step -> step.rule()).toList();
        assert top.equals(java.util.List.of(Rule.NESTED_INTEGRAL, Rule.LIMIT_SPECIAL_VALUE, Rule.LIMIT_SPECIAL_VALUE, Rule.LIMIT_DIRECT)) : top;
        assert uses(s, Rule.INT_GAMMA_LOG) : "la integral lleva su resolución dentro";
        String es = s.render(Locale.of("es"), Format.PLAIN);
        String en = s.render(Locale.ENGLISH, Format.PLAIN);
        assert es.contains("0! = 1") && es.contains("digamma(1) = -γ") : es;
        assert en.contains("Solve the inner integral first") : en;
    }

    @Test
    void symbolicLimitsOfIntegrationAreSolvedWithTheAntiderivative() {
        Solution s = Integrals.definite("3*x^2", "x", "0", "b");
        assert s.isSolved() && s.result().equals(ExprParser.parse("b^3")) : s.status() + " " + s.result();
        Solution reversed = Integrals.definite("2*x", "x", "a", "3");
        assert reversed.isSolved() && Math.abs(Eval.value(reversed.result(), Map.of("a", 1.0)) - 8) < 1e-12;
    }

    @Test
    void eulerGammaConstantRoundTrips() {
        assert ExprParser.parse("γ").equals(Expr.EULER_GAMMA);
        assert ExprParser.parse(Expr.EULER_GAMMA.toString()).equals(Expr.EULER_GAMMA);
        assert Expr.EULER_GAMMA.toLatex().equals("\\gamma");
        assert Math.abs(Eval.value(ExprParser.parse("2γ")) - 2 * Special.EULER_GAMMA) < 1e-15;
    }

    @Test
    void digammaOfSmallArgumentsIsExact() {
        assert ExprParser.parse("digamma(1)").equals(ExprParser.parse("-γ"));
        assert ExprParser.parse("digamma(3)").equals(ExprParser.parse("3/2 - γ"));
        assert ExprParser.parse("digamma(1/2)").equals(ExprParser.parse("-γ - 2*ln(2)"));
    }
}
