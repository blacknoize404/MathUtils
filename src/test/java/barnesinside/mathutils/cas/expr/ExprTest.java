package barnesinside.mathutils.cas.expr;

import barnesinside.mathutils.exceptions.ExpressionParseException;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;

class ExprTest {

    private static Expr p(String source) {
        return ExprParser.parse(source);
    }

    private static void assertPrints(String expected, String source) {
        String actual = p(source).toString();
        assert expected.equals(actual) : source + " -> expected '" + expected + "' but was '" + actual + "'";
    }

    private static final Expr X = Expr.sym("x");
    private static final Expr Y = Expr.sym("y");

    // ------------------------------------------------------------ Rational

    @Test
    void rationalNormalizes() {
        assert Rational.of(2, 4).equals(Rational.of(1, 2));
        assert Rational.of(1, -2).equals(Rational.of(-1, 2));
        assert Rational.of(0, 5).equals(Rational.ZERO);
        assert Rational.of(6, 3).isInteger();
    }

    @Test
    void rationalArithmetic() {
        Rational a = Rational.of(1, 3);
        Rational b = Rational.of(1, 6);
        assert a.add(b).equals(Rational.of(1, 2));
        assert a.subtract(b).equals(Rational.of(1, 6));
        assert a.multiply(b).equals(Rational.of(1, 18));
        assert a.divide(b).equals(Rational.of(2));
        assert Rational.of(2, 3).pow(-2).equals(Rational.of(9, 4));
        assert a.compareTo(b) > 0;
    }

    @Test
    void rationalParsesDecimalsExactly() {
        assert Rational.parse("0.25").equals(Rational.of(1, 4));
        assert Rational.parse("1e3").equals(Rational.of(1000));
        assert Rational.parse("2.5e-1").equals(Rational.of(1, 4));
        assert Rational.fromDouble(0.1).equals(Rational.of(1, 10));
    }

    @Test
    void rationalDivisionByZeroThrows() {
        try {
            Rational.ONE.divide(Rational.ZERO);
            assert false;
        } catch (ArithmeticException e) {
            assert true;
        }
    }

    @Test
    void rationalFloor() {
        assert Rational.of(7, 2).floor().equals(Rational.of(3));
        assert Rational.of(-7, 2).floor().equals(Rational.of(-4));
        assert Rational.of(4).floor().equals(Rational.of(4));
    }

    // ------------------------------------------------------------ Forma canónica

    @Test
    void likeTermsCombine() {
        assert Expr.add(X, X).equals(p("2x"));
        assert Expr.add(X, Expr.neg(X)).isZero();
        assert p("x + y + x").equals(p("2x + y"));
        assert p("2x + 3x").equals(p("5x"));
        assert p("x/2 + x/2").equals(X);
    }

    @Test
    void commutativityAndAssociativity() {
        assert p("x + y + 1").equals(p("1 + y + x"));
        assert p("x*y*2").equals(p("2*y*x"));
        assert p("(x + y) + z").equals(p("x + (y + z)"));
    }

    @Test
    void powersCombine() {
        assert p("x*x").equals(p("x^2"));
        assert p("x^2*x^3").equals(p("x^5"));
        assert p("x/x").isOne();
        assert p("x^2/x").equals(X);
        assert p("(2x)^2").equals(p("4x^2"));
        assert p("(x^2)^3").equals(p("x^6"));
        assert p("sqrt(x)*sqrt(x)").equals(X);
        assert p("e^x*e^y").equals(p("e^(x+y)"));
    }

    @Test
    void numbersFoldExactly() {
        assert p("1/3 + 1/6").equals(Expr.num(1, 2));
        assert p("2^10").equals(Expr.num(1024));
        assert p("2^(-2)").equals(Expr.num(1, 4));
        assert p("(2/3)^2").equals(Expr.num(4, 9));
        assert p("0.1 + 0.2").equals(Expr.num(3, 10));
        assert p("x*0").isZero();
        assert p("x*1").equals(X);
        assert p("x + 0").equals(X);
        assert p("x^0").isOne();
        assert p("x^1").equals(X);
        assert p("1^x").isOne();
    }

