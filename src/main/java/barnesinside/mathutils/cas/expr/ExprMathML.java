package barnesinside.mathutils.cas.expr;

import java.math.BigInteger;
import java.util.List;

/**
 * Impresor en MathML de presentación, con fracciones, raíces y potencias.
 */
final class ExprMathML {

    private static final int ADD = 1;
    private static final int MUL = 2;
    private static final int POW = 3;
    private static final int ATOM = 4;

    private static final String MINUS = "<mo>&#x2212;</mo>";
    private static final String INVISIBLE_TIMES = "<mo>&#x2062;</mo>";
    private static final String DOT = "<mo>&#x22C5;</mo>";
    private static final String APPLY = "<mo>&#x2061;</mo>";

    private ExprMathML() {
    }

    static String print(Expr e) {
        return "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">" + row(print(e, 0)) + "</math>";
    }

    private static String row(String content) {
        return "<mrow>" + content + "</mrow>";
    }

    private static String parentheses(String content) {
        return "<mrow><mo>(</mo>" + content + "<mo>)</mo></mrow>";
    }

    private static String print(Expr e, int required) {
        String text = raw(e);
        return precedence(e) < required ? parentheses(text) : text;
    }

    private static int precedence(Expr e) {
        return switch (e) {
            case Expr.Num n -> n.value().signum() < 0 ? ADD : ATOM;
            case Expr.Const c -> ATOM;
            case Expr.Sym s -> ATOM;
            case Expr.Func f -> ATOM;
            case Expr.Add a -> ADD;
            case Expr.Mul m -> MUL;
            case Expr.Pow p -> isRoot(p) || isReciprocal(p) ? ATOM : POW;
        };
    }

    private static boolean isReciprocal(Expr.Pow p) {
        return p.exponent() instanceof Expr.Num n && n.value().signum() < 0;
    }

    private static boolean isRoot(Expr.Pow p) {
        return p.exponent() instanceof Expr.Num n && n.value().numerator().equals(BigInteger.ONE)
                && n.value().denominator().signum() > 0 && !n.value().isInteger();
    }

    private static String raw(Expr e) {
        return switch (e) {
            case Expr.Num n -> number(n.value());
            case Expr.Const c -> switch (c.kind()) {
                case PI -> "<mi>&#x3C0;</mi>";
                case E -> "<mi>e</mi>";
                case INFINITY -> "<mi>&#x221E;</mi>";
                case EULER_GAMMA -> "<mi>&#x3B3;</mi>";
            };
            case Expr.Sym s -> name(s.name());
            case Expr.Func f -> function(f);
            case Expr.Add a -> row(sum(a));
            case Expr.Mul m -> product(Display.split(m));
            case Expr.Pow p -> power(p);
        };
    }

    /**
     * El nombre de una variable. {@code a_1} lleva subíndice ({@code <msub>}), como lo escribe quien lo usa; los demás de una letra van en
     * cursiva y los de varias, en redonda.
     */
    private static String name(String name) {
        int underscore = name.indexOf('_');
        if (underscore > 0 && underscore < name.length() - 1) {
            String subscript = name.substring(underscore + 1);
            String shown = subscript.chars().allMatch(Character::isDigit) ? "<mn>" + subscript + "</mn>" : name(subscript);
            return "<msub>" + name(name.substring(0, underscore)) + shown + "</msub>";
        }
        return name.length() == 1 ? "<mi>" + escape(name) + "</mi>" : "<mi mathvariant=\"normal\">" + escape(name) + "</mi>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String number(Rational r) {
        String sign = r.signum() < 0 ? MINUS : "";
        String body = r.isInteger()
                ? "<mn>" + r.numerator().abs() + "</mn>"
                : "<mfrac><mn>" + r.numerator().abs() + "</mn><mn>" + r.denominator() + "</mn></mfrac>";
        return sign.isEmpty() ? body : row(sign + body);
    }

    private static String name(Fn fn) {
        return switch (fn) {
            case ASIN -> "arcsin";
            case ACOS -> "arccos";
            case ATAN -> "arctan";
            default -> fn.symbol();
        };
    }

    private static String function(Expr.Func f) {
        String arg = print(f.arg(), 0);
        if (f.fn() == Fn.ABS) return row("<mo>|</mo>" + arg + "<mo>|</mo>");
        if (f.fn() == Fn.FLOOR) return row("<mo>&#x230A;</mo>" + arg + "<mo>&#x230B;</mo>");
        if (f.fn() == Fn.CEIL) return row("<mo>&#x2308;</mo>" + arg + "<mo>&#x2309;</mo>");
        if (f.fn() == Fn.ROUND) return row("<mo>&#x230A;</mo>" + arg + "<mo>&#x2309;</mo>");
        if (f.fn() == Fn.FACTORIAL) return row((isAtom(f.arg()) ? arg : parentheses(arg)) + "<mo>!</mo>");
        return row("<mi>" + name(f.fn()) + "</mi>" + APPLY + parentheses(arg));
    }

    private static String sum(Expr.Add sum) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Expr term : Display.sortedTerms(sum.terms())) {
            boolean negative = Display.isNegative(term);
            Expr shown = negative ? Expr.neg(term) : term;
            String text = print(shown, negative ? MUL : ADD);
            if (first) sb.append(negative ? MINUS : "").append(text);
            else sb.append(negative ? MINUS : "<mo>+</mo>").append(text);
            first = false;
        }
        return sb.toString();
    }

    private static String product(Display.Fraction f) {
        String numerator = f.numerator().isEmpty() ? "<mn>1</mn>" : join(f.numerator());
        String body = numerator;
        if (f.hasDenominator()) {
            String denominator = f.denominator().size() == 1 ? print(f.denominator().get(0), 0) : join(f.denominator());
            body = "<mfrac>" + row(numerator) + row(denominator) + "</mfrac>";
        }
        return f.negative() ? row(MINUS + body) : (f.hasDenominator() ? body : row(body));
    }

    /** Une factores por yuxtaposición, salvo cuando el siguiente es un número. */
    private static String join(List<Expr> factors) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < factors.size(); i++) {
            Expr factor = factors.get(i);
            if (i > 0) sb.append(factor instanceof Expr.Num ? DOT : INVISIBLE_TIMES);
            sb.append(print(factor, MUL + 1));
        }
        return sb.toString();
    }

    private static String power(Expr.Pow p) {
        if (isReciprocal(p)) return product(Display.split(p));

        if (isRoot(p)) {
            BigInteger index = ((Expr.Num) p.exponent()).value().denominator();
            if (index.equals(BigInteger.TWO)) return "<msqrt>" + row(print(p.base(), 0)) + "</msqrt>";
            return "<mroot>" + row(print(p.base(), 0)) + "<mn>" + index + "</mn></mroot>";
        }
        return "<msup>" + print(p.base(), ATOM) + row(print(p.exponent(), 0)) + "</msup>";
    }

    /** Si se puede poner antes de un {@code !} sin paréntesis: un nombre, una constante o un entero no negativo. */
    private static boolean isAtom(Expr e) {
        return e instanceof Expr.Sym || e instanceof Expr.Const || (e instanceof Expr.Num n && n.value().isInteger() && n.value().signum() >= 0);
    }
}
