package barnesinside.mathutils.cas.integral;

import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.poly.Polynomial;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Utilidades para integrandos con radicales: localizar el radicando, reescribir sus potencias en
 * otra variable y deshacer una sustitución trigonométrica.
 */
final class Radicals {

    /** Radicando lineal {@code base} con las raíces de índice {@code n} (mínimo común múltiplo). */
    record LinearRadical(Expr base, int n, Polynomial polynomial) {
    }

    /** Radicando cuadrático {@code base} con raíces cuadradas (exponentes semienteros). */
    record QuadraticRadical(Expr base, Polynomial polynomial) {
    }

    private Radicals() {
    }

    // ================================================================ Localización

    /** Bases de potencias de exponente no entero que dependen de {@code v}, con sus denominadores. */
    private static Map<Expr, Set<BigInteger>> radicands(Expr f, String v) {
        Map<Expr, Set<BigInteger>> found = new LinkedHashMap<>();
        collect(f, v, found);
        return found;
    }

    private static void collect(Expr e, String v, Map<Expr, Set<BigInteger>> out) {
        if (e.isFreeOf(v)) return;
        switch (e) {
            case Expr.Add a -> a.terms().forEach(t -> collect(t, v, out));
            case Expr.Mul m -> m.factors().forEach(t -> collect(t, v, out));
            case Expr.Pow p -> {
                if (p.exponent() instanceof Expr.Num n && !n.value().isInteger()) {
                    out.computeIfAbsent(p.base(), k -> new LinkedHashSet<>()).add(n.value().denominator());
                }
                collect(p.base(), v, out);
                collect(p.exponent(), v, out);
            }
            case Expr.Func fn -> collect(fn.arg(), v, out);
            default -> { }
        }
    }

    /** El único radicando lineal de {@code f} si lo hay (una sola base, de primer grado). */
    static Optional<LinearRadical> linear(Expr f, String v) {
        Map<Expr, Set<BigInteger>> radicands = radicands(f, v);
        if (radicands.size() != 1) return Optional.empty();
        Map.Entry<Expr, Set<BigInteger>> only = radicands.entrySet().iterator().next();
        Optional<Polynomial> p = Polynomial.from(only.getKey(), v);
        if (p.isEmpty() || p.get().degree() != 1) return Optional.empty();

        BigInteger n = BigInteger.ONE;
        for (BigInteger d : only.getValue()) n = n.divide(n.gcd(d)).multiply(d);
        if (n.bitLength() > 5) return Optional.empty();
        return Optional.of(new LinearRadical(only.getKey(), n.intValueExact(), p.get()));
    }

    /** El único radicando cuadrático de {@code f} con raíces cuadradas, si lo hay. */
    static Optional<QuadraticRadical> quadratic(Expr f, String v) {
        Map<Expr, Set<BigInteger>> radicands = radicands(f, v);
        if (radicands.size() != 1) return Optional.empty();
        Map.Entry<Expr, Set<BigInteger>> only = radicands.entrySet().iterator().next();
        if (!only.getValue().equals(Set.of(BigInteger.TWO))) return Optional.empty();
        Optional<Polynomial> p = Polynomial.from(only.getKey(), v);
        if (p.isEmpty() || p.get().degree() != 2) return Optional.empty();
        return Optional.of(new QuadraticRadical(only.getKey(), p.get()));
    }

    // ================================================================ Reescritura

    /**
     * Reemplaza cada potencia {@code base^e} (y la propia {@code base}, con {@code e = 1}) por
     * {@code power.apply(e)}.
     */
    static Expr mapPowers(Expr e, Expr base, Function<Rational, Expr> power) {
        if (e.equals(base)) return power.apply(Rational.ONE);
        return switch (e) {
            case Expr.Num n -> n;
            case Expr.Const c -> c;
            case Expr.Sym s -> s;
            case Expr.Add a -> Expr.add(a.terms().stream().map(t -> mapPowers(t, base, power)).toList());
            case Expr.Mul m -> Expr.mul(m.factors().stream().map(t -> mapPowers(t, base, power)).toList());
            case Expr.Pow p -> {
                if (p.base().equals(base) && p.exponent() instanceof Expr.Num n) yield power.apply(n.value());
                yield Expr.pow(mapPowers(p.base(), base, power), mapPowers(p.exponent(), base, power));
            }
            case Expr.Func fn -> Expr.func(fn.fn(), mapPowers(fn.arg(), base, power));
        };
    }

