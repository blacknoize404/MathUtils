package barnesinside.mathutils.cas.expr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionPowersTest {

    private static boolean sameValue(String a, String b) {
        Expr x = ExprParser.parse(a);
        Expr y = ExprParser.parse(b);
        for (long v : new long[]{2, 3, 5}) {
            double p = Eval.value(x.substitute(java.util.Map.of("x", Expr.num(v))));
            double q = Eval.value(y.substitute(java.util.Map.of("x", Expr.num(v))));
            if (Math.abs(p - q) > 1e-9) return false;
        }
        return true;
    }

    @Test
    void aFunctionRaisedToAPowerIsParsedAsTheCallRaisedToIt() {
        assertTrue(sameValue("sin^2(x)", "(sin(x))^2"));
        assertTrue(sameValue("cos^3(x) + 1", "cos(x)^3 + 1"));
        assertTrue(sameValue("ln^2(x)", "ln(x)^2"));
        assertTrue(sameValue("sin^(1+1)(x)", "sin(x)^2"));
        assertTrue(sameValue("sin^-2(x)", "1/sin(x)^2"));
        assertTrue(sameValue("2*sin^2(3*x) + tan^2(x)", "2*sin(3*x)^2 + tan(x)^2"));
    }

    @Test
    void minusOneMeansTheInverseFunction() {
        assertEquals("asin(x)", FunctionPowers.rewrite("sin^-1(x)"));
        assertTrue(sameValue("tan^-1(x)", "atan(x)"));
    }

    @Test
    void ordinaryTextIsLeftAlone() {
        for (String text : new String[]{"x^2", "sin(x)^2", "sin(x^2)", "e^x", "2^sin(x)", "x^2*sin(x)"}) {
            assertEquals(text, FunctionPowers.rewrite(text));
        }
    }
}
