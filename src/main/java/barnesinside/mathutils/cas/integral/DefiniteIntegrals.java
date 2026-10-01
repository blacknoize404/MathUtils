package barnesinside.mathutils.cas.integral;

import barnesinside.mathutils.cas.expr.Equivalence;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.limit.Limits;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Integrales definidas e impropias con la regla de Barrow. Los extremos infinitos o singulares se
 * sustituyen por límites, las singularidades interiores parten el intervalo y los resultados
 * exactos se contrastan con cuadratura numérica antes de devolverlos.
 */
final class DefiniteIntegrals {

    private DefiniteIntegrals() {
    }

    /** Resultado de una resolución parcial: el valor (o {@code null}), el estado y los pasos. */
    private record Outcome(Solution.Status status, Expr value, List<Step> steps) {
        static Outcome of(Solution.Status status, List<Step> steps) {
            return new Outcome(status, null, steps);
        }
    }

    static Solution solve(Expr f, String v, Expr lower, Expr upper) {
        return solve(f, v, lower, upper, IntegralMethod.AUTO);
    }

    static Solution solve(Expr f, String v, Expr lower, Expr upper, IntegralMethod method) {
        Map<String, Expr> extras = new LinkedHashMap<>();
        extras.put("lower", lower);
        extras.put("upper", upper);

        // Extremos con parámetros (0 a b): se integra con la antiderivada y se contrasta en valores de prueba
        if (!lower.variables().isEmpty() || !upper.variables().isEmpty()) return parametric(f, v, lower, upper);

        double a;
        double b;
        try {
            a = Eval.value(lower);
            b = Eval.value(upper);
        } catch (RuntimeException e) {
            return new Solution(Solution.Kind.INTEGRAL, Solution.Status.UNSOLVED, f, null, List.of(), v, 1, extras);
        }
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return new Solution(Solution.Kind.INTEGRAL, Solution.Status.UNSOLVED, f, null, List.of(), v, 1, extras);
        }

        Outcome outcome;
        try {
            outcome = compute(f, v, lower, upper, a, b, method);
        } catch (RuntimeException e) {
            outcome = Outcome.of(Solution.Status.UNSOLVED, List.of());
        }

