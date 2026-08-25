package barnesinside.mathutils.cas.expr;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Expansión de productos (propiedad distributiva) y de potencias enteras de sumas
 * (binomio de Newton y teorema multinomial).
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
        if (base instanceof Expr.Add a && exponent instanceof Expr.Num n && n.value().fitsInt()) {
            int k = n.value().intValueExact();
            if (k >= 2 && k <= MAX_POWER) {
                Expr expanded = multinomial(a.terms(), k);
                if (expanded != null) return expanded;
            }
        }
        return Expr.pow(base, exponent);
    }

    /**
     * Teorema multinomial: {@code (t1 + ... + tm)^k} es la suma de
     * {@code k!/(e1!...em!) · t1^e1 ... tm^em} con {@code e1 + ... + em = k}.
     * Con dos términos es el binomio de Newton. Devuelve {@code null} si habría demasiados términos.
     */
    private static Expr multinomial(List<Expr> terms, int k) {
        int m = terms.size();
        if (compositions(k, m).compareTo(BigInteger.valueOf(MAX_TERMS)) > 0) return null;

        List<Expr> out = new ArrayList<>();
        collect(terms, k, 0, new int[m], factorial(k), out);
        return Expr.add(out);
    }

    private static void collect(List<Expr> terms, int left, int index, int[] exponents, BigInteger numerator, List<Expr> out) {
        int m = terms.size();
        if (index == m - 1) {
            exponents[index] = left;
            BigInteger denominator = BigInteger.ONE;
            for (int e : exponents) denominator = denominator.multiply(factorial(e));

            List<Expr> factors = new ArrayList<>();
            factors.add(Expr.num(Rational.of(numerator.divide(denominator))));
            for (int i = 0; i < m; i++) if (exponents[i] > 0) factors.add(Expr.pow(terms.get(i), exponents[i]));
            out.add(Expr.mul(factors));
            return;
        }
        for (int e = left; e >= 0; e--) {
            exponents[index] = e;
            collect(terms, left - e, index + 1, exponents, numerator, out);
        }
    }

    /** Número de formas de repartir {@code k} unidades entre {@code m} términos: C(k + m - 1, m - 1). */
    private static BigInteger compositions(int k, int m) {
        BigInteger result = BigInteger.ONE;
        for (int i = 1; i < m; i++) {
            result = result.multiply(BigInteger.valueOf(k + i)).divide(BigInteger.valueOf(i));
        }
        return result;
    }

    private static BigInteger factorial(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) result = result.multiply(BigInteger.valueOf(i));
        return result;
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
