package barnesinside.mathutils.cas.expr;

import java.math.BigInteger;
import java.util.List;

/**
 * Impresor en ASCIIMath, la notación de texto que los visores de fórmulas convierten en MathML.
 */
final class ExprAsciiMath {

    private static final int ADD = 1;
    private static final int MUL = 2;
    private static final int POW = 3;
    private static final int ATOM = 4;

    private ExprAsciiMath() {
    }

    static String print(Expr e) {
        return print(e, 0);
    }

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
        return p.exponent() instanceof Expr.Num n && n.value().numerator().equals(BigInteger.ONE)
                && n.value().denominator().signum() > 0 && !n.value().isInteger();
    }

    private static String raw(Expr e) {
        return switch (e) {
            case Expr.Num n -> n.value().toString();
            case Expr.Const c -> switch (c.kind()) {
                case PI -> "pi";
                case E -> "e";
                case INFINITY -> "oo";
            };
            case Expr.Sym s -> name(s.name());
            case Expr.Func f -> function(f);
            case Expr.Add a -> sum(a);
            case Expr.Mul m -> product(Display.split(m));
            case Expr.Pow p -> power(p);
        };
    }

    /**
     * El nombre de una variable. {@code a_1} se queda con su subíndice ({@code a_1}, y {@code a_(12)} si el subíndice tiene varias
     * cifras o letras); los nombres de varias letras se citan, porque si no ASCIIMath los lee como letras sueltas.
     */
    private static String name(String name) {
        int underscore = name.indexOf('_');
        if (underscore > 0 && underscore < name.length() - 1) {
            String subscript = name.substring(underscore + 1);
            String shown = subscript.chars().allMatch(Character::isDigit) ? subscript : name(subscript);
            return name(name.substring(0, underscore)) + "_" + (shown.length() == 1 ? shown : "(" + shown + ")");
        }
        return name.length() == 1 ? name : "\"" + name + "\"";
    }

    private static String function(Expr.Func f) {
        String arg = print(f.arg());
        return switch (f.fn()) {
            case FACTORIAL -> (isAtom(f.arg()) ? arg : "(" + arg + ")") + "!";
            case ABS -> "abs(" + arg + ")";
            case ASIN -> "arcsin(" + arg + ")";
            case ACOS -> "arccos(" + arg + ")";
            case ATAN -> "arctan(" + arg + ")";
            case SIN, COS, TAN, COT, SEC, CSC, SINH, COSH, TANH, LN -> f.fn().symbol() + "(" + arg + ")";
            default -> "\"" + f.fn().symbol() + "\"(" + arg + ")";
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
            String denominator = f.denominator().size() == 1 ? print(f.denominator().get(0), MUL + 1) : join(f.denominator());
            body = group(numerator) + "/" + group(denominator);
        }
        return (f.negative() ? "-" : "") + body;
    }

    /** Une factores por yuxtaposición, salvo cuando el siguiente empieza por un dígito. */
    private static String join(List<Expr> factors) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < factors.size(); i++) {
            String text = print(factors.get(i), MUL + 1);
            if (i > 0) sb.append(Character.isDigit(text.charAt(0)) ? " cdot " : " ");
            sb.append(text);
        }
        return sb.toString();
    }

    /** Entre paréntesis si no es una sola letra, un número, una función o algo que ya los lleva. */
    private static String group(String text) {
        boolean simple = text.matches("[A-Za-z0-9.]+|[A-Za-z]+\\([^()]*\\)");
        return simple || isWrapped(text) ? text : "(" + text + ")";
    }

    /** El primer paréntesis se cierra al final: {@code (a + b)} sí, {@code (a)(b)} no. */
    private static boolean isWrapped(String text) {
        if (!text.startsWith("(") || !text.endsWith(")")) return false;
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '(') depth++;
            else if (text.charAt(i) == ')' && --depth == 0) return i == text.length() - 1;
        }
        return false;
    }

    private static String power(Expr.Pow p) {
        if (isReciprocal(p)) return product(Display.split(p));

        if (isRoot(p)) {
            BigInteger index = ((Expr.Num) p.exponent()).value().denominator();
            if (index.equals(BigInteger.TWO)) return "sqrt(" + print(p.base()) + ")";
            return "root(" + index + ")(" + print(p.base()) + ")";
        }
        return print(p.base(), ATOM) + "^" + exponent(p.exponent());
    }

    private static String exponent(Expr e) {
        boolean simple = e instanceof Expr.Sym s && s.name().length() == 1 || e instanceof Expr.Const
                || (e instanceof Expr.Num n && n.value().isInteger() && n.value().signum() >= 0);
        return simple ? raw(e) : "(" + raw(e) + ")";
    }

    /** Si se puede poner antes de un {@code !} sin paréntesis: un nombre, una constante o un entero no negativo. */
    private static boolean isAtom(Expr e) {
        return e instanceof Expr.Sym || e instanceof Expr.Const || (e instanceof Expr.Num n && n.value().isInteger() && n.value().signum() >= 0);
    }
}