        double approximation = Double.NaN;
        boolean withParameters = f.variables().stream().anyMatch(name -> !name.equals(v));
        if (withParameters) {
            // Con parámetros no hay un único valor que contrastar: se comprueba en varios valores de prueba
            if (outcome.value() != null && !sampledAgree(f, v, lower, upper, outcome.value())) {
                outcome = Outcome.of(Solution.Status.UNSOLVED, outcome.steps());
            }
            return new Solution(Solution.Kind.INTEGRAL, outcome.status(), f, outcome.value(), outcome.steps(), v, 1, extras);
        }
        if (outcome.status() != Solution.Status.DOES_NOT_EXIST) {
            approximation = numeric(f, v, a, b);
            if (outcome.value() != null) {
                double exact = Eval.value(outcome.value());
                if (Double.isFinite(approximation)) {
                    if (!close(exact, approximation)) {
                        outcome = Outcome.of(Solution.Status.UNSOLVED, outcome.steps());
                    }
                } else {
                    // Sin contraste numérico no se da por bueno un resultado exacto
                    outcome = Outcome.of(Solution.Status.UNSOLVED, outcome.steps());
                }
            }
        }
        if (Double.isFinite(approximation)) extras.put("approximation", Expr.num(Rational.fromDouble(approximation)));
        return new Solution(Solution.Kind.INTEGRAL, outcome.status(), f, outcome.value(), outcome.steps(), v, 1, extras);
    }

    /**
     * Integral definida con parámetros: el integrando o los límites contienen otras variables. Se
     * evalúa la antiderivada en los límites y el resultado se contrasta con la cuadratura numérica en
     * varios valores de los parámetros; no se tratan extremos infinitos ni singularidades.
     */
    static Solution parametric(Expr f, String v, Expr lower, Expr upper) {
        Map<String, Expr> extras = new LinkedHashMap<>();
        extras.put("lower", lower);
        extras.put("upper", upper);
        List<Step> steps = new ArrayList<>();

        Solution indefinite = Integrals.indefinite(f, v);
        if (!indefinite.isSolved() || Double.isInfinite(safeValue(lower)) || Double.isInfinite(safeValue(upper))) {
            return new Solution(Solution.Kind.INTEGRAL, Solution.Status.UNSOLVED, f, null, indefinite.steps(), v, 1, extras);
        }
        Expr antiderivative = indefinite.result();
        steps.add(new Step(Rule.INT_DEFINITE_ANTIDERIVATIVE, f, antiderivative, Map.of("x", Expr.sym(v), "f", f),
                indefinite.steps()));
        Expr atUpper = antiderivative.substitute(v, upper);
        Expr atLower = antiderivative.substitute(v, lower);
        Expr value = Expr.sub(atUpper, atLower);
        steps.add(new Step(Rule.INT_BARROW, null, value,
                Map.of("p", lower, "q", upper, "Fp", atLower, "Fq", atUpper), List.of()));

        Solution.Status status = sampledAgree(f, v, lower, upper, value) ? Solution.Status.SOLVED : Solution.Status.UNSOLVED;
        return new Solution(Solution.Kind.INTEGRAL, status, f, status == Solution.Status.SOLVED ? value : null,
                steps, v, 1, extras);
    }

    private static double safeValue(Expr e) {
        try {
            return e.variables().isEmpty() ? Eval.value(e) : 0;
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
    }

    /**
     * Comprueba el valor exacto en puntos de muestra de los parámetros: en cada uno, la integral numérica
     * entre los límites correspondientes debe coincidir. Hace falta al menos un punto válido.
     */
    private static boolean sampledAgree(Expr f, String v, Expr lower, Expr upper, Expr value) {
        java.util.Set<String> names = new java.util.TreeSet<>(f.variables());
        names.addAll(lower.variables());
        names.addAll(upper.variables());
        names.remove(v);

        java.util.Random random = new java.util.Random(20_240_611L);
        int valid = 0;
        for (int attempt = 0; attempt < 30 && valid < 3; attempt++) {
            Map<String, Expr> env = new LinkedHashMap<>();
            Map<String, Double> numbers = new LinkedHashMap<>();
            // Se alternan rangos (dentro de (0,1), mayores que 1 y negativos) para cubrir dominios distintos
            double low = attempt % 3 == 0 ? 0.15 : attempt % 3 == 1 ? 0.6 : -0.9;
            double high = attempt % 3 == 0 ? 0.9 : attempt % 3 == 1 ? 2.4 : -0.15;
            for (String name : names) {
                double sample = Math.round((low + random.nextDouble() * (high - low)) * 100) / 100.0;
                numbers.put(name, sample);
                env.put(name, Expr.num(barnesinside.mathutils.cas.expr.Rational.fromDouble(sample)));
            }
            double a = Eval.value(lower.substitute(env));
            double b = Eval.value(upper.substitute(env));
            if (Double.isNaN(a) || Double.isNaN(b) || Math.abs(b - a) < 1e-9) continue;

            double exact = Eval.value(value, numbers);
            double numeric = a < b ? numeric(f.substitute(env), v, a, b) : -numeric(f.substitute(env), v, b, a);
            if (!Double.isFinite(exact) || !Double.isFinite(numeric)) continue;
            if (!close(exact, numeric)) return false;
            valid++;
        }
        return valid >= 1;
    }

    private static boolean close(double x, double y) {
        return Math.abs(x - y) <= 1e-6 * (1 + Math.max(Math.abs(x), Math.abs(y)));
    }

    // ================================================================ Resolución

    private static Outcome compute(Expr f, String v, Expr lower, Expr upper, double a, double b, IntegralMethod method) {
        Outcome outcome = byAntiderivative(f, v, lower, upper, a, b, method);
        if (outcome.status() == Solution.Status.SOLVED) return outcome;
        // Lo que la antiderivada no alcanza (exponente simbólico, logaritmo) se resuelve con la función gamma
        Optional<Outcome> gamma = gammaForm(f, v, a, b);
        if (gamma.isPresent()) return gamma.get();
        if (outcome.status() == Solution.Status.UNSOLVED) return absolutePieces(f, v, a, b, method).orElse(outcome);
        return outcome;
    }

    private static Outcome byAntiderivative(Expr f, String v, Expr lower, Expr upper, double a, double b, IntegralMethod method) {
        List<Step> steps = new ArrayList<>();

        if (a == b && Double.isFinite(a)) {
            steps.add(new Step(Rule.INT_EMPTY_INTERVAL, null, Expr.ZERO, Map.of("a", lower), List.of()));
            return new Outcome(Solution.Status.SOLVED, Expr.ZERO, steps);
        }
        if (a > b) {
            Outcome swapped = compute(f, v, upper, lower, b, a, method);
            steps.add(new Step(Rule.INT_REVERSE_BOUNDS, null, swapped.value() == null ? null : Expr.neg(swapped.value()),
                    Map.of("a", lower, "b", upper), swapped.steps()));
            return new Outcome(swapped.status(), swapped.value() == null ? null : Expr.neg(swapped.value()), steps);
        }

        // Con parámetros las singularidades se buscan con valores de muestra: si no hay ninguna con ellos, no se supone que haya
        Singularities.Found found = Singularities.find(sampled(f, v), v, a, b);
        if (!found.complete()) return Outcome.of(Solution.Status.UNSOLVED, steps);

        Optional<Outcome> symmetric = symmetry(f, v, lower, upper, a, b, found, steps);
        if (symmetric.isPresent()) return symmetric.get();

        pTest(f, v, lower, upper, a, b).ifPresent(steps::add);

        Solution indefinite = Integrals.indefinite(f, v, method);
        if (!indefinite.isSolved()) {
            return Outcome.of(indefinite.status() == Solution.Status.NON_ELEMENTAL
                    ? Solution.Status.NON_ELEMENTAL : Solution.Status.UNSOLVED, steps);
        }
        Expr antiderivative = indefinite.result();
        steps.add(new Step(Rule.INT_DEFINITE_ANTIDERIVATIVE, f, antiderivative, Map.of("x", Expr.sym(v), "f", f),
                indefinite.steps()));

        // Puntos de corte: extremos y singularidades (los extremos singulares son impropios)
        List<Expr> exact = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        exact.add(lower);
        values.add(a);
        List<Singularities.Point> interior = new ArrayList<>();
        for (Singularities.Point p : found.points()) {
            if (p.value() > a + 1e-12 && p.value() < b - 1e-12) {
                interior.add(p);
                exact.add(p.exact());
                values.add(p.value());
            }
        }
        exact.add(upper);
        values.add(b);

        boolean improper = Double.isInfinite(a) || Double.isInfinite(b) || !interior.isEmpty()
                || found.points().stream().anyMatch(p -> Math.abs(p.value() - a) < 1e-12 || Math.abs(p.value() - b) < 1e-12);
        if (improper) {
            steps.add(new Step(Rule.INT_IMPROPER, null, null,
                    Map.of("x", Expr.sym(v), "a", lower, "b", upper), List.of()));
        }
        if (!interior.isEmpty()) {
            steps.add(new Step(Rule.INT_SPLIT_SINGULARITY, null, null, Map.of("s", interior.get(0).exact()), List.of()));
        }

        Expr total = Expr.ZERO;
        for (int i = 0; i + 1 < exact.size(); i++) {
            Expr p = exact.get(i);
            Expr q = exact.get(i + 1);
            Expr atP = endpoint(antiderivative, v, p, values.get(i), Limits.Direction.RIGHT, found, steps);
            if (atP == null) return Outcome.of(Solution.Status.UNSOLVED, steps);
            if (isInfinite(atP)) return diverges(steps, p);
            Expr atQ = endpoint(antiderivative, v, q, values.get(i + 1), Limits.Direction.LEFT, found, steps);
            if (atQ == null) return Outcome.of(Solution.Status.UNSOLVED, steps);
            if (isInfinite(atQ)) return diverges(steps, q);

            Expr piece = Expr.sub(atQ, atP);
            steps.add(new Step(Rule.INT_BARROW, null, piece,
                    Map.of("p", p, "q", q, "Fp", atP, "Fq", atQ), List.of()));
            total = Expr.add(total, piece);
        }
        if (exact.size() > 2) steps.add(new Step(Rule.INT_ADD_PIECES, null, total, Map.of(), List.of()));

        if (found.points().isEmpty() && total.variables().isEmpty() && !Double.isFinite(Eval.value(total))) return Outcome.of(Solution.Status.UNSOLVED, steps);
        return new Outcome(Solution.Status.SOLVED, total, steps);
    }

    /** El integrando con un valor de muestra en cada parámetro (los de siempre si no tiene). */
    private static Expr sampled(Expr f, String v) {
        Map<String, Expr> env = new LinkedHashMap<>();
        double[] samples = {0.73, 1.37, 2.11, 0.41};
        int k = 0;
        for (String name : f.variables()) {
            if (name.equals(v)) continue;
            env.put(name, Expr.num(Rational.fromDouble(samples[k++ % samples.length])));
        }
        return env.isEmpty() ? f : f.substitute(env);
    }

    private static Outcome diverges(List<Step> steps, Expr at) {
        steps.add(new Step(Rule.INT_DIVERGES, null, null, Map.of("a", at), List.of()));
        return Outcome.of(Solution.Status.DOES_NOT_EXIST, steps);
    }

    private static boolean isInfinite(Expr e) {
        if (!e.variables().isEmpty()) {
            // Con parámetros se mira con valores de muestra
            try {
                return Double.isInfinite(Eval.value(sampled(e, "")));
            } catch (RuntimeException ex) {
                return false;
            }
        }
        double value = Eval.value(e);
        return Double.isInfinite(value);
    }

    /**
     * Valor de la antiderivada en un extremo: sustitución directa si es regular y límite (lateral)
     * si es infinito o singular. Devuelve {@code null} si el límite no se sabe calcular y una
     * expresión infinita si diverge.
     */
    private static Expr endpoint(Expr f, String v, Expr point, double value, Limits.Direction side,
                                 Singularities.Found found, List<Step> steps) {
        boolean singular = Double.isInfinite(value)
                || found.points().stream().anyMatch(p -> Math.abs(p.value() - value) < 1e-12);
        if (!singular) {
            try {
                Expr direct = f.substitute(v, point);
                if (!direct.variables().isEmpty() || Double.isFinite(Eval.value(direct))) return direct;
            } catch (RuntimeException e) {
                // Se prueba con el límite
            }
        }
        Limits.Direction direction = Double.isInfinite(value) ? Limits.Direction.BOTH : side;
        Solution limit = Limits.limit(f, v, point, direction);
        if (limit.status() == Solution.Status.DOES_NOT_EXIST) return Expr.INFINITY;
        if (limit.isSolved()) {
            steps.add(new Step(Rule.INT_ENDPOINT_LIMIT, f, limit.result(),
                    Map.of("x", Expr.sym(v), "a", point), limit.steps()));
            return limit.result();
        }
        return termwiseLimit(f, v, point, direction, steps);
    }

    /**
     * Límite de una suma término a término, para cuando cada sumando tiene límite aunque la suma no
     * se sepa resolver de una vez (como {@code x·ln x − x} en 0). Si hay infinitos, deben ser del
     * mismo signo.
     */
    private static Expr termwiseLimit(Expr f, String v, Expr point, Limits.Direction direction, List<Step> steps) {
        if (!(f instanceof Expr.Add sum)) return null;
        List<Step> found = new ArrayList<>();
        List<Expr> finite = new ArrayList<>();
        int infinity = 0;
        for (Expr term : sum.terms()) {
            Solution limit = Limits.limit(term, v, point, direction);
            if (!limit.isSolved()) return null;
            found.add(new Step(Rule.INT_ENDPOINT_LIMIT, term, limit.result(),
                    Map.of("x", Expr.sym(v), "a", point), limit.steps()));
            double value = Eval.value(limit.result());
            if (Double.isInfinite(value)) {
                int sign = value > 0 ? 1 : -1;
                if (infinity != 0 && infinity != sign) return null;
                infinity = sign;
            } else {
                finite.add(limit.result());
            }
        }
        steps.addAll(found);
        if (infinity != 0) return infinity > 0 ? Expr.INFINITY : Expr.neg(Expr.INFINITY);
        return Expr.add(finite);
    }

    // ================================================================ Valor absoluto

    /**
     * Una integral con {@code |g|}, con {@code g} de primer o segundo grado de ceros racionales: se parte el intervalo donde
     * {@code g} se anula y en cada trozo el valor absoluto es {@code g} o {@code -g}.
     */
    private static Optional<Outcome> absolutePieces(Expr f, String v, double a, double b, IntegralMethod method) {
        Expr.Func abs = firstAbsolute(f);
        if (abs == null) return Optional.empty();
        Expr g = abs.arg();
        Optional<barnesinside.mathutils.cas.poly.Polynomial> polynomial = barnesinside.mathutils.cas.poly.Polynomial.from(g, v);
        if (polynomial.isEmpty() || polynomial.get().degree() < 1 || polynomial.get().degree() > 2) return Optional.empty();
        List<Rational> roots = rationalRoots(polynomial.get());

        List<Rational> cuts = new ArrayList<>();
        for (Rational r : roots) {
            double d = r.doubleValue();
            if (d > a + 1e-12 && d < b - 1e-12 && !cuts.contains(r)) cuts.add(r);
        }
        cuts.sort(Rational::compareTo);
        List<Expr> bounds = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        bounds.add(a == Double.NEGATIVE_INFINITY ? Expr.neg(Expr.INFINITY) : a == Double.POSITIVE_INFINITY ? Expr.INFINITY : null);
        values.add(a);
        for (Rational r : cuts) {
            bounds.add(Expr.num(r));
            values.add(r.doubleValue());
        }
        bounds.add(null);
        values.add(b);

        List<Step> steps = new ArrayList<>();
        steps.add(new Step(Rule.INT_SPLIT_ABSOLUTE, null, null, Map.of("x", Expr.sym(v), "g", g), List.of()));
        Expr total = Expr.ZERO;
        for (int i = 0; i + 1 < values.size(); i++) {
            double p = values.get(i);
            double q = values.get(i + 1);
            double mid = Double.isInfinite(p) ? q - 1 : Double.isInfinite(q) ? p + 1 : (p + q) / 2;
            double gm = polynomial.get().evaluate(Rational.fromDouble(mid)).doubleValue();
            if (gm == 0) return Optional.empty();
            Expr signed = gm > 0 ? g : Expr.neg(g);
            Expr piece = replace(f, abs, signed);
            Expr lower = i == 0 ? lowerBoundExpr(a, bounds.get(0)) : bounds.get(i);
            Expr upper = i + 2 == values.size() ? upperBoundExpr(b) : bounds.get(i + 1);
            Solution solved = solve(piece, v, lower, upper, method);
            steps.add(new Step(Rule.INT_ABSOLUTE_PIECE, piece, solved.result(),
                    Map.of("x", Expr.sym(v), "g", g, "p", lower, "q", upper, "h", signed,
                            "sign", Expr.sym(gm > 0 ? "+" : "−")), solved.steps()));
            if (solved.status() == Solution.Status.DOES_NOT_EXIST) return Optional.of(Outcome.of(Solution.Status.DOES_NOT_EXIST, steps));
            if (!solved.isSolved()) return Optional.empty();
            total = Expr.add(total, solved.result());
        }
        if (values.size() > 2) steps.add(new Step(Rule.INT_ADD_PIECES, null, total, Map.of(), List.of()));
        return Optional.of(new Outcome(Solution.Status.SOLVED, total, steps));
    }

    private static Expr lowerBoundExpr(double a, Expr given) {
        return given != null ? given : Expr.num(Rational.fromDouble(a));
    }

    private static Expr upperBoundExpr(double b) {
        return Double.isInfinite(b) ? Expr.INFINITY : Expr.num(Rational.fromDouble(b));
    }

    private static List<Rational> rationalRoots(barnesinside.mathutils.cas.poly.Polynomial p) {
        List<Rational> roots = new ArrayList<>();
        if (p.degree() == 1) {
            roots.add(p.coefficient(0).negate().divide(p.coefficient(1)));
            return roots;
        }
        Rational c = p.coefficient(0);
        Rational bb = p.coefficient(1);
        Rational aa = p.coefficient(2);
        Rational disc = bb.multiply(bb).subtract(Rational.of(4).multiply(aa).multiply(c));
        if (disc.signum() < 0) return roots;
        double sqrt = Math.sqrt(disc.doubleValue());
        Rational root = Rational.fromDouble(sqrt);
        if (!root.multiply(root).equals(disc)) return roots;
        Rational twoA = Rational.of(2).multiply(aa);
        roots.add(bb.negate().add(root).divide(twoA));
        roots.add(bb.negate().subtract(root).divide(twoA));
        return roots;
    }

    /** Los ceros de lo que va dentro de cada valor absoluto de primer o segundo grado con ceros racionales. */
    private static List<Double> absoluteZeros(Expr e, String v) {
        List<Double> zeros = new ArrayList<>();
        collectAbsoluteZeros(e, v, zeros);
        return zeros;
    }

    private static void collectAbsoluteZeros(Expr e, String v, List<Double> out) {
        switch (e) {
            case Expr.Func fn -> {
                if (fn.fn() == Fn.ABS) {
                    barnesinside.mathutils.cas.poly.Polynomial.from(fn.arg(), v)
                            .filter(p -> p.degree() >= 1 && p.degree() <= 2)
                            .ifPresent(p -> rationalRoots(p).forEach(r -> out.add(r.doubleValue())));
                }
                collectAbsoluteZeros(fn.arg(), v, out);
            }
            case Expr.Add a -> a.terms().forEach(t -> collectAbsoluteZeros(t, v, out));
            case Expr.Mul m -> m.factors().forEach(t -> collectAbsoluteZeros(t, v, out));
            case Expr.Pow p -> {
                collectAbsoluteZeros(p.base(), v, out);
                collectAbsoluteZeros(p.exponent(), v, out);
            }
            default -> { }
        }
    }

    private static Expr.Func firstAbsolute(Expr e) {
        return switch (e) {
            case Expr.Func fn -> fn.fn() == Fn.ABS && !fn.arg().variables().isEmpty() ? fn : firstAbsolute(fn.arg());
            case Expr.Add a -> a.terms().stream().map(DefiniteIntegrals::firstAbsolute).filter(java.util.Objects::nonNull).findFirst().orElse(null);
            case Expr.Mul m -> m.factors().stream().map(DefiniteIntegrals::firstAbsolute).filter(java.util.Objects::nonNull).findFirst().orElse(null);
            case Expr.Pow p -> {
                Expr.Func inBase = firstAbsolute(p.base());
                yield inBase != null ? inBase : firstAbsolute(p.exponent());
            }
            default -> null;
        };
    }

    private static Expr replace(Expr e, Expr target, Expr by) {
        if (e.equals(target)) return by;
        return switch (e) {
            case Expr.Func fn -> Expr.func(fn.fn(), replace(fn.arg(), target, by));
            case Expr.Add a -> Expr.add(a.terms().stream().map(t -> replace(t, target, by)).toList());
            case Expr.Mul m -> Expr.mul(m.factors().stream().map(t -> replace(t, target, by)).toList());
            case Expr.Pow p -> Expr.pow(replace(p.base(), target, by), replace(p.exponent(), target, by));
            default -> e;
        };
    }

    // ================================================================ Integrales de tipo gamma

    /**
     * {@code C·x^p·e^(-kx)·(ln x)^m} entre 0 e infinito, con {@code k > 0} racional, {@code m} 0 o 1 y
     * {@code p} constante (puede llevar parámetros). Vale {@code C·p!/k^(p+1)} si {@code m = 0} y
     * {@code C·p!/k^(p+1)·(ψ(p+1) − ln k)} si {@code m = 1}; se supone {@code p > -1} para que converja.
     */
    private static Optional<Outcome> gammaForm(Expr f, String v, double a, double b) {
        if (a != 0 || b != Double.POSITIVE_INFINITY) return Optional.empty();
        Expr x = Expr.sym(v);

        List<Expr> constants = new ArrayList<>();
        List<Expr> exponents = new ArrayList<>();
        Expr rate = null;
        int logs = 0;
        for (Expr factor : f instanceof Expr.Mul m ? m.factors() : List.of(f)) {
            if (factor.isFreeOf(v)) {
                constants.add(factor);
            } else if (factor.equals(x)) {
                exponents.add(Expr.ONE);
            } else if (factor instanceof Expr.Pow p && p.base().equals(x) && p.exponent().isFreeOf(v)) {
                exponents.add(p.exponent());
            } else if (factor instanceof Expr.Pow p && p.base().equals(Expr.E)) {
                if (rate != null) return Optional.empty();
                Expr k = decay(p.exponent(), x, v);
                if (k == null) return Optional.empty();
                rate = k;
            } else if (factor instanceof Expr.Func fn && fn.fn() == Fn.LN && fn.arg().equals(x)) {
                logs++;
            } else if (factor instanceof Expr.Pow p && p.base() instanceof Expr.Func fn && fn.fn() == Fn.LN
                    && fn.arg().equals(x) && p.exponent().equals(Expr.ONE)) {
                logs++;
            } else {
                return Optional.empty();
            }
        }
        if (rate == null || logs > 1) return Optional.empty();

        Expr p = Expr.add(exponents);
        Expr shifted = Expr.add(p, Expr.ONE);
        Expr scale = Expr.mul(Expr.mul(constants), Expr.func(Fn.FACTORIAL, p), Expr.pow(rate, Expr.neg(shifted)));
        Expr value = logs == 0
                ? scale
                : Expr.mul(scale, Expr.sub(Expr.func(Fn.DIGAMMA, shifted), Expr.ln(rate)));
        Map<String, Expr> params = new LinkedHashMap<>();
        params.put("x", x);
        params.put("p", p);
        params.put("k", rate);
        params.put("value", value);
        List<Step> steps = new ArrayList<>();
        // La deducción, paso a paso: cambio de escala si k ≠ 1, definición de Γ y, con ln x, la derivada respecto al exponente
        List<Step> derivation = new ArrayList<>();
        Expr plain = Expr.mul(Expr.func(Fn.FACTORIAL, p), Expr.pow(rate, Expr.neg(shifted)));
        if (!rate.isOne()) derivation.add(new Step(Rule.INT_GAMMA_SCALE, null, null, params, List.of()));
        if (logs == 0) {
            Map<String, Expr> definition = new LinkedHashMap<>(params);
            definition.put("u", Expr.func(Fn.FACTORIAL, p));
            derivation.add(new Step(Rule.INT_GAMMA_DEFINITION, null, null, definition, List.of()));
        } else {
            Map<String, Expr> derive = new LinkedHashMap<>(params);
            derivation.add(new Step(Rule.INT_GAMMA_PARAMETER, null, null, derive, List.of()));
            Map<String, Expr> digamma = new LinkedHashMap<>(params);
            digamma.put("u", Expr.mul(plain, Expr.sub(Expr.func(Fn.DIGAMMA, shifted), Expr.ln(rate))));
            derivation.add(new Step(Rule.INT_GAMMA_DIGAMMA, null, null, digamma, List.of()));
        }
        steps.add(new Step(logs == 0 ? Rule.INT_GAMMA : Rule.INT_GAMMA_LOG, f, value, params, derivation));
        return Optional.of(new Outcome(Solution.Status.SOLVED, value, steps));
    }

    /** {@code k} si el exponente es {@code -k·x} con {@code k > 0} racional; {@code null} si no tiene esa forma. */
    private static Expr decay(Expr exponent, Expr x, String v) {
        if (!(exponent instanceof Expr.Mul m)) return null;
        List<Expr> rest = new ArrayList<>();
        boolean hasX = false;
        for (Expr factor : m.factors()) {
            if (factor.equals(x) && !hasX) hasX = true;
            else if (factor.isFreeOf(v)) rest.add(factor);
            else return null;
        }
        if (!hasX) return null;
        Expr k = Expr.neg(Expr.mul(rest));
        return k instanceof Expr.Num n && n.value().signum() > 0 ? k : null;
    }

    // ================================================================ Simetría

    private static Optional<Outcome> symmetry(Expr f, String v, Expr lower, Expr upper, double a, double b,
                                              Singularities.Found found, List<Step> steps) {
        if (!(b > 0) || a != -b || !found.points().isEmpty()) return Optional.empty();
        Expr x = Expr.sym(v);
        Expr mirrored = f.substitute(v, Expr.neg(x));
        if (Double.isFinite(b) && Equivalence.check(mirrored, Expr.neg(f)) == Equivalence.Result.EQUAL) {
            steps.add(new Step(Rule.INT_ODD_SYMMETRY, null, Expr.ZERO, Map.of("x", x, "a", upper), List.of()));
            return Optional.of(new Outcome(Solution.Status.SOLVED, Expr.ZERO, steps));
        }
        if (Equivalence.check(mirrored, f) == Equivalence.Result.EQUAL) {
            Solution half = solve(f, v, Expr.ZERO, upper);
            if (half.isSolved() || half.status() == Solution.Status.DOES_NOT_EXIST) {
                Expr value = half.isSolved() ? Expr.mul(Expr.num(2), half.result()) : null;
                steps.add(new Step(Rule.INT_EVEN_SYMMETRY, null, value, Map.of("x", x, "a", upper), half.steps()));
                return Optional.of(new Outcome(half.status(), value, steps));
            }
        }
        return Optional.empty();
    }

    // ================================================================ Criterio de la p

    /** {@code c·x^p} en {@code [a, ∞)} con {@code a > 0} o en {@code (0, b]}: explica cuándo converge. */
    private static Optional<Step> pTest(Expr f, String v, Expr lower, Expr upper, double a, double b) {
        Optional<Expr> exponent = powerExponent(f, v);
        if (exponent.isEmpty()) return Optional.empty();
        Expr p = exponent.get();
        if (!(p instanceof Expr.Num n) || n.value().signum() >= 0) return Optional.empty();
        boolean atInfinity = b == Double.POSITIVE_INFINITY && a > 0 && Double.isFinite(a);
        boolean atZero = a == 0 && b > 0 && Double.isFinite(b);
        if (!atInfinity && !atZero) return Optional.empty();
        return Optional.of(new Step(atInfinity ? Rule.INT_P_TEST_INFINITY : Rule.INT_P_TEST_ZERO, null, null,
                Map.of("p", Expr.neg(p), "a", lower, "b", upper), List.of()));
    }

    /** El exponente {@code p} si {@code f = c·x^p}, con {@code c} constante. */
    private static Optional<Expr> powerExponent(Expr f, String v) {
        Expr x = Expr.sym(v);
        for (Expr factor : f instanceof Expr.Mul m ? m.factors() : List.of(f)) {
            if (factor.isFreeOf(v)) continue;
            if (factor.equals(x) && (f.equals(x) || f instanceof Expr.Mul)) {
                if (!onlyConstantsBesides(f, factor, v)) return Optional.empty();
                return Optional.of(Expr.ONE);
            }
            if (factor instanceof Expr.Pow p && p.base().equals(x) && p.exponent() instanceof Expr.Num) {
                if (!onlyConstantsBesides(f, factor, v)) return Optional.empty();
                return Optional.of(p.exponent());
            }
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static boolean onlyConstantsBesides(Expr f, Expr factor, String v) {
        if (!(f instanceof Expr.Mul m)) return true;
        return m.factors().stream().filter(t -> !t.equals(factor)).allMatch(t -> t.isFreeOf(v));
    }

    // ================================================================ Verificación numérica

    private static double numeric(Expr f, String v, double a, double b) {
        Singularities.Found found = Singularities.find(f, v, a, b);
        if (!found.complete()) return Double.NaN;
        List<Double> interior = new ArrayList<>();
        for (Singularities.Point p : found.points()) {
            if (p.value() > a + 1e-12 && p.value() < b - 1e-12) interior.add(p.value());
        }
        // Donde un valor absoluto se anula la función tiene un pico: se parte ahí para que la cuadratura converja
        for (Double cut : absoluteZeros(f, v)) if (cut > a + 1e-12 && cut < b - 1e-12 && !interior.contains(cut)) interior.add(cut);
        try {
            return Quadrature.integrate(x -> Eval.value(f, Map.of(v, x)), a, b, interior);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }
}
