package barnesinside.mathutils.cas.limit;

import barnesinside.mathutils.cas.diff.Derivatives;
import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.poly.Polynomial;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Trace;

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
                if (isSubstitution(f, c, lv.value())) specialValues(f, c);
                else if (c.isInfinite()) knownValues(f, c);
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
        if (result.isEmpty() && c.isInfinite()) result = commonBase(f, c, depth);
        if (result.isEmpty()) result = quotient(f, c, depth);
        if (result.isEmpty() && c.isInfinite()) result = divideDominant(f, c, depth);
        if (result.isEmpty()) result = product(f, c, depth);
        if (result.isEmpty()) result = difference(f, c, depth);
        if (result.isEmpty()) result = exponentialForm(f, c, depth);
        if (result.isEmpty()) result = termwise(f, c, depth);
        if (result.isEmpty() && !c.isInfinite() && c.direction() == Limits.Direction.BOTH) result = sides(f, c, depth);
        return result.orElse(UNSOLVED);
    }

    /**
     * Suma cuyos sumandos tienen límite por separado (cada uno indeterminado a su manera, como
     * {@code x²e^(-x) + 2e^(-x)}) o producto de una constante por un factor con límite. Los
     * límites infinitos solo se combinan si tienen el mismo signo.
     */
    private Optional<Outcome> termwise(Expr f, Context c, int depth) {
        List<Expr> parts;
        if (f instanceof Expr.Add sum) {
            parts = sum.terms();
        } else if (f instanceof Expr.Mul m && m.factors().stream().filter(t -> !t.isFreeOf(x)).count() == 1
                && m.factors().stream().anyMatch(t -> t.isFreeOf(x))) {
            parts = m.factors();
        } else {
            return Optional.empty();
        }
        boolean sum = f instanceof Expr.Add;
        Optional<Expr> done = trace.attempt(Rule.LIMIT_ALGEBRA, f, params(c), () -> {
            List<Expr> values = new ArrayList<>();
            int infinity = 0;
            for (Expr part : parts) {
                if (part.isFreeOf(x)) {
                    values.add(part);
                    continue;
                }
                Optional<Expr> limit = valueOf(solve(part, c, depth + 1));
                if (limit.isEmpty()) return Optional.empty();
                int sign = infinitySign(limit.get());
                if (sign == 0) {
                    values.add(limit.get());
                } else if (sum) {
                    if (infinity != 0 && infinity != sign) return Optional.empty();
                    infinity = sign;
                } else {
                    infinity = sign;
                }
            }
            if (infinity == 0) return Optional.of(sum ? Expr.add(values) : Expr.mul(values));
            if (sum) return Optional.of(infinity(infinity));
            int sign = infinity;
            for (Expr value : values) {
                int s = constantSign(value);
                if (s == 0) return Optional.empty();
                sign *= s;
            }
            return Optional.of(infinity(sign));
        });
        return done.map(Value::new);
    }

    /**
     * Antes de sustituir, el factorial y la digamma de lo que sale tras poner el valor (0! = 1, ψ(1) = −γ): un paso por cada
     * una, para que el resultado no parezca salir de la nada.
     */
    private void specialValues(Expr f, Context c) {
        List<Expr.Func> found = new ArrayList<>();
        collectSpecial(f, found);
        for (Expr.Func special : found) {
            try {
                Expr point = special.arg().substitute(x, c.point());
                if (!(point instanceof Expr.Num)) continue;
                Expr before = new Expr.Func(special.fn(), point);
                Expr after = Expr.func(special.fn(), point);
                if (after instanceof Expr.Func) continue;
                trace.leaf(Rule.LIMIT_SPECIAL_VALUE, before, after, Trace.params("u", before, "v", after));
            } catch (ArithmeticException e) {
                // Sin valor exacto: no se explica
            }
        }
    }

    /**
     * En el infinito, las funciones de argumento infinito con límite conocido (arctan, tanh, erf, e^x): un paso por cada una,
     * antes de combinar los resultados.
     */
    private void knownValues(Expr f, Context c) {
        List<Expr> found = new ArrayList<>();
        collectInfinite(f, found);
        for (Expr e : found) {
            try {
                Expr arg = e instanceof Expr.Func fn ? fn.arg() : ((Expr.Pow) e).exponent();
                LV la = evaluate(arg, c);
                if (!la.isInf()) continue;
                LV lv = evaluate(e, c);
                if (lv.kind() != Kind.FINITE && !lv.isInf()) continue;
                Expr after = lv.kind() == Kind.FINITE ? lv.value() : infinity(lv.sign());
                Expr limit = infinity(la.sign());
                Expr before = e instanceof Expr.Func fn ? new Expr.Func(fn.fn(), limit) : new Expr.Pow(Expr.E, limit);
                trace.leaf(Rule.LIMIT_KNOWN_VALUE, before, after, Trace.params("u", before, "v", after));
            } catch (ArithmeticException ex) {
                // Sin explicación
            }
        }
    }

    private void collectInfinite(Expr e, List<Expr> out) {
        switch (e) {
            case Expr.Func fn -> {
                if ((fn.fn() == Fn.ATAN || fn.fn() == Fn.TANH || fn.fn() == Fn.ERF) && !fn.arg().isFreeOf(x) && !out.contains(fn)) out.add(fn);
                collectInfinite(fn.arg(), out);
            }
            case Expr.Pow p -> {
                if (p.base().equals(Expr.E) && !p.exponent().isFreeOf(x) && !out.contains(p)) out.add(p);
                collectInfinite(p.base(), out);
                collectInfinite(p.exponent(), out);
            }
            case Expr.Add a -> a.terms().forEach(t -> collectInfinite(t, out));
            case Expr.Mul m -> m.factors().forEach(t -> collectInfinite(t, out));
            default -> { }
        }
    }

    /** {@code a^x / b^x = (a/b)^x}: potencias de bases numéricas con el mismo exponente (o el opuesto). */
    private Optional<Outcome> commonBase(Expr f, Context c, int depth) {
        if (!(f instanceof Expr.Mul m)) return Optional.empty();
        List<Expr> factors = m.factors();
        for (int i = 0; i < factors.size(); i++) {
            for (int j = i + 1; j < factors.size(); j++) {
                if (!(factors.get(i) instanceof Expr.Pow p) || !(factors.get(j) instanceof Expr.Pow q)) continue;
                if (!(p.base() instanceof Expr.Num) || !(q.base() instanceof Expr.Num) || p.exponent().isFreeOf(x)) continue;
                Expr joined;
                if (p.exponent().equals(q.exponent())) joined = Expr.pow(Expr.mul(p.base(), q.base()), p.exponent());
                else if (p.exponent().equals(Expr.neg(q.exponent()))) joined = Expr.pow(Expr.div(p.base(), q.base()), p.exponent());
                else continue;
                List<Expr> rest = new ArrayList<>(factors);
                rest.remove(j);
                rest.set(i, joined);
                Expr rewritten = Expr.mul(rest);
                if (rewritten.equals(f)) continue;
                Optional<Expr> done = trace.attempt(Rule.LIMIT_COMMON_BASE, f, params(c, "after", rewritten),
                        () -> valueOf(solve(rewritten, c, depth + 1)));
                if (done.isPresent()) return done.map(Value::new);
            }
        }
        return Optional.empty();
    }

    /**
     * Cociente en el infinito con términos acotados (sen x, cos x): se divide numerador y denominador entre la mayor potencia de
     * {@code x} y lo acotado entre algo que crece tiende a 0.
     */
    private Optional<Outcome> divideDominant(Expr f, Context c, int depth) {
        Algebra.Fraction fraction = Algebra.fraction(f);
        if (fraction.denominator().isOne()) return Optional.empty();
        int k = Math.max(highestPower(fraction.numerator()), highestPower(fraction.denominator()));
        if (k <= 0) return Optional.empty();
        Expr divisor = Expr.pow(xs, Expr.num(k));
        Expr rewritten = Expr.div(Expand.expand(Expr.div(fraction.numerator(), divisor)), Expand.expand(Expr.div(fraction.denominator(), divisor)));
        if (rewritten.equals(f)) return Optional.empty();
        Optional<Expr> done = trace.attempt(Rule.LIMIT_DIVIDE_DOMINANT, f, params(c, "p", divisor, "after", rewritten),
                () -> valueOf(solve(rewritten, c, depth + 1)));
        return done.map(Value::new);
    }

    /** La mayor potencia entera de {@code x} entre los términos (0 si no hay). */
    private int highestPower(Expr e) {
        List<Expr> terms = e instanceof Expr.Add a ? a.terms() : List.of(e);
        int best = 0;
        for (Expr term : terms) {
            List<Expr> factors = term instanceof Expr.Mul m ? m.factors() : List.of(term);
            for (Expr factor : factors) {
                if (factor.equals(xs)) best = Math.max(best, 1);
                else if (factor instanceof Expr.Pow p && p.base().equals(xs) && p.exponent() instanceof Expr.Num n && n.value().isInteger()
                        && n.value().signum() > 0) best = Math.max(best, n.value().numerator().intValueExact());
            }
        }
        return best;
    }

    private static void collectSpecial(Expr e, List<Expr.Func> out) {
        switch (e) {
            case Expr.Func fn -> {
                if ((fn.fn() == Fn.FACTORIAL || fn.fn() == Fn.DIGAMMA) && out.stream().noneMatch(fn::equals)) out.add(fn);
                collectSpecial(fn.arg(), out);
            }
            case Expr.Add a -> a.terms().forEach(t -> collectSpecial(t, out));
            case Expr.Mul m -> m.factors().forEach(t -> collectSpecial(t, out));
            case Expr.Pow p -> {
                collectSpecial(p.base(), out);
                collectSpecial(p.exponent(), out);
            }
            default -> { }
        }
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
        leadSteps = new ArrayList<>();
        Optional<Lead> lead = leading(g);
        List<Expr[]> collected = leadSteps;
        leadSteps = null;
        if (lead.isEmpty()) return Optional.empty();
        for (Expr[] pair : collected) trace.leaf(Rule.LIMIT_LEADING_TERM, pair[0], pair[1], params(c, "before", pair[0], "after", pair[1]));

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

    /** Los términos de mayor grado que se van encontrando al analizar una función en el infinito, para explicarlos. */
    private List<Expr[]> leadSteps;

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
                if (leadSteps != null && !sum.isZero() && a.terms().size() > 1) {
                    leadSteps.add(new Expr[]{a, Expr.mul(sum, Expr.pow(xs, Expr.num(max)))});
                }
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
            Expr numerator = Expand.expand(Expr.sub(Expr.pow(a, Expr.num(2)), Expr.pow(b, Expr.num(2))));
            Expr conjugated = Expr.div(numerator, Expr.sub(a, b));
            Optional<Expr> done = trace.attempt(Rule.LIMIT_CONJUGATE, f, params(c), () -> {
                conjugateNumeratorStep(a, b, numerator);
                return valueOf(solve(conjugated, c, depth + 1));
            });
            return done.map(Value::new);
        }
        return Optional.empty();
    }

    /** El paso de A² − B²: lo que sale de multiplicar por el conjugado, sin simplificar y simplificado. */
    private void conjugateNumeratorStep(Expr a, Expr b, Expr numerator) {
        Expr raw = new Expr.Add(List.of(new Expr.Pow(a, Expr.num(2)), new Expr.Mul(List.of(Expr.MINUS_ONE, new Expr.Pow(b, Expr.num(2))))));
        trace.leaf(Rule.LIMIT_CONJUGATE_NUMERATOR, raw, numerator, Trace.params("x", xs));
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

            Expr numerator = Expand.expand(Expr.sub(Expr.pow(a, Expr.num(2)), Expr.pow(b, Expr.num(2))));
            Optional<Expr> done = trace.attempt(Rule.LIMIT_CONJUGATE, f, params(c), () -> {
                conjugateNumeratorStep(a, b, numerator);
                return valueOf(solve(rewritten, c, depth + 1));
            });
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
            Optional<Expr> oneSided = oneSidedDomain(f, c, left, right);
            if (oneSided.isPresent()) {
                holder[0] = left instanceof Value ? left : right;
                return oneSided;
            }
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

    /**
     * La función solo existe en reales a un lado del punto (x·ln x en 0): el límite es el del lado donde existe. Se mira numéricamente
     * si al otro lado no está definida.
     */
    private Optional<Expr> oneSidedDomain(Expr f, Context c, Outcome left, Outcome right) {
        if (c.isInfinite() || (left instanceof Value) == (right instanceof Value)) return Optional.empty();
        boolean leftWorks = left instanceof Value;
        if (!(c.point() instanceof Expr.Num) && !(c.point().variables().isEmpty())) return Optional.empty();
        double point = numeric(c.point());
        if (Double.isNaN(point)) return Optional.empty();
        for (double offset : new double[]{1e-3, 1e-5, 1e-7}) {
            double value = Eval.value(f, Map.of(x, leftWorks ? point + offset : point - offset));
            if (!Double.isNaN(value)) return Optional.empty();
        }
        Outcome kept = leftWorks ? left : right;
        trace.leaf(Rule.LIMIT_DOMAIN_ONE_SIDE, f, ((Value) kept).value(), params(c));
        return Optional.of(((Value) kept).value());
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
        if (la.kind() == Kind.UNSIGNED_INF && (fn == Fn.SIN || fn == Fn.COS)) return LV.OSC;
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
        if (Double.isNaN(v) && !e.variables().isEmpty()) return Assumptions.sign(e);
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
