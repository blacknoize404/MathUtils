package barnesinside.mathutils.cas.limit;

import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Equivalence;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.expr.Fn;
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

        // 1b. Factor común que se anula en el punto
        Expr cancelled = cancelCommon(f);
        if (!cancelled.equals(f)) {
            Solution inner = limit(cancelled, variables, point);
            if (inner.status() != Solution.Status.UNSOLVED) {
                List<Step> all = new ArrayList<>();
                all.add(new Step(Rule.LIMIT_MULTI_CANCEL, f, cancelled, Map.of("after", cancelled), List.of()));
                all.addAll(inner.steps());
                return new Solution(Solution.Kind.LIMIT, inner.status(), f, inner.result(), all, first, 1, extras);
            }
        }

        // 1c. Una función acotada por un factor que tiende a 0
        Solution squeezed = squeeze(f, variables, point, first, extras);
        if (squeezed != null) return squeezed;

        // 1d. sen(g) con g tendiendo a 0, y 1e. cambio de variable u = g(x, y)
        Solution equivalent = infinitesimal(f, variables, point, first, extras);
        if (equivalent != null) return equivalent;
        Solution changed = newVariable(f, variables, point, first, extras);
        if (changed != null) return changed;

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

    // ================================================================ Reglas algebraicas

    /** Numerador y denominador factorizados y simplificados: {@code (x²−y²)/(x−y)} pasa a {@code x+y}. */
    private static Expr cancelCommon(Expr f) {
        try {
            Algebra.Fraction fraction = Algebra.fraction(f);
            if (fraction.denominator().isOne()) return f;
            Expr n = factored(fraction.numerator());
            Expr d = factored(fraction.denominator());
            Expr result = Expr.div(n, d);
            // Solo cuenta si desapareció el denominador de verdad (si no, no se ganó nada)
            return Algebra.fraction(result).denominator().equals(fraction.denominator()) ? f : result;
        } catch (RuntimeException e) {
            return f;
        }
    }

    private static Expr factored(Expr e) {
        Solution s = barnesinside.mathutils.cas.algebra.Factoring.factor(e);
        return s.isSolved() && s.result() != null ? s.result() : e;
    }

    private static boolean isBounded(Expr factor) {
        return factor instanceof Expr.Func fn && (fn.fn() == Fn.SIN || fn.fn() == Fn.COS) && fn.arg().variables().size() > 0;
    }

    /** {@code g(x, y)·sen(h)} o {@code cos(h)}: si el resto tiende a 0, el producto también. */
    private static Solution squeeze(Expr f, List<String> variables, List<Expr> point, String first, Map<String, Expr> extras) {
        if (!(f instanceof Expr.Mul m)) return null;
        List<Expr> bounded = m.factors().stream().filter(MultiLimits::isBounded).toList();
        if (bounded.isEmpty()) return null;
        List<Expr> rest = m.factors().stream().filter(t -> !isBounded(t)).toList();
        if (rest.isEmpty()) return null;
        Expr other = Expr.mul(rest);
        Solution inner = limit(other, variables, point);
        if (!inner.isSolved() || inner.result() == null || !inner.result().isZero()) return null;
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(Rule.LIMIT_MULTI_SQUEEZE, f, Expr.ZERO, Map.of("g", Expr.mul(bounded)), List.of()));
        steps.addAll(inner.steps());
        return new Solution(Solution.Kind.LIMIT, Solution.Status.SOLVED, f, Expr.ZERO, steps, first, 1, extras);
    }

    /** {@code sen(g)·R} con {@code g → 0}: el límite es el de {@code g·R} (porque sen u / u → 1). */
    private static Solution infinitesimal(Expr f, List<String> variables, List<Expr> point, String first, Map<String, Expr> extras) {
        List<Expr> factors = f instanceof Expr.Mul m ? m.factors() : List.of(f);
        for (int i = 0; i < factors.size(); i++) {
            if (!(factors.get(i) instanceof Expr.Func fn) || fn.fn() != Fn.SIN || fn.arg().variables().isEmpty()) continue;
            Expr g = fn.arg();
            Expr atPoint = substituteAll(g, variables, point);
            if (atPoint == null || !atPoint.isZero()) continue;
            List<Expr> rest = new ArrayList<>(factors);
            rest.remove(i);
            Expr replaced = Expr.mul(g, Expr.mul(rest));
            Solution inner = limit(replaced, variables, point);
            if (inner.status() == Solution.Status.UNSOLVED) continue;
            List<Step> steps = new ArrayList<>();
            steps.add(new Step(Rule.LIMIT_MULTI_INFINITESIMAL, f, replaced, Map.of("g", g, "after", replaced), List.of()));
            steps.addAll(inner.steps());
            return new Solution(Solution.Kind.LIMIT, inner.status(), f, inner.result(), steps, first, 1, extras);
        }
        return null;
    }

    /** Toda la función es {@code φ(g(x, y))}: el límite es el de {@code φ(u)} cuando {@code u → g(punto)}. */
    private static Solution newVariable(Expr f, List<String> variables, List<Expr> point, String first, Map<String, Expr> extras) {
        List<Expr> candidates = new ArrayList<>();
        collectComposite(f, variables, candidates);
        candidates.sort((a, b) -> Integer.compare(b.toString().length(), a.toString().length()));
        String u = freshName(f, variables);
        for (Expr g : candidates) {
            Expr replaced = replaceProductPowers(Algebra.replace(f, g, Expr.sym(u)), g, u);
            if (!replaced.variables().equals(java.util.Set.of(u))) continue;
            Expr g0 = substituteAll(g, variables, point);
            if (g0 == null) continue;
            Limits.Direction direction = directionOf(g, g0, variables, point);
            Solution inner = Limits.limit(replaced, u, g0, direction);
            if (!inner.isSolved() && inner.status() != Solution.Status.DOES_NOT_EXIST) continue;
            List<Step> steps = new ArrayList<>();
            steps.add(new Step(Rule.LIMIT_MULTI_NEW_VARIABLE, f, replaced, Map.of("g", g, "a", g0, "after", replaced), List.of()));
            steps.addAll(inner.steps());
            return new Solution(Solution.Kind.LIMIT, inner.status(), f, inner.result(), steps, first, 1, extras);
        }
        return null;
    }

    /**
     * Un producto {@code x·y} que quedó repartido en potencias sueltas ({@code x^-1·y^-1}) se vuelve a juntar como {@code u^-1}.
     */
    private static Expr replaceProductPowers(Expr e, Expr g, String u) {
        if (!(e instanceof Expr.Mul m) || !(g instanceof Expr.Mul gm)) return e;
        for (int k = -3; k <= 3; k++) {
            if (k == 0) continue;
            List<Expr> rest = new ArrayList<>(m.factors());
            boolean all = true;
            for (Expr factor : gm.factors()) {
                Expr wanted = Expr.pow(factor, Expr.num(k));
                if (!rest.remove(wanted)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                rest.add(Expr.pow(Expr.sym(u), Expr.num(k)));
                return Expr.mul(rest);
            }
        }
        return e;
    }

    private static void collectComposite(Expr e, List<String> variables, List<Expr> out) {
        long present = variables.stream().filter(v -> !e.isFreeOf(v)).count();
        if (present >= 2 && !(e instanceof Expr.Sym) && !out.contains(e)) out.add(e);
        switch (e) {
            case Expr.Func fn -> collectComposite(fn.arg(), variables, out);
            case Expr.Add a -> a.terms().forEach(t -> collectComposite(t, variables, out));
            case Expr.Mul m -> m.factors().forEach(t -> collectComposite(t, variables, out));
            case Expr.Pow p -> {
                collectComposite(p.base(), variables, out);
                collectComposite(p.exponent(), variables, out);
            }
            default -> { }
        }
    }

    /** Por qué lado se acerca {@code g} a su valor en el punto (sumas de cuadrados: solo por encima, por ejemplo). */
    private static Limits.Direction directionOf(Expr g, Expr g0, List<String> variables, List<Expr> point) {
        try {
            double target = Eval.value(g0);
            boolean above = false;
            boolean below = false;
            double[] radii = {1e-3, 1e-4};
            for (double r : radii) {
                for (int k = 0; k < 16; k++) {
                    double angle = 2 * Math.PI * k / 16;
                    Map<String, Double> env = new HashMap<>();
                    for (int i = 0; i < variables.size(); i++) {
                        double base = Eval.value(point.get(i));
                        env.put(variables.get(i), base + r * (i == 0 ? Math.cos(angle) : i == 1 ? Math.sin(angle) : 0.5 * Math.cos(angle + i)));
                    }
                    double v = Eval.value(g, env);
                    if (v > target + 1e-15) above = true;
                    else if (v < target - 1e-15) below = true;
                }
            }
            if (above && !below) return Limits.Direction.RIGHT;
            if (below && !above) return Limits.Direction.LEFT;
        } catch (RuntimeException e) {
            // Por ambos lados
        }
        return Limits.Direction.BOTH;
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
            // Si el denominador se anula en todo el camino, la función no existe sobre él: no cuenta
            Algebra.Fraction fraction = Algebra.fraction(f);
            if (!fraction.denominator().isOne() && fraction.denominator().substitute(env).isZero()) return null;
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
            Expr g = f.substitute(shift);

            // Una raíz en el denominador, (x² + y²)^(-1/2): cuenta como un denominador de orden (1/2)·grado
            Expr rootBase = null;
            Rational rootPower = Rational.ZERO;
            if (g instanceof Expr.Pow p && p.exponent() instanceof Expr.Num n && n.value().signum() < 0 && !n.value().isInteger()) {
                rootBase = p.base();
                rootPower = n.value().negate();
                g = Expr.ONE;
            } else if (g instanceof Expr.Mul m) {
                for (Expr factor : m.factors()) {
                    if (factor instanceof Expr.Pow p && p.exponent() instanceof Expr.Num n && n.value().signum() < 0 && !n.value().isInteger()) {
                        rootBase = p.base();
                        rootPower = n.value().negate();
                        List<Expr> rest = new ArrayList<>(m.factors());
                        rest.remove(factor);
                        g = Expr.mul(rest);
                        break;
                    }
                }
            }

            Algebra.Fraction fraction = Algebra.fraction(g);
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

            // El orden efectivo del denominador suma el de la raíz
            Rational order = Rational.of(q);
            if (rootBase != null) {
                Map<Integer, Rational> root = polynomial(Expand.expand(rootBase), u, v);
                if (root == null) return null;
                root.values().removeIf(Rational::isZero);
                if (root.isEmpty()) return null;
                int rq = root.keySet().stream().mapToInt(MultiLimits::degree).min().orElse(0);
                if (!definite(lowest(root, rq))) return null;
                order = order.add(rootPower.multiply(Rational.of(rq)));
            }

            Map<String, Expr> params = new LinkedHashMap<>();
            params.put("p", Expr.num(p));
            params.put("q", Expr.num(order));
            int compare = Rational.of(p).compareTo(order);
            if (compare > 0) return new Polar(Rule.LIMIT_MULTI_POLAR_ZERO, Expr.ZERO, params);
            if (compare == 0 && rootBase == null) {
                Rational ratio = constantRatio(lowN, lowD);
                if (ratio != null) return new Polar(Rule.LIMIT_MULTI_POLAR_CONSTANT, Expr.num(ratio), params);
            }
            if (compare < 0 && definite(lowN)) {
                // Signo fijo en todos los ángulos: crece sin límite con ese signo
                double sign = Math.signum(evaluate(lowN, 1, 0) == 0 ? evaluate(lowN, 0, 1) : evaluate(lowN, 1, 0))
                        * Math.signum(evaluate(lowD, 1, 0) == 0 ? evaluate(lowD, 0, 1) : evaluate(lowD, 1, 0));
                if (sign != 0) return new Polar(Rule.LIMIT_MULTI_POLAR_INFINITE, sign > 0 ? Expr.INFINITY : Expr.neg(Expr.INFINITY), params);
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
