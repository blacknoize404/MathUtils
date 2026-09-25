package barnesinside.mathutils.cas.expr;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Impresor en LaTeX con fracciones, raíces, potencias y paréntesis escalables.
 */
final class ExprLatex {

    private static final int ADD = 1;
    private static final int MUL = 2;
    private static final int POW = 3;
    private static final int ATOM = 4;

    private ExprLatex() {
    }

    static String print(Expr e) {
        return print(e, 0);
    }

    private static String print(Expr e, int required) {
        String text = raw(e);
        return precedence(e) < required ? "\\left(" + text + "\\right)" : text;
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
        return p.exponent() instanceof Expr.Num n && n.value().numerator().equals(java.math.BigInteger.ONE)
                && n.value().denominator().signum() > 0 && !n.value().isInteger();
    }

    private static String raw(Expr e) {
        return switch (e) {
            case Expr.Num n -> number(n.value());
            case Expr.Const c -> switch (c.kind()) {
                case PI -> "\\pi";
                case E -> "e";
                case INFINITY -> "\\infty";
            };
            case Expr.Sym s -> s.name().length() == 1 ? s.name() : "\\mathrm{" + s.name() + "}";
            case Expr.Func f -> function(f);
            case Expr.Add a -> sum(a);
            case Expr.Mul m -> product(Display.split(m));
            case Expr.Pow p -> power(p);
        };
    }

    private static String number(Rational r) {
        if (r.isInteger()) return r.toString();
        String sign = r.signum() < 0 ? "-" : "";
        return sign + "\\frac{" + r.numerator().abs() + "}{" + r.denominator() + "}";
    }

    private static String function(Expr.Func f) {
        String arg = print(f.arg());
        return switch (f.fn()) {
            case FLOOR -> "\\left\\lfloor " + arg + "\\right\\rfloor";
            case CEIL -> "\\left\\lceil " + arg + "\\right\\rceil";
            case ROUND -> "\\left\\lfloor " + arg + "\\right\\rceil";
            case FACTORIAL -> (isAtom(f.arg()) ? arg : "\\left(" + arg + "\\right)") + "!";
            case ABS -> "\\left|" + arg + "\\right|";
            case LN -> "\\ln\\left(" + arg + "\\right)";
            case ERF -> "\\operatorname{erf}\\left(" + arg + "\\right)";
            case SI -> "\\operatorname{Si}\\left(" + arg + "\\right)";
            case CI -> "\\operatorname{Ci}\\left(" + arg + "\\right)";
            case EI -> "\\operatorname{Ei}\\left(" + arg + "\\right)";
            case LI -> "\\operatorname{li}\\left(" + arg + "\\right)";
            case ASIN -> "\\arcsin\\left(" + arg + "\\right)";
            case ACOS -> "\\arccos\\left(" + arg + "\\right)";
            case ATAN -> "\\arctan\\left(" + arg + "\\right)";
            default -> "\\" + f.fn().symbol() + "\\left(" + arg + "\\right)";
        };
    }

    private static String sum(Expr.Add sum) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Expr term : Display.sortedTerms(sum.terms())) {
            boolean negative = Display.isNegative(term);
            Expr shown = negative ? Expr.neg(term) : term;
            String text = print(shown, negative ? MUL : ADD);
            if (first) sb.append(negative ? "-" : "").append(text);
            else sb.append(negative ? " - " : " + ").append(text);
            first = false;
        }
        return sb.toString();
    }

    private static String product(Display.Fraction f) {
        String numerator = f.numerator().isEmpty() ? "1" : join(f.numerator());
        String body = numerator;
        if (f.hasDenominator()) {
            String denominator = f.denominator().size() == 1 ? print(f.denominator().get(0)) : join(f.denominator());
            body = "\\frac{" + numerator + "}{" + denominator + "}";
        }
        return (f.negative() ? "-" : "") + body;
    }

    /** Une factores por yuxtaposición, salvo cuando el siguiente empieza por un dígito. */
    private static String join(List<Expr> factors) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < factors.size(); i++) {
            String text = print(factors.get(i), MUL + 1);
            if (i > 0) sb.append(Character.isDigit(text.charAt(0)) ? " \\cdot " : " ");
            sb.append(text);
        }
        return sb.toString();
    }

    private static String power(Expr.Pow p) {
        if (isReciprocal(p)) return product(Display.split(p));

        if (p.exponent() instanceof Expr.Num n && n.value().numerator().equals(java.math.BigInteger.ONE) && !n.value().isInteger()) {
            if (n.value().denominator().equals(java.math.BigInteger.TWO)) return "\\sqrt{" + print(p.base()) + "}";
            return "\\sqrt[" + n.value().denominator() + "]{" + print(p.base()) + "}";
        }
        return print(p.base(), ATOM) + "^{" + print(p.exponent()) + "}";
    }

    /** Si se puede poner antes de un {@code !} sin paréntesis: un nombre, una constante o un entero no negativo. */
    private static boolean isAtom(Expr e) {
        return e instanceof Expr.Sym || e instanceof Expr.Const || (e instanceof Expr.Num n && n.value().isInteger() && n.value().signum() >= 0);
    }
}
