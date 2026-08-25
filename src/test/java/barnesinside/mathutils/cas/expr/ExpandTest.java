package barnesinside.mathutils.cas.expr;

import org.junit.jupiter.api.Test;

class ExpandTest {

    private static Expr p(String source) {
        return ExprParser.parse(source);
    }

    @Test
    void distributesProducts() {
        assert Expand.expand(p("(x + 1)*(x - 1)")).equals(p("x^2 - 1"));
        assert Expand.expand(p("2*(x + y)")).equals(p("2x + 2y"));
        assert Expand.expand(p("(a + b)*(c + d)")).equals(p("a*c + a*d + b*c + b*d"));
    }

    @Test
    void binomialTheorem() {
        assert Expand.expand(p("(x + 1)^2")).equals(p("x^2 + 2x + 1"));
        assert Expand.expand(p("(x - 2)^3")).equals(p("x^3 - 6x^2 + 12x - 8"));
        assert Expand.expand(p("(a + b)^4")).equals(p("a^4 + 4a^3*b + 6a^2*b^2 + 4a*b^3 + b^4"));
        assert Expand.expand(p("(2x + 3)^2")).equals(p("4x^2 + 12x + 9"));
    }

    @Test
    void multinomialTheorem() {
        assert Expand.expand(p("(a + b + c)^2")).equals(p("a^2 + b^2 + c^2 + 2a*b + 2a*c + 2b*c"));
        Expr cubed = Expand.expand(p("(x + y + z)^3"));
        assert cubed.equals(p("x^3 + y^3 + z^3 + 3x^2*y + 3x^2*z + 3y^2*x + 3y^2*z + 3z^2*x + 3z^2*y + 6x*y*z")) : cubed;
        assert Expand.expand(p("(1 + x + x^2)^2")).equals(p("x^4 + 2x^3 + 3x^2 + 2x + 1"));
    }

    @Test
    void largePowersStayExact() {
        Expr e = Expand.expand(p("(x + 1)^30"));
        assert e.substitute("x", Expr.ONE).equals(Expr.num(1L << 30));
        assert e.expand().equals(e);
    }

    @Test
    void expandsInsideFunctionsAndLeavesTheRestAlone() {
        assert Expand.expand(p("sin((x + 1)^2)")).equals(p("sin(x^2 + 2x + 1)"));
        assert Expand.expand(p("(x + 1)^(1/2)")).equals(p("sqrt(x + 1)"));
        assert Expand.expand(p("x*y")).equals(p("x*y"));
    }
}
