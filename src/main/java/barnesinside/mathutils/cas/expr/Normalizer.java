package barnesinside.mathutils.cas.expr;

import barnesinside.mathutils.discrete.NumberTheory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Constructores inteligentes de {@link Expr}: reciben operandos ya normalizados y devuelven
 * la forma canónica. Ver la documentación de {@link Expr} para las convenciones.
 */
final class Normalizer {

    /** Límite para factorizar enteros al simplificar radicales (división de prueba). */
    private static final BigInteger FACTOR_LIMIT = BigInteger.valueOf(1_000_000_000_000L);
    private static final int MAX_EXACT_POWER = 2000;
    private static final int MAX_POWER_BITS = 20_000;

    private Normalizer() {
    }

    // ================================================================ Suma

    static Expr add(List<Expr> terms) {
        List<Expr> flat = new ArrayList<>();
        for (Expr t : terms) {
            if (t instanceof Expr.Add a) flat.addAll(a.terms());
            else flat.add(t);
        }

        Rational constant = Rational.ZERO;
        Map<Expr, Rational> coefficients = new LinkedHashMap<>();
        for (Expr t : flat) {
            if (t instanceof Expr.Num n) {
                constant = constant.add(n.value());
                continue;
            }
            Rational k = Rational.ONE;
            Expr core = t;
            if (t instanceof Expr.Mul m && m.factors().get(0) instanceof Expr.Num n) {
                k = n.value();
                List<Expr> rest = m.factors().subList(1, m.factors().size());
                core = rest.size() == 1 ? rest.get(0) : new Expr.Mul(rest);
            }
            coefficients.merge(core, k, Rational::add);
        }

        List<Expr> out = new ArrayList<>();
        for (Map.Entry<Expr, Rational> entry : coefficients.entrySet()) {
            if (!entry.getValue().isZero()) out.add(times(entry.getValue(), entry.getKey()));
        }
        if (!constant.isZero()) out.add(new Expr.Num(constant));

        if (out.isEmpty()) return Expr.ZERO;
        if (out.size() == 1) return out.get(0);
        out.sort(ExprOrder.INSTANCE);
        return new Expr.Add(out);
    }

    /** {@code k · core} sin volver a normalizar (core ya es un producto o un factor normalizado). */
    private static Expr times(Rational k, Expr core) {
        if (k.isOne()) return core;
        List<Expr> factors = new ArrayList<>();
        factors.add(new Expr.Num(k));
        if (core instanceof Expr.Mul m) factors.addAll(m.factors());
        else factors.add(core);
        return new Expr.Mul(factors);
    }

    // ================================================================ Producto

    static Expr mul(List<Expr> factors) {
        List<Expr> flat = new ArrayList<>();
        for (Expr f : factors) {
            if (f instanceof Expr.Mul m) flat.addAll(m.factors());
            else flat.add(f);
        }

        Rational k = Rational.ONE;
        Map<Expr, List<Expr>> exponents = new LinkedHashMap<>();
        Map<Expr, Expr> original = new LinkedHashMap<>();
        boolean hasInfinity = false;

        for (Expr f : flat) {
            if (f instanceof Expr.Num n) {
                k = k.multiply(n.value());
                continue;
            }
            if (f instanceof Expr.Const c && c.kind() == Expr.Const.Kind.INFINITY) hasInfinity = true;

            Expr base = f;
            Expr exponent = Expr.ONE;
            if (f instanceof Expr.Pow p) {
                base = p.base();
                exponent = p.exponent();
            }
            exponents.computeIfAbsent(base, x -> new ArrayList<>()).add(exponent);
            original.putIfAbsent(base, f);
        }

        if (k.isZero() && !hasInfinity) return Expr.ZERO;

        List<Expr> out = new ArrayList<>();
        boolean remerge = false;
        for (Map.Entry<Expr, List<Expr>> entry : exponents.entrySet()) {
            List<Expr> es = entry.getValue();
            Expr factor;
            if (es.size() == 1) {
                factor = original.get(entry.getKey());
            } else {
                Expr exponent = add(es);
                factor = exponent.isOne() ? entry.getKey() : pow(entry.getKey(), exponent);
            }

            if (factor instanceof Expr.Num n) {
                k = k.multiply(n.value());
                remerge = true;
            } else if (factor instanceof Expr.Mul m) {
                out.addAll(m.factors());
                remerge = true;
            } else {
                out.add(factor);
            }
        }

        if (remerge) {
            List<Expr> again = new ArrayList<>(out);
            again.add(new Expr.Num(k));
            return mulNoRemerge(again);
        }
        return finishMul(k, out);
    }

