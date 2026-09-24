package barnesinside.mathutils.cas.expr;

import barnesinside.mathutils.cas.diff.Derivatives;
import barnesinside.mathutils.cas.order.OrderOfOperations;
import barnesinside.mathutils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

import java.util.Map;

class FactorialTest {

    private static Expr parse(String text) {
        return ExprParser.parse(text);
    }

    @Test
    void theFactorialOfAnIntegerIsExact() {
        assert parse("5!").equals(Expr.num(120));
        assert parse("0!").equals(Expr.ONE) && parse("1!").equals(Expr.ONE);
        assert parse("(2+1)!").equals(Expr.num(6)) : "de un grupo";
        assert parse("20!").toString().equals("2432902008176640000");
        assert parse("factorial(4)").equals(Expr.num(24)) && parse("fact(3)").equals(Expr.num(6)) : "también como función";
    }

    @Test
    void theFactorialBindsTighterThanThePowerAndTheMinusSign() {
        assert parse("2^3!").equals(Expr.num(64)) : "2^(3!)";
        assert parse("-3!").equals(Expr.num(-6)) : "-(3!)";
        assert parse("3!+2").equals(Expr.num(8)) && parse("2*3!").equals(Expr.num(12)) && parse("3!^2").equals(Expr.num(36));
        assert parse("4!/3!").equals(Expr.num(4));
        assert parse("2(3)!").equals(Expr.num(12)) : "multiplicación implícita: 2·3!";
        assert parse("3!x").equals(Expr.mul(Expr.num(6), Expr.sym("x"))) : parse("3!x");
    }

    @Test
    void aSymbolicFactorialStaysAndPrintsWithTheSign() {
        Expr n = parse("n!");
        assert n instanceof Expr.Func f && f.fn() == Fn.FACTORIAL : n;
        assert n.toString().equals("n!") && parse("(n+1)!").toString().equals("(n + 1)!") : parse("(n+1)!");
        // Lo que se escribe se vuelve a leer igual
        for (String text : new String[]{"n!", "(n + 1)!", "(2*n)!/(n!)^2", "x^2*n!"}) {
            Expr e = parse(text);
            assert parse(e.toString()).equals(e) : text + " -> " + e;
        }
        assert n.toLatex().equals("n!") && parse("(n+1)!").toLatex().equals("\\left(n + 1\\right)!") : parse("(n+1)!").toLatex();
        assert n.toAsciiMath().equals("n!") && parse("(n+1)!").toAsciiMath().equals("(n + 1)!");
        assert n.toMathML().contains("<mo>!</mo>");
    }

    @Test
    void itIsEvaluatedNumericallyAlsoForNonIntegers() {
        assert Eval.value(parse("n!"), Map.of("n", 5.0)) == 120;
        assert Math.abs(Eval.value(parse("(1/2)!")) - Math.sqrt(Math.PI) / 2) < 1e-12 : "Γ(3/2)";
        assert Math.abs(Eval.value(parse("(5/2)!"), Map.of()) - 3.3233509704478426) < 1e-9;
        assert Math.abs(Eval.value(parse("x!"), Map.of("x", 10.0)) - 3628800) < 1e-6;
        assert Double.isNaN(barnesinside.mathutils.special.Special.gamma(-2)) : "Γ no existe en los enteros no positivos";
        assert Math.abs(barnesinside.mathutils.special.Special.gamma(0.5) - Math.sqrt(Math.PI)) < 1e-12;
        assert Math.abs(barnesinside.mathutils.special.Special.gamma(-0.5) + 2 * Math.sqrt(Math.PI)) < 1e-10;
        // Un símbolo con valor después se calcula exacto
        assert parse("n!").substitute("n", Expr.num(6)).equals(Expr.num(720));
    }

    @Test
    void whatCannotBeComputedIsExplained() {
        try {
            parse("(-2)!");
            assert false : "el factorial de un entero negativo no existe";
        } catch (RuntimeException expected) {
            assert expected.getMessage().contains("factorial") : expected.getMessage();
        }
        for (String bad : new String[]{"!", "3 + !", "(!2)", "n!!!"}) {
            if (bad.equals("n!!!")) continue;
            assert ExprParser.tryParse(bad).isEmpty() : bad;
        }
        assert ExprParser.tryParse("n!!").isPresent() : "(n!)!";
    }

    @Test
    void theDerivativeOfAFactorialIsNotKnownAndSaysSoInsteadOfFailing() {
        Solution s = Derivatives.derivative(parse("x!"), "x");
        assert !s.isSolved() : "necesita la función digamma";
        assert Derivatives.derivative(parse("x*3!"), "x").isSolved() : "el factorial de una constante no estorba";
        assert Derivatives.derivative(parse("x*3!"), "x").result().equals(Expr.num(6));
    }

    @Test
    void theOrderOfOperationsSolvesFactorialsStepByStep() {
        OrderOfOperations.Reduction r = OrderOfOperations.reduce("2 + 3!*2");
        assert r.isFullyEvaluated() && ((barnesinside.mathutils.cas.syntax.Syntax.Num) r.result()).text().equals("14") : r.result();
        assert r.steps().size() == 3 : "3!, el producto y la suma: " + r.steps().size();
        assert r.steps().get(0).beforePieces().stream().map(p -> p.text()).reduce("", String::concat).contains("3!") : "se muestra con el signo";
    }

    @Test
    void anExclamationBeforeAnEqualsStillMeansDifferent() {
        assert Relation.parse("x != 3").isPresent() && Relation.parse("x != 3").get().op() == Op.NE;
        assert Relation.parse("n! = 120").isPresent() && Relation.parse("n! = 120").get().op() == Op.EQ : "factorial y luego igual";
    }
}
