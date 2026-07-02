package MathUtils.algebra;

import MathUtils.algebra.primitives.MathElement;
import MathUtils.parser.ExpressionParser;
import org.junit.jupiter.api.Test;

class LatexPrinterTest {

    private static String latex(String source) {
        return LatexPrinter.toLatex(ExpressionParser.parse(source));
    }

    @Test
    void atoms() {
        assert "x".equals(latex("x"));
        assert "42".equals(latex("42"));
        assert "\\pi".equals(latex("pi"));
        assert "e".equals(latex("e"));
        assert "\\mathrm{alpha}".equals(latex("alpha"));
    }

    @Test
    void operators() {
        assert "x + 1".equals(latex("x + 1"));
        assert "x - 1".equals(latex("x - 1"));
        assert "2 \\cdot x".equals(latex("2 * x"));
        assert "\\frac{x}{2}".equals(latex("x / 2"));
        assert "x^{2}".equals(latex("x ^ 2"));
    }

    @Test
    void parenthesesWhereNeeded() {
        assert "\\left(x + 1\\right) \\cdot 2".equals(latex("(x + 1) * 2"));
        assert "\\left(x + 1\\right)^{2}".equals(latex("(x + 1) ^ 2"));
        assert "x - \\left(y - 1\\right)".equals(latex("x - (y - 1)"));
        assert "x + y - 1".equals(latex("x + y - 1"));
        assert "2^{x + 1}".equals(latex("2 ^ (x + 1)"));
    }

    @Test
    void fractionsNestAndChain() {
        assert "\\frac{x + 1}{x - 1}".equals(latex("(x + 1) / (x - 1)"));
        assert "\\frac{1}{\\frac{x}{2}}".equals(latex("1 / (x / 2)"));
    }

    @Test
    void negation() {
        assert "-x".equals(latex("-x"));
        assert "-\\left(x + 1\\right)".equals(latex("-(x + 1)"));
    }

    @Test
    void functions() {
        assert "\\sin\\left(x\\right)".equals(latex("sin(x)"));
        assert "\\cos\\left(2 \\cdot x\\right)".equals(latex("cos(2*x)"));
        assert "\\tan\\left(x\\right)".equals(latex("tan(x)"));
        assert "\\cot\\left(x\\right)".equals(latex("cot(x)"));
        assert "\\ln\\left(x\\right)".equals(latex("ln(x)"));
        assert "\\log_{2}\\left(x\\right)".equals(latex("log(x, 2)"));
        assert "\\sqrt{x}".equals(latex("sqrt(x)"));
        assert "\\sqrt[3]{x}".equals(latex("cbrt(x)"));
        assert "\\sqrt[n]{x}".equals(latex("root(x, n)"));
    }

    @Test
    void negativeConstantsAreWrappedAsFactors() {
        MathElement e = new MathUtils.algebra.operators.MultOperator(new Const(-2), new Var("x"));
        assert "\\left(-2\\right) \\cdot x".equals(LatexPrinter.toLatex(e));
    }

    @Test
    void derivativeOutputIsReadable() {
        MathElement d = MathUtils.calculus.Derivative.of(ExpressionParser.parse("sin(x)^2"), "x");
        String text = LatexPrinter.toLatex(d);
        assert text.contains("\\cos") && text.contains("\\sin") : text;
    }
}
