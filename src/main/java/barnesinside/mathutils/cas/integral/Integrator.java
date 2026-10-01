package barnesinside.mathutils.cas.integral;

import barnesinside.mathutils.cas.diff.Derivatives;
import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Equivalence;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.poly.Polynomial;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Trace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Integra con una cadena de estrategias, registrando cada regla como un paso:
 * constantes, linealidad (suma y constante por función), tabla de integrales, sustitución,
 * expansión e integración por partes (incluido el caso cíclico {@code e^x·sin x}).
 * <p>Todo resultado intermedio se verifica derivándolo: si no coincide con el integrando se
 * descarta, así que el motor no devuelve antiderivadas incorrectas.
 */
final class Integrator {

    private static final int MAX_DEPTH = 9;
    private static final int MAX_CANDIDATES = 10;
    private static final List<String> FRESH_NAMES = List.of("u", "v", "w", "t", "s", "z");

    /** Integrando y variable de integración. */
    private record Key(Expr f, String variable) {
    }

    private final Trace trace;
    private final int maxCalls;
    /** Técnica obligada en la primera llamada, o {@code null} para probarlas todas. */
    private final IntegralMethod only;
    private int calls;
    private final Set<Key> inProgress = new HashSet<>();
    private final Set<Key> failed;
    private final Map<Key, Expr> cycles = new LinkedHashMap<>();

    /**
     * @param maxCalls máximo de llamadas de integración antes de rendirse, para que un integrando
     *                 sin solución falle rápido en vez de agotar el tiempo
     */
    Integrator(Trace trace, int maxCalls) {
        this(trace, maxCalls, new HashSet<>(), null);
    }

    /** Integrador que en la primera llamada usa solo la técnica indicada (todas si es {@code AUTO} o {@code null}). */
    Integrator(Trace trace, int maxCalls, IntegralMethod only) {
        this(trace, maxCalls, new HashSet<>(), only == IntegralMethod.AUTO ? null : only);
    }

    /** Los integradores auxiliares comparten la memoria de integrandos que ya fallaron. */
    private Integrator(Trace trace, int maxCalls, Set<Key> failed, IntegralMethod only) {
        this.trace = trace;
        this.maxCalls = maxCalls;
        this.failed = failed;
        this.only = only;
    }

    // ================================================================ Punto de entrada

    Optional<Expr> integrate(Expr f, String v, int depth) {
        trace.checkBudget();
        if (++calls > maxCalls) throw new Trace.BudgetExceededException("Se superó el máximo de intentos de integración");
        if (depth > MAX_DEPTH) return Optional.empty();

        // ¿Reaparece una integral que se está resolviendo por partes?
        for (Map.Entry<Key, Expr> cycle : cycles.entrySet()) {
            if (!cycle.getKey().variable().equals(v)) continue;
            try {
                Expr ratio = Expr.div(f, cycle.getKey().f());
                if (ratio.isFreeOf(v) && !ratio.isZero()) return Optional.of(Expr.mul(ratio, cycle.getValue()));
            } catch (ArithmeticException e) {
                // No es un múltiplo
            }
        }

        Key key = new Key(f, v);
        if (failed.contains(key) || !inProgress.add(key)) return Optional.empty();
        try {
            IntegralMethod forced = depth == 0 ? only : null;
            Optional<Expr> result;
            if (forced != null) {
                result = forced(forced, f, v, depth);
            } else {
                result = constant(f, v);
                if (result.isEmpty()) result = sum(f, v, depth);
                if (result.isEmpty()) result = splitExponent(f, v, depth);
                if (result.isEmpty()) result = constantMultiple(f, v, depth);
                if (result.isEmpty()) result = table(f, v);
                if (result.isEmpty()) result = reduction(f, v, depth);
                if (result.isEmpty()) result = exponentialReduction(f, v, depth);
                if (result.isEmpty()) result = quadraticDenominator(f, v, depth);
                if (result.isEmpty()) result = partialFractions(f, v, depth);
                if (result.isEmpty()) result = productToSum(f, v, depth);
                if (result.isEmpty()) result = substitution(f, v, depth);
                if (result.isEmpty()) result = trigonometric(f, v, depth);
                if (result.isEmpty()) result = radical(f, v, depth);
                if (result.isEmpty()) result = polynomialDivision(f, v, depth);
                if (result.isEmpty()) result = expand(f, v, depth);
                if (result.isEmpty()) result = parts(f, v, depth);
            }
            if (result.isEmpty() && cycles.isEmpty()) failed.add(key);
            return result;
        } finally {
            inProgress.remove(key);
        }
    }

