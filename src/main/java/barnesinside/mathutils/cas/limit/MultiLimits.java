package barnesinside.mathutils.cas.limit;

import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Equivalence;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Límites de funciones de varias variables en un punto, como {@code lim (x,y)→(0,0)}.
 * <p>A diferencia del caso de una variable, un límite múltiple no se puede demostrar acercándose por
 * unos pocos caminos, así que el motor distingue lo que prueba de lo que solo sugiere:
 * <ul>
 *   <li><b>Se resuelve</b> cuando la función es continua en el punto (sustitución directa) o cuando
 *       una cota en coordenadas polares lo demuestra (cociente de polinomios con denominador definido).</li>
 *   <li><b>No existe</b> cuando dos caminos, o los dos límites iterados, dan valores distintos: eso sí es
 *       una demostración.</li>
 *   <li><b>No se concluye</b> si todos los caminos coinciden pero no hay cota: el estado es
 *       {@link Solution.Status#UNSOLVED} y el extra {@code candidate} trae el valor que sugieren.</li>
 * </ul>
 */
public final class MultiLimits {

    private MultiLimits() {
    }

    public static Solution limit(String function, List<String> variables, List<String> point) {
        return limit(ExprParser.parse(function), variables, point.stream().map(ExprParser::parse).toList());
    }

    public static Solution limit(Expr f, List<String> variables, List<Expr> point) {
        if (variables.size() != point.size() || variables.size() < 2) {
            throw new IllegalArgumentException("Indica un valor del punto para cada variable (al menos dos variables)");
        }
        Map<String, Expr> extras = new LinkedHashMap<>();
        for (int i = 0; i < variables.size(); i++) {
            extras.put("var" + (i + 1), Expr.sym(variables.get(i)));
            extras.put("point" + (i + 1), point.get(i));
        }
        String first = variables.get(0);
        List<Step> steps = new ArrayList<>();

        // 1. Función continua en el punto
        Expr direct = substituteAll(f, variables, point);
        if (direct != null) {
            steps.add(new Step(Rule.LIMIT_MULTI_SUBSTITUTION, null, direct, Map.of("a", pointExpr(point)), List.of()));
            return new Solution(Solution.Kind.LIMIT, Solution.Status.SOLVED, f, direct, steps, first, 1, extras);
        }

        // 2. Límites iterados en los dos órdenes
        Expr forward = iterated(f, variables, point, false);
        Expr backward = iterated(f, variables, point, true);
        steps.add(new Step(Rule.LIMIT_MULTI_ITERATED, null, null,
                Map.of("one", forward == null ? Expr.sym("?") : forward, "other", backward == null ? Expr.sym("?") : backward),
                List.of()));
        if (forward != null && backward != null && differ(forward, backward)) {
            steps.add(new Step(Rule.LIMIT_MULTI_DIFFERENT, null, null,
                    Map.of("one", forward, "other", backward), List.of()));
            return new Solution(Solution.Kind.LIMIT, Solution.Status.DOES_NOT_EXIST, f, null, steps, first, 1, extras);
        }

        // 3. Caminos
        String t = freshName(f, variables);
        List<Expr> found = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        for (Path path : paths(variables.size())) {
            Expr value = alongPath(f, variables, point, path, t);
            if (value == null) continue;
            if (!found.isEmpty() && differ(found.get(0), value)) {
                steps.add(new Step(Rule.LIMIT_MULTI_PATHS, null, null,
                        Map.of("one", Expr.sym(descriptions.get(0)), "oneValue", found.get(0),
                                "other", Expr.sym(path.describe(variables, point)), "otherValue", value), List.of()));
                steps.add(new Step(Rule.LIMIT_MULTI_DIFFERENT, null, null,
                        Map.of("one", found.get(0), "other", value), List.of()));
                return new Solution(Solution.Kind.LIMIT, Solution.Status.DOES_NOT_EXIST, f, null, steps, first, 1, extras);
            }
            found.add(value);
            descriptions.add(path.describe(variables, point));
        }

        // 4. Cota en polares (dos variables)
        if (variables.size() == 2) {
            Polar polar = polar(f, variables, point);
            if (polar != null) {
                steps.add(new Step(polar.rule(), null, polar.value(), polar.params(), List.of()));
                return new Solution(Solution.Kind.LIMIT, Solution.Status.SOLVED, f, polar.value(), steps, first, 1, extras);
            }
        }

        // 5. Sin demostración: los caminos coinciden, pero eso no basta
        Expr candidate = !found.isEmpty() ? found.get(0) : forward;
        if (candidate != null) {
            steps.add(new Step(Rule.LIMIT_MULTI_CANDIDATE, null, candidate,
                    Map.of("count", Expr.num(found.size()), "candidate", candidate), List.of()));
            extras.put("candidate", candidate);
        }
        return new Solution(Solution.Kind.LIMIT, Solution.Status.UNSOLVED, f, null, steps, first, 1, extras);
    }

    // ================================================================ Sustitución e iterados

    private static Expr substituteAll(Expr f, List<String> variables, List<Expr> point) {
        try {
            Map<String, Expr> env = new HashMap<>();
            for (int i = 0; i < variables.size(); i++) env.put(variables.get(i), point.get(i));
            Expr value = f.substitute(env);
            if (!variables.stream().allMatch(value::isFreeOf)) return null;
            double number = value.variables().isEmpty() ? Eval.value(value) : 0;
            return Double.isFinite(number) ? value : null;
        } catch (ArithmeticException | IllegalArgumentException e) {
            return null;
        }
    }

    private static Expr pointExpr(List<Expr> point) {
        return Expr.sym("(" + String.join(", ", point.stream().map(Expr::toString).toList()) + ")");
    }

    /** Límite iterado: primero en una variable y luego en la siguiente. */
    private static Expr iterated(Expr f, List<String> variables, List<Expr> point, boolean reversed) {
        Expr current = f;
        for (int k = 0; k < variables.size(); k++) {
            int i = reversed ? variables.size() - 1 - k : k;
            Solution s = Limits.limit(current, variables.get(i), point.get(i));
            if (!s.isSolved()) return null;
            current = s.result();
        }
        return current;
    }

    private static boolean differ(Expr a, Expr b) {
        double x = Eval.value(a);
        double y = Eval.value(b);
        if (Double.isInfinite(x) || Double.isInfinite(y)) return x != y;
        if (!Double.isFinite(x) || !Double.isFinite(y)) return false;
        return Math.abs(x - y) > 1e-9 * (1 + Math.max(Math.abs(x), Math.abs(y))) && !Equivalence.areEqual(a, b);
    }

    // ================================================================ Caminos

    /** Un camino hacia el punto: cada variable avanza como {@code coefficient · t^power}. */
    private record Path(int[] coefficients, int[] powers) {

        String describe(List<String> variables, List<Expr> point) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < variables.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(variables.get(i)).append(" = ").append(point.get(i));
                if (coefficients[i] != 0) {
                    sb.append(coefficients[i] < 0 ? " − " : " + ");
                    int c = Math.abs(coefficients[i]);
                    sb.append(c == 1 ? "" : c + "·").append("t").append(powers[i] == 1 ? "" : "^" + powers[i]);
                }
            }
            return sb.toString();
        }
    }

    private static List<Path> paths(int n) {
        List<Path> paths = new ArrayList<>();
        int[][] lines = n == 2
                ? new int[][]{{1, 0}, {0, 1}, {1, 1}, {1, -1}, {1, 2}, {2, 1}, {1, -2}, {-1, 1}}
                : directions(n);
        for (int[] d : lines) {
            int[] powers = new int[n];
            java.util.Arrays.fill(powers, 1);
            paths.add(new Path(d, powers));
        }
        if (n == 2) {
            paths.add(new Path(new int[]{1, 1}, new int[]{1, 2}));
            paths.add(new Path(new int[]{1, 1}, new int[]{2, 1}));
            paths.add(new Path(new int[]{1, -1}, new int[]{1, 2}));
            paths.add(new Path(new int[]{1, 1}, new int[]{1, 3}));
        }
        return paths;
    }

    private static int[][] directions(int n) {
        List<int[]> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int[] axis = new int[n];
            axis[i] = 1;
            list.add(axis);
        }
        int[] ones = new int[n];
        int[] alternating = new int[n];
        int[] growing = new int[n];
        for (int i = 0; i < n; i++) {
            ones[i] = 1;
            alternating[i] = i % 2 == 0 ? 1 : -1;
            growing[i] = i + 1;
        }
        list.add(ones);
        list.add(alternating);
        list.add(growing);
        return list.toArray(new int[0][]);
    }

    private static Expr alongPath(Expr f, List<String> variables, List<Expr> point, Path path, String t) {
        try {
            Map<String, Expr> env = new HashMap<>();
            Expr ts = Expr.sym(t);
            for (int i = 0; i < variables.size(); i++) {
                Expr displacement = path.coefficients()[i] == 0 ? Expr.ZERO
                        : Expr.mul(Expr.num(path.coefficients()[i]), Expr.pow(ts, Expr.num(path.powers()[i])));
                env.put(variables.get(i), Expr.add(point.get(i), displacement));
            }
            Solution s = Limits.limit(f.substitute(env), t, Expr.ZERO);
            return s.isSolved() ? s.result() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String freshName(Expr f, List<String> variables) {
        for (String candidate : List.of("t", "s", "w", "z", "k")) {
            if (!variables.contains(candidate) && f.isFreeOf(candidate)) return candidate;
        }
        return "t_1";
    }

    // ================================================================ Cota en polares

    private record Polar(Rule rule, Expr value, Map<String, Expr> params) {
    }

    /** Monomios {@code u^i·v^j} con coeficientes racionales, indexados por {@code i·1000 + j}. */
    private static Map<Integer, Rational> polynomial(Expr e, String u, String v) {
        switch (e) {
            case Expr.Num n -> {
                return Map.of(0, n.value());
            }
            case Expr.Sym s -> {
                if (s.name().equals(u)) return Map.of(1000, Rational.ONE);
                if (s.name().equals(v)) return Map.of(1, Rational.ONE);
                return null;
            }
            case Expr.Pow p -> {
                if (!(p.exponent() instanceof Expr.Num n) || !n.value().isInteger() || n.value().signum() < 0
                        || !n.value().fitsInt() || n.value().intValueExact() > 40) {
                    return null;
                }
                Map<Integer, Rational> base = polynomial(p.base(), u, v);
                if (base == null) return null;
                Map<Integer, Rational> result = Map.of(0, Rational.ONE);
                for (int i = 0; i < n.value().intValueExact(); i++) result = multiply(result, base);
                return result;
            }
            case Expr.Mul m -> {
                Map<Integer, Rational> result = Map.of(0, Rational.ONE);
                for (Expr factor : m.factors()) {
                    Map<Integer, Rational> part = polynomial(factor, u, v);
                    if (part == null) return null;
                    result = multiply(result, part);
                }
                return result;
            }
            case Expr.Add a -> {
                Map<Integer, Rational> result = new HashMap<>();
                for (Expr term : a.terms()) {
                    Map<Integer, Rational> part = polynomial(term, u, v);
                    if (part == null) return null;
                    part.forEach((k, c) -> result.merge(k, c, Rational::add));
                }
                return result;
            }
            default -> {
                return null;
            }
        }
    }

    private static Map<Integer, Rational> multiply(Map<Integer, Rational> a, Map<Integer, Rational> b) {
        Map<Integer, Rational> result = new HashMap<>();
        a.forEach((ka, ca) -> b.forEach((kb, cb) -> result.merge(ka + kb, ca.multiply(cb), Rational::add)));
        return result;
    }

    private static int degree(int key) {
        return key / 1000 + key % 1000;
    }

    private static Polar polar(Expr f, List<String> variables, List<Expr> point) {
        String u = "u_";
        String v = "v_";
        try {
            Map<String, Expr> shift = new HashMap<>();
            shift.put(variables.get(0), Expr.add(point.get(0), Expr.sym(u)));
            shift.put(variables.get(1), Expr.add(point.get(1), Expr.sym(v)));
            Algebra.Fraction fraction = Algebra.fraction(f.substitute(shift));
            Map<Integer, Rational> numerator = polynomial(Expand.expand(fraction.numerator()), u, v);
            Map<Integer, Rational> denominator = polynomial(Expand.expand(fraction.denominator()), u, v);
            if (numerator == null || denominator == null) return null;
            numerator.values().removeIf(Rational::isZero);
            denominator.values().removeIf(Rational::isZero);
            if (numerator.isEmpty() || denominator.isEmpty()) return null;

            int p = numerator.keySet().stream().mapToInt(MultiLimits::degree).min().orElse(0);
            int q = denominator.keySet().stream().mapToInt(MultiLimits::degree).min().orElse(0);
            Map<Integer, Rational> lowN = lowest(numerator, p);
            Map<Integer, Rational> lowD = lowest(denominator, q);
            if (!definite(lowD)) return null;

            Map<String, Expr> params = new LinkedHashMap<>();
            params.put("p", Expr.num(p));
            params.put("q", Expr.num(q));
            if (p > q) return new Polar(Rule.LIMIT_MULTI_POLAR_ZERO, Expr.ZERO, params);
            if (p == q) {
                Rational ratio = constantRatio(lowN, lowD);
                if (ratio != null) return new Polar(Rule.LIMIT_MULTI_POLAR_CONSTANT, Expr.num(ratio), params);
            }
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Map<Integer, Rational> lowest(Map<Integer, Rational> polynomial, int degree) {
        Map<Integer, Rational> part = new HashMap<>();
        polynomial.forEach((k, c) -> {
            if (degree(k) == degree) part.put(k, c);
        });
        return part;
    }

    private static double evaluate(Map<Integer, Rational> part, double c, double s) {
        double sum = 0;
        for (Map.Entry<Integer, Rational> e : part.entrySet()) {
            sum += e.getValue().doubleValue() * Math.pow(c, e.getKey() / 1000) * Math.pow(s, e.getKey() % 1000);
        }
        return sum;
    }

    /** Si la parte homogénea no se anula en ningún ángulo (muestreo fino con margen). */
    private static boolean definite(Map<Integer, Rational> part) {
        double min = Double.MAX_VALUE;
        double max = 0;
        int sign = 0;
        for (int i = 0; i < 7200; i++) {
            double angle = 2 * Math.PI * i / 7200;
            double value = evaluate(part, Math.cos(angle), Math.sin(angle));
            min = Math.min(min, Math.abs(value));
            max = Math.max(max, Math.abs(value));
            int s = value > 0 ? 1 : -1;
            if (sign != 0 && s != sign) return false;
            sign = s;
        }
        return min > 1e-3 * max;
    }

    /** {@code L} si {@code N = L·D} como polinomios homogéneos, o {@code null}. */
    private static Rational constantRatio(Map<Integer, Rational> n, Map<Integer, Rational> d) {
        if (!n.keySet().equals(d.keySet())) return null;
        Rational ratio = null;
        for (Map.Entry<Integer, Rational> e : d.entrySet()) {
            Rational r = n.get(e.getKey()).divide(e.getValue());
            if (ratio == null) ratio = r;
            else if (!ratio.equals(r)) return null;
        }
        return ratio;
    }
}
