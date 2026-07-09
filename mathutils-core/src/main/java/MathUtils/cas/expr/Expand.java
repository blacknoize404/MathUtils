package MathUtils.cas.expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Expansión de productos y potencias enteras de sumas (propiedad distributiva).
 */
public final class Expand {

    /** Máximo de términos intermedios antes de renunciar a expandir. */
    private static final int MAX_TERMS = 5_000;
    private static final int MAX_POWER = 60;

    private Expand() {
    }

    /**
     * Expande la expresión, incluyendo el interior de funciones y potencias.
     * Si el resultado tendría demasiados términos, esa parte se deja sin expandir.
     */
    public static Expr expand(Expr e) {
        return switch (e) {
            case Expr.Num n -> n;
            case Expr.Const c -> c;
            case Expr.Sym s -> s;
            case Expr.Func f -> Expr.func(f.fn(), expand(f.arg()));
            case Expr.Add a -> Expr.add(a.terms().stream().map(Expand::expand).toList());
            case Expr.Mul m -> expandProduct(m.factors().stream().map(Expand::expand).toList());
            case Expr.Pow p -> expandPower(expand(p.base()), expand(p.exponent()));
        };
    }

    private static Expr expandPower(Expr base, Expr exponent) {
        if (base instanceof Expr.Add && exponent instanceof Expr.Num n && n.value().fitsInt()) {
            int k = n.value().intValueExact();
            if (k >= 2 && k <= MAX_POWER) {
                List<Expr> factors = new ArrayList<>();
                for (int i = 0; i < k; i++) factors.add(base);
                return expandProduct(factors);
            }
        }
        return Expr.pow(base, exponent);
    }

    private static Expr expandProduct(List<Expr> factors) {
        List<Expr> terms = List.of(Expr.ONE);
        for (Expr factor : factors) {
            List<Expr> options = factor instanceof Expr.Add a ? a.terms() : List.of(factor);
            if ((long) terms.size() * options.size() > MAX_TERMS) return Expr.mul(factors);

            List<Expr> next = new ArrayList<>(terms.size() * options.size());
            for (Expr t : terms) for (Expr o : options) next.add(Expr.mul(t, o));
            terms = next;
        }
        return Expr.add(terms);
    }
}