    /** Solo las estrategias de una técnica, en el orden en que las usa el modo automático. */
    private Optional<Expr> forced(IntegralMethod method, Expr f, String v, int depth) {
        Optional<Expr> result = Optional.empty();
        switch (method) {
            case LINEARITY -> {
                result = sum(f, v, depth);
                if (result.isEmpty()) result = splitExponent(f, v, depth);
                if (result.isEmpty()) result = constantMultiple(f, v, depth);
            }
            case TABLE -> result = table(f, v);
            case REDUCTION -> {
                result = reduction(f, v, depth);
                if (result.isEmpty()) result = exponentialReduction(f, v, depth);
            }
            case COMPLETE_SQUARE -> result = quadraticDenominator(f, v, depth);
            case PARTIAL_FRACTIONS -> result = partialFractions(f, v, depth);
            case TRIGONOMETRIC -> {
                result = productToSum(f, v, depth);
                if (result.isEmpty()) result = trigonometric(f, v, depth);
            }
            case SUBSTITUTION -> result = substitution(f, v, depth);
            case RADICAL -> result = radical(f, v, depth);
            case POLYNOMIAL_DIVISION -> result = polynomialDivision(f, v, depth);
            case EXPAND -> result = expand(f, v, depth);
            case BY_PARTS -> result = parts(f, v, depth);
            case AUTO -> { }
        }
        return result;
    }

    private static Map<String, Expr> params(String v, Object... extra) {
        Map<String, Expr> map = new LinkedHashMap<>();
        map.put("x", Expr.sym(v));
        for (int i = 0; i < extra.length; i += 2) map.put((String) extra[i], (Expr) extra[i + 1]);
        return map;
    }

    // ================================================================ Estrategias simples

    private Optional<Expr> constant(Expr f, String v) {
        if (!f.isFreeOf(v)) return Optional.empty();
        Expr result = Expr.mul(f, Expr.sym(v));
        return Optional.of(trace.leaf(Rule.INT_CONSTANT, f, result, params(v)));
    }

    private Optional<Expr> sum(Expr f, String v, int depth) {
        if (!(f instanceof Expr.Add sum)) return Optional.empty();
        return trace.attempt(Rule.INT_SUM, f, params(v), () -> {
            List<Expr> parts = new ArrayList<>();
            for (Expr term : sum.terms()) {
                Optional<Expr> integral = integrate(term, v, depth + 1);
                if (integral.isEmpty()) return Optional.empty();
                parts.add(integral.get());
            }
            return Optional.of(Expr.add(parts));
        });
    }

    /** {@code e^(a + g(x)) = e^a·e^g(x)}: la parte del exponente que no depende de la variable sale como constante. */
    private Optional<Expr> splitExponent(Expr f, String v, int depth) {
        if (!(f instanceof Expr.Pow p) || !p.base().equals(Expr.E) || !(p.exponent() instanceof Expr.Add sum)) return Optional.empty();
        List<Expr> free = new ArrayList<>();
        List<Expr> varying = new ArrayList<>();
        for (Expr term : sum.terms()) (term.isFreeOf(v) ? free : varying).add(term);
        if (free.isEmpty() || varying.isEmpty()) return Optional.empty();
        Expr constant = Expr.exp(Expr.add(free));
        Expr rest = Expr.exp(Expr.add(varying));
        return trace.attempt(Rule.INT_SPLIT_EXPONENT, f, params(v, "c", constant, "f", rest),
                () -> integrate(rest, v, depth + 1).map(inner -> Expr.mul(constant, inner)));
    }

    private Optional<Expr> constantMultiple(Expr f, String v, int depth) {
        if (!(f instanceof Expr.Mul m)) return Optional.empty();

        List<Expr> constants = new ArrayList<>();
        List<Expr> varying = new ArrayList<>();
        for (Expr factor : m.factors()) (factor.isFreeOf(v) ? constants : varying).add(factor);
        if (constants.isEmpty() || varying.isEmpty()) return Optional.empty();

        Expr c = Expr.mul(constants);
        Expr rest = Expr.mul(varying);
        return trace.attempt(Rule.INT_CONSTANT_MULTIPLE, f, params(v, "c", c, "f", rest), () ->
                integrate(rest, v, depth + 1).map(inner -> Expr.mul(c, inner)));
    }

    private Optional<Expr> table(Expr f, String v) {
        Optional<Table.Hit> hit = Table.lookup(f, v);
        if (hit.isEmpty()) return Optional.empty();
        Table.Hit h = hit.get();
        java.util.List<barnesinside.mathutils.cas.steps.Step> derivation = Table.derivation(h, f, Expr.sym(v));
        if (derivation.isEmpty()) return Optional.of(trace.leaf(h.rule(), f, h.antiderivative(), h.params()));
        // Una fórmula que no es de un solo paso lleva su deducción dentro
        return Optional.of(trace.step(h.rule(), f, h.params(), () -> {
            for (barnesinside.mathutils.cas.steps.Step s : derivation) trace.leaf(s.rule(), s.before(), s.after(), s.params());
            return h.antiderivative();
        }));
    }

    // ================================================================ Trigonométricas

    private Optional<Expr> trigonometric(Expr f, String v, int depth) {
        Optional<Trig.Form> form = Trig.parse(f, v);
        if (form.isPresent()) {
            Optional<Expr> result = trigonometricPower(f, v, depth, form.get());
            if (result.isPresent()) return result;
        }
        return weierstrass(f, v, depth);
    }

    /** Un producto de ángulos lineales distintos se pasa a suma antes de intentar sustituciones. */
    private Optional<Expr> productToSum(Expr f, String v, int depth) {
        Optional<Trig.Rewrite> product = Trig.productToSum(f, v);
        return product.isPresent() ? rewrite(f, v, depth, product.get()) : Optional.empty();
    }

