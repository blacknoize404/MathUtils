package barnesinside.mathutils.cas.calc;

import barnesinside.mathutils.cas.calc.CalculatorSession.Result;
import barnesinside.mathutils.cas.calc.CalculatorSession.Status;
import barnesinside.mathutils.cas.expr.AngleUnit;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import org.junit.jupiter.api.Test;

class CalculatorSessionTest {

    private static Result eval(CalculatorSession calc, String input) {
        return calc.evaluate(input);
    }

    private static void assertValue(CalculatorSession calc, String input, String exact, String decimal) {
        Result r = calc.evaluate(input);
        assert r.status() == Status.OK : input + " -> " + r;
        assert exact.equals(r.exactText()) : input + " -> exacto esperado " + exact + " pero fue " + r.exactText();
        assert decimal.equals(r.decimalText()) : input + " -> decimal esperado " + decimal + " pero fue " + r.decimalText();
    }

    // ------------------------------------------------------------ Aritmética

    @Test
    void exactAndDecimalResults() {
        CalculatorSession calc = new CalculatorSession();
        assertValue(calc, "2 + 3*4", "14", "14");
        assertValue(calc, "1/3 + 1/6", "1/2", "0.5");
        assertValue(calc, "1/3", "1/3", "0.333333333333");
        assertValue(calc, "2^10", "1024", "1024");
        assertValue(calc, "2^(-2)", "1/4", "0.25");
        assertValue(calc, "sqrt(2)", "sqrt(2)", "1.41421356237");
        assertValue(calc, "sqrt(12)", "2*sqrt(3)", "3.46410161514");
        assertValue(calc, "2pi", "2*pi", "6.28318530718");
        assertValue(calc, "e", "e", "2.71828182846");
        assertValue(calc, "0.1 + 0.2", "3/10", "0.3");
        assertValue(calc, "(1+2)(3+4)", "21", "21");
        assertValue(calc, "100!".replace("!", "") , "100", "100");
    }

    @Test
    void exactTrigonometry() {
        CalculatorSession calc = new CalculatorSession();
        assertValue(calc, "sin(pi/6)", "1/2", "0.5");
        assertValue(calc, "cos(pi)", "-1", "-1");
        assertValue(calc, "sin(pi/4)", "sqrt(2)/2", "0.707106781187");
        assertValue(calc, "tan(pi/4)", "1", "1");
    }

    @Test
    void largeAndSmallNumbers() {
        CalculatorSession calc = new CalculatorSession();
        assertValue(calc, "2^100", "1267650600228229401496703205376", "1.26765060023E+30".replace("E+", "E"));
        Result tiny = calc.evaluate("1/2^40");
        assert tiny.status() == Status.OK && tiny.decimalText().startsWith("9.09494701773E-13") : tiny.decimalText();
    }

