package barnesinside.mathutils.cas.integral;

import barnesinside.mathutils.cas.expr.Equivalence;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expr;
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
        Map<String, Expr> extras = new LinkedHashMap<>();
        extras.put("lower", lower);
        extras.put("upper", upper);

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
            outcome = compute(f, v, lower, upper, a, b);
        } catch (RuntimeException e) {
            outcome = Outcome.of(Solution.Status.UNSOLVED, List.of());
        }

        double approximation = Double.NaN;
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
            if (!Double.isFinite(a) || !Double.isFinite(b) || Math.abs(b - a) < 1e-9) continue;

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

    private static Outcome compute(Expr f, String v, Expr lower, Expr upper, double a, double b) {
        List<Step> steps = new ArrayList<>();

        if (a == b && Double.isFinite(a)) {
            steps.add(new Step(Rule.INT_EMPTY_INTERVAL, null, Expr.ZERO, Map.of("a", lower), List.of()));
            return new Outcome(Solution.Status.SOLVED, Expr.ZERO, steps);
        }
        if (a > b) {
            Outcome swapped = compute(f, v, upper, lower, b, a);
            steps.add(new Step(Rule.INT_REVERSE_BOUNDS, null, swapped.value() == null ? null : Expr.neg(swapped.value()),
                    Map.of("a", lower, "b", upper), swapped.steps()));
            return new Outcome(swapped.status(), swapped.value() == null ? null : Expr.neg(swapped.value()), steps);
        }

        Singularities.Found found = Singularities.find(f, v, a, b);
        if (!found.complete()) return Outcome.of(Solution.Status.UNSOLVED, steps);

        Optional<Outcome> symmetric = symmetry(f, v, lower, upper, a, b, found, steps);
        if (symmetric.isPresent()) return symmetric.get();

        pTest(f, v, lower, upper, a, b).ifPresent(steps::add);

        Solution indefinite = Integrals.indefinite(f, v);
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

        if (found.points().isEmpty() && !Double.isFinite(Eval.value(total))) return Outcome.of(Solution.Status.UNSOLVED, steps);
        return new Outcome(Solution.Status.SOLVED, total, steps);
    }

    private static Outcome diverges(List<Step> steps, Expr at) {
        steps.add(new Step(Rule.INT_DIVERGES, null, null, Map.of("a", at), List.of()));
        return Outcome.of(Solution.Status.DOES_NOT_EXIST, steps);
    }

    private static boolean isInfinite(Expr e) {
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
                if (Double.isFinite(Eval.value(direct))) return direct;
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
        try {
            return Quadrature.integrate(x -> Eval.value(f, Map.of(v, x)), a, b, interior);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }
}
