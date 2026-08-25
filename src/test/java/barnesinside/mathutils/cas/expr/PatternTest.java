package barnesinside.mathutils.cas.expr;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class PatternTest {

    private static Expr p(String source) {
        return ExprParser.parse(source);
    }

    @Test
    void wildcardsBindToSubexpressions() {
        Optional<Map<String, Expr>> m = Pattern.match(p("sin(a)^2"), p("sin(3x + 1)^2"), Set.of("a"));
        assert m.isPresent();
        assert m.get().get("a").equals(p("3x + 1"));
    }

    @Test
    void aWildcardMustMatchTheSameThingEverywhere() {
        assert Pattern.match(p("sin(a)^2 + cos(a)^2"), p("sin(x)^2 + cos(x)^2"), Set.of("a")).isPresent();
        assert Pattern.match(p("sin(a)^2 + cos(a)^2"), p("sin(x)^2 + cos(y)^2"), Set.of("a")).isEmpty();
    }

    @Test
    void structureAndLiteralsMustAgree() {
        assert Pattern.match(p("a^2"), p("x^3"), Set.of("a")).isEmpty();
        assert Pattern.match(p("a + 1"), p("x + 2"), Set.of("a")).isEmpty();
        assert Pattern.match(p("sin(a)"), p("cos(x)"), Set.of("a")).isEmpty();
        assert Pattern.match(p("x + a"), p("x + y"), Set.of("a")).isPresent();
    }

    @Test
    void sumsAndProductsIgnoreTheOrder() {
        Optional<Map<String, Expr>> m = Pattern.match(p("a*sin(b)"), p("sin(2x)*5"), Set.of("a", "b"));
        assert m.isPresent();
        assert m.get().get("a").equals(p("5"));
        assert m.get().get("b").equals(p("2x"));

        assert Pattern.match(p("sin(a) + cos(b)"), p("cos(y) + sin(x)"), Set.of("a", "b")).isPresent();
    }

    @Test
    void aWildcardTermAbsorbsTheRest() {
        Optional<Map<String, Expr>> m = Pattern.match(p("a + sin(x)"), p("y + z + sin(x)"), Set.of("a"));
        assert m.isPresent();
        assert m.get().get("a").equals(p("y + z"));

        Optional<Map<String, Expr>> n = Pattern.match(p("2*a"), p("2*x*y"), Set.of("a"));
        assert n.isPresent();
        assert n.get().get("a").equals(p("x*y"));
    }

    @Test
    void findsEveryMatchInsideAnExpression() {
        List<Pattern.Match> found = Pattern.findAll(p("sin(a)"), p("sin(x) + cos(sin(2y))"), Set.of("a"));
        assert found.size() == 2 : found;
        assert found.stream().anyMatch(f -> f.subexpression().equals(p("sin(x)")));
        assert found.stream().anyMatch(f -> f.bindings().get("a").equals(p("2y")));
    }

    @Test
    void rewritesEveryOccurrence() {
        Expr result = Pattern.rewrite(p("sin(x)^2 + sin(y)^2*z"), p("sin(a)^2"), p("1 - cos(a)^2"), Set.of("a"));
        assert result.equals(p("1 - cos(x)^2 + (1 - cos(y)^2)*z")) : result;
    }

    @Test
    void rewritesASubsetOfTheTermsOfASum() {
        Expr identity = p("sin(a)^2 + cos(a)^2");
        Expr result = Pattern.rewrite(p("sin(3x)^2 + cos(3x)^2 + y"), identity, Expr.ONE, Set.of("a"));
        assert result.equals(p("1 + y")) : result;

        Expr inside = Pattern.rewrite(p("sqrt(sin(x)^2 + z + cos(x)^2)"), identity, Expr.ONE, Set.of("a"));
        assert inside.equals(p("sqrt(1 + z)")) : inside;
    }

    @Test
    void leavesTheExpressionAloneWhenNothingMatches() {
        Expr e = p("x^2 + 3x + 1");
        assert Pattern.rewrite(e, p("sin(a)"), p("a"), Set.of("a")).equals(e);
    }
}