    /** Segunda pasada tras un re-aplanado; agrupa de nuevo hasta que no haya cambios. */
    private static Expr mulNoRemerge(List<Expr> factors) {
        return mul(factors);
    }

    private static Expr finishMul(Rational k, List<Expr> out) {
        if (out.isEmpty()) return new Expr.Num(k);
        out.sort(ExprOrder.INSTANCE);
        if (k.isOne()) return out.size() == 1 ? out.get(0) : new Expr.Mul(out);
        return times(k, out.size() == 1 ? out.get(0) : new Expr.Mul(out));
    }

    // ================================================================ Potencia

    static Expr pow(Expr base, Expr exponent) {
        if (exponent instanceof Expr.Num en) {
            Rational r = en.value();
            if (r.isZero()) return Expr.ONE;
            if (r.isOne()) return base;
            return powRational(base, r);
        }
        if (base.isOne()) return Expr.ONE;
        if (base.equals(Expr.E) && exponent instanceof Expr.Func f && f.fn() == Fn.LN) return f.arg();
        return new Expr.Pow(base, exponent);
    }

    private static Expr powRational(Expr base, Rational r) {
        Expr raw = new Expr.Pow(base, new Expr.Num(r));
        switch (base) {
            case Expr.Num n -> {
                return numberPow(n.value(), r);
            }
            case Expr.Pow p -> {
                if (r.isInteger()) return pow(p.base(), mul(List.of(p.exponent(), new Expr.Num(r))));
                if (p.exponent() instanceof Expr.Num pe) {
                    Rational inner = pe.value();
                    boolean evenInner = inner.isInteger() && inner.numerator().testBit(0) == false;
                    if (evenInner && !r.denominator().testBit(0)) {
                        // (x^2k)^(a/2b) = |x|^(2k·a/2b)
                        return pow(func(Fn.ABS, p.base()), new Expr.Num(inner.multiply(r)));
                    }
                    if (!inner.isInteger()) {
                        // Un exponente no entero ya implica base no negativa: se pueden multiplicar
                        return pow(p.base(), new Expr.Num(inner.multiply(r)));
                    }
                }
                return raw;
            }
            case Expr.Mul m -> {
                if (r.isInteger()) {
                    List<Expr> parts = new ArrayList<>();
                    for (Expr f : m.factors()) parts.add(pow(f, new Expr.Num(r)));
                    return mul(parts);
                }
                if (m.factors().get(0) instanceof Expr.Num k && k.value().signum() > 0) {
                    List<Expr> rest = m.factors().subList(1, m.factors().size());
                    Expr restExpr = rest.size() == 1 ? rest.get(0) : new Expr.Mul(rest);
                    return mul(List.of(numberPow(k.value(), r), pow(restExpr, new Expr.Num(r))));
                }
                return raw;
            }
            default -> {
                return raw;
            }
        }
    }

