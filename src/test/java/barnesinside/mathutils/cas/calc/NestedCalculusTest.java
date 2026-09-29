package barnesinside.mathutils.cas.calc;

import barnesinside.mathutils.cas.calc.CalculatorSession.Result;
import barnesinside.mathutils.cas.calc.CalculatorSession.Status;
import barnesinside.mathutils.special.Special;
import org.junit.jupiter.api.Test;

class NestedCalculusTest {

    private static Result ok(CalculatorSession calc, String input) {
        Result r = calc.evaluate(input);
        assert r.status() == Status.OK : input + " -> " + r.status() + " " + r.message();
        return r;
    }

    @Test
    void definiteIntegralInsideAnExpression() {
        CalculatorSession calc = new CalculatorSession();
        assert ok(calc, "2 + int(x^2, x, 0, 1)").exactText().equals("7/3");
        assert ok(calc, "2 + ∫_0^1 x^2 dx").exactText().equals("7/3");
        assert ok(calc, "3 * ∫_(0)^(1) (x^2 + 1) dx").exactText().equals("4");
        assert ok(calc, "int(x, 0, 2) + int(x, 0, 2)").exactText().equals("4");
    }

    @Test
    void reciprocalOfTheGammaTypeIntegralWithAParameterSetToZero() {
        CalculatorSession calc = new CalculatorSession();
        calc.evaluate("z = 0");
        for (String input : new String[]{
                "1/int(exp(-x) x^(z) ln(x), x, 0, oo)",
                "1/(∫_(0)^(∞) (exp(-x) x^(z) ln(x)) dx)",
                "1/∫_0^∞ e^(-x) x^z ln(x) dx"}) {
            Result r = ok(calc, input);
            assert r.exactText().equals("-1/γ") : input + " -> " + r.exactText();
            assert Math.abs(r.approximate() + 1 / Special.EULER_GAMMA) < 1e-12;
            assert r.decimalText().startsWith("-1.73245471");
        }
    }

    @Test
    void parameterValuesAreUsedInsideTheCall() {
        CalculatorSession calc = new CalculatorSession();
        calc.evaluate("a = 3");
        assert ok(calc, "int(a*x^2, x, 0, 1)").exactText().equals("1");
    }

    @Test
    void numericValueWhenThereIsNoExactOne() {
        CalculatorSession calc = new CalculatorSession();
        Result r = ok(calc, "int(exp(-x) ln(x), x, 0, 2)");
        assert Math.abs(r.approximate() - (-0.719923445615)) < 1e-9 : r.approximate();
        assert r.decimalText().startsWith("-0.7199234456");
        assert r.exactText().equals(r.decimalText());
    }

    @Test
    void derivativeAndLimitInsideAnExpression() {
        CalculatorSession calc = new CalculatorSession();
        Result derivative = calc.evaluate("diff(x^3, x) + 1");
        assert derivative.status() == Status.SYMBOLIC && derivative.exactText().contains("3*x^2") : derivative;
        assert ok(calc, "limit(sin(x)/x, x, 0) + 1").exactText().equals("2");
        assert ok(calc, "limit(1/x, x, oo) + 5").exactText().equals("5");
    }

    @Test
    void divergentIntegralIsReportedAtTheCall() {
        CalculatorSession calc = new CalculatorSession();
        Result r = calc.evaluate("1 + int(1/x, x, 0, 1)");
        assert r.status() == Status.ERROR;
        assert r.message().contains("diverge") : r.message();
        assert r.errorPosition() == 4 : r.errorPosition();
    }

    @Test
    void parameterWithoutValueIsExplained() {
        CalculatorSession calc = new CalculatorSession();
        Result r = calc.evaluate("int(exp(-x^2) x^w, x, 0, 2) + 1");
        assert r.status() == Status.ERROR;
        assert r.message().contains("w") && r.message().contains("no tiene valor") : r.message();
    }

    @Test
    void missingDifferentialIsExplained() {
        CalculatorSession calc = new CalculatorSession();
        Result r = calc.evaluate("2 * ∫_0^1 x^2");
        assert r.status() == Status.ERROR && r.message().contains("diferencial") : r.message();
    }

    @Test
    void validationDoesNotSolveAnything() {
        CalculatorSession calc = new CalculatorSession();
        assert calc.validate("1/int(exp(-x) x^z ln(x), x, 0, oo)").valid();
        assert calc.validate("1/∫_0^∞ e^(-x) x^z ln(x) dx").valid();
        assert !calc.validate("1/int(exp(-x) x^z ln(x), x, 0, oo").valid();
    }

    @Test
    void ordinaryExpressionsAreUntouched() {
        CalculatorSession calc = new CalculatorSession();
        assert ok(calc, "2 + 3*4").exactText().equals("14");
        assert ok(calc, "ln(10) + sin(0)").exactText().equals("ln(10)");
    }
}
