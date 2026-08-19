package barnesinside.mathutils.parser;

import barnesinside.mathutils.cas.expr.Equivalence;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.complex.Complex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotationTest {

    private static void same(String symbols, String keyboard) {
        assertTrue(Equivalence.areEqual(ExprParser.parse(symbols), ExprParser.parse(keyboard)),
                symbols + " debía valer lo mismo que " + keyboard);
    }

    @Test
    void symbolsMeanTheirKeyboardEquivalents() {
        same("2π", "2*pi");
        same("√(x²+1)", "sqrt(x^2+1)");
        same("√x", "sqrt(x)");
        same("x²·3", "3*x^2");
        same("x×y÷2", "x*y/2");
        same("x−1", "x-1");
        same("x⁻¹", "1/x");
        same("x¹²", "x^12");
        same("sin(30°)", "1/2");
        same("∛8", "2");
        same("e^∞", "e^oo");
    }

    @Test
    void greekLettersAreVariables() {
        same("θ+θ", "2*θ");
        same("sin(α)^2+cos(α)^2", "1");
    }

    @Test
    void plainTextIsLeftAlone() {
        assertEquals("2*x^2 + sqrt(y)", Notation.normalize("2*x^2 + sqrt(y)"));
    }

    @Test
    void complexNumbersAcceptTheSymbolsToo() {
        assertTrue(Complex.parse("2π").isCloseTo(Complex.real(2 * Math.PI), 1e-12));
        assertTrue(Complex.parse("(1+i)²").isCloseTo(Complex.of(0, 2), 1e-12));
        assertTrue(Complex.parse("(3+4i)÷(1−2i)").isCloseTo(Complex.of(-1, 2), 1e-12));
    }
}