    /**
     * Potencia exacta de un racional. Extrae del radical los factores primos que se pueden
     * sacar ({@code 12^(1/2) = 2·3^(1/2)}) y racionaliza ({@code 2^(-1/2) = 2^(1/2)/2}).
     */
    private static Expr numberPow(Rational n, Rational r) {
        Expr raw = new Expr.Pow(new Expr.Num(n), new Expr.Num(r));

        if (n.isZero()) {
            if (r.signum() < 0) throw new ArithmeticException("División por cero");
            return Expr.ZERO;
        }
        if (n.isOne()) return Expr.ONE;

        if (r.isInteger()) {
            if (r.numerator().abs().compareTo(BigInteger.valueOf(MAX_EXACT_POWER)) > 0) return raw;
            int e = r.intValueExact();
            long bits = (long) Math.abs(e) * Math.max(n.numerator().bitLength(), n.denominator().bitLength());
            if (bits > MAX_POWER_BITS) return raw;
            return new Expr.Num(n.pow(e));
        }

        BigInteger q = r.denominator();
        if (n.signum() < 0) {
            if (!q.testBit(0)) return raw; // raíz par de un negativo: no es real
            Expr positive = numberPow(n.negate(), r);
            return r.numerator().testBit(0) ? mul(List.of(Expr.MINUS_ONE, positive)) : positive;
        }

        if (n.numerator().compareTo(FACTOR_LIMIT) > 0 || n.denominator().compareTo(FACTOR_LIMIT) > 0
                || q.bitLength() > 31 || r.numerator().bitLength() > 31) {
            return raw;
        }

        long p = r.numerator().longValueExact();
        long qq = q.longValueExact();

        Map<Long, Long> primeExponents = new LinkedHashMap<>();
        if (!n.numerator().equals(BigInteger.ONE)) {
            NumberTheory.factorize(n.numerator().longValueExact()).forEach((prime, e) -> primeExponents.merge(prime, (long) e, Long::sum));
        }
        if (!n.denominator().equals(BigInteger.ONE)) {
            NumberTheory.factorize(n.denominator().longValueExact()).forEach((prime, e) -> primeExponents.merge(prime, (long) -e, Long::sum));
        }

        Rational coefficient = Rational.ONE;
        List<Expr> radicals = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : primeExponents.entrySet()) {
            long total = Math.multiplyExact(entry.getValue(), p);
            long whole = Math.floorDiv(total, qq);
            long fraction = Math.floorMod(total, qq);
            if (Math.abs(whole) > MAX_EXACT_POWER) return raw;
            coefficient = coefficient.multiply(Rational.of(entry.getKey()).pow((int) whole));
            if (fraction != 0) {
                radicals.add(new Expr.Pow(new Expr.Num(Rational.of(entry.getKey())), new Expr.Num(Rational.of(fraction, qq))));
            }
        }

