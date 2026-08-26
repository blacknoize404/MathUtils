package barnesinside.mathutils.cas.poly;

import barnesinside.mathutils.cas.expr.Equivalence;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import org.junit.jupiter.api.Test;

class FactorTest {

    private static Expr p(String source) {
        return ExprParser.parse(source);
    }

    private static void assertFactors(String source, String expected) {
        Expr actual = Factor.factor(p(source));
        assert actual.equals(p(expected)) : source + " -> expected " + p(expected) + " but was " + actual;
        assert Expand.expand(actual).equals(Expand.expand(p(source))) : "the factorization of " + source + " does not expand back";
    }

    @Test
    void commonFactor() {
        assertFactors("2x + 4", "2*(x + 2)");
        assertFactors("6x^2 + 9x", "3*x*(2x + 3)");
        assertFactors("x^3 y + x^2 y^2", "x^2*y*(x + y)");
        assertFactors("-x^2 + 3x", "-x*(x - 3)");
        assertFactors("x/2 + 1/3", "(3x + 2)/6");
    }

    @Test
    void differenceOfSquares() {
        assertFactors("x^2 - 1", "(x - 1)*(x + 1)");
        assertFactors("4x^2 - 9", "(2x - 3)*(2x + 3)");
        assertFactors("a^2 - b^2", "(a - b)*(a + b)");
        assertFactors("x^4 - 1", "(x - 1)*(x + 1)*(x^2 + 1)");
        assertFactors("x^4 - y^4", "(x - y)*(x + y)*(x^2 + y^2)");
        assertFactors("sin(x)^2 - cos(x)^2", "(sin(x) - cos(x))*(sin(x) + cos(x))");
    }

    @Test
    void sumAndDifferenceOfCubes() {
        assertFactors("x^3 - 8", "(x - 2)*(x^2 + 2x + 4)");
        assertFactors("x^3 + 27", "(x + 3)*(x^2 - 3x + 9)");
        assertFactors("a^3 - b^3", "(a - b)*(a^2 + a*b + b^2)");
        assertFactors("a^3 + b^3", "(a + b)*(a^2 - a*b + b^2)");
    }

    @Test
    void trinomials() {
        assertFactors("x^2 + 5x + 6", "(x + 2)*(x + 3)");
        assertFactors("x^2 - 5x + 6", "(x - 2)*(x - 3)");
        assertFactors("2x^2 + 3x + 1", "(2x + 1)*(x + 1)");
        assertFactors("6x^2 - 7x - 3", "(3x + 1)*(2x - 3)");
        assertFactors("x^2 + 2x + 1", "(x + 1)^2");
        assertFactors("x^2 - 6x + 9", "(x - 3)^2");
    }

    @Test
    void perfectSquareWithSeveralVariables() {
        assertFactors("a^2 + 2a*b + b^2", "(a + b)^2");
        assertFactors("a^2 - 2a*b + b^2", "(a - b)^2");
        assertFactors("4x^2 + 12x*y + 9y^2", "(2x + 3y)^2");
    }

    @Test
    void homogeneousTrinomials() {
        assertFactors("x^2 + 3x*y + 2y^2", "(x + y)*(x + 2y)");
        assertFactors("6x^2 - x*y - 2y^2", "(3x - 2y)*(2x + y)");
    }

    @Test
    void polynomialsWithSeveralRoots() {
        assertFactors("x^3 - 6x^2 + 11x - 6", "(x - 1)*(x - 2)*(x - 3)");
        assertFactors("x^3 - x", "x*(x - 1)*(x + 1)");
        assertFactors("2x^3 - 3x^2 - 3x + 2", "(x + 1)*(x - 2)*(2x - 1)");
        assertFactors("x^4 + 3x^2 + 2", "(x^2 + 1)*(x^2 + 2)");
        assertFactors("x^4 - 5x^2 + 4", "(x - 1)*(x + 1)*(x - 2)*(x + 2)");
    }

    @Test
    void grouping() {
        assertFactors("a*x + a*y + b*x + b*y", "(a + b)*(x + y)");
        assertFactors("x^3 + x^2 + x + 1", "(x + 1)*(x^2 + 1)");
        assertFactors("a*c - a*d - b*c + b*d", "(a - b)*(c - d)");
    }

    @Test
    void factorsTheExpandedFormOfAProduct() {
        assertFactors("(x + 1)^2 - 1", "x*(x + 2)");
        assertFactors("(x - 1)*(x + 2) + (x - 1)*3", "(x - 1)*(x + 5)");
    }

    @Test
    void irreducibleExpressionsStayAsTheyAre() {
        for (String s : new String[]{"x^2 + 1", "x^2 - 2", "x^2 + x + 1", "x + 1", "x", "7", "x^3 + x + 1", "sin(x) + cos(x)"}) {
            Expr e = p(s);
            assert Factor.factor(e).equals(e) : s + " should not change but was " + Factor.factor(e);
        }
    }

    @Test
    void factorsInsideFunctionsAndProducts() {
        assertFactors("sin(x^2 - 1)", "sin((x - 1)*(x + 1))");
        assertFactors("(x^2 - 1)/(x + 3)", "(x - 1)*(x + 1)/(x + 3)");
    }

    @Test
    void theFactorizationIsEquivalentToTheOriginal() {
        String[] sources = {"x^5 - x", "12x^3 - 27x", "x^6 - 1", "x^4 + 4x^3 + 6x^2 + 4x + 1",
                "a^4 - 2a^2*b^2 + b^4", "8x^3 + 12x^2 + 6x + 1", "x^2*y - y^3", "2x^4 - 32"};
        for (String s : sources) {
            Expr e = p(s);
            Expr f = Factor.factor(e);
            assert Equivalence.areEqual(e, f) : s + " -> " + f;
        }
    }
}