    /**
     * Deshace una sustitución trigonométrica: las funciones de {@code t} se reemplazan por su valor
     * en la variable original y la propia {@code t} por el ángulo. Los ángulos múltiplos de
     * {@code t} se expanden antes.
     */
    static Expr undo(Expr e, Expr t, Expr sin, Expr cos, Expr angle) {
        Expr expanded = expandMultiples(e, t);
        return replaceTrig(expanded, t, sin, cos, angle);
    }

    private static Expr replaceTrig(Expr e, Expr t, Expr sin, Expr cos, Expr angle) {
        if (e.equals(t)) return angle;
        return switch (e) {
            case Expr.Num n -> n;
            case Expr.Const c -> c;
            case Expr.Sym s -> s;
            case Expr.Add a -> Expr.add(a.terms().stream().map(x -> replaceTrig(x, t, sin, cos, angle)).toList());
            case Expr.Mul m -> Expr.mul(m.factors().stream().map(x -> replaceTrig(x, t, sin, cos, angle)).toList());
            case Expr.Pow p -> Expr.pow(replaceTrig(p.base(), t, sin, cos, angle),
                    replaceTrig(p.exponent(), t, sin, cos, angle));
            case Expr.Func fn -> {
                if (fn.arg().equals(t)) {
                    yield switch (fn.fn()) {
                        case SIN -> sin;
                        case COS -> cos;
                        case TAN -> Expr.div(sin, cos);
                        case COT -> Expr.div(cos, sin);
                        case SEC -> Expr.div(Expr.ONE, cos);
                        case CSC -> Expr.div(Expr.ONE, sin);
                        default -> Expr.func(fn.fn(), replaceTrig(fn.arg(), t, sin, cos, angle));
                    };
                }
                yield Expr.func(fn.fn(), replaceTrig(fn.arg(), t, sin, cos, angle));
            }
        };
    }

    /** {@code sin(kt)} y {@code cos(kt)} con {@code k} entero de 2 a 8, en función de {@code sin t} y {@code cos t}. */
    private static Expr expandMultiples(Expr e, Expr t) {
        return switch (e) {
            case Expr.Num n -> n;
            case Expr.Const c -> c;
            case Expr.Sym s -> s;
            case Expr.Add a -> Expr.add(a.terms().stream().map(x -> expandMultiples(x, t)).toList());
            case Expr.Mul m -> Expr.mul(m.factors().stream().map(x -> expandMultiples(x, t)).toList());
            case Expr.Pow p -> Expr.pow(expandMultiples(p.base(), t), expandMultiples(p.exponent(), t));
            case Expr.Func fn -> {
                int k = multiple(fn.arg(), t);
                if (k >= 2 && (fn.fn() == Fn.SIN || fn.fn() == Fn.COS)) {
                    Expr s1 = Expr.func(Fn.SIN, t);
                    Expr c1 = Expr.func(Fn.COS, t);
                    Expr sk = s1;
                    Expr ck = c1;
                    for (int i = 2; i <= k; i++) {
                        Expr nextSin = Expr.add(Expr.mul(sk, c1), Expr.mul(ck, s1));
                        Expr nextCos = Expr.sub(Expr.mul(ck, c1), Expr.mul(sk, s1));
                        sk = nextSin;
                        ck = nextCos;
                    }
                    yield fn.fn() == Fn.SIN ? sk : ck;
                }
                yield Expr.func(fn.fn(), expandMultiples(fn.arg(), t));
            }
        };
    }

    /** {@code k} si {@code arg = k·t} con {@code k} entero de 2 a 8; 0 en otro caso. */
    private static int multiple(Expr arg, Expr t) {
        if (arg instanceof Expr.Mul m && m.factors().size() == 2 && m.factors().get(1).equals(t)
                && m.factors().get(0) instanceof Expr.Num n && n.value().isInteger() && n.value().fitsInt()) {
            int k = n.value().intValueExact();
            return k >= 2 && k <= 8 ? k : 0;
        }
        return 0;
    }
}
