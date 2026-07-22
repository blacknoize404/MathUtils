package MathUtils.cas.limit;

import MathUtils.cas.diff.Derivatives;
import MathUtils.cas.expr.Algebra;
import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.Expand;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.Fn;
import MathUtils.cas.expr.Rational;
import MathUtils.cas.poly.Polynomial;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calcula límites con reglas: sustitución directa y álgebra de límites; cocientes de polinomios
 * en el infinito; L'Hôpital para las formas 0/0 y ∞/∞; reescritura de 0·∞, ∞−∞ y de las formas
 * exponenciales 1^∞, 0^0 y ∞^0; y límites laterales. Cada regla se registra como un paso.
 */
final class LimitEngine {

    private static final int MAX_DEPTH = 8;

    /** Símbolo que representa "el límite no existe" en un paso. */
    static final Expr DOES_NOT_EXIST = Expr.sym("DNE");

    // ================================================================ Resultados

    sealed interface Outcome permits Value, NoLimit, Unsolved {
    }

    /** El límite existe (finito o infinito). */
    record Value(Expr value) implements Outcome {
    }

    /** Se demostró que el límite no existe. */
    record NoLimit() implements Outcome {
    }

    /** El motor no supo calcularlo. */
    record Unsolved() implements Outcome {
    }

    private static final Outcome UNSOLVED = new Unsolved();

    /** Punto al que tiende la variable y lado de aproximación. */
    record Context(Expr point, Limits.Direction direction) {
        /** +1 si tiende a +∞, -1 si a -∞, 0 si el punto es finito. */
        int infinity() {
            return infinitySign(point);
        }

        boolean isInfinite() {
            return infinity() != 0;
        }

        /** Lados a comprobar al estudiar el signo cerca del punto: -1 izquierda, +1 derecha. */
        int[] sides() {
            if (isInfinite()) return new int[]{infinity()};
            return switch (direction) {
                case LEFT -> new int[]{-1};
                case RIGHT -> new int[]{1};
                case BOTH -> new int[]{-1, 1};
            };
        }

        Context withDirection(Limits.Direction d) {
            return new Context(point, d);
        }
    }

    // ================================================================ Valor de un límite parcial

    private enum Kind {FINITE, INF, OSCILLATING, UNSIGNED_INF, INDETERMINATE}

    private record LV(Kind kind, Expr value, int sign) {
        static LV finite(Expr v) {
            return new LV(Kind.FINITE, v, 0);
        }

        static LV inf(int sign) {
            return new LV(Kind.INF, null, sign);
        }

        static final LV OSC = new LV(Kind.OSCILLATING, null, 0);
        static final LV UNSIGNED = new LV(Kind.UNSIGNED_INF, null, 0);
        static final LV IND = new LV(Kind.INDETERMINATE, null, 0);

        boolean isZero() {
            return kind == Kind.FINITE && value.isZero();
        }

        boolean isInf() {
            return kind == Kind.INF;
        }
    }

    private final String x;
    private final Expr xs;
    private final Trace trace;

    LimitEngine(String variable, Trace trace) {
        this.x = variable;
        this.xs = Expr.sym(variable);
        this.trace = trace;
    }

    // ================================================================ Infinitos

    static int infinitySign(Expr e) {
        if (e.equals(Expr.INFINITY)) return 1;
        if (e instanceof Expr.Mul m && m.factors().size() == 2 && m.factors().get(0).equals(Expr.MINUS_ONE)
                && m.factors().get(1).equals(Expr.INFINITY)) return -1;
        return 0;
    }

    static Expr infinity(int sign) {
        return sign >= 0 ? Expr.INFINITY : Expr.neg(Expr.INFINITY);
    }

    // ================================================================ Resolución con pasos

