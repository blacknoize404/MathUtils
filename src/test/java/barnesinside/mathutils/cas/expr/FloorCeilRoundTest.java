package barnesinside.mathutils.cas.expr;

import barnesinside.mathutils.cas.diff.Derivatives;
import org.junit.jupiter.api.Test;

import java.util.Map;

class FloorCeilRoundTest {

    private static Expr parse(String text) {
        return ExprParser.parse(text);
    }

    private static Expr num(long n) {
        return Expr.num(n);
    }

    @Test
    void floorCeilAndRoundOfNumbersAreExact() {
        assert parse("floor(2.7)").equals(num(2)) && parse("floor(-2.5)").equals(num(-3)) && parse("floor(7/2)").equals(num(3));
        assert parse("ceil(2.1)").equals(num(3)) && parse("ceil(-2.5)").equals(num(-2)) && parse("ceil(4)").equals(num(4));
        assert parse("round(2.4)").equals(num(2)) && parse("round(2.5)").equals(num(3)) : "las mitades al más lejano de cero";
        assert parse("round(-2.5)").equals(num(-3)) && parse("round(-2.4)").equals(num(-2)) && parse("round(0)").equals(num(0));
        assert parse("floor(1000000000000000000000.5)").toString().equals("1000000000000000000000") : "con enteros grandes también";
    }

    @Test
    void theOriginalSymbolsMeanTheSameAsTheFunctions() {
        assert parse("⌊2.7⌋").equals(parse("floor(2.7)")) && parse("⌈2.1⌉").equals(parse("ceil(2.1)")) && parse("⌊2.5⌉").equals(parse("round(2.5)"));
        assert parse("⌊x⌋").equals(parse("floor(x)")) && parse("⌈x⌉").equals(parse("ceil(x)")) && parse("⌊x⌉").equals(parse("round(x)"));
        assert parse("⌊⌈1.2⌉/2⌋").equals(num(1)) : "anidados: ⌈1,2⌉ = 2 y ⌊2/2⌋ = 1";
        assert parse("⌊7.5⌋ + ⌈7.5⌉ + ⌊7.5⌉").equals(num(7 + 8 + 8));
        assert parse("2⌊x⌋").equals(parse("2*floor(x)")) : "con multiplicación implícita";
        assert parse("⌊x+1⌋^2").equals(parse("floor(x+1)^2"));
        assert ExprParser.tryParse("⌊x").isEmpty() && ExprParser.tryParse("x⌋").isEmpty() : "sin pareja";
    }

    @Test
    void theNamesWorkInEnglishAndSpanish() {
        assert parse("ceiling(1.2)").equals(num(2)) && parse("suelo(1.9)").equals(num(1)) && parse("techo(1.1)").equals(num(2));
        assert parse("redondear(1.5)").equals(num(2)) && parse("FLOOR(2.9)").equals(num(2));
    }

    @Test
    void constantsAreFoldedWhenTheAnswerIsSafe() {
        assert parse("floor(pi)").equals(num(3)) && parse("ceil(pi)").equals(num(4)) && parse("round(pi)").equals(num(3));
        assert parse("floor(sqrt(2))").equals(num(1)) && parse("round(e)").equals(num(3)) && parse("ceil(-pi)").equals(num(-3));
        assert parse("floor(sqrt(4))").equals(num(2));
    }

    @Test
    void aSymbolicOneStaysAndIsEvaluatedNumerically() {
        Expr f = parse("floor(x)");
        assert f instanceof Expr.Func fn && fn.fn() == Fn.FLOOR : f;
        assert f.toString().equals("floor(x)") && parse("⌈x⌉").toString().equals("ceil(x)");
        assert Eval.value(f, Map.of("x", 2.7)) == 2 && Eval.value(f, Map.of("x", -2.2)) == -3;
        assert Eval.value(parse("ceil(x)"), Map.of("x", 2.1)) == 3 && Eval.value(parse("ceil(x)"), Map.of("x", -2.9)) == -2;
        assert Eval.value(parse("round(x)"), Map.of("x", 0.5)) == 1 && Eval.value(parse("round(x)"), Map.of("x", -0.5)) == -1;
        assert Eval.value(parse("round(x)"), Map.of("x", 1.49)) == 1 && Eval.value(parse("round(x)"), Map.of("x", -1.51)) == -2;
        assert parse("floor(x)").substitute("x", Expr.num(7, 2)).equals(num(3)) : "con valor después se calcula exacto";
        // Aplicarlos otra vez no cambia nada
        assert parse("floor(floor(x))").equals(parse("floor(x)")) && parse("ceil(round(x))").equals(parse("round(x)")) && parse("floor(ceil(x))").equals(parse("ceil(x)"));
    }

    @Test
    void theyAreWrittenWithTheirOwnSymbolsInLatexAndMathML() {
        assert parse("floor(x)").toLatex().equals("\\left\\lfloor x\\right\\rfloor") : parse("floor(x)").toLatex();
        assert parse("ceil(x)").toLatex().equals("\\left\\lceil x\\right\\rceil") && parse("round(x)").toLatex().equals("\\left\\lfloor x\\right\\rceil");
        assert parse("floor(x)").toMathML().contains("&#x230A;") && parse("floor(x)").toMathML().contains("&#x230B;");
        assert parse("ceil(x)").toMathML().contains("&#x2308;") && parse("ceil(x)").toMathML().contains("&#x2309;");
        assert parse("round(x)").toMathML().contains("&#x230A;") && parse("round(x)").toMathML().contains("&#x2309;");
        assert parse("floor(x)").toAsciiMath().equals("floor(x)") && parse("ceil(x)").toAsciiMath().equals("ceil(x)");
    }

    @Test
    void theirDerivativeIsNotAnElementaryOneAndSaysSo() {
        assert !Derivatives.derivative(parse("floor(x)"), "x").isSolved();
        assert !Derivatives.derivative(parse("x*ceil(x)"), "x").isSolved();
        assert Derivatives.derivative(parse("x*floor(2.5)"), "x").result().equals(num(2)) : "el de una constante no estorba";
    }
}
