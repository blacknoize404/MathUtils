package MathUtils.cas.integral;

import MathUtils.cas.diff.Derivatives;
import MathUtils.cas.expr.Algebra;
import MathUtils.cas.expr.Equivalence;
import MathUtils.cas.expr.Expand;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.Fn;
import MathUtils.cas.expr.Rational;
import MathUtils.cas.poly.Polynomial;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Trace;

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
    private int calls;
    private final Set<Key> inProgress = new HashSet<>();
    private final Set<Key> failed;
    private final Map<Key, Expr> cycles = new LinkedHashMap<>();

    /**
     * @param maxCalls máximo de llamadas de integración antes de rendirse, para que un integrando
     *                 sin solución falle rápido en vez de agotar el tiempo
     */
    Integrator(Trace trace, int maxCalls) {
        this(trace, maxCalls, new HashSet<>());
    }

    /** Los integradores auxiliares comparten la memoria de integrandos que ya fallaron. */
    private Integrator(Trace trace, int maxCalls, Set<Key> failed) {
        this.trace = trace;
        this.maxCalls = maxCalls;
        this.failed = failed;
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
            Optional<Expr> result = constant(f, v);
            if (result.isEmpty()) result = sum(f, v, depth);
            if (result.isEmpty()) result = constantMultiple(f, v, depth);
            if (result.isEmpty()) result = table(f, v);
            if (result.isEmpty()) result = reduction(f, v, depth);
            if (result.isEmpty()) result = quadraticDenominator(f, v, depth);
            if (result.isEmpty()) result = partialFractions(f, v, depth);
            if (result.isEmpty()) result = substitution(f, v, depth);
            if (result.isEmpty()) result = polynomialDivision(f, v, depth);
            if (result.isEmpty()) result = expand(f, v, depth);
            if (result.isEmpty()) result = parts(f, v, depth);
            if (result.isEmpty() && cycles.isEmpty()) failed.add(key);
            return result;
        } finally {
            inProgress.remove(key);
        }
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
        return Optional.of(trace.leaf(h.rule(), f, h.antiderivative(), h.params()));
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

    /** {@code 1/(x² + d)^k} con {@code k ≥ 2}: fórmula de reducción que baja la potencia. */
    private Optional<Expr> reduction(Expr f, String v, int depth) {
        if (!(f instanceof Expr.Pow p) || !(p.exponent() instanceof Expr.Num e) || !e.value().isInteger()
                || e.value().signum() >= 0 || !e.value().fitsInt()) {
            return Optional.empty();
        }
        int k = -e.value().intValueExact();
        if (k < 2 || k > 12) return Optional.empty();
        Expr x = Expr.sym(v);
        if (!(Table.squarePlusConstant(p.base(), x) instanceof Expr.Num dn) || dn.value().signum() <= 0) {
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
        collect(f, v, found);
        found.remove(f);
        List<Expr> sorted = new ArrayList<>(found);
        sorted.sort(Comparator.comparingInt(Integrator::size));
        return sorted.size() > MAX_CANDIDATES ? sorted.subList(0, MAX_CANDIDATES) : sorted;
    }

    private static void collect(Expr e, String v, Set<Expr> out) {
        if (e.isFreeOf(v)) return;
        switch (e) {
            case Expr.Sym s -> { }
            case Expr.Add a -> {
                out.add(e);
                a.terms().forEach(t -> collect(t, v, out));
            }
            case Expr.Mul m -> {
                out.add(e);
                m.factors().forEach(t -> collect(t, v, out));
            }
            case Expr.Pow p -> {
                out.add(e);
                collect(p.base(), v, out);
                collect(p.exponent(), v, out);
            }
            case Expr.Func fn -> {
                out.add(e);
                collect(fn.arg(), v, out);
            }
            default -> { }
        }
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
        Integrator scratch = new Integrator(new Trace(100, 800), 150, failed);
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
                && (fn.fn() == Fn.LN || fn.fn() == Fn.ASIN || fn.fn() == Fn.ACOS || fn.fn() == Fn.ATAN);
    }

    /** LIATE: logarítmica, trigonométrica inversa, algebraica, trigonométrica, exponencial. */
    private static int liateRank(Expr u, String v) {
        if (u instanceof Expr.Func fn) {
            return switch (fn.fn()) {
                case LN -> 0;
                case ASIN, ACOS, ATAN -> 1;
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
