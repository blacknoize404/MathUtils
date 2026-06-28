package MathUtils.algebra;

import MathUtils.algebra.primitives.MathElement;
import MathUtils.parser.ExpressionParser;
import org.junit.jupiter.api.Test;

class SimplifierTest {

    private static String simplified(String source) {
        return Simplifier.simplify(ExpressionParser.parse(source)).toString();
    }

    @Test
    void identities() {
        assert "x".equals(simplified("x + 0"));
        assert "x".equals(simplified("0 + x"));
        assert "x".equals(simplified("x - 0"));
        assert "x".equals(simplified("x * 1"));
        assert "x".equals(simplified("1 * x"));
        assert "x".equals(simplified("x / 1"));
        assert "x".equals(simplified("x ^ 1"));
        assert "1".equals(simplified("x ^ 0"));
    }

    @Test
    void zeros() {
        assert "0".equals(simplified("x * 0"));
        assert "0".equals(simplified("0 * (x + y)"));
        assert "0".equals(simplified("0 / x"));
    }

    @Test
    void constantFolding() {
        assert "5".equals(simplified("2 + 3"));
        assert "6".equals(simplified("2 * 3"));
        assert "8".equals(simplified("2 ^ 3"));
        assert "2".equals(simplified("10 / 5"));
        assert "-1".equals(simplified("2 - 3"));
    }

    @Test
    void negation() {
        assert "(-1 * x)".equals(simplified("0 - x"));
        assert "(-1 * x)".equals(simplified("-x"));
    }

    @Test
    void flattensNestedOperators() {
        assert "(x + 6)".equals(simplified("(x + 1) + 5"));
        assert "(6 * x)".equals(simplified("2 * (3 * x)"));
    }

    @Test
    void simplifiesInsideFunctions() {
        assert "sin(x)".equals(simplified("sin(x + 0)"));
    }

    @Test
    void doesNotMutateOriginal() throws Exception {
        MathElement original = ExpressionParser.parse("x + 0");
        String before = original.toString();
        Simplifier.simplify(original);
        assert before.equals(original.toString());
    }

    @Test
    void preservesValue() throws Exception {
        MathElement e = ExpressionParser.parse("(x + 0) * 1 + 2 * 3 - x ^ 1 / 1");
        MathElement s = Simplifier.simplify(e);
        Evaluation x = new Evaluation("x", 7);
        assert Math.abs(e.eval(x) - s.eval(x)) < 1e-12;
    }
}