    @Test
    void radicalsSimplify() {
        assert p("sqrt(4)").equals(Expr.num(2));
        assert p("sqrt(12)").equals(p("2*sqrt(3)"));
        assert p("sqrt(8)").equals(p("2*sqrt(2)"));
        assert p("cbrt(8)").equals(Expr.num(2));
        assert p("cbrt(-8)").equals(Expr.num(-2));
        assert p("sqrt(2)*sqrt(2)").equals(Expr.num(2));
        assert p("1/sqrt(2)").equals(p("sqrt(2)/2"));
        assert p("sqrt(1/4)").equals(Expr.num(1, 2));
        assert p("sqrt(4x)").equals(p("2*sqrt(x)"));
        assert p("8^(2/3)").equals(Expr.num(4));
    }

    @Test
    void squareRootOfSquareIsAbsoluteValue() {
        assert p("sqrt(x^2)").equals(p("abs(x)"));
        assert p("(x^2)^(3/2)").equals(p("abs(x)^3"));
    }

    @Test
    void divisionByZeroIsReported() {
        try {
            p("1/0");
            assert false;
        } catch (ExpressionParseException e) {
            assert true;
        }
    }

    @Test
    void exponentialAndLogarithm() {
        assert p("e^ln(x)").equals(X);
        assert p("ln(e^x)").equals(X);
        assert p("ln(1)").isZero();
        assert p("ln(e)").isOne();
        assert p("exp(0)").isOne();
    }

    @Test
    void parity() {
        assert p("sin(-x)").equals(p("-sin(x)"));
        assert p("cos(-x)").equals(p("cos(x)"));
        assert p("tan(-2x)").equals(p("-tan(2x)"));
        assert p("abs(-x)").equals(p("abs(x)"));
        assert p("atan(-x)").equals(p("-atan(x)"));
        assert p("sin(-x-1)").equals(p("-sin(x+1)"));
    }

    @Test
    void absoluteValueRules() {
        assert p("abs(-3)").equals(Expr.num(3));
        assert p("abs(x^2)").equals(p("x^2"));
        assert p("abs(-2x)").equals(p("2*abs(x)"));
        assert p("abs(abs(x))").equals(p("abs(x)"));
        assert p("abs(e^x)").equals(p("e^x"));
        assert p("abs(sqrt(x))").equals(p("sqrt(x)"));
    }

    @Test
    void exactTrigonometricValues() {
        assert p("sin(0)").isZero();
        assert p("cos(0)").isOne();
        assert p("sin(pi)").isZero();
        assert p("cos(pi)").equals(Expr.num(-1));
        assert p("sin(pi/2)").isOne();
        assert p("cos(pi/2)").isZero();
        assert p("sin(pi/6)").equals(Expr.num(1, 2));
        assert p("cos(pi/3)").equals(Expr.num(1, 2));
        assert p("sin(pi/4)").equals(p("sqrt(2)/2"));
        assert p("cos(pi/4)").equals(p("sqrt(2)/2"));
        assert p("tan(pi/4)").isOne();
        assert p("tan(pi/3)").equals(p("sqrt(3)"));
        assert p("sin(3pi/2)").equals(Expr.num(-1));
        assert p("cos(2pi)").isOne();
        assert p("sin(5pi/6)").equals(Expr.num(1, 2));
        assert p("cos(5pi/6)").equals(p("-sqrt(3)/2"));
        assert p("sin(7pi/4)").equals(p("-sqrt(2)/2"));
        assert p("sec(0)").isOne();
        assert p("csc(pi/2)").isOne();
        assert p("cot(pi/4)").isOne();
    }

    @Test
    void trigonometricValuesMatchNumerics() {
        for (int m = 0; m < 48; m++) {
            if (m % 2 != 0 && m % 3 != 0) continue;
            Expr angle = Expr.mul(Expr.num(m, 12), Expr.PI);
            for (Fn fn : new Fn[]{Fn.SIN, Fn.COS}) {
                Expr exact = Expr.func(fn, angle);
                double expected = fn == Fn.SIN ? Math.sin(m * Math.PI / 12) : Math.cos(m * Math.PI / 12);
                assert exact instanceof Expr.Func == false : fn + " en " + m + "pi/12 no se evaluó";
                assert Math.abs(Eval.value(exact) - expected) < 1e-12 : fn + " " + m + "pi/12";
            }
        }
    }

    @Test
    void undefinedTrigonometricValuesStayUnevaluated() {
        assert p("tan(pi/2)") instanceof Expr.Func;
        assert p("cot(0)") instanceof Expr.Func;
        assert p("sin(1)") instanceof Expr.Func;
    }

