package MathUtils.cas.expr;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Impresor en texto plano. La salida es interpretable por {@link ExprParser}
 * (la expresión impresa y reinterpretada da la misma forma canónica).
 */
final class ExprPrinter {

    private static final int ADD = 1;
    private static final int MUL = 2;
    private static final int POW = 3;
    private static final int ATOM = 4;

    private ExprPrinter() {
    }

    static String print(Expr e) {
        return print(e, 0);
    }

    /** Imprime añadiendo paréntesis si la precedencia de la expresión es menor que {@code required}. */
    private static String print(Expr e, int required) {
        String text = raw(e);
        return precedence(e) < required ? "(" + text + ")" : text;
    }

    private static int precedence(Expr e) {
        return switch (e) {
            case Expr.Num n -> n.value().signum() < 0 ? ADD : (n.value().isInteger() ? ATOM : MUL);
            case Expr.Const c -> ATOM;
            case Expr.Sym s -> ATOM;
            case Expr.Func f -> ATOM;
            case Expr.Add a -> ADD;
            case Expr.Mul m -> MUL;
            case Expr.Pow p -> isRoot(p) ? ATOM : (isReciprocal(p) ? MUL : POW);
        };
    }

    private static boolean isReciprocal(Expr.Pow p) {
        return p.exponent() instanceof Expr.Num n && n.value().signum() < 0;
    }

    private static boolean isRoot(Expr.Pow p) {
        if (!(p.exponent() instanceof Expr.Num n)) return false;
        return n.value().equals(Rational.of(1, 2)) || n.value().equals(Rational.of(1, 3));
    }

    private static String raw(Expr e) {
        return switch (e) {
            case Expr.Num n -> n.value().toString();
            case Expr.Const c -> switch (c.kind()) {
                case PI -> "pi";
                case E -> "e";
                case INFINITY -> "oo";
            };
            case Expr.Sym s -> s.name();
            case Expr.Func f -> f.fn().symbol() + "(" + print(f.arg()) + ")";
            case Expr.Add a -> printSum(a);
            case Expr.Mul m -> printProduct(Display.split(m));
            case Expr.Pow p -> printPower(p);
        };
    }

    private static String printSum(Expr.Add sum) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Expr term : Display.sortedTerms(sum.terms())) {
            boolean negative = Display.isNegative(term);
            Expr shown = negative ? Expr.neg(term) : term;
            if (first) {
                sb.append(negative ? "-" : "").append(print(shown, negative ? MUL : ADD));
            } else {
                sb.append(negative ? " - " : " + ").append(print(shown, negative ? MUL : ADD));
            }
            first = false;
        }
        return sb.toString();
    }

    private static String printProduct(Display.Fraction f) {
        String numerator = f.numerator().isEmpty()
                ? "1"
                : f.numerator().stream().map(x -> print(x, MUL + 1)).collect(Collectors.joining("*"));
        // Un numerador con una única potencia o un producto largo se deja tal cual
        String body = numerator;
        if (f.hasDenominator()) {
            String denominator = joinDenominator(f.denominator());
            body = numerator + "/" + denominator;
        }
        return (f.negative() ? "-" : "") + body;
    }

    private static String joinDenominator(List<Expr> denominator) {
        if (denominator.size() == 1) return print(denominator.get(0), MUL + 1);
        return "(" + denominator.stream().map(x -> print(x, MUL + 1)).collect(Collectors.joining("*")) + ")";
    }

    private static String printPower(Expr.Pow p) {
        if (isReciprocal(p)) return printProduct(Display.split(p));

        Rational r = p.exponent() instanceof Expr.Num n ? n.value() : null;
        if (r != null && r.equals(Rational.of(1, 2))) return "sqrt(" + print(p.base()) + ")";
        if (r != null && r.equals(Rational.of(1, 3))) return "cbrt(" + print(p.base()) + ")";

        String base = print(p.base(), ATOM);
        return base + "^" + exponent(p.exponent());
    }

    /** Los exponentes simples van sin paréntesis; el resto, entre paréntesis. */
    private static String exponent(Expr e) {
        boolean simple = e instanceof Expr.Sym || e instanceof Expr.Const
                || (e instanceof Expr.Num n && n.value().isInteger() && n.value().signum() >= 0);
        return simple ? raw(e) : "(" + raw(e) + ")";
    }
}