    Outcome solve(Expr f, Context c, int depth) {
        if (depth > MAX_DEPTH) return UNSOLVED;

        if (f.isFreeOf(x)) {
            trace.leaf(Rule.LIMIT_CONSTANT, f, f, params(c));
            return new Value(f);
        }

        // Con un solo lado, el signo de los valores absolutos está fijo cerca del punto
        if (c.sides().length == 1 && containsAbs(f)) {
            Expr resolved = resolveAbs(f, c);
            if (!resolved.equals(f)) {
                trace.leaf(Rule.LIMIT_ABSOLUTE_VALUE, f, resolved, params(c));
                return solve(resolved, c, depth + 1);
            }
        }
        // Con dos lados, el valor absoluto suele dar límites laterales distintos
        if (c.direction() == Limits.Direction.BOTH && !c.isInfinite() && containsAbs(f)) {
            Optional<Outcome> bySides = sides(f, c, depth);
            if (bySides.isPresent()) return bySides.get();
        }

        LV lv = evaluate(f, c);
        switch (lv.kind()) {
            case FINITE -> {
                trace.leaf(isSubstitution(f, c, lv.value()) ? Rule.LIMIT_DIRECT : Rule.LIMIT_ALGEBRA, f, lv.value(), params(c));
                return new Value(lv.value());
            }
            case INF -> {
                Expr value = infinity(lv.sign());
                trace.leaf(isSubstitution(f, c, value) ? Rule.LIMIT_DIRECT : Rule.LIMIT_ALGEBRA, f, value, params(c));
                return new Value(value);
            }
            case OSCILLATING -> {
                trace.leaf(Rule.LIMIT_OSCILLATES, f, DOES_NOT_EXIST, params(c));
                return new NoLimit();
            }
            default -> {
                // Se prueban las estrategias para formas indeterminadas
            }
        }

        Optional<Outcome> result = Optional.empty();
        if (c.isInfinite()) result = rationalAtInfinity(f, c).map(Value::new);
        if (result.isEmpty() && c.isInfinite()) result = dominantTerms(f, c).map(Value::new);
        if (result.isEmpty()) result = quotient(f, c, depth);
        if (result.isEmpty()) result = product(f, c, depth);
        if (result.isEmpty()) result = difference(f, c, depth);
        if (result.isEmpty()) result = exponentialForm(f, c, depth);
        if (result.isEmpty() && !c.isInfinite() && c.direction() == Limits.Direction.BOTH) result = sides(f, c, depth);
        return result.orElse(UNSOLVED);
    }

    private Map<String, Expr> params(Context c) {
        return Trace.params("x", xs, "a", c.point());
    }

    private Map<String, Expr> params(Context c, Object... extra) {
        Map<String, Expr> map = new java.util.LinkedHashMap<>(params(c));
        for (int i = 0; i < extra.length; i += 2) map.put((String) extra[i], (Expr) extra[i + 1]);
        return map;
    }

    /** Distingue "sustituir directamente" de "usar las propiedades de los límites". */
    private boolean isSubstitution(Expr f, Context c, Expr value) {
        if (c.isInfinite()) return false;
        try {
            return f.substitute(x, c.point()).equals(value);
        } catch (ArithmeticException e) {
            return false;
        }
    }

    // ================================================================ Estrategias

    /** Cociente de polinomios cuando la variable tiende a infinito. */
    private Optional<Expr> rationalAtInfinity(Expr f, Context c) {
        Expr combined = Algebra.together(f);
        Algebra.Fraction fraction = Algebra.fraction(combined);
        Optional<Polynomial> numerator = Polynomial.from(fraction.numerator(), x);
        Optional<Polynomial> denominator = Polynomial.from(fraction.denominator(), x);
        if (numerator.isEmpty() || denominator.isEmpty() || denominator.get().isZero() || numerator.get().isZero()) {
            return Optional.empty();
        }

        int n = numerator.get().degree();
        int m = denominator.get().degree();
        Rational ratio = numerator.get().leading().divide(denominator.get().leading());

        Expr result;
        if (n < m) {
            result = Expr.ZERO;
        } else if (n == m) {
            result = Expr.num(ratio);
        } else {
            int sign = ratio.signum();
            if (c.infinity() < 0 && ((n - m) % 2 == 1)) sign = -sign;
            result = infinity(sign);
        }

        Expr degreeN = Expr.num(n);
        Expr degreeM = Expr.num(m);
        trace.leaf(Rule.LIMIT_INFINITY_RATIONAL, f, result, params(c, "n", degreeN, "m", degreeM));
        return Optional.of(result);
    }

    // ---------------------------------------------------------------- Términos dominantes

