package MathUtils.complex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplexParserTest {

    private static void assertParses(String text, double re, double im) {
        Complex z = Complex.parse(text);
        assertTrue(z.isCloseTo(Complex.of(re, im), 1e-12), text + " -> " + z);
    }

    @Test
    void rectangularForms() {
        assertParses("3+4i", 3, 4);
        assertParses("3 - 4i", 3, -4);
        assertParses("-2i", 0, -2);
        assertParses("i", 0, 1);
        assertParses("-i", 0, -1);
        assertParses("5", 5, 0);
        assertParses("1.5-2.5i", 1.5, -2.5);
        assertParses("0,5+i", 0.5, 1);
    }

    @Test
    void arithmeticAndPowers() {
        assertParses("(1+i)^2", 0, 2);
        assertParses("(1+i)*(1-i)", 2, 0);
        assertParses("(3+4i)/(1-2i)", -1, 2);
        assertParses("i^4", 1, 0);
        assertParses("2(1+i)", 2, 2);
        assertParses("(1+i)^-1", 0.5, -0.5);
        assertParses("2pi", 2 * Math.PI, 0);
        assertParses("e", Math.E, 0);
    }

    @Test
    void invalidTextIsReportedWithAPosition() {
        for (String bad : new String[]{"", "3+", "(1+i", "1/0", "x+1", "2^i", "3 $ 4"}) {
            assertThrows(IllegalArgumentException.class, () -> Complex.parse(bad), bad);
        }
    }
}
