package barnesinside.mathutils.cas.expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones algebraicas sobre expresiones usadas por los motores de límites e integrales:
 * numerador y denominador, combinación de fracciones y sustitución de subexpresiones.
 */
public final class Algebra {

    /** Cociente {@code numerator / denominator} con ambos términos como expresiones. */
    public record Fraction(Expr numerator, Expr denominator) {
    }

    private Algebra() {
    }

    /**
     * Separa una expresión en numerador y denominador: los factores con exponente negativo
     * pasan al denominador. Una suma se considera un único numerador.
     */
    public static Fraction fraction(Expr e) {
        Display.Fraction split = Display.split(e);
        Expr numerator = split.numerator().isEmpty() ? Expr.ONE : Expr.mul(split.numerator());
        if (split.negative()) numerator = Expr.neg(numerator);
        Expr denominator = split.denominator().isEmpty() ? Expr.ONE : Expr.mul(split.denominator());
        return new Fraction(numerator, denominator);
    }

    /**
     * Combina una suma de fracciones en una sola {@code N/D} con el numerador expandido. Si la
     * expresión no es una suma con denominadores, se devuelve igual.
     */
    public static Expr together(Expr e) {
        if (!(e instanceof Expr.Add sum)) return e;

        List<Fraction> parts = new ArrayList<>();
        List<Expr> denominators = new ArrayList<>();
        for (Expr term : sum.terms()) {
            Fraction f = fraction(term);
            parts.add(f);
            if (!f.denominator().isOne() && !denominators.contains(f.denominator())) denominators.add(f.denominator());
        }
        if (denominators.isEmpty()) return e;

        Expr common = Expr.mul(denominators);
        List<Expr> numerators = new ArrayList<>();
        for (Fraction f : parts) {
            List<Expr> factors = new ArrayList<>();
            factors.add(f.numerator());
            for (Expr d : denominators) if (!d.equals(f.denominator())) factors.add(d);
            numerators.add(Expr.mul(factors));
        }
        return Expr.div(Expand.expand(Expr.add(numerators)), common);
    }

    /**
     * Sustituye todas las apariciones exactas de {@code target} por {@code replacement}.
     * La comparación es estructural sobre la forma canónica.
     */
    public static Expr replace(Expr e, Expr target, Expr replacement) {
        if (e.equals(target)) return replacement;
        return switch (e) {
            case Expr.Num n -> n;
            case Expr.Const c -> c;
            case Expr.Sym s -> s;
            case Expr.Add a -> Expr.add(a.terms().stream().map(t -> replace(t, target, replacement)).toList());
            case Expr.Mul m -> Expr.mul(m.factors().stream().map(t -> replace(t, target, replacement)).toList());
            case Expr.Pow p -> Expr.pow(replace(p.base(), target, replacement), replace(p.exponent(), target, replacement));
            case Expr.Func f -> Expr.func(f.fn(), replace(f.arg(), target, replacement));
        };
    }

    /** Sumandos de una suma (o la propia expresión si no es una suma). */
    public static List<Expr> terms(Expr e) {
        return e instanceof Expr.Add a ? a.terms() : List.of(e);
    }

    /** Factores de un producto (o la propia expresión si no es un producto). */
    public static List<Expr> factors(Expr e) {
        return e instanceof Expr.Mul m ? m.factors() : List.of(e);
    }

    /** Indica si la expresión contiene {@code target} como subexpresión. */
    public static boolean contains(Expr e, Expr target) {
        if (e.equals(target)) return true;
        return switch (e) {
            case Expr.Num n -> false;
            case Expr.Const c -> false;
            case Expr.Sym s -> false;
            case Expr.Add a -> a.terms().stream().anyMatch(t -> contains(t, target));
            case Expr.Mul m -> m.factors().stream().anyMatch(t -> contains(t, target));
            case Expr.Pow p -> contains(p.base(), target) || contains(p.exponent(), target);
            case Expr.Func f -> contains(f.arg(), target);
        };
    }
}