    /** Comportamiento {@code coefficient · x^exponent} de una expresión cuando x tiende a +∞. */
    private record Lead(Expr coefficient, Rational exponent) {
    }

    /**
     * Funciones algebraicas (polinomios, cocientes y raíces) en el infinito: se compara el término
     * dominante de cada parte. Para x → −∞ se sustituye x por −y y se estudia y → +∞.
     */
    private Optional<Expr> dominantTerms(Expr f, Context c) {
        Expr g = c.infinity() < 0 ? f.substitute(x, Expr.neg(xs)) : f;
        Optional<Lead> lead = leading(g);
        if (lead.isEmpty()) return Optional.empty();

        Expr coefficient = lead.get().coefficient();
        Rational exponent = lead.get().exponent();
        Expr result;
        if (exponent.signum() < 0) {
            result = Expr.ZERO;
        } else if (exponent.isZero()) {
            result = coefficient;
        } else {
            int sign = constantSign(coefficient);
            if (sign == 0) return Optional.empty();
            result = infinity(sign);
        }

        trace.leaf(Rule.LIMIT_DOMINANT_TERMS, f, result, params(c, "c", coefficient, "p", Expr.num(exponent)));
        return Optional.of(result);
    }

    private Optional<Lead> leading(Expr e) {
        if (e.isFreeOf(x)) {
            return e.isZero() ? Optional.empty() : Optional.of(new Lead(e, Rational.ZERO));
        }
        switch (e) {
            case Expr.Sym s -> {
                return Optional.of(new Lead(Expr.ONE, Rational.ONE));
            }
            case Expr.Mul m -> {
                Expr coefficient = Expr.ONE;
                Rational exponent = Rational.ZERO;
                for (Expr factor : m.factors()) {
                    Optional<Lead> l = leading(factor);
                    if (l.isEmpty()) return Optional.empty();
                    coefficient = Expr.mul(coefficient, l.get().coefficient());
                    exponent = exponent.add(l.get().exponent());
                }
                return Optional.of(new Lead(coefficient, exponent));
            }
            case Expr.Pow p -> {
                if (!(p.exponent() instanceof Expr.Num n)) return Optional.empty();
                Optional<Lead> base = leading(p.base());
                if (base.isEmpty()) return Optional.empty();
                Rational r = n.value();
                Expr c0 = base.get().coefficient();
                if (!r.isInteger() && constantSign(c0) <= 0) return Optional.empty();
                try {
                    return Optional.of(new Lead(Expr.pow(c0, n), base.get().exponent().multiply(r)));
                } catch (ArithmeticException ex) {
                    return Optional.empty();
                }
            }
            case Expr.Add a -> {
                List<Lead> leads = new ArrayList<>();
                for (Expr term : a.terms()) {
                    Optional<Lead> l = leading(term);
                    if (l.isEmpty()) return Optional.empty();
                    leads.add(l.get());
                }
                Rational max = leads.get(0).exponent();
                for (Lead l : leads) if (l.exponent().compareTo(max) > 0) max = l.exponent();

                Expr sum = Expr.ZERO;
                for (Lead l : leads) if (l.exponent().equals(max)) sum = Expr.add(sum, l.coefficient());
                return sum.isZero() ? Optional.empty() : Optional.of(new Lead(sum, max));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    // ---------------------------------------------------------------- Valor absoluto por un lado

    private static boolean containsAbs(Expr e) {
        return switch (e) {
            case Expr.Func f -> f.fn() == Fn.ABS || containsAbs(f.arg());
            case Expr.Add a -> a.terms().stream().anyMatch(LimitEngine::containsAbs);
            case Expr.Mul m -> m.factors().stream().anyMatch(LimitEngine::containsAbs);
            case Expr.Pow p -> containsAbs(p.base()) || containsAbs(p.exponent());
            default -> false;
        };
    }

    /** Reescribe {@code abs(u)} como {@code u} o {@code -u} cuando el signo de {@code u} es fijo cerca del punto. */
    private Expr resolveAbs(Expr e, Context c) {
        return switch (e) {
            case Expr.Func f when f.fn() == Fn.ABS && !f.arg().isFreeOf(x) -> {
                Expr inner = resolveAbs(f.arg(), c);
                int[] signs = signsNear(inner, c);
                if (signs != null && signs.length == 1 && signs[0] > 0) yield inner;
                if (signs != null && signs.length == 1 && signs[0] < 0) yield Expr.neg(inner);
                yield Expr.abs(inner);
            }
            case Expr.Func f -> Expr.func(f.fn(), resolveAbs(f.arg(), c));
            case Expr.Add a -> Expr.add(a.terms().stream().map(t -> resolveAbs(t, c)).toList());
            case Expr.Mul m -> Expr.mul(m.factors().stream().map(t -> resolveAbs(t, c)).toList());
            case Expr.Pow p -> Expr.pow(resolveAbs(p.base(), c), resolveAbs(p.exponent(), c));
            default -> e;
        };
    }

    /** L'Hôpital sobre el cociente de la función cuando es 0/0 o ∞/∞. */
    private Optional<Outcome> quotient(Expr f, Context c, int depth) {
        Algebra.Fraction fraction = Algebra.fraction(f);
        if (fraction.denominator().isOne()) return Optional.empty();
        return lhopital(f, fraction.numerator(), fraction.denominator(), c, depth);
    }

    private Optional<Outcome> lhopital(Expr original, Expr numerator, Expr denominator, Context c, int depth) {
        LV ln = evaluate(numerator, c);
        LV ld = evaluate(denominator, c);

        Rule rule;
        if (ln.isZero() && ld.isZero()) rule = Rule.LIMIT_LHOPITAL_ZERO;
        else if (ln.isInf() && ld.isInf()) rule = Rule.LIMIT_LHOPITAL_INFINITY;
        else return Optional.empty();

        Outcome[] holder = {UNSOLVED};
        Optional<Expr> done = trace.attempt(rule, original, params(c, "n", numerator, "d", denominator), () -> {
            Expr dn;
            Expr dd;
            Expr next;
            try {
                dn = Derivatives.differentiate(numerator, x, trace);
                dd = Derivatives.differentiate(denominator, x, trace);
                next = Expr.div(dn, dd);
            } catch (ArithmeticException e) {
                return Optional.empty();
            }
            Outcome outcome = solve(next, c, depth + 1);
            holder[0] = outcome;
            return outcome instanceof Value v ? Optional.of(v.value()) : Optional.empty();
        });
        return done.map(Value::new);
    }

    /**
     * Producto 0·∞: se escribe como cociente, primero ∞/∞ (suele derivar mejor) y después 0/0,
     * y se aplica L'Hôpital.
     */
    private Optional<Outcome> product(Expr f, Context c, int depth) {
        if (!(f instanceof Expr.Mul m)) return Optional.empty();

        Expr zero = null;
        List<Expr> rest = new ArrayList<>();
        boolean hasInfinite = false;
        for (Expr factor : m.factors()) {
            LV lv = evaluate(factor, c);
            if (zero == null && lv.isZero() && !factor.isFreeOf(x)) {
                zero = factor;
                continue;
            }
            if (lv.isInf()) hasInfinite = true;
            rest.add(factor);
        }
        if (zero == null || !hasInfinite) return Optional.empty();

        Expr other = Expr.mul(rest);
        Expr[][] variants = {
                {other, Expr.pow(zero, Expr.MINUS_ONE)},
                {zero, Expr.pow(other, Expr.MINUS_ONE)}
        };
        for (Expr[] variant : variants) {
            Expr n = variant[0];
            Expr d = variant[1];
            Optional<Expr> done = trace.attempt(Rule.LIMIT_PRODUCT_TO_QUOTIENT, f, params(c, "n", n, "d", d), () -> {
                Optional<Outcome> inner = lhopital(f, n, d, c, depth + 1);
                return inner.flatMap(o -> o instanceof Value v ? Optional.of(v.value()) : Optional.empty());
            });
            if (done.isPresent()) return done.map(Value::new);
        }
        return Optional.empty();
    }

    /** ∞−∞: se combinan fracciones o se multiplica por el conjugado. */
    private Optional<Outcome> difference(Expr f, Context c, int depth) {
        if (f instanceof Expr.Mul product) return conjugateInsideFactor(f, product, c, depth);
        if (!(f instanceof Expr.Add sum)) return Optional.empty();

        Expr combined = Algebra.together(f);
        if (!combined.equals(f)) {
            Optional<Expr> done = trace.attempt(Rule.LIMIT_COMBINE_FRACTIONS, f, params(c), () -> valueOf(solve(combined, c, depth + 1)));
            if (done.isPresent()) return done.map(Value::new);
        }

        if (sum.terms().size() == 2 && (hasSquareRoot(sum.terms().get(0)) || hasSquareRoot(sum.terms().get(1)))) {
            Expr a = sum.terms().get(0);
            Expr b = sum.terms().get(1);
            Expr conjugated = Expr.div(Expand.expand(Expr.sub(Expr.pow(a, Expr.num(2)), Expr.pow(b, Expr.num(2)))), Expr.sub(a, b));
            Optional<Expr> done = trace.attempt(Rule.LIMIT_CONJUGATE, f, params(c), () -> valueOf(solve(conjugated, c, depth + 1)));
            return done.map(Value::new);
        }
        return Optional.empty();
    }

    /** Un factor de la forma ∞ − ∞ con raíces dentro de un producto: se racionaliza ese factor. */
    private Optional<Outcome> conjugateInsideFactor(Expr f, Expr.Mul product, Context c, int depth) {
        for (Expr factor : product.factors()) {
            if (!(factor instanceof Expr.Add sum) || sum.terms().size() != 2) continue;
            if (!hasSquareRoot(sum.terms().get(0)) && !hasSquareRoot(sum.terms().get(1))) continue;
            if (evaluate(factor, c).kind() != Kind.INDETERMINATE) continue;

            Expr a = sum.terms().get(0);
            Expr b = sum.terms().get(1);
            Expr rationalized = Expr.div(Expand.expand(Expr.sub(Expr.pow(a, Expr.num(2)), Expr.pow(b, Expr.num(2)))),
                    Expr.sub(a, b));
            List<Expr> parts = new ArrayList<>();
            for (Expr other : product.factors()) parts.add(other == factor ? rationalized : other);
            Expr rewritten = Expr.mul(parts);

            Optional<Expr> done = trace.attempt(Rule.LIMIT_CONJUGATE, f, params(c), () -> valueOf(solve(rewritten, c, depth + 1)));
            if (done.isPresent()) return done.map(Value::new);
        }
        return Optional.empty();
    }

    private static boolean hasSquareRoot(Expr e) {
        return switch (e) {
            case Expr.Pow p -> (p.exponent() instanceof Expr.Num n && n.value().equals(Rational.of(1, 2)))
                    || hasSquareRoot(p.base());
            case Expr.Mul m -> m.factors().stream().anyMatch(LimitEngine::hasSquareRoot);
            case Expr.Add a -> a.terms().stream().anyMatch(LimitEngine::hasSquareRoot);
            case Expr.Func f -> hasSquareRoot(f.arg());
            default -> false;
        };
    }

    /** 1^∞, 0^0 y ∞^0: {@code b^e = exp(e·ln b)}. */
    private Optional<Outcome> exponentialForm(Expr f, Context c, int depth) {
        if (!(f instanceof Expr.Pow p) || p.exponent().isFreeOf(x)) return Optional.empty();

        Expr exponent = p.exponent();
        Expr base = p.base();
        Expr product = Expr.mul(exponent, Expr.ln(base));
        Optional<Expr> done = trace.attempt(Rule.LIMIT_EXPONENTIAL_FORM, f, params(c, "b", base, "e", exponent), () -> {
            Outcome inner = solve(product, c, depth + 1);
            if (!(inner instanceof Value v)) return Optional.empty();
            int sign = infinitySign(v.value());
            if (sign > 0) return Optional.of(Expr.INFINITY);
            if (sign < 0) return Optional.of(Expr.ZERO);
            return Optional.of(Expr.exp(v.value()));
        });
        return done.map(Value::new);
    }

    /** Límites laterales: coinciden o el límite no existe. */
    private Optional<Outcome> sides(Expr f, Context c, int depth) {
        Outcome[] holder = {UNSOLVED};
        trace.attempt(Rule.LIMIT_SIDES, f, params(c), () -> {
            Outcome left = solve(f, c.withDirection(Limits.Direction.LEFT), depth + 1);
            Outcome right = solve(f, c.withDirection(Limits.Direction.RIGHT), depth + 1);
            if (left instanceof Value l && right instanceof Value r) {
                if (l.value().equals(r.value())) {
                    holder[0] = l;
                    return Optional.of(l.value());
                }
                trace.leaf(Rule.LIMIT_DIFFERENT_SIDES, f, DOES_NOT_EXIST, params(c));
                holder[0] = new NoLimit();
                return Optional.of(DOES_NOT_EXIST);
            }
            return Optional.empty();
        });
        return holder[0] instanceof Unsolved ? Optional.empty() : Optional.of(holder[0]);
    }

    private static Optional<Expr> valueOf(Outcome outcome) {
        return outcome instanceof Value v ? Optional.of(v.value()) : Optional.empty();
    }

    // ================================================================ Álgebra de límites

    private LV evaluate(Expr e, Context c) {
        if (e.isFreeOf(x)) return LV.finite(e);
        return switch (e) {
            case Expr.Sym s -> c.isInfinite() ? LV.inf(c.infinity()) : LV.finite(c.point());
            case Expr.Add a -> evaluateSum(a, c);
            case Expr.Mul m -> evaluateProduct(m, c);
            case Expr.Pow p -> evaluatePower(p, c);
            case Expr.Func f -> evaluateFunction(f, c);
            default -> LV.IND;
        };
    }

    private LV evaluateSum(Expr.Add sum, Context c) {
        int positive = 0;
        int negative = 0;
        boolean oscillates = false;
        List<Expr> values = new ArrayList<>();
        for (Expr term : sum.terms()) {
            LV lv = evaluate(term, c);
            switch (lv.kind()) {
                case INDETERMINATE, UNSIGNED_INF -> {
                    return LV.IND;
                }
                case INF -> {
                    if (lv.sign() > 0) positive++;
                    else negative++;
                }
                case OSCILLATING -> oscillates = true;
                case FINITE -> values.add(lv.value());
            }
        }
        if (positive > 0 && negative > 0) return LV.IND;
        if (positive > 0) return LV.inf(1);
        if (negative > 0) return LV.inf(-1);
        if (oscillates) return LV.OSC;
        return LV.finite(Expr.add(values));
    }

    private LV evaluateProduct(Expr.Mul product, Context c) {
        boolean hasInfinity = false;
        boolean hasZero = false;
        boolean oscillates = false;
        int sign = 1;
        List<Expr> values = new ArrayList<>();
        for (Expr factor : product.factors()) {
            LV lv = evaluate(factor, c);
            switch (lv.kind()) {
                case INDETERMINATE, UNSIGNED_INF -> {
                    return LV.IND;
                }
                case INF -> {
                    hasInfinity = true;
                    sign *= lv.sign();
                }
                case OSCILLATING -> oscillates = true;
                case FINITE -> {
                    if (lv.value().isZero()) {
                        hasZero = true;
                    } else {
                        values.add(lv.value());
                        int s = constantSign(lv.value());
                        if (s == 0) sign = 0;
                        else if (sign != 0) sign *= s;
                    }
                }
            }
        }

        if (hasInfinity && hasZero) return LV.IND;
        if (oscillates) {
            if (hasZero && !hasInfinity) return LV.finite(Expr.ZERO); // acotada por cero: se aprieta a 0
            return hasInfinity ? LV.IND : LV.OSC;
        }
        if (hasInfinity) return sign == 0 ? LV.IND : LV.inf(sign);
        if (hasZero) return LV.finite(Expr.ZERO);
        return LV.finite(Expr.mul(values));
    }

    private LV evaluatePower(Expr.Pow p, Context c) {
        Expr base = p.base();
        Expr exponent = p.exponent();

        if (exponent.isFreeOf(x)) {
            LV lb = evaluate(base, c);
            if (!(exponent instanceof Expr.Num en)) {
                if (lb.kind() == Kind.FINITE && !lb.value().isZero() && constantSign(lb.value()) > 0) {
                    return LV.finite(Expr.pow(lb.value(), exponent));
                }
                return LV.IND;
            }
            Rational r = en.value();
            switch (lb.kind()) {
                case FINITE -> {
                    Expr v = lb.value();
                    if (v.isZero()) {
                        return r.signum() > 0 ? LV.finite(Expr.ZERO) : zeroToNegativePower(base, r, c);
                    }
                    if (v instanceof Expr.Num vn && vn.value().signum() < 0 && !r.isInteger()
                            && !r.denominator().testBit(0)) {
                        return LV.IND;
                    }
                    try {
                        return LV.finite(Expr.pow(v, exponent));
                    } catch (ArithmeticException e) {
                        return LV.IND;
                    }
                }
                case INF -> {
                    if (r.signum() < 0) return LV.finite(Expr.ZERO);
                    if (r.isInteger()) return LV.inf(lb.sign() < 0 && r.numerator().testBit(0) ? -1 : 1);
                    return lb.sign() > 0 ? LV.inf(1) : LV.IND;
                }
                default -> {
                    return LV.IND;
                }
            }
        }

        if (base.isFreeOf(x)) {
            double b = numeric(base);
            boolean growing = base.equals(Expr.E) || b > 1;
            boolean decaying = !growing && b > 0 && b < 1;
            if (!growing && !decaying) return LV.IND;

            LV le = evaluate(exponent, c);
            return switch (le.kind()) {
                case INF -> (le.sign() > 0) == growing ? LV.inf(1) : LV.finite(Expr.ZERO);
                case FINITE -> LV.finite(Expr.pow(base, le.value()));
                default -> LV.IND;
            };
        }
        return LV.IND;
    }

    /** {@code 0^r} con {@code r < 0}: el signo depende de cómo se acerca la base a cero. */
    private LV zeroToNegativePower(Expr base, Rational r, Context c) {
        int[] signs = signsNear(base, c);
        if (signs == null) return LV.IND;

        boolean allPositive = true;
        boolean same = true;
        for (int s : signs) {
            if (s == 0) return LV.IND;
            if (s < 0) allPositive = false;
            if (s != signs[0]) same = false;
        }

        if (r.isInteger()) {
            if (!r.numerator().testBit(0)) return LV.inf(1); // exponente par
            return same ? LV.inf(signs[0]) : LV.UNSIGNED;
        }
        return allPositive ? LV.inf(1) : LV.IND;
    }

    private LV evaluateFunction(Expr.Func f, Context c) {
        LV la = evaluate(f.arg(), c);
        Fn fn = f.fn();

        if (la.kind() == Kind.FINITE) {
            Expr v = la.value();
            switch (fn) {
                case LN -> {
                    if (v.isZero()) {
                        int[] signs = signsNear(f.arg(), c);
                        boolean positive = signs != null;
                        if (signs != null) for (int s : signs) if (s <= 0) positive = false;
                        return positive ? LV.inf(-1) : LV.IND;
                    }
                    return constantSign(v) > 0 ? LV.finite(Expr.ln(v)) : LV.IND;
                }
                case TAN, SEC -> {
                    return Expr.func(Fn.COS, v).isZero() ? singular(f, c) : LV.finite(Expr.func(fn, v));
                }
                case COT, CSC -> {
                    return Expr.func(Fn.SIN, v).isZero() ? singular(f, c) : LV.finite(Expr.func(fn, v));
                }
                case ASIN, ACOS -> {
                    double d = numeric(v);
                    return Double.isNaN(d) || Math.abs(d) > 1 ? LV.IND : LV.finite(Expr.func(fn, v));
                }
                default -> {
                    return LV.finite(Expr.func(fn, v));
                }
            }
        }

        if (la.kind() == Kind.INF) {
            int s = la.sign();
            return switch (fn) {
                case SIN, COS -> LV.OSC;
                case ATAN -> LV.finite(Expr.mul(Expr.num(s), Expr.num(1, 2), Expr.PI));
                case TANH, ERF -> LV.finite(Expr.num(s));
                case SINH -> LV.inf(s);
                case COSH, ABS -> LV.inf(1);
                case LN -> s > 0 ? LV.inf(1) : LV.IND;
                default -> LV.IND;
            };
        }
        return LV.IND;
    }

    /** La función se hace infinita en el punto: se decide el signo mirando cerca. */
    private LV singular(Expr f, Context c) {
        int[] signs = signsNear(f, c);
        if (signs == null) return LV.IND;
        for (int s : signs) if (s == 0) return LV.IND;
        for (int s : signs) if (s != signs[0]) return LV.UNSIGNED;
        return LV.inf(signs[0]);
    }

    // ================================================================ Signos y valores numéricos

    private static double numeric(Expr e) {
        try {
            if (!e.variables().isEmpty()) return Double.NaN;
            return Eval.value(e);
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
    }

    private static int constantSign(Expr e) {
        double v = numeric(e);
        if (Double.isNaN(v) || v == 0) return 0;
        return v > 0 ? 1 : -1;
    }

    /**
     * Signo de {@code g} cerca del punto, uno por lado a comprobar ({@code 0} si no se puede
     * decidir). Devuelve {@code null} si la función depende de otras variables.
     */
    private int[] signsNear(Expr g, Context c) {
        if (g.variables().stream().anyMatch(v -> !v.equals(x))) return null;
        double a = numeric(c.point());
        int[] sides = c.sides();
        int[] result = new int[sides.length];

        for (int i = 0; i < sides.length; i++) {
            int agreed = 2;
            for (double distance : new double[]{1e-3, 1e-5, 1e-7}) {
                double point = c.isInfinite() ? sides[i] / distance : a + sides[i] * distance;
                double value;
                try {
                    value = Eval.value(g, Map.of(x, point));
                } catch (RuntimeException e) {
                    agreed = 0;
                    break;
                }
                int s = Double.isNaN(value) || value == 0 ? 0 : (value > 0 ? 1 : -1);
                if (agreed == 2) agreed = s;
                else if (agreed != s) agreed = 0;
                if (agreed == 0) break;
            }
            result[i] = agreed == 2 ? 0 : agreed;
        }
        return result;
    }

    // ================================================================ Verificación numérica

    /**
     * Comprueba el resultado evaluando la función cerca del punto. Con resultado finito basta con
     * que alguna muestra se acerque al valor (hay cancelación numérica y convergencias lentas);
     * con resultado infinito, las muestras deben crecer con el signo correcto.
     */
    boolean verify(Expr f, Context c, Expr result) {
        if (f.variables().stream().anyMatch(v -> !v.equals(x))) return true;

        double a = numeric(c.point());
        double[] distances = c.isInfinite()
                ? new double[]{1e2, 1e3, 1e4, 1e5, 1e6}
                : new double[]{1e-2, 1e-3, 1e-4, 1e-5};
        int infinity = infinitySign(result);
        double target = infinity == 0 ? numeric(result) : Double.NaN;
        if (infinity == 0 && Double.isNaN(target)) return true;

        for (int side : c.sides()) {
            List<Double> values = new ArrayList<>();
            for (double distance : distances) {
                double point = c.isInfinite() ? side * distance : a + side * distance;
                try {
                    double value = Eval.value(f, Map.of(x, point));
                    if (!Double.isNaN(value) && !Double.isInfinite(value)) values.add(value);
                } catch (RuntimeException e) {
                    // Punto fuera del dominio: se ignora
                }
            }
            if (values.isEmpty()) continue;

            if (infinity == 0) {
                boolean closeEnough = values.stream().anyMatch(v -> Math.abs(v - target) <= 0.05 * (1 + Math.abs(target)));
                if (!closeEnough) return false;
            } else if (values.size() >= 2) {
                // Al acercarse al punto, la magnitud debe crecer con el signo esperado
                for (int i = 0; i < values.size(); i++) {
                    if (Math.signum(values.get(i)) != infinity) return false;
                    if (i > 0 && Math.abs(values.get(i)) <= Math.abs(values.get(i - 1))) return false;
                }
                if (Math.abs(values.get(values.size() - 1)) < 3) return false;
            } else if (Math.signum(values.get(0)) != infinity) {
                return false;
            }
        }
        return true;
    }

    /** Valores de muestra de la función, útil para diagnósticos. */
    Map<Double, Double> sample(Expr f, double... points) {
        Map<Double, Double> map = new HashMap<>();
        for (double p : points) {
            try {
                map.put(p, Eval.value(f, Map.of(x, p)));
            } catch (RuntimeException e) {
                map.put(p, Double.NaN);
            }
        }
        return map;
    }
}
