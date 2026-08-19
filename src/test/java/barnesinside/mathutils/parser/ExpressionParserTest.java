package barnesinside.mathutils.parser;

import barnesinside.mathutils.algebra.Evaluation;
import barnesinside.mathutils.algebra.Function;
import barnesinside.mathutils.algebra.primitives.MathElement;
import barnesinside.mathutils.exceptions.ExpressionParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

class ExpressionParserTest {

    private static final double EPS = 1e-10;

    private static double eval(String source, Evaluation... evaluations) throws Exception {
        return ExpressionParser.parse(source).eval(evaluations);
    }

    private static void assertEval(String source, double expected) throws Exception {
        double actual = eval(source);
        assert Math.abs(actual - expected) < EPS : source + " -> expected " + expected + " but got " + actual;
    }

    private static void assertFails(String source, int expectedPosition) {
        try {
            ExpressionParser.parse(source);
            assert false : "Should have failed: " + source;
        } catch (ExpressionParseException e) {
            assert e.getPosition() == expectedPosition
                    : source + " -> expected position " + expectedPosition + " but was " + e.getPosition();
        }
    }

    @Test
    void tokenizes() {
        List<Token> tokens = ExpressionParser.tokenize("3.5 + x1*(2)");
        assert tokens.size() == 7;
        assert tokens.get(0).type() == Token.Type.NUMBER && tokens.get(0).text().equals("3.5");
        assert tokens.get(2).type() == Token.Type.IDENTIFIER && tokens.get(2).text().equals("x1");
        assert tokens.get(2).position() == 6;
    }

    @Test
    void precedenceAndAssociativity() throws Exception {
        assertEval("1 + 2 * 3", 7);
        assertEval("(1 + 2) * 3", 9);
        assertEval("10 - 4 - 3", 3);
        assertEval("100 / 10 / 5", 2);
        assertEval("2 ^ 3 ^ 2", 512);
    }

    @Test
    void unaryMinus() throws Exception {
        assertEval("-3", -3);
        assertEval("-2 ^ 2", -4);
        assertEval("2 ^ -1", 0.5);
        assertEval("3 - -2", 5);
        assertEval("-(1 + 2) * 2", -6);
        assertEval("+5", 5);
    }

    @Test
    void numbersAndConstants() throws Exception {
        assertEval("2.5e2", 250);
        assertEval(".5 + .5", 1);
        assertEval("pi", Math.PI);
        assertEval("2 * e", 2 * Math.E);
    }

    @Test
    void variables() throws Exception {
        double result = eval("x^2 + 3*x + 1", new Evaluation("x", 2));
        assert Math.abs(result - 11) < EPS;
        double multi = eval("a*b - c", new Evaluation("a", 3), new Evaluation("b", 4), new Evaluation("c", 5));
        assert Math.abs(multi - 7) < EPS;
    }

    @Test
    void functions() throws Exception {
        assertEval("sqrt(16)", 4);
        assertEval("cbrt(27)", 3);
        assertEval("sin(0)", 0);
        assertEval("cos(0)", 1);
        assertEval("tan(0)", 0);
        assertEval("ln(e)", 1);
        assertEval("exp(0)", 1);
        assertEval("log(1000)", 3);
        assertEval("log(8, 2)", 3);
        assertEval("root(32, 5)", 2);
        assertEval("pow(2, 10)", 1024);
        assertEval("sqrt(1 + 2*4)", 3);
        assertEval("cot(pi / 4)", 1);
    }

    @Test
    void functionsAreCaseInsensitive() throws Exception {
        assertEval("SQRT(9) + Sin(0)", 3);
    }

    @Test
    void parseFunctionWrapsResult() throws Exception {
        Function f = ExpressionParser.parseFunction("2*x");
        assert f.eval(new Evaluation("x", 4)) == 8;
    }

    @Test
    void postfix() {
        assert ExpressionParser.toPostfix("1 + 2 * 3").equals(List.of("1", "2", "3", "*", "+"));
        assert ExpressionParser.toPostfix("-x").equals(List.of("x", "neg"));
        assert ExpressionParser.toPostfix("sin(x)").equals(List.of("x", "sin/1"));
        assert ExpressionParser.toPostfix("log(x, 2)").equals(List.of("x", "2", "log/2"));
    }

    @Test
    void roundTripsThroughToString() throws Exception {
        MathElement parsed = ExpressionParser.parse("x * (y + 2)");
        MathElement reparsed = ExpressionParser.parse(parsed.toString());
        Evaluation[] values = {new Evaluation("x", 3), new Evaluation("y", 4)};
        assert parsed.eval(values) == reparsed.eval(values);
    }

    @Test
    void reportsErrorPositions() {
        assertFails("", 0);
        assertFails("1 +", 3);
        assertFails("1 + * 2", 4);
        assertFails("(1 + 2", 0);
        assertFails("1 + 2)", 5);
        assertFails("2 3", 2);
        assertFails("2 $ 3", 2);
        assertFails("()", 1);
        assertFails("2(3)", 1);
        assertFails("foo(1)", 0);
        assertFails("sin(1, 2)", 0);
        assertFails("sin()", 0);
        assertFails("1, 2", 1);
        assertFails("sqrt(,)", 5);
    }

    @Test
    void reportsLineAndColumn() {
        try {
            ExpressionParser.parse("1 +\n  * 2");
            assert false;
        } catch (ExpressionParseException e) {
            assert e.getLine() == 2 && e.getColumn() == 3 : e.getLine() + ":" + e.getColumn();
        }
    }
}