    @Test
    void inverseTrigonometricValues() {
        assert p("asin(1)").equals(p("pi/2"));
        assert p("acos(0)").equals(p("pi/2"));
        assert p("atan(1)").equals(p("pi/4"));
        assert p("asin(1/2)").equals(p("pi/6"));
        assert p("acos(-1)").equals(p("pi"));
        assert p("sin(asin(x))").equals(X);
        assert p("tan(atan(x))").equals(X);
    }

    @Test
    void hyperbolicAndErfAtZero() {
        assert p("sinh(0)").isZero();
        assert p("cosh(0)").isOne();
        assert p("tanh(0)").isZero();
        assert p("erf(0)").isZero();
    }

    // ------------------------------------------------------------ Impresión

    @Test
    void plainTextPrinting() {
        assertPrints("x^2 + 3*x + 1", "1 + 3x + x^2");
        assertPrints("x - 1", "x - 1");
        assertPrints("-x", "-x");
        assertPrints("-x - 1", "-x - 1");
        assertPrints("x/2", "x/2");
        assertPrints("1/x", "1/x");
        assertPrints("1/x^2", "x^(-2)");
        assertPrints("1/(x + 1)", "1/(x+1)");
        assertPrints("2*x/3", "2x/3");
        assertPrints("sqrt(x)", "x^(1/2)");
        assertPrints("cbrt(x)", "x^(1/3)");
        assertPrints("x^(2/3)", "x^(2/3)");
        assertPrints("e^x", "exp(x)");
        assertPrints("e^(2*x)", "exp(2x)");
        assertPrints("e^(-x)", "e^(-x)");
        assertPrints("sin(x)", "sin x".replace(" x", "(x)"));
        assertPrints("(x + 1)^2", "(x+1)^2");
        assertPrints("2*sqrt(3)", "sqrt(12)");
        assertPrints("1/2", "0.5");
        assertPrints("(1/2)^x", "0.5^x");
        assertPrints("x*y", "y x".replace(" ", "*"));
        assertPrints("pi", "π");
    }

    @Test
    void latexPrinting() {
        assert "x^{2} + 3 x + 1".equals(p("x^2+3x+1").toLatex());
        assert "\\frac{x}{2}".equals(p("x/2").toLatex());
        assert "\\frac{1}{x + 1}".equals(p("1/(x+1)").toLatex());
        assert "\\sqrt{x}".equals(p("sqrt(x)").toLatex());
        assert "\\sqrt[3]{x}".equals(p("cbrt(x)").toLatex());
        assert "e^{x}".equals(p("e^x").toLatex());
        assert "\\sin\\left(x\\right)".equals(p("sin(x)").toLatex());
        assert "2 \\sin\\left(x\\right)".equals(p("2sin(x)").toLatex());
        assert "\\left|x\\right|".equals(p("abs(x)").toLatex());
        assert "\\arctan\\left(x\\right)".equals(p("atan(x)").toLatex());
        assert "\\pi".equals(p("pi").toLatex());
        assert "-\\frac{1}{2}".equals(p("-1/2").toLatex());
        assert "\\left(x + 1\\right)^{2}".equals(p("(x+1)^2").toLatex());
        assert "2 \\sqrt{3}".equals(p("sqrt(12)").toLatex());
        assert "\\infty".equals(p("oo").toLatex());
    }

    // ------------------------------------------------------------ Parser

    @Test
    void implicitMultiplication() {
        assert p("2x").equals(Expr.mul(Expr.num(2), X));
        assert p("2 x").equals(p("2*x"));
        assert p("x y").equals(p("x*y"));
        assert p("2(x+1)").equals(p("2*(x+1)"));
        assert p("(x+1)(x-1)").equals(p("(x+1)*(x-1)"));
        assert p("x(x+1)").equals(p("x*(x+1)"));
        assert p("3sin(x)").equals(p("3*sin(x)"));
        assert p("sin(x)cos(x)").equals(p("sin(x)*cos(x)"));
        assert p("2pi").equals(p("2*pi"));
        assert p("1/2x").equals(p("x/2"));
        assert p("2x^2").equals(p("2*(x^2)"));
        assert p("-2x^2").equals(p("-(2*x^2)"));
    }

    @Test
    void parserAliasesAndFunctions() {
        assert p("arcsin(x)").equals(p("asin(x)"));
        assert p("arctan(x)").equals(p("atan(x)"));
        assert p("log(100)").equals(p("ln(100)/ln(10)"));
        assert p("log(x, 2)").equals(p("ln(x)/ln(2)"));
        assert p("root(x, 3)").equals(p("cbrt(x)"));
        assert p("pow(x, 3)").equals(p("x^3"));
        assert p("EXP(x)").equals(p("e^x"));
    }