    private Optional<Expr> rewrite(Expr f, String v, int depth, Trig.Rewrite rewrite) {
        if (rewrite.expression().equals(f)) return Optional.empty();
        return trace.attempt(rewrite.rule(), f, rewrite.params(), () -> integrate(rewrite.expression(), v, depth + 1));
    }

    private Optional<Expr> trigonometricPower(Expr f, String v, int depth, Trig.Form form) {
        Expr x = Expr.sym(v);
        int sin = form.sin();
        int cos = form.cos();

        // tan^n y cot^n: se baja la potencia en dos con la identidad pitagórica
        if (sin + cos == 0 && Math.abs(sin) >= 2) {
            int n = Math.abs(sin);
            boolean tangent = sin > 0;
            Expr base = Expr.func(tangent ? Fn.TAN : Fn.COT, x);
            Expr lower = Expr.pow(base, Expr.num(n - 2));
            Expr first = Expr.div(Expr.pow(base, Expr.num(n - 1)), Expr.num(n - 1));
            return trace.attempt(tangent ? Rule.INT_TRIG_TAN_REDUCTION : Rule.INT_TRIG_COT_REDUCTION, f,
                    params(v, "n", Expr.num(n)), () -> integrate(lower, v, depth + 1).map(rest ->
                            tangent ? Expr.sub(first, rest) : Expr.sub(Expr.neg(first), rest)));
        }

        // sec^n y csc^n con n impar: fórmula de reducción
        boolean secant = sin == 0 && cos <= -3 && cos % 2 != 0;
        boolean cosecant = cos == 0 && sin <= -3 && sin % 2 != 0;
        if (secant || cosecant) {
            int n = -(secant ? cos : sin);
            Expr base = Expr.func(secant ? Fn.SEC : Fn.CSC, x);
            Expr other = Expr.func(secant ? Fn.TAN : Fn.COT, x);
            Expr lower = Expr.pow(base, Expr.num(n - 2));
            Expr first = Expr.div(Expr.mul(lower, other), Expr.num(n - 1));
            Expr factor = Expr.num(n - 2, n - 1);
            return trace.attempt(secant ? Rule.INT_TRIG_SEC_REDUCTION : Rule.INT_TRIG_CSC_REDUCTION, f,
                    params(v, "n", Expr.num(n)), () -> integrate(lower, v, depth + 1).map(rest ->
                            Expr.add(secant ? first : Expr.neg(first), Expr.mul(factor, rest))));
        }

        Optional<Trig.Rewrite> rewrite = Trig.odd(form, v);
        if (rewrite.isEmpty()) rewrite = Trig.halfAngle(form, v);
        if (rewrite.isEmpty()) rewrite = Trig.tangentSubstitution(form, v);
        return rewrite.isPresent() ? rewrite(f, v, depth, rewrite.get()) : Optional.empty();
    }

    /** Funciones racionales de seno y coseno: {@code t = tan(x/2)} las vuelve racionales en {@code t}. */
    private Optional<Expr> weierstrass(Expr f, String v, int depth) {
        Optional<RationalFunction> converted = Trig.weierstrass(f, v);
        if (converted.isEmpty()) return Optional.empty();

        String t = freshName(f, v);
        Expr ts = Expr.sym(t);
        Expr g = converted.get().toExpr(t);
        Expr half = Expr.func(Fn.TAN, Expr.div(Expr.sym(v), Expr.num(2)));
        return trace.attempt(Rule.INT_TRIG_WEIERSTRASS, f, params(v, "s", ts, "g", g), () -> {
            Optional<Expr> integral = integrate(g, t, depth + 1);
            if (integral.isEmpty()) return Optional.empty();
            Expr back = integral.get().substitute(t, half);
            return verified(f, back, v) ? Optional.of(back) : Optional.empty();
        });
    }

    // ================================================================ Radicales

    private Optional<Expr> radical(Expr f, String v, int depth) {
        // Si al expandir sale una suma, se integra término a término antes de sustituir
        if (!(f instanceof Expr.Add) && Expand.expand(f) instanceof Expr.Add) return Optional.empty();
        Optional<Expr> result = linearRadical(f, v, depth);
        return result.isPresent() ? result : quadraticRadical(f, v, depth);
    }

    /** Raíces de una expresión lineal: {@code t = (ax+b)^(1/n)} las quita todas a la vez. */
    private Optional<Expr> linearRadical(Expr f, String v, int depth) {
        Optional<Radicals.LinearRadical> found = Radicals.linear(f, v);
        if (found.isEmpty()) return Optional.empty();
        Radicals.LinearRadical r = found.get();

        int n = r.n();
        Rational a = r.polynomial().coefficient(1);
        Rational b = r.polynomial().coefficient(0);
        String t = freshName(f, v);
        Expr ts = Expr.sym(t);
        Expr root = Expr.pow(r.base(), Expr.num(1, n));

        Expr mapped = Radicals.mapPowers(f, r.base(), e -> Expr.pow(ts, Expr.num(e.multiply(Rational.of(n)))));
        Expr x = Expr.div(Expr.sub(Expr.pow(ts, Expr.num(n)), Expr.num(b)), Expr.num(a));
        Expr dx = Expr.div(Expr.mul(Expr.num(n), Expr.pow(ts, Expr.num(n - 1))), Expr.num(a));
        Expr g = Expr.mul(mapped.substitute(v, x), dx);
        if (!g.isFreeOf(v)) return Optional.empty();

        return trace.attempt(Rule.INT_RADICAL_SUBSTITUTION, f, params(v, "u", root, "s", ts, "g", g), () -> {
            Optional<Expr> integral = integrate(g, t, depth + 1);
            if (integral.isEmpty()) return Optional.empty();
            Expr back = integral.get().substitute(t, root);
            return verified(f, back, v) ? Optional.of(back) : Optional.empty();
        });
    }