    @Test
    void precisionIsConfigurable() {
        CalculatorSession calc = new CalculatorSession();
        calc.setPrecision(5);
        assert "0.33333".equals(calc.evaluate("1/3").decimalText());
        calc.setPrecision(20);
        assert "0.33333333333333333333".equals(calc.evaluate("1/3").decimalText());
        assert calc.precision() == 20;
        try {
            calc.setPrecision(0);
            assert false;
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void irrationalDecimalsAreCappedAtFifteenDigits() {
        CalculatorSession calc = new CalculatorSession();
        calc.setPrecision(30);
        assert "1.41421356237310".equals(calc.evaluate("sqrt(2)").decimalText())
                || "1.4142135623731".equals(calc.evaluate("sqrt(2)").decimalText());
    }

    @Test
    void errorMessagesDoNotRepeatTheCoordinates() {
        CalculatorSession calc = new CalculatorSession();
        Result r = calc.evaluate("2 + * 3");
        assert !r.message().contains("línea") : r.message();
    }

    @Test
    void latexOutput() {
        CalculatorSession calc = new CalculatorSession();
        assert "\\frac{1}{2}".equals(calc.evaluate("1/2").latex());
        assert "2 \\sqrt{3}".equals(calc.evaluate("sqrt(12)").latex());
    }

    // ------------------------------------------------------------ Variables y funciones

    @Test
    void variablesPersist() {
        CalculatorSession calc = new CalculatorSession();
        Result definition = calc.evaluate("x = 4");
        assert definition.status() == Status.DEFINED && "x = 4".equals(definition.message());
        assertValue(calc, "x^2 + 1", "17", "17");
        assertValue(calc, "2x", "8", "8");

        calc.evaluate("y = x + 1");
        assertValue(calc, "x*y", "20", "20");
        calc.evaluate("x = 10");
        assertValue(calc, "x*y", "50", "50"); // y se evaluó al definirla
        assert calc.variables().containsKey("x") && calc.variables().containsKey("y");
    }

    @Test
    void symbolicResultsWhenVariablesAreUndefined() {
        CalculatorSession calc = new CalculatorSession();
        Result r = calc.evaluate("x^2 + 2x + x");
        assert r.status() == Status.SYMBOLIC;
        assert r.exact().equals(ExprParser.parse("x^2 + 3x"));
        assert r.approximate() == null && r.decimalText() == null;
    }

    @Test
    void ansHoldsTheLastResult() {
        CalculatorSession calc = new CalculatorSession();
        calc.evaluate("6*7");
        assertValue(calc, "ans + 8", "50", "50");
        assertValue(calc, "ans/2", "25", "25");
        calc.evaluate("zzz +");
        assertValue(calc, "ans", "25", "25");
    }

    @Test
    void userFunctions() {
        CalculatorSession calc = new CalculatorSession();
        Result definition = calc.evaluate("f(t) = t^2 + 1");
        assert definition.status() == Status.DEFINED && definition.message().equals("f(t) = t^2 + 1");
        assertValue(calc, "f(3)", "10", "10");
        assertValue(calc, "f(f(2))", "26", "26");
        assertValue(calc, "2f(1)", "4", "4");

        calc.evaluate("g(a, b) = a*b - 1");
        assertValue(calc, "g(3, 4)", "11", "11");
        assert calc.functions().containsKey("g");

        Result symbolic = calc.evaluate("f(x)");
        assert symbolic.status() == Status.SYMBOLIC && symbolic.exact().equals(ExprParser.parse("x^2 + 1"));
    }

    @Test
    void userFunctionsSeeVariablesAtCallTime() {
        CalculatorSession calc = new CalculatorSession();
        calc.evaluate("h(x) = x + k");
        calc.evaluate("k = 100");
        assertValue(calc, "h(1)", "101", "101");
    }

    @Test
    void userFunctionArityIsChecked() {
        CalculatorSession calc = new CalculatorSession();
        calc.evaluate("f(x) = x");
        Result r = calc.evaluate("f(1, 2)");
        assert r.isError() && r.message().contains("1 argumento");
    }

    @Test
    void duplicateParametersAreRejected() {
        CalculatorSession calc = new CalculatorSession();
        assert calc.evaluate("f(a, a) = a").isError();
    }

    @Test
    void reservedNamesAreRejected() {
        CalculatorSession calc = new CalculatorSession();
        for (String bad : new String[]{"pi = 3", "e = 2", "sin = 1", "ans = 5", "sqrt(x) = x", "SIN(x) = x", "log = 2"}) {
            Result r = calc.evaluate(bad);
            assert r.isError() && r.message().contains("reservado") : bad + " -> " + r;
        }
    }

    @Test
    void removalAndReset() {
        CalculatorSession calc = new CalculatorSession();
        calc.evaluate("a = 5");
        calc.evaluate("f(x) = x");
        calc.removeVariable("a");
        calc.removeFunction("f");
        assert !calc.variables().containsKey("a") && calc.functions().isEmpty();

        calc.evaluate("b = 2");
        calc.setAngleUnit(AngleUnit.DEGREES);
        calc.setPrecision(8);
        calc.reset();
        assert calc.variables().isEmpty() && calc.history().isEmpty();
        assert calc.angleUnit() == AngleUnit.DEGREES && calc.precision() == 8;
    }

    // ------------------------------------------------------------ Ángulos

    @Test
    void degreesAndGradians() {
        CalculatorSession calc = new CalculatorSession();
        calc.setAngleUnit(AngleUnit.DEGREES);
        assertValue(calc, "sin(30)", "1/2", "0.5");
        assertValue(calc, "cos(60)", "1/2", "0.5");
        assertValue(calc, "tan(45)", "1", "1");
        assertValue(calc, "asin(1/2)", "30", "30");
        assertValue(calc, "atan(1)", "45", "45");
        assertValue(calc, "cos(180)", "-1", "-1");

        calc.setAngleUnit(AngleUnit.GRADIANS);
        assertValue(calc, "sin(100)", "1", "1");
        assertValue(calc, "atan(1)", "50", "50");

        calc.setAngleUnit(AngleUnit.RADIANS);
        assertValue(calc, "sin(pi/2)", "1", "1");
    }

    @Test
    void functionsKeepTheAngleUnitTheyWereDefinedWith() {
        CalculatorSession calc = new CalculatorSession();
        calc.setAngleUnit(AngleUnit.DEGREES);
        calc.evaluate("s(a) = sin(a)");
        calc.setAngleUnit(AngleUnit.RADIANS);
        assertValue(calc, "s(30)", "1/2", "0.5");
    }

    // ------------------------------------------------------------ Errores

    @Test
    void syntaxErrorsReportPositions() {
        CalculatorSession calc = new CalculatorSession();
        Result r = calc.evaluate("2 + * 3");
        assert r.isError() && r.errorPosition() == 4 : r;
        assert r.exact() == null && r.decimalText() == null;

        Result unmatched = calc.evaluate("(1 + 2");
        assert unmatched.isError() && unmatched.errorPosition() == 0;

        Result late = calc.evaluate("x = 2 +");
        assert late.isError() && late.errorPosition() == 7 : late;
    }

    @Test
    void errorPositionInFunctionDefinitionsIsRelativeToTheWholeInput() {
        CalculatorSession calc = new CalculatorSession();
        Result r = calc.evaluate("f(x) = x + * 2");
        assert r.isError() && r.errorPosition() == 11 : r;
    }

    @Test
    void divisionByZeroAndUndefinedResults() {
        CalculatorSession calc = new CalculatorSession();
        assert calc.evaluate("1/0").isError();
        assert "No se puede dividir entre 0.".equals(calc.evaluate("1/0").message());
        assert calc.evaluate("ln(0)").isError();
        assert calc.evaluate("sqrt(-4)").isError();
        assert calc.evaluate("x = 1/0").isError();
        assert !calc.variables().containsKey("x");
    }

    @Test
    void emptyInputIsAnError() {
        CalculatorSession calc = new CalculatorSession();
        assert calc.evaluate("").isError();
        assert calc.evaluate("   ").isError();
        assert calc.evaluate(null).isError();
    }

    @Test
    void failedEvaluationDoesNotChangeAns() {
        CalculatorSession calc = new CalculatorSession();
        calc.evaluate("5");
        calc.evaluate("1/0");
        assertValue(calc, "ans", "5", "5");
    }

    // ------------------------------------------------------------ Validación, historial, memoria

    @Test
    void validationGivesFeedbackWithoutSideEffects() {
        CalculatorSession calc = new CalculatorSession();
        assert calc.validate("2 + 3").valid();
        assert calc.validate("").valid();
        CalculatorSession.Validation bad = calc.validate("2 + * 3");
        assert !bad.valid() && bad.position() == 4;
        assert calc.validate("x = 2 +").position() == 7;
        assert calc.validate("f(x) = x^").position() == 9;
        assert calc.history().isEmpty() && calc.variables().isEmpty();
    }

    @Test
    void historyRecordsEveryEvaluationInOrder() {
        CalculatorSession calc = new CalculatorSession();
        calc.evaluate("1 + 1");
        calc.evaluate("oops +");
        calc.evaluate("x = 3");
        assert calc.history().size() == 3;
        assert calc.history().get(0).exactText().equals("2");
        assert calc.history().get(1).isError();
        assert calc.history().get(2).status() == Status.DEFINED;
    }

    @Test
    void historyIsBounded() {
        CalculatorSession calc = new CalculatorSession();
        for (int i = 0; i < 600; i++) calc.evaluate(String.valueOf(i));
        assert calc.history().size() == 500;
        assert calc.history().get(499).exactText().equals("599");
    }

    @Test
    void memory() {
        CalculatorSession calc = new CalculatorSession();
        calc.memoryAdd(5);
        calc.memoryAdd(2.5);
        calc.memorySubtract(1);
        assert calc.memory() == 6.5;
        calc.memoryClear();
        assert calc.memory() == 0;
    }

    @Test
    void symbolicValuesCanBeAssigned() {
        CalculatorSession calc = new CalculatorSession();
        Result r = calc.evaluate("p = x + 1");
        assert r.status() == Status.DEFINED && r.approximate() == null;
        assert calc.variables().get("p").equals(ExprParser.parse("x + 1"));
        Result usage = calc.evaluate("p*2");
        assert barnesinside.mathutils.cas.expr.Equivalence.areEqual(usage.exact(), ExprParser.parse("2x + 2")) : usage.exact();
    }

    @Test
    void exactResultsAreExprValues() {
        CalculatorSession calc = new CalculatorSession();
        Expr exact = calc.evaluate("sqrt(8)").exact();
        assert exact.equals(ExprParser.parse("2*sqrt(2)"));
    }
}