    @Test
    void parserKeepsStrictErrors() {
        for (String bad : new String[]{"", "1 +", "(1", "1)", "2 3", "sin(1,2)", "x +* 2", "sin"}) {
            try {
                p(bad);
                assert false : "Debía fallar: '" + bad + "'";
            } catch (ExpressionParseException e) {
                assert true;
            }
        }
    }

    @Test
    void legacyParserStillRejectsImplicitMultiplication() {
        try {
            barnesinside.mathutils.parser.ExpressionParser.parse("2x");
            assert false;
        } catch (ExpressionParseException e) {
            assert true;
        }
    }

    @Test
    void unknownNameBeforeParenthesisIsAProduct() {
        assert p("foo(1)").equals(p("foo*1"));
        assert p("f(x+1)").equals(p("f*(x+1)"));
    }

    @Test
    void tryParseReturnsEmptyOnError() {
        assert ExprParser.tryParse("x +").isEmpty();
        assert ExprParser.tryParse("x + 1").isPresent();
    }

    // ------------------------------------------------------------ Round trip

    @Test
    void printingThenParsingIsIdentityForRandomExpressions() {
        Random random = new Random(7);
        for (int i = 0; i < 3000; i++) {
            Expr e = randomExpr(random, 4);
            Expr back;
            try {
                back = ExprParser.parse(e.toString());
            } catch (ExpressionParseException ex) {
                throw new AssertionError("No se pudo reinterpretar '" + e + "': " + ex.getMessage());
            }
            assert e.equals(back) : "'" + e + "' se reinterpretó como '" + back + "'";
        }
    }

    private static Expr randomExpr(Random r, int depth) {
        if (depth == 0 || r.nextInt(5) == 0) {
            return switch (r.nextInt(7)) {
                case 0 -> Expr.num(r.nextInt(11) - 5);
                case 1 -> Expr.num(r.nextInt(9) - 4, r.nextInt(5) + 1);
                case 2 -> X;
                case 3 -> Y;
                case 4 -> Expr.PI;
                case 5 -> Expr.E;
                default -> Expr.num(r.nextInt(9) + 1);
            };
        }
        Expr a = randomExpr(r, depth - 1);
        Expr b = randomExpr(r, depth - 1);
        try {
            return switch (r.nextInt(11)) {
                case 0 -> Expr.add(a, b);
                case 1 -> Expr.sub(a, b);
                case 2 -> Expr.mul(a, b);
                case 3 -> Expr.div(a, b);
                case 4 -> Expr.pow(a, Expr.num(r.nextInt(7) - 3));
                case 5 -> Expr.sqrt(a);
                case 6 -> Expr.exp(a);
                case 7 -> Expr.func(Fn.values()[r.nextInt(Fn.values().length)], a);
                case 8 -> Expr.pow(a, Expr.num(r.nextInt(5) + 1, 3));
                case 9 -> Expr.pow(a, b);
                default -> Expr.neg(a);
            };
        } catch (ArithmeticException e) {
            return a;
        }
    }

    // ------------------------------------------------------------ Utilidades

    @Test
    void substitution() {
        Expr e = p("x^2 + y");
        assert e.substitute("x", Expr.num(3)).equals(p("9 + y"));
        assert e.substitute("x", Y).equals(p("y^2 + y"));
        assert e.substitute(Map.of("x", Expr.num(1), "y", Expr.num(2))).equals(Expr.num(3));
        assert p("sin(x)").substitute("x", p("pi/6")).equals(Expr.num(1, 2));
    }

    @Test
    void variablesAndFreeOf() {
        Expr e = p("x*sin(y) + 2");
        assert e.variables().equals(java.util.Set.of("x", "y"));
        assert e.isFreeOf("z");
        assert !e.isFreeOf("x");
        assert p("pi + e").variables().isEmpty();
    }