    /** Raíces de un cuadrático: se completa el cuadrado y se aplica una sustitución trigonométrica. */
    private Optional<Expr> quadraticRadical(Expr f, String v, int depth) {
        Optional<Radicals.QuadraticRadical> found = Radicals.quadratic(f, v);
        if (found.isEmpty()) return Optional.empty();
        Expr q = found.get().base();
        Polynomial poly = found.get().polynomial();
        Rational alpha = poly.coefficient(2);
        Rational beta = poly.coefficient(1);
        Rational gamma = poly.coefficient(0);
        String t = freshName(f, v);
        Expr ts = Expr.sym(t);
        Expr x = Expr.sym(v);

        if (!beta.isZero()) {
            Rational h = beta.divide(Rational.of(2).multiply(alpha));
            Rational k = gamma.subtract(beta.multiply(beta).divide(Rational.of(4).multiply(alpha)));
            Expr centered = Expr.add(Expr.mul(Expr.num(alpha), Expr.pow(ts, Expr.num(2))), Expr.num(k));
            Expr g = Radicals.mapPowers(f, q, e -> Expr.pow(centered, Expr.num(e))).substitute(v, Expr.sub(ts, Expr.num(h)));
            if (!g.isFreeOf(v)) return Optional.empty();
            Expr shift = Expr.add(x, Expr.num(h));
            Expr completed = Expr.add(Expr.mul(Expr.num(alpha), Expr.pow(shift, Expr.num(2))), Expr.num(k));
            return trace.attempt(Rule.INT_RADICAL_COMPLETE_SQUARE, f,
                    params(v, "q", q, "c", completed, "s", ts, "shift", shift, "g", g), () -> {
                        Optional<Expr> integral = integrate(g, t, depth + 1);
                        if (integral.isEmpty()) return Optional.empty();
                        Expr back = integral.get().substitute(t, shift);
                        return verified(f, back, v) ? Optional.of(back) : Optional.empty();
                    });
        }

        // a·x² + c sin término lineal
        Expr sqrtGamma = Expr.sqrt(Expr.num(gamma.abs()));
        Expr m = Expr.sqrt(Expr.num(gamma.abs().divide(alpha.abs())));
        Rule rule;
        Expr xs;
        Expr root;
        Expr dx;
        Expr sin;
        Expr cos;
        Expr angle;
        Expr rootOfQ = Expr.sqrt(q);
        if (alpha.signum() < 0 && gamma.signum() > 0) {
            rule = Rule.INT_TRIG_SUBSTITUTION_SIN;
            xs = Expr.mul(m, Expr.sin(ts));
            root = Expr.mul(sqrtGamma, Expr.cos(ts));
            dx = Expr.mul(m, Expr.cos(ts));
            sin = Expr.div(x, m);
            cos = Expr.div(rootOfQ, sqrtGamma);
            angle = Expr.func(Fn.ASIN, Expr.div(x, m));
        } else if (alpha.signum() > 0 && gamma.signum() > 0) {
            rule = Rule.INT_TRIG_SUBSTITUTION_TAN;
            Expr sec = Expr.func(Fn.SEC, ts);
            xs = Expr.mul(m, Expr.func(Fn.TAN, ts));
            root = Expr.mul(sqrtGamma, sec);
            dx = Expr.mul(m, Expr.pow(sec, Expr.num(2)));
            cos = Expr.div(sqrtGamma, rootOfQ);
            sin = Expr.mul(Expr.div(x, m), cos);
            angle = Expr.func(Fn.ATAN, Expr.div(x, m));
        } else if (alpha.signum() > 0 && gamma.signum() < 0) {
            rule = Rule.INT_TRIG_SUBSTITUTION_SEC;
            Expr tan = Expr.func(Fn.TAN, ts);
            Expr magnitude = Expr.abs(x);
            xs = Expr.mul(m, Expr.func(Fn.SEC, ts));
            root = Expr.mul(sqrtGamma, tan);
            dx = Expr.mul(m, Expr.func(Fn.SEC, ts), tan);
            cos = Expr.div(m, magnitude);
            sin = Expr.div(Expr.sqrt(Expr.div(q, Expr.num(alpha))), magnitude);
            angle = Expr.func(Fn.ACOS, Expr.div(m, magnitude));
        } else {
            return Optional.empty();
        }

        Expr rootT = root;
        Expr mapped = Radicals.mapPowers(f, q, e -> {
            Rational doubled = e.multiply(Rational.of(2));
            Expr factors = rootT instanceof Expr.Mul mul ? Expr.mul(mul.factors().stream()
                    .map(fa -> Expr.pow(fa, Expr.num(doubled))).toList()) : Expr.pow(rootT, Expr.num(doubled));
            return factors;
        });
        Expr g = Expr.mul(mapped.substitute(v, xs), dx);
        if (!g.isFreeOf(v)) return Optional.empty();

        Expr sinX = sin;
        Expr cosX = cos;
        Expr angleX = angle;
        boolean secant = rule == Rule.INT_TRIG_SUBSTITUTION_SEC;
        return trace.attempt(rule, f, params(v, "m", m, "q", q, "xs", xs, "dx", dx, "root", root, "s", ts, "g", g), () -> {
            Optional<Expr> integral = integrate(g, t, depth + 1);
            if (integral.isEmpty()) return Optional.empty();
            Expr back = Radicals.undo(integral.get(), ts, sinX, cosX, angleX);
            if (verified(f, back, v)) return Optional.of(back);
            if (secant) {
                // La sustitución vale para x > 0: con f par la primitiva es impar, y con f impar es par
                Expr odd = Expr.mul(Expr.div(x, Expr.abs(x)), back);
                if (verified(f, odd, v)) return Optional.of(odd);
            }
            return Optional.empty();
        });
    }