        List<Expr> parts = new ArrayList<>(radicals);
        parts.add(new Expr.Num(coefficient));
        return mul(parts);
    }

    // ================================================================ Funciones

    /**
     * El suelo, el techo o el redondeo de un valor sin variables, como entero exacto: de un racional se calcula con enteros; de lo demás
     * (π, √2, e…) se mira el valor decimal y solo se da si no cae cerca de un cambio de entero, para no equivocarse por redondeo.
     */
    private static Expr integerPart(Fn fn, Expr arg) {
        if (arg instanceof Expr.Num n) return new Expr.Num(Rational.of(integerOf(fn, n.value())));
        if (!arg.variables().isEmpty()) return null;
        double v;
        try {
            v = Eval.value(arg);
        } catch (RuntimeException unreadable) {
            return null;
        }
        if (!Double.isFinite(v) || Math.abs(v) > 1e15) return null;
        // El punto en que cambia el resultado: un entero para el suelo y el techo, una mitad para el redondeo
        double edge = fn == Fn.ROUND ? Math.abs(Math.abs(v - Math.floor(v)) - 0.5) : Math.abs(v - Math.rint(v));
        if (edge < 1e-9) return null;
        return new Expr.Num(Rational.of((long) (fn == Fn.FLOOR ? Math.floor(v) : fn == Fn.CEIL ? Math.ceil(v)
                : Math.signum(v) * Math.floor(Math.abs(v) + 0.5))));
    }

    private static java.math.BigInteger integerOf(Fn fn, Rational r) {
        return switch (fn) {
            case FLOOR -> floor(r);
            case CEIL -> floor(r.negate()).negate();
            default -> {
                // Redondeo: las mitades se van al más lejano de cero
                java.math.BigInteger magnitude = floor(r.abs().add(Rational.of(1, 2)));
                yield r.signum() < 0 ? magnitude.negate() : magnitude;
            }
        };
    }

    /** El mayor entero que no pasa del racional. */
    private static java.math.BigInteger floor(Rational r) {
        java.math.BigInteger[] qr = r.numerator().divideAndRemainder(r.denominator());
        return qr[1].signum() < 0 ? qr[0].subtract(java.math.BigInteger.ONE) : qr[0];
    }

    /** El mayor n del que se calcula n! de forma exacta. */
    private static final int MAX_FACTORIAL = 1000;

    static Expr func(Fn fn, Expr arg) {
        if ((fn.isOdd() || fn.isEven()) && isNegated(arg)) {
            Expr inner = func(fn, negate(arg));
            return fn.isOdd() ? mul(List.of(Expr.MINUS_ONE, inner)) : inner;
        }

        switch (fn) {
            case FLOOR, CEIL, ROUND -> {
                // Ya es un entero: el suelo, el techo y el redondeo de un entero (o de otro de ellos) no cambian nada
                if (arg instanceof Expr.Func inner && (inner.fn() == Fn.FLOOR || inner.fn() == Fn.CEIL || inner.fn() == Fn.ROUND)) return arg;
                Expr integer = integerPart(fn, arg);
                if (integer != null) return integer;
            }
            case FACTORIAL -> {
                // n! de un entero no negativo es exacto; el de un entero negativo no existe; el resto queda como está
                if (arg instanceof Expr.Num n && n.value().isInteger()) {
                    if (n.value().signum() < 0) throw new ArithmeticException("El factorial no está definido para enteros negativos");
                    if (n.value().numerator().bitLength() <= 10 && n.value().numerator().intValue() <= MAX_FACTORIAL) {
                        java.math.BigInteger product = java.math.BigInteger.ONE;
                        for (int i = 2; i <= n.value().numerator().intValue(); i++) product = product.multiply(java.math.BigInteger.valueOf(i));
                        return new Expr.Num(Rational.of(product));
                    }
                }
            }
            case DIGAMMA -> {
                // ψ(n) = H(n-1) − γ para los enteros positivos y ψ(1/2) = −γ − 2 ln 2
                if (arg instanceof Expr.Num n) {
                    Rational r = n.value();
                    if (r.isInteger() && r.signum() > 0 && r.numerator().bitLength() <= 10 && r.numerator().intValue() <= MAX_FACTORIAL) {
                        Rational harmonic = Rational.ZERO;
                        for (int i = 1; i < r.numerator().intValue(); i++) harmonic = harmonic.add(Rational.of(1, i));
                        return add(List.of(new Expr.Num(harmonic), mul(List.of(Expr.MINUS_ONE, Expr.EULER_GAMMA))));
                    }
                    if (r.equals(Rational.of(1, 2))) {
                        return add(List.of(mul(List.of(Expr.MINUS_ONE, Expr.EULER_GAMMA)),
                                mul(List.of(Expr.num(-2), new Expr.Func(Fn.LN, Expr.num(2))))));
                    }
                }
            }
            case LN -> {
                if (arg.isOne()) return Expr.ZERO;
                if (arg.equals(Expr.E)) return Expr.ONE;
                if (arg instanceof Expr.Pow p && p.base().equals(Expr.E)) return p.exponent();
            }
            case ABS -> {
                Expr simplified = simplifyAbs(arg);
                if (simplified != null) return simplified;
            }
            case ERF, ASIN, ATAN, SINH, TANH, SI -> {
                if (arg.isZero()) return Expr.ZERO;
            }
            case COSH -> {
                if (arg.isZero()) return Expr.ONE;
            }
            default -> { }
        }

        Expr exact = switch (fn) {
            case SIN, COS, TAN, COT, SEC, CSC -> exactTrig(fn, arg);
            case ASIN, ACOS, ATAN -> exactInverseTrig(fn, arg);
            default -> null;
        };
        if (exact != null) return exact;

        // Composiciones directas con la función inversa
        if (arg instanceof Expr.Func inner) {
            if (fn == Fn.SIN && inner.fn() == Fn.ASIN) return inner.arg();
            if (fn == Fn.COS && inner.fn() == Fn.ACOS) return inner.arg();
            if (fn == Fn.TAN && inner.fn() == Fn.ATAN) return inner.arg();
        }
        return new Expr.Func(fn, arg);
    }

    /** {@code -e}, distribuyendo el signo sobre las sumas para que el resultado deje de ser "negativo". */
    private static Expr negate(Expr e) {
        if (e instanceof Expr.Add a) {
            List<Expr> terms = new ArrayList<>();
            for (Expr t : a.terms()) terms.add(mul(List.of(Expr.MINUS_ONE, t)));
            return add(terms);
        }
        return mul(List.of(Expr.MINUS_ONE, e));
    }

    /** Indica si la expresión tiene coeficiente líder negativo (para aplicar paridad). */
    private static boolean isNegated(Expr e) {
        return switch (e) {
            case Expr.Num n -> n.value().signum() < 0;
            case Expr.Mul m -> m.factors().get(0) instanceof Expr.Num n && n.value().signum() < 0;
            case Expr.Add a -> a.terms().stream().allMatch(Normalizer::isNegated);
            default -> false;
        };
    }

    /** Indica si la expresión nunca es negativa (cuadrados, exponenciales, raíces y sumas o productos de ellos). */
    private static boolean isNonNegative(Expr e) {
        return switch (e) {
            case Expr.Num n -> n.value().signum() >= 0;
            case Expr.Const c -> c.kind() != Expr.Const.Kind.INFINITY;
            case Expr.Pow p -> {
                if (p.base().equals(Expr.E)) yield true;
                if (p.base() instanceof Expr.Num b && b.value().signum() > 0) yield true;
                if (!(p.exponent() instanceof Expr.Num n)) yield false;
                Rational r = n.value();
                if (r.isInteger()) yield !r.numerator().testBit(0) || isNonNegative(p.base());
                yield !r.denominator().testBit(0) || isNonNegative(p.base());
            }
            case Expr.Func f -> f.fn() == Fn.ABS || f.fn() == Fn.COSH;
            case Expr.Add a -> a.terms().stream().allMatch(Normalizer::isNonNegative);
            case Expr.Mul m -> m.factors().stream().allMatch(Normalizer::isNonNegative);
            case Expr.Sym s -> false;
        };
    }

    private static Expr simplifyAbs(Expr arg) {
        if (arg instanceof Expr.Add && isNonNegative(arg)) return arg;
        switch (arg) {
            case Expr.Num n -> {
                return new Expr.Num(n.value().abs());
            }
            case Expr.Const c -> {
                return c.kind() == Expr.Const.Kind.INFINITY ? null : arg;
            }
            case Expr.Func f -> {
                if (f.fn() == Fn.ABS || f.fn() == Fn.COSH) return arg;
            }
            case Expr.Pow p -> {
                if (p.base().equals(Expr.E)) return arg;
                if (p.exponent() instanceof Expr.Num e) {
                    Rational r = e.value();
                    if (r.isInteger() && !r.numerator().testBit(0)) return arg;           // potencia par
                    if (!r.isInteger() && !r.denominator().testBit(0)) return arg;        // raíz par
                }
            }
            case Expr.Mul m -> {
                if (m.factors().get(0) instanceof Expr.Num k) {
                    List<Expr> rest = m.factors().subList(1, m.factors().size());
                    Expr restExpr = rest.size() == 1 ? rest.get(0) : new Expr.Mul(rest);
                    return mul(List.of(new Expr.Num(k.value().abs()), func(Fn.ABS, restExpr)));
                }
            }
            default -> { }
        }
        return null;
    }

    // ---------------------------------------------------------------- Valores exactos

    private static Rational piMultiple(Expr arg) {
        if (arg.isZero()) return Rational.ZERO;
        if (arg.equals(Expr.PI)) return Rational.ONE;
        if (arg instanceof Expr.Mul m && m.factors().size() == 2
                && m.factors().get(0) instanceof Expr.Num n && m.factors().get(1).equals(Expr.PI)) {
            return n.value();
        }
        return null;
    }

    private static Expr exactTrig(Fn fn, Expr arg) {
        Rational r = piMultiple(arg);
        if (r == null) return null;

        // Ángulo en múltiplos de π/12 (solo los que son múltiplos de π/6 o π/4)
        Rational twelfths = r.multiply(Rational.of(12));
        if (!twelfths.isInteger()) return null;
        int m = Math.floorMod(twelfths.numerator().mod(BigInteger.valueOf(24)).intValue(), 24);
        if (m % 2 != 0 && m % 3 != 0) return null;

        Expr sin = sineOfTwelfths(m);
        Expr cos = sineOfTwelfths((m + 6) % 24);
        return switch (fn) {
            case SIN -> sin;
            case COS -> cos;
            case TAN -> cos.isZero() ? null : mul(List.of(sin, pow(cos, Expr.MINUS_ONE)));
            case COT -> sin.isZero() ? null : mul(List.of(cos, pow(sin, Expr.MINUS_ONE)));
            case SEC -> cos.isZero() ? null : pow(cos, Expr.MINUS_ONE);
            case CSC -> sin.isZero() ? null : pow(sin, Expr.MINUS_ONE);
            default -> null;
        };
    }

    /** {@code sin(m·π/12)} para m ∈ [0, 24) con m múltiplo de 2 o de 3. */
    private static Expr sineOfTwelfths(int m) {
        int reduced;
        boolean negative = false;
        if (m <= 6) reduced = m;
        else if (m <= 12) reduced = 12 - m;
        else if (m <= 18) {
            reduced = m - 12;
            negative = true;
        } else {
            reduced = 24 - m;
            negative = true;
        }
        if (reduced > 6) { // m - 12 puede exceder 6: reflejar de nuevo
            reduced = 12 - reduced;
        }

        Expr value = switch (reduced) {
            case 0 -> Expr.ZERO;
            case 2 -> new Expr.Num(Rational.of(1, 2));
            case 3 -> mul(List.of(new Expr.Num(Rational.of(1, 2)), numberPow(Rational.of(2), Rational.of(1, 2))));
            case 4 -> mul(List.of(new Expr.Num(Rational.of(1, 2)), numberPow(Rational.of(3), Rational.of(1, 2))));
            case 6 -> Expr.ONE;
            default -> throw new IllegalStateException("Ángulo no soportado: " + m);
        };
        return negative ? mul(List.of(Expr.MINUS_ONE, value)) : value;
    }

    private static Expr exactInverseTrig(Fn fn, Expr arg) {
        if (!(arg instanceof Expr.Num n)) return null;
        Rational v = n.value();
        Rational half = Rational.of(1, 2);
        switch (fn) {
            case ASIN -> {
                if (v.isOne()) return mul(List.of(new Expr.Num(half), Expr.PI));
                if (v.equals(half)) return mul(List.of(new Expr.Num(Rational.of(1, 6)), Expr.PI));
            }
            case ACOS -> {
                if (v.isOne()) return Expr.ZERO;
                if (v.isZero()) return mul(List.of(new Expr.Num(half), Expr.PI));
                if (v.equals(half)) return mul(List.of(new Expr.Num(Rational.of(1, 3)), Expr.PI));
                if (v.equals(Rational.MINUS_ONE)) return Expr.PI;
                if (v.equals(half.negate())) return mul(List.of(new Expr.Num(Rational.of(2, 3)), Expr.PI));
            }
            case ATAN -> {
                if (v.isOne()) return mul(List.of(new Expr.Num(Rational.of(1, 4)), Expr.PI));
            }
            default -> { }
        }
        return null;
    }
}
