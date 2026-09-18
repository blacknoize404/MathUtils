package barnesinside.mathutils.cas.expr;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.regex.Matcher;

class ExprExportTest {

    private static Expr p(String source) {
        return ExprParser.parse(source);
    }

    private static void assertAscii(String expected, String source) {
        String actual = p(source).toAsciiMath();
        assert expected.equals(actual) : source + " -> expected '" + expected + "' but was '" + actual + "'";
    }

    @Test
    void aSubscriptedNameKeepsItsSubscriptInAsciiMathAndMathML() {
        assertAscii("a_1", "a_1");
        assertAscii("a_1 + b", "a_1 + b");
        assertAscii("x_(12)", "x_12");
        assertAscii("a_1^2", "a_1^2");
        assertAscii("(a_1 + a_2)/2", "(a_1 + a_2)/2");
        assertAscii("\"abc\"_1", "abc_1");
        assertAscii("x_i", "x_i");
        assertAscii("\"abc\"", "abc");
        String one = p("a_1").toMathML();
        assert one.contains("<msub><mi>a</mi><mn>1</mn></msub>") : one;
        assert p("x_12").toMathML().contains("<msub><mi>x</mi><mn>12</mn></msub>");
        assert p("x_i").toMathML().contains("<msub><mi>x</mi><mi>i</mi></msub>");
        assert p("abc_1").toMathML().contains("<msub><mi mathvariant=\"normal\">abc</mi><mn>1</mn></msub>");
        assert p("a_1^2").toMathML().contains("<msup><msub><mi>a</mi><mn>1</mn></msub>");
        assert p("abc").toMathML().contains("<mi mathvariant=\"normal\">abc</mi>");
    }

    @Test
    void asciiMathPrintsFractionsPowersAndRoots() {
        assertAscii("x^2 + 1", "x^2 + 1");
        assertAscii("(x + 1)/(x - 1)", "(x + 1)/(x - 1)");
        assertAscii("1/x", "1/x");
        assertAscii("sqrt(x + 1)", "sqrt(x + 1)");
        assertAscii("root(3)(x)", "cbrt(x)");
        assertAscii("(x + 1)^2", "(x + 1)^2");
        assertAscii("2 x", "2x");
        assertAscii("pi", "pi");
        assertAscii("oo", "oo");
    }

    @Test
    void asciiMathPrintsFunctions() {
        assertAscii("sin(x)", "sin(x)");
        assertAscii("arcsin(x)", "asin(x)");
        assertAscii("abs(x)", "abs(x)");
        assertAscii("ln(2 x)", "ln(2x)");
        assertAscii("\"erf\"(x)", "erf(x)");
        assertAscii("e^x", "exp(x)");
    }

    @Test
    void asciiMathQuotesNamesOfSeveralLetters() {
        assertAscii("\"alpha\" + x", "alpha + x");
    }

    @Test
    void mathMlIsWellFormed() {
        String[] sources = {"x^2 + 1", "(x + 1)/(x - 1)", "sqrt(x^2 + 1)", "cbrt(x)", "-3x/4", "sin(x)^2 + cos(2x)",
                "abs(x - 1)", "pi*r^2", "1/2", "e^(-x^2)", "alpha*beta", "2^x - x^2 + 7/3"};
        for (String s : sources) {
            String mathml = p(s).toMathML();
            assert mathml.startsWith("<math xmlns=\"http://www.w3.org/1998/Math/MathML\">") && mathml.endsWith("</math>") : mathml;
            assert wellFormed(mathml) : s + " -> " + mathml;
        }
    }

    @Test
    void mathMlUsesTheRightElements() {
        assert p("1/(x + 1)").toMathML().contains("<mfrac>");
        assert p("x^2").toMathML().contains("<msup><mi>x</mi>");
        assert p("sqrt(x)").toMathML().contains("<msqrt>");
        assert p("cbrt(x)").toMathML().contains("<mroot>");
        assert p("3").toMathML().contains("<mn>3</mn>");
        assert p("x - 2").toMathML().contains("<mo>&#x2212;</mo>");
        assert p("sin(x)").toMathML().contains("<mi>sin</mi><mo>&#x2061;</mo>");
        assert p("pi").toMathML().contains("<mi>&#x3C0;</mi>");
    }

    /** Comprueba que las etiquetas se abren y se cierran en orden. */
    private static boolean wellFormed(String xml) {
        ArrayDeque<String> open = new ArrayDeque<>();
        Matcher m = java.util.regex.Pattern.compile("<(/?)([a-z]+)[^>]*?(/?)>").matcher(xml);
        while (m.find()) {
            boolean closing = !m.group(1).isEmpty();
            boolean selfClosing = !m.group(3).isEmpty();
            if (selfClosing) continue;
            if (closing) {
                if (open.isEmpty() || !open.pop().equals(m.group(2))) return false;
            } else {
                open.push(m.group(2));
            }
        }
        return open.isEmpty();
    }
}