    /** Cociente de polinomios con grado del numerador mayor o igual que el del denominador. */
    private Optional<Expr> polynomialDivision(Expr f, String v, int depth) {
        Algebra.Fraction fraction = Algebra.fraction(f);
        if (fraction.denominator().isOne()) return Optional.empty();

        Optional<Polynomial> numerator = Polynomial.from(fraction.numerator(), v);
        Optional<Polynomial> denominator = Polynomial.from(fraction.denominator(), v);
        if (numerator.isEmpty() || denominator.isEmpty() || denominator.get().degree() < 1
                || numerator.get().degree() < denominator.get().degree()) {
            return Optional.empty();
        }

        Polynomial.DivMod division = numerator.get().divide(denominator.get());
        Expr quotient = division.quotient().toExpr(v);
        Expr remainder = division.remainder().toExpr(v);
        Expr rewritten = Expr.add(quotient, Expr.div(remainder, fraction.denominator()));
        if (rewritten.equals(f)) return Optional.empty();

        return trace.attempt(Rule.INT_POLYNOMIAL_DIVISION, f,
                params(v, "n", fraction.numerator(), "d", fraction.denominator(), "q", quotient, "r", remainder),
                () -> integrate(rewritten, v, depth + 1));
    }

    // ================================================================ Funciones racionales

    /** Factoriza el denominador y descompone en fracciones parciales (grado del numerador menor). */
    private Optional<Expr> partialFractions(Expr f, String v, int depth) {
        Algebra.Fraction fraction = Algebra.fraction(f);
        Optional<Polynomial> n = Polynomial.from(fraction.numerator(), v);
        Optional<Polynomial> d = Polynomial.from(fraction.denominator(), v);
        if (n.isEmpty() || d.isEmpty() || d.get().degree() < 2 || n.get().degree() >= d.get().degree()) {
            return Optional.empty();
        }
        Optional<Rationals.Factorization> factored = Rationals.factor(d.get());
        if (factored.isEmpty() || isSubstitutionCase(n.get(), factored.get())) return Optional.empty();
        Optional<List<Rationals.Term>> terms = Rationals.partialFractions(n.get(), factored.get());
        if (terms.isEmpty() || terms.get().isEmpty()) return Optional.empty();

        Expr parts = Expr.add(terms.get().stream().map(t -> t.toExpr(v)).toList());
        if (parts.equals(f)) return Optional.empty();
        return trace.attempt(Rule.INT_PARTIAL_FRACTIONS, f,
                params(v, "n", fraction.numerator(), "d", fraction.denominator(),
                        "factored", factored.get().toExpr(v), "parts", parts),
                () -> integrate(parts, v, depth + 1));
    }

    /**
     * Una sola potencia {@code base^m} en el denominador y, si el numerador es un múltiplo de la
     * derivada de la base (o la base es lineal), basta una sustitución {@code u = base}.
     */
    private static boolean isSubstitutionCase(Polynomial numerator, Rationals.Factorization denominator) {
        if (denominator.factors().size() != 1) return false;
        Polynomial base = denominator.factors().get(0).base();
        if (base.degree() == 1) return numerator.degree() <= 0;
        Polynomial derivative = base.derivative();
        if (numerator.degree() != derivative.degree()) return numerator.isZero();
        Rational ratio = numerator.leading().divide(derivative.leading());
        return numerator.equals(derivative.scale(ratio));
    }

