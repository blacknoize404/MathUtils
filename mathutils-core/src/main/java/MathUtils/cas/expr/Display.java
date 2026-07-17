package MathUtils.cas.expr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utilidades de presentación compartidas por los impresores y por cualquier interfaz que dibuje
 * expresiones: orden natural de términos y factores y separación de un producto en fracción.
 */
public final class Display {

    private Display() {
    }

    /**
     * Resultado de separar un producto en parte entera de numerador y denominador:
     * {@code sign · numerator / denominator}.
     */
    public record Fraction(boolean negative, List<Expr> numerator, List<Expr> denominator) {
        public boolean hasDenominator() {
            return !denominator.isEmpty();
        }
    }

    /** Un número negativo, o un producto con coeficiente negativo, se imprime con signo menos. */
    public static boolean isNegative(Expr e) {
        if (e instanceof Expr.Num n) return n.value().signum() < 0;
        return e instanceof Expr.Mul m && m.factors().get(0) instanceof Expr.Num n && n.value().signum() < 0;
    }

    /** Separa un producto (o una potencia negativa) en numerador y denominador positivos. */
    public static Fraction split(Expr e) {
        List<Expr> factors = e instanceof Expr.Mul m ? m.factors() : List.of(e);

        boolean negative = false;
        List<Expr> numerator = new ArrayList<>();
        List<Expr> denominator = new ArrayList<>();

        for (Expr f : factors) {
            if (f instanceof Expr.Num n) {
                Rational v = n.value();
                if (v.signum() < 0) {
                    negative = !negative;
                    v = v.negate();
                }
                if (!v.numerator().equals(java.math.BigInteger.ONE)) numerator.add(new Expr.Num(Rational.of(v.numerator())));
                if (!v.denominator().equals(java.math.BigInteger.ONE)) denominator.add(new Expr.Num(Rational.of(v.denominator())));
            } else if (f instanceof Expr.Pow p && p.exponent() instanceof Expr.Num n && n.value().signum() < 0) {
                denominator.add(Expr.pow(p.base(), new Expr.Num(n.value().negate())));
            } else {
                numerator.add(f);
            }
        }
        return new Fraction(negative, sortedFactors(numerator), sortedFactors(denominator));
    }

    /**
     * Ordena los factores de un producto como se escriben a mano: números, constantes, variables
     * y potencias de variables, otras expresiones, funciones y, al final, exponenciales.
     */
    public static List<Expr> sortedFactors(List<Expr> factors) {
        List<Expr> sorted = new ArrayList<>(factors);
        sorted.sort(Comparator.comparingInt(Display::factorRank).thenComparing(ExprOrder.INSTANCE));
        return sorted;
    }

    private static int factorRank(Expr e) {
        return switch (e) {
            case Expr.Num n -> 0;
            case Expr.Const c -> 1;
            case Expr.Sym s -> 2;
            case Expr.Pow p -> p.base() instanceof Expr.Sym || p.base() instanceof Expr.Const && !p.base().equals(Expr.E)
                    ? 2
                    : (p.base().equals(Expr.E) ? 5 : 4);
            case Expr.Add a -> 3;
            case Expr.Mul m -> 3;
            case Expr.Func f -> 4;
        };
    }

    /** Ordena los términos de una suma para mostrarlos: mayor grado primero, constantes al final. */
    public static List<Expr> sortedTerms(List<Expr> terms) {
        List<Expr> sorted = new ArrayList<>(terms);
        sorted.sort(Comparator
                .comparingDouble((Expr t) -> -degree(t))
                .thenComparing(ExprOrder.INSTANCE));
        return sorted;
    }

    /** Grado aproximado para ordenar: los números van al final y lo no polinómico cuenta como grado 0,5. */
    private static double degree(Expr e) {
        return switch (e) {
            case Expr.Num n -> -1e9;
            case Expr.Const c -> 0;
            case Expr.Sym s -> 1;
            case Expr.Func f -> 0.5;
            case Expr.Pow p -> p.exponent() instanceof Expr.Num n
                    ? n.value().doubleValue() * Math.max(1, degree(p.base()) > 0 ? degree(p.base()) : 0.5)
                    : 0.5;
            case Expr.Mul m -> m.factors().stream()
                    .filter(f -> !(f instanceof Expr.Num))
                    .mapToDouble(Display::degree).sum();
            case Expr.Add a -> a.terms().stream().mapToDouble(Display::degree).max().orElse(0);
        };
    }
}