    @Test
    void numericEvaluation() {
        assert Math.abs(Eval.value(p("x^2 + 3x + 1"), Map.of("x", 2.0)) - 11) < 1e-12;
        assert Math.abs(Eval.value(p("sin(pi/6)")) - 0.5) < 1e-12;
        assert Math.abs(Eval.value(p("cbrt(-8)")) + 2) < 1e-12;
        assert Math.abs(Eval.value(p("erf(1)")) - 0.8427007929497149) < 1e-12;
        assert Math.abs(Eval.value(p("ln(x)"), Map.of("x", Math.E)) - 1) < 1e-12;
        assert Double.isNaN(Eval.value(p("sqrt(x)"), Map.of("x", -1.0)));
        try {
            Eval.value(p("x"), Map.of());
            assert false;
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void expand() {
        assert Expand.expand(p("(x+1)^2")).equals(p("x^2 + 2x + 1"));
        assert Expand.expand(p("(x+1)(x-1)")).equals(p("x^2 - 1"));
        assert Expand.expand(p("(x+y)^3")).equals(p("x^3 + 3x^2*y + 3x*y^2 + y^3"));
        assert Expand.expand(p("2(x+3)")).equals(p("2x + 6"));
        assert Expand.expand(p("sin((x+1)^2)")).equals(p("sin(x^2 + 2x + 1)"));
        assert Expand.expand(p("x*(y+1)/2")).equals(p("x*y/2 + x/2"));
    }

    @Test
    void equivalence() {
        assert Equivalence.areEqual(p("(x+1)^2"), p("x^2+2x+1"));
        assert Equivalence.areEqual(p("sin(x)^2 + cos(x)^2"), Expr.ONE);
        assert Equivalence.areEqual(p("2sin(x)cos(x)"), p("sin(2x)"));
        assert Equivalence.check(p("x^2"), p("x^3")) == Equivalence.Result.DIFFERENT;
        assert Equivalence.check(p("sin(x)"), p("cos(x)")) == Equivalence.Result.DIFFERENT;
        assert Equivalence.check(p("sqrt(x^2)"), X) == Equivalence.Result.DIFFERENT;
        assert Equivalence.check(p("sqrt(x)^2"), X) == Equivalence.Result.EQUAL;
    }

    @Test
    void differByConstant() {
        assert Equivalence.differByConstant(p("x^2 + 5"), p("x^2"), "x");
        assert Equivalence.differByConstant(p("sin(x)^2"), p("-cos(x)^2"), "x");
        assert !Equivalence.differByConstant(p("x^2"), p("x^3"), "x");
    }

    // ------------------------------------------------------------ Conversión

    @Test
    void convertsFromLegacyElements() {
        var legacy = barnesinside.mathutils.parser.ExpressionParser.parse("x^2 + 3*x - sin(x) / 2");
        assert ElementConverter.toExpr(legacy).equals(p("x^2 + 3x - sin(x)/2"));
        assert ElementConverter.toExpr(barnesinside.mathutils.parser.ExpressionParser.parse("sqrt(x) + cbrt(x)")).equals(p("sqrt(x) + cbrt(x)"));
        assert ElementConverter.toExpr(barnesinside.mathutils.parser.ExpressionParser.parse("log(x, 2)")).equals(p("ln(x)/ln(2)"));
        assert ElementConverter.toExpr(barnesinside.mathutils.parser.ExpressionParser.parse("pi * e")).equals(p("pi*e"));
        assert ElementConverter.toExpr(new barnesinside.mathutils.algebra.Fraction(3, 4)).equals(Expr.num(3, 4));
    }

    @Test
    void convertsToLegacyElementsPreservingValue() throws Exception {
        for (String s : new String[]{"x^2 + 3x + 1", "sin(x)/x", "sqrt(x) + 1/x", "e^x*cos(x)", "ln(x) - 3x^4", "x/2"}) {
            Expr e = p(s);
            var element = ElementConverter.toElement(e);
            double legacy = element.eval(new barnesinside.mathutils.algebra.Evaluation("x", 1.7));
            double symbolic = Eval.value(e, Map.of("x", 1.7));
            assert Math.abs(legacy - symbolic) < 1e-9 : s;
        }
    }

    @Test
    void functionsWithoutLegacyClassAreReported() {
        try {
            ElementConverter.toElement(p("atan(x)"));
            assert false;
        } catch (UnsupportedOperationException e) {
            assert true;
        }
    }

    @Test
    void largeNumbersStayExact() {
        Expr e = p("2^200");
        assert e instanceof Expr.Num n && n.value().numerator().equals(BigInteger.TWO.pow(200));
        assert p("2^2000").toString().length() > 0;
        List<Expr> many = new java.util.ArrayList<>();
        for (int i = 0; i < 200; i++) many.add(Expr.num(i));
        assert Expr.add(many).equals(Expr.num(19900));
    }
}