    /**
     * {@code (Bx + C) / (x² + bx + c)^k} con el cuadrático sin raíces reales: separa la parte
     * logarítmica o completa el cuadrado para llegar a la forma de arcotangente.
     */
    private Optional<Expr> quadraticDenominator(Expr f, String v, int depth) {
        Algebra.Fraction fraction = Algebra.fraction(f);
        Optional<Polynomial> n = Polynomial.from(fraction.numerator(), v);
        Optional<Polynomial> d = Polynomial.from(fraction.denominator(), v);
        if (n.isEmpty() || d.isEmpty() || d.get().degree() < 2 || n.get().degree() > 1) return Optional.empty();
        Optional<Rationals.Factorization> factored = Rationals.factor(d.get());
        if (factored.isEmpty() || factored.get().factors().size() != 1
                || factored.get().factors().get(0).base().degree() != 2
                || isSubstitutionCase(n.get(), factored.get())) {
            return Optional.empty();
        }

        Rationals.Factor factor = factored.get().factors().get(0);
        Rational c = factored.get().constant();
        Rational leading = n.get().coefficient(1);
        Rational constant = n.get().coefficient(0);
        Rational b = factor.base().coefficient(1);
        Rational cc = factor.base().coefficient(0);
        Expr x = Expr.sym(v);
        Expr q = factor.base().toExpr(v);
        Expr power = Expr.pow(q, Expr.num(factor.multiplicity()));

        if (!leading.isZero()) {
            // Bx + C = (B/2)(2x + b) + (C - B·b/2)
            Expr dq = Expr.add(Expr.mul(Expr.num(2), x), Expr.num(b));
            Rational lambda = leading.divide(Rational.of(2)).divide(c);
            Rational mu = constant.subtract(leading.multiply(b).divide(Rational.of(2))).divide(c);
            Expr rewritten = Expr.add(Expr.div(Expr.mul(Expr.num(lambda), dq), power),
                    Expr.div(Expr.num(mu), power));
            if (rewritten.equals(f)) return Optional.empty();
            return trace.attempt(Rule.INT_QUADRATIC_SPLIT, f, params(v, "q", q, "dq", dq),
                    () -> integrate(rewritten, v, depth + 1));
        }

        if (b.isZero()) return Optional.empty();
        Rational p = b.divide(Rational.of(2));
        Rational remaining = cc.subtract(p.multiply(p));
        Expr shifted = Expr.add(x, Expr.num(p));
        Expr square = Expr.add(Expr.pow(shifted, Expr.num(2)), Expr.num(remaining));
        Expr rewritten = Expr.mul(Expr.num(constant.divide(c)), Expr.pow(square, Expr.num(-factor.multiplicity())));
        return trace.attempt(Rule.INT_COMPLETE_SQUARE, f, params(v, "q", q, "s", square, "u", shifted),
                () -> integrate(rewritten, v, depth + 1));
    }

    /** {@code e^x / x^n} con {@code n ≥ 2}: por partes baja la potencia hasta la exponencial integral. */
    private Optional<Expr> exponentialReduction(Expr f, String v, int depth) {
        Expr x = Expr.sym(v);
        if (!(f instanceof Expr.Mul m) || m.factors().size() != 2) return Optional.empty();
        Expr exp = Expr.exp(x);
        Expr power = m.factors().get(0).equals(exp) ? m.factors().get(1) : m.factors().get(1).equals(exp) ? m.factors().get(0) : null;
        if (!(power instanceof Expr.Pow p) || !p.base().equals(x) || !(p.exponent() instanceof Expr.Num e)
                || !e.value().isInteger() || e.value().signum() >= 0 || !e.value().fitsInt()) {
            return Optional.empty();
        }
        int n = -e.value().intValueExact();
        if (n < 2 || n > 12) return Optional.empty();
        Expr lower = Expr.mul(exp, Expr.pow(x, Expr.num(-(n - 1))));
        Expr first = Expr.neg(Expr.div(lower, Expr.num(n - 1)));
        return trace.attempt(Rule.INT_EI_REDUCTION, f, params(v, "n", Expr.num(n)), () ->
                integrate(lower, v, depth + 1).map(rest -> Expr.add(first, Expr.div(rest, Expr.num(n - 1)))));
    }

    /** {@code 1/(x² + d)^k} con {@code k ≥ 2}: fórmula de reducción que baja la potencia. */
    private Optional<Expr> reduction(Expr f, String v, int depth) {
        if (!(f instanceof Expr.Pow p) || !(p.exponent() instanceof Expr.Num e) || !e.value().isInteger()
                || e.value().signum() >= 0 || !e.value().fitsInt()) {
            return Optional.empty();
        }
        int k = -e.value().intValueExact();
        if (k < 2 || k > 12) return Optional.empty();
        Expr x = Expr.sym(v);
        if (!(Table.squarePlusConstant(p.base(), x) instanceof Expr.Num dn) || dn.value().isZero()) {
            return Optional.empty();
        }

        Rational d = dn.value();
        Expr lower = Expr.pow(p.base(), Expr.num(-(k - 1)));
        Rational factor = Rational.of(2L * k - 3).divide(Rational.of(2L * (k - 1)).multiply(d));
        Expr first = Expr.div(x, Expr.mul(Expr.num(Rational.of(2L * (k - 1)).multiply(d)),
                Expr.pow(p.base(), Expr.num(k - 1))));
        return trace.attempt(Rule.INT_REDUCTION, f, params(v, "d", dn, "k", Expr.num(k)), () ->
                integrate(lower, v, depth + 1).map(rest -> Expr.add(first, Expr.mul(Expr.num(factor), rest))));
    }

    private Optional<Expr> expand(Expr f, String v, int depth) {
        Expr expanded = Expand.expand(f);
        if (expanded.equals(f)) return Optional.empty();
        return trace.attempt(Rule.INT_EXPAND, f, params(v), () -> integrate(expanded, v, depth + 1));
    }

    // ================================================================ Sustitución

