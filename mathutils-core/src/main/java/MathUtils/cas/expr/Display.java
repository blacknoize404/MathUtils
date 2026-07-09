package MathUtils.cas.expr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utilidades comunes a los impresores: orden de visualización y separación de fracciones.
 */
final class Display {

    private Display() {
    }

    /**
     * Resultado de separar un producto en parte entera de numerador y denominador:
     * {@code sign · numerator / denominator}.
     */
    record Fraction(boolean negative, List<Expr> numerator, List<Expr> denominator) {
        boolean hasDenominator() {
            return !denominator.isEmpty();
        }
    }

    /** Un número negativo, o un producto con coeficiente negativo, se imprime con signo menos. */
    static boolean isNegative(Expr e) {
        if (e instanceof Expr.Num n) return n.value().signum() < 0;
        return e instanceof Expr.Mul m && m.factors().get(0) instanceof Expr.Num n && n.value().signum() < 0;
    }

    /** Separa un producto (o una potencia negativa) en numerador y denominador positivos. */
    static Fraction split(Expr e) {
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
        return new Fraction(negative, numerator, denominator);
    }

    /** Ordena los términos de una suma para mostrarlos: mayor grado primero, constantes al final. */
    static List<Expr> sortedTerms(List<Expr> terms) {
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
