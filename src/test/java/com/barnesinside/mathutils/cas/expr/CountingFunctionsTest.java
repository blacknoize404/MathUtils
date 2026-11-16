package com.barnesinside.mathutils.cas.expr;

import com.barnesinside.mathutils.cas.calc.CalculatorSession;
import org.junit.jupiter.api.Test;

import java.util.Map;

class CountingFunctionsTest {

    private static Expr parse(String text) {
        return ExprParser.parse(text);
    }

    @Test
    void theClassicFormulasAreSolvedAsFactorialsAndPowers() {
        assert parse("C(10, 2)").equals(Expr.num(45)) : parse("C(10, 2)");
        assert parse("P(10, 4)").equals(Expr.num(5040));
        assert parse("P(5)").equals(Expr.num(120)) : "P(n) es n!";
        assert parse("V(10, 4)").equals(Expr.num(5040)) : "V = P con dos argumentos";
        assert parse("VR(10, 4)").equals(Expr.num(10000)) : "n^k";
        assert parse("CR(4, 3)").equals(Expr.num(20)) : "C(n + k - 1, k)";
        assert parse("PC(5)").equals(Expr.num(24)) : "mesa circular: (n - 1)!";
        assert parse("M(6, 3, 2, 1)").equals(Expr.num(60)) : "multinomial de BANANA";
        assert parse("Cat(4)").equals(Expr.num(14));
    }

    @Test
    void theWordNamesAndLowerCaseSiglasWorkToo() {
        assert parse("comb(10, 2)").equals(Expr.num(45));
        assert parse("choose(10, 2)").equals(Expr.num(45)) && parse("binom(10, 2)").equals(Expr.num(45));
        assert parse("perm(10, 4)").equals(Expr.num(5040)) && parse("permutation(10, 4)").equals(Expr.num(5040));
        assert parse("arr(10, 4)").equals(Expr.num(5040)) : "var queda para la varianza, arr sí son variaciones";
        assert parse("varr(10, 4)").equals(Expr.num(10000)) && parse("vr(10, 4)").equals(Expr.num(10000));
        assert parse("combr(4, 3)").equals(Expr.num(20)) && parse("cr(4, 3)").equals(Expr.num(20));
        assert parse("circ(5)").equals(Expr.num(24)) && parse("pc(5)").equals(Expr.num(24));
        assert parse("permr(6, 3, 2, 1)").equals(Expr.num(60)) && parse("multinom(6, 3, 2, 1)").equals(Expr.num(60));
        assert parse("catalan(4)").equals(Expr.num(14));
    }

    @Test
    void singleLetterVariablesStayVariables() {
        // Solo las mayúsculas son funciones de conteo: c, v, p y m siguen siendo variables
        Expr c = parse("c");
        assert c instanceof Expr.Sym && ((Expr.Sym) c).name().equals("c") : c;
        assert ExprParser.tryParse("c(10, 2)").isEmpty() : "c minúscula no es la función C: error claro, no resultado raro";
        assert ExprParser.tryParse("v(10, 4)").isEmpty() : "v minúscula tampoco: se escribe V(10, 4)";
    }

    @Test
    void variablesSimplifyThroughFactorialRatios() {
        // n!/(n - 2)! es n·(n - 1), así que C(n, 2) se simplifica de verdad
        Expr e = parse("C(n, 2)");
        assert e.toString().equals("n*(n - 1)/2") : e.toString();
        assert e.substitute("n", Expr.num(10)).equals(Expr.num(45)) : "y evalúa con la variable ligada";
        Expr p = parse("P(n, 3)");
        assert p.toString().contains("n") && !p.toString().contains("!") : "P(n,3) = n(n-1)(n-2): " + p;
        assert p.substitute("n", Expr.num(10)).equals(Expr.num(720));
        assert parse("x!/x!").equals(Expr.ONE) : "un factorial entre sí mismo es 1";
    }

    @Test
    void theSessionEvaluatesAndRemembers() {
        CalculatorSession session = new CalculatorSession();
        CalculatorSession.Result r = session.evaluate("C(10, 2)");
        assert r.status() == CalculatorSession.Status.OK && r.exact().equals(Expr.num(45)) : r;
        session.evaluate("n = 10");
        r = session.evaluate("C(n, 2)");
        assert r.status() == CalculatorSession.Status.OK && r.exact().equals(Expr.num(45)) : r;
        session.evaluate("r = 4");
        r = session.evaluate("P(n, r)");
        assert r.status() == CalculatorSession.Status.OK && r.exact().equals(Expr.num(5040)) : r;
    }

    @Test
    void aDeclaredNameIsAVariableAndTheCallIsStillTheFunction() {
        // M = 30 (límite de una suma, un punto…) convive con M(6,3,2,1): la llamada manda
        CalculatorSession session = new CalculatorSession();
        session.evaluate("M = 30");
        CalculatorSession.Result r = session.evaluate("M + 1");
        assert r.status() == CalculatorSession.Status.OK && r.exact().equals(Expr.num(31)) : r;
        r = session.evaluate("M(6,3,2,1)");
        assert r.status() == CalculatorSession.Status.OK && r.exact().equals(Expr.num(60)) : r;
        r = session.evaluate("C(M, 2)");
        assert r.status() == CalculatorSession.Status.OK && r.exact().equals(Expr.num(435)) : r;
        // Sin declarar, la sigla suelta se reconoce como función y lo dice
        r = new CalculatorSession().evaluate("P");
        assert r.isError() && r.message().contains("'(") : r.message();
    }

    @Test
    void badCallsAreExplained() {
        assert ExprParser.tryParse("C(10)").isEmpty() : "faltan argumentos";
        assert ExprParser.tryParse("C(2, 5)").isEmpty() : "k > n pediría el factorial de un negativo";
        assert ExprParser.tryParse("M(6)").isEmpty() : "el multinomial necesita los repes";
        assert ExprParser.tryParse("PC()").isEmpty() || ExprParser.tryParse("PC(2, 3)").isEmpty();
        assert Eval.value(parse("C(10, 2)"), Map.of()) == 45;
    }
}