    private Optional<Expr> substitution(Expr f, String v, int depth) {
        String t = freshName(f, v);
        Expr ts = Expr.sym(t);

        for (Expr u : candidates(f, v)) {
            Expr du;
            try {
                du = Derivatives.derivative(u, v).result();
            } catch (RuntimeException e) {
                continue;
            }
            if (du == null || du.isZero()) continue;

            Expr g = transformed(f, u, du, v, ts);
            if (g == null) continue;

            Expr inner = u;
            Expr derivative = du;
            Expr integrand = g;
            Optional<Expr> result = trace.attempt(Rule.INT_SUBSTITUTION, f,
                    params(v, "u", inner, "du", derivative, "g", integrand, "s", ts), () -> {
                        Optional<Expr> integral = integrate(integrand, t, depth + 1);
                        if (integral.isEmpty()) return Optional.empty();
                        Expr back = integral.get().substitute(t, inner);
                        return verified(f, back, v) ? Optional.of(back) : Optional.empty();
                    });
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    /**
     * El integrando reescrito en la nueva variable {@code t = u(x)}, o {@code null} si no se puede
     * escribir solo en función de {@code t}. Para {@code u} lineal se despeja {@code x}; en otro caso
     * se divide por {@code du} y se reemplaza {@code u} por {@code t}.
     */
    private Expr transformed(Expr f, Expr u, Expr du, String v, Expr t) {
        Optional<Polynomial> linear = Polynomial.from(u, v);
        if (linear.isPresent() && linear.get().degree() == 1) {
            Rational a = linear.get().coefficient(1);
            Rational b = linear.get().coefficient(0);
            Expr inverse = Expr.div(Expr.sub(t, Expr.num(b)), Expr.num(a));
            Expr g = Expr.div(f.substitute(v, inverse), Expr.num(a));
            return g.isFreeOf(v) ? g : null;
        }

        try {
            Expr ratio = Expr.div(f, du);
            Expr g = Algebra.replace(ratio, u, t);
            if (g.isFreeOf(v)) return g;
            g = Algebra.replace(Expand.expand(ratio), u, t);
            return g.isFreeOf(v) ? g : null;
        } catch (ArithmeticException e) {
            return null;
        }
    }

    /** Subexpresiones que pueden ser la nueva variable, de las más pequeñas a las más grandes. */
    private List<Expr> candidates(Expr f, String v) {
        Set<Expr> found = new LinkedHashSet<>();
        collect(f, v, found, false);
        found.remove(f);
        List<Expr> sorted = new ArrayList<>(found);
        sorted.sort(Comparator.comparingInt(Integrator::size));
        return sorted.size() > MAX_CANDIDATES ? sorted.subList(0, MAX_CANDIDATES) : sorted;
    }

    /**
     * Recoge subexpresiones. Una expresión lineal solo es candidata si es el argumento de una
     * función o la base de una potencia: dentro de un polinomio (como el {@code 4x} de
     * {@code x² + 4x + 5}) solo cambiaría la escala de la variable.
     */
    private static void collect(Expr e, String v, Set<Expr> out, boolean direct) {
        if (e.isFreeOf(v)) return;
        switch (e) {
            case Expr.Sym s -> { }
            case Expr.Add a -> {
                if (direct || !isLinear(e, v)) out.add(e);
                a.terms().forEach(t -> collect(t, v, out, false));
            }
            case Expr.Mul m -> {
                if (direct || !isLinear(e, v)) out.add(e);
                m.factors().forEach(t -> collect(t, v, out, false));
            }
            case Expr.Pow p -> {
                out.add(e);
                collect(p.base(), v, out, true);
                collect(p.exponent(), v, out, true);
            }
            case Expr.Func fn -> {
                out.add(e);
                collect(fn.arg(), v, out, true);
            }
            default -> { }
        }
    }

    private static boolean isLinear(Expr e, String v) {
        Optional<Polynomial> p = Polynomial.from(e, v);
        return p.isPresent() && p.get().degree() == 1;
    }

    private static int size(Expr e) {
        return switch (e) {
            case Expr.Add a -> 1 + a.terms().stream().mapToInt(Integrator::size).sum();
            case Expr.Mul m -> 1 + m.factors().stream().mapToInt(Integrator::size).sum();
            case Expr.Pow p -> 1 + size(p.base()) + size(p.exponent());
            case Expr.Func f -> 1 + size(f.arg());
            default -> 1;
        };
    }

    private static String freshName(Expr f, String v) {
        Set<String> taken = new HashSet<>(f.variables());
        taken.add(v);
        return FRESH_NAMES.stream().filter(n -> !taken.contains(n)).findFirst().orElse("u_1");
    }

    // ================================================================ Integración por partes

    private Optional<Expr> parts(Expr f, String v, int depth) {
        List<Expr[]> choices = new ArrayList<>(); // {u, dv}
        if (f instanceof Expr.Mul m) {
            List<Expr> factors = m.factors();
            for (int i = 0; i < factors.size(); i++) {
                List<Expr> others = new ArrayList<>(factors);
                Expr u = others.remove(i);
                if (u.isFreeOf(v)) continue;
                choices.add(new Expr[]{u, Expr.mul(others)});
            }
        } else if (isLogarithmicOrInverseTrigonometric(f)) {
            choices.add(new Expr[]{f, Expr.ONE});
        }
        choices.sort(Comparator.comparingInt(c -> liateRank(c[0], v)));

        int tried = 0;
        for (Expr[] choice : choices) {
            if (tried++ >= 3) break;
            Expr u = choice[0];
            Expr dv = choice[1];

            Optional<Expr> vValue = quiet(dv, v, depth);
            if (vValue.isEmpty()) continue;

            Expr du;
            try {
                du = Derivatives.derivative(u, v).result();
            } catch (RuntimeException e) {
                continue;
            }
            if (du == null) continue;

            Expr vv = vValue.get();
            Optional<Expr> result = trace.attempt(Rule.INT_PARTS, f, params(v, "u", u, "dv", dv, "du", du, "v", vv),
                    () -> partsStep(f, v, u, dv, du, vv, depth));
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    private Optional<Expr> partsStep(Expr f, String v, Expr u, Expr dv, Expr du, Expr vv, int depth) {
        // Se vuelve a integrar dv con la traza activa para que sus pasos queden como subpasos
        if (!dv.isOne()) integrate(dv, v, depth + 1);

        Key key = new Key(f, v);
        Expr unknown = Expr.sym(cycles.isEmpty() ? "I" : "I" + (cycles.size() + 1));
        cycles.put(key, unknown);
        Optional<Expr> remaining;
        try {
            remaining = integrate(Expr.mul(vv, du), v, depth + 1);
        } finally {
            cycles.remove(key);
        }
        if (remaining.isEmpty()) return Optional.empty();

        Expr result = Expr.sub(Expr.mul(u, vv), remaining.get());
        String name = ((Expr.Sym) unknown).name();

        if (!result.isFreeOf(name)) {
            // La integral original reaparece: I = A + c·I  =>  I = A / (1 - c)
            Expr a = Expand.expand(result.substitute(name, Expr.ZERO));
            Expr c = Expand.expand(Expr.sub(result.substitute(name, Expr.ONE), a));
            if (!c.isFreeOf(name) || c.equals(Expr.ONE)) return Optional.empty();
            Expr solved;
            try {
                solved = Expr.div(a, Expand.expand(Expr.sub(Expr.ONE, c)));
            } catch (ArithmeticException e) {
                return Optional.empty();
            }
            trace.leaf(Rule.INT_CYCLE, f, solved, params(v, "a", a, "c", c));
            result = solved;
        }

        if (containsCycleSymbol(result)) return Optional.of(result);
        return verified(f, result, v) ? Optional.of(result) : Optional.empty();
    }

    /** Integra sin dejar rastro en los pasos, con un integrador independiente. */
    private Optional<Expr> quiet(Expr g, String v, int depth) {
        if (g.isOne()) return Optional.of(Expr.sym(v));
        Integrator scratch = new Integrator(new Trace(100, 800), 150, failed, null);
        try {
            return scratch.integrate(g, v, depth + 1);
        } catch (Trace.BudgetExceededException e) {
            return Optional.empty();
        }
    }

    private static boolean isLogarithmicOrInverseTrigonometric(Expr f) {
        Expr core = f instanceof Expr.Pow p && p.exponent() instanceof Expr.Num n && n.value().isInteger()
                && n.value().signum() > 0 ? p.base() : f;
        return core instanceof Expr.Func fn
                && (fn.fn() == Fn.LN || fn.fn() == Fn.ASIN || fn.fn() == Fn.ACOS || fn.fn() == Fn.ATAN
                || fn.fn() == Fn.ERF || fn.fn() == Fn.SI || fn.fn() == Fn.CI || fn.fn() == Fn.EI || fn.fn() == Fn.LI);
    }

    /** LIATE: logarítmica, trigonométrica inversa, algebraica, trigonométrica, exponencial. */
    private static int liateRank(Expr u, String v) {
        if (u instanceof Expr.Func fn) {
            return switch (fn.fn()) {
                case LN -> 0;
                case ASIN, ACOS, ATAN, ERF, SI, CI, EI, LI -> 1;
                case SIN, COS, TAN, COT, SEC, CSC -> 3;
                case SINH, COSH, TANH -> 4;
                default -> 5;
            };
        }
        if (u instanceof Expr.Pow p) {
            if (p.base().equals(Expr.E) || (p.base().isFreeOf(v) && !p.exponent().isFreeOf(v))) return 4;
            if (p.base() instanceof Expr.Func) return liateRank(p.base(), v);
        }
        if (Polynomial.from(u, v).isPresent() || (u instanceof Expr.Pow p && p.base().equals(Expr.sym(v)))) return 2;
        return 5;
    }

    // ================================================================ Verificación

    private static boolean containsCycleSymbol(Expr e) {
        return e.variables().stream().anyMatch(name -> name.matches("I\\d*"));
    }

    /** Comprueba que la derivada de {@code antiderivative} coincide con el integrando. */
    static boolean verified(Expr f, Expr antiderivative, String v) {
        if (containsCycleSymbol(antiderivative)) return true;
        try {
            Expr derivative = Derivatives.derivative(antiderivative, v).result();
            if (derivative == null) return false;
            return Equivalence.check(derivative, f) != Equivalence.Result.DIFFERENT;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
