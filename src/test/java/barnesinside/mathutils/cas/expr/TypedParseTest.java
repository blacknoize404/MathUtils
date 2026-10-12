package barnesinside.mathutils.cas.expr;

import org.junit.jupiter.api.Test;

import java.util.List;

/** El enunciado se enseña como se escribió: el orden de los sumandos y de los factores no cambia. */
class TypedParseTest {

    private static Expr typed(String text) {
        return ExprParser.parseTyped(text).orElseThrow(() -> new AssertionError("sin forma escrita: " + text));
    }

    @Test
    void theTypedExpressionKeepsTheWrittenOrder() {
        // 1 + 1/x: el uno va primero (la expresión normal pone 1/x delante)
        List<Expr> terms = ((Expr.Add) typed("1+1/x")).terms();
        assert terms.getFirst().equals(Expr.num(1)) : terms;
        // x^2*y^3 + sin(x*y): el polinomio va delante del seno
        List<Expr> mixed = ((Expr.Add) typed("x^2*y^3 + sin(x*y)")).terms();
        assert mixed.getFirst() instanceof Expr.Mul && mixed.getLast() instanceof Expr.Func : mixed;
        // (1 - cos x)/x^2: el numerador empieza por el 1
        Expr.Mul quotient = (Expr.Mul) typed("(1-cos(x))/x^2");
        Expr.Add numerator = (Expr.Add) quotient.factors().getFirst();
        assert numerator.terms().getFirst().equals(Expr.num(1)) : numerator;
    }

    @Test
    void itIsAlwaysEquivalentToTheNormalExpression() {
        for (String text : new String[]{"1+1/x", "x^2*sin(x)", "(x^2+1)/(x-3)", "sqrt(1+x^2)", "e^(2x)*cos(x)", "-x^2 + 3*x - 1", "a*b*c + d"}) {
            assert Equivalence.areEqual(typed(text), ExprParser.parse(text)) : text;
        }
    }

    @Test
    void textsWithNestedCalculusOrErrorsHaveNoTypedForm() {
        assert ExprParser.parseTyped("int(x^2, x, 0, 1) + 1").isEmpty();
        assert ExprParser.parseTyped("(((").isEmpty();
    }
}
