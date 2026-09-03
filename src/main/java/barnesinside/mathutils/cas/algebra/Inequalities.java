package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.expr.Interval;
import barnesinside.mathutils.cas.expr.Op;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.expr.SolutionSet;
import barnesinside.mathutils.cas.poly.Polynomial;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Trace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Inecuaciones en una variable real, paso a paso: las de primer grado se despejan (cambiando el sentido al dividir
 * entre un número negativo), las de valor absoluto se separan en casos y todas las demás se resuelven con una
 * tabla de signos: se buscan los puntos donde la expresión se anula o no existe y se comprueba su signo en cada
 * intervalo. Sirve para polinomios, fracciones algebraicas, radicales, exponenciales y logaritmos.
 * <pre>{@code
 * Solution s = Inequalities.solve("(x - 1)/(x + 2) >= 0");
 * s.answer();                                               // (-∞, -2) ∪ [1, ∞)
 * }</pre>
 */
public final class Inequalities {

    private Inequalities() {
    }

    // ================================================================ API

    /**
     * Resuelve el texto como inecuación en su única variable ({@code x} si hay varias).
     * @throws IllegalArgumentException si el texto no es una relación entre dos expresiones
     */
    public static Solution solve(String text) {
        Relation relation = Relation.parse(text).orElseThrow(() -> new IllegalArgumentException("No es una inecuación: " + text));
        return solve(relation, Equations.mainVariable(relation));
    }

    public static Solution solve(Relation relation, String variable) {
        return solve(relation, variable, SolveMethod.AUTO);
    }

    /** Resuelve con el método pedido; si no sirve, el estado es {@link Solution.Status#UNSOLVED}. */
    public static Solution solve(Relation relation, String variable, SolveMethod method) {
        if (relation.op() == Op.EQ) return Equations.solve(relation, variable, method);

        Rec rec = new Rec(new Trace(200, 10_000));
        Expr problem = relation.difference();
        for (String other : relation.variables()) {
            if (!other.equals(variable)) return unsolved(relation, variable, rec);
        }
        if (method != SolveMethod.AUTO && !applicableMethods(relation, variable).contains(method)) {
            rec.note(Rule.POLY_NOT_APPLICABLE, "f", problem);
            return unsolved(relation, variable, rec);
        }

        try {
            Chart chart = new Chart(rec, variable);
            SolutionSet answer = chart.solve(relation, method);
            Map<String, Expr> extras = chart.approximate ? Map.of("approximate", Expr.ONE) : Map.of();
            return new Solution(Solution.Kind.INEQUALITY, Solution.Status.SOLVED, problem, null, rec.steps(), variable, 1,
                    extras, relation, answer);
        } catch (Trace.BudgetExceededException | ArithmeticException e) {
            return unsolved(relation, variable, rec);
        }
    }

    private static Solution unsolved(Relation relation, String variable, Rec rec) {
        return new Solution(Solution.Kind.INEQUALITY, Solution.Status.UNSOLVED, relation.difference(), null, rec.steps(), variable,
                1, Map.of(), relation, null);
    }

    /** Los métodos con los que se puede resolver la inecuación; vacío si ninguno sirve, si no empieza por AUTO. */
    public static List<SolveMethod> applicableMethods(Relation relation, String variable) {
        List<SolveMethod> methods = new ArrayList<>();
        Expr f = relation.difference();
        if (f.isFreeOf(variable)) return methods;
        if (relation.op() == Op.EQ) return Equations.applicableMethods(relation, variable);

        if (relation.op() != Op.NE && Linear.applies(relation, variable)) methods.add(SolveMethod.ISOLATE);
        else if (relation.op() != Op.NE && Chart.absoluteForm(relation, variable).isPresent()) methods.add(SolveMethod.ABSOLUTE_CASES);
        if (relation.op() != Op.NE) methods.add(SolveMethod.SIGN_CHART);
        else methods.addAll(Equations.applicableMethods(new Relation(relation.left(), Op.EQ, relation.right()), variable));
        methods.removeIf(m -> m == SolveMethod.AUTO);
        if (!methods.isEmpty()) methods.add(0, SolveMethod.AUTO);
        return methods;
    }

    // ================================================================ Resolución

    private static final class Chart {

        private final Rec rec;
        private final String v;
        private final Expr x;
        boolean approximate;

        Chart(Rec rec, String v) {
            this.rec = rec;
            this.v = v;
            this.x = Expr.sym(v);
        }

        SolutionSet solve(Relation r, SolveMethod method) {
            if (r.op() == Op.NE) return notEqual(r, method);
            if (method != SolveMethod.SIGN_CHART && Linear.applies(r, v)) return Linear.solve(rec, r, v);
            if (method != SolveMethod.SIGN_CHART) {
                Optional<AbsoluteForm> form = absoluteForm(r, v);
                if (form.isPresent()) return absolute(r, form.get());
            }
            return signChart(r);
        }

        /** {@code a ≠ b}: se resuelve {@code a = b} y se toma el complemento. */
        private SolutionSet notEqual(Relation r, SolveMethod method) {
            Relation equation = new Relation(r.left(), Op.EQ, r.right());
            EquationSolver solver = new EquationSolver(rec, v, method == SolveMethod.AUTO ? SolveMethod.AUTO : method);
            SolutionSet equal = solver.solve(equation);
            approximate |= solver.approximate();
            rec.rel(Rule.INEQ_COLLECT, r);
            return switch (equal) {
                case SolutionSet.Empty e -> SolutionSet.allReals();
                case SolutionSet.AllReals a -> SolutionSet.empty();
                case SolutionSet.Finite f -> {
                    List<Interval> parts = new ArrayList<>();
                    Expr previous = null;
                    for (Expr value : f.values()) {
                        parts.add(new Interval(previous, false, value, false));
                        previous = value;
                    }
                    parts.add(new Interval(previous, false, null, false));
                    yield SolutionSet.intervals(parts);
                }
                default -> throw new ArithmeticException("Solución periódica");
            };
        }

        // ---------------------------------------------------------------- Valor absoluto

        private SolutionSet absolute(Relation r, AbsoluteForm form) {
            Expr g = form.inner();
            Expr h = form.bound();
            Op op = form.op();
            double hv = Eval.value(h);
            Relation normalized = new Relation(Expr.abs(g), op, h);
            if (!r.equals(normalized)) rec.rel(Rule.ALG_MOVE_TO_ONE_SIDE, normalized);

            boolean less = op == Op.LT || op == Op.LE;
            boolean strict = op == Op.LT || op == Op.GT;
            if (hv < 0 || (hv == 0 && strict && less)) {
                // |g| < negativo es imposible; |g| > negativo se cumple siempre
                rec.rel(less ? Rule.INEQ_NEVER : Rule.INEQ_ALWAYS, normalized);
                return less ? SolutionSet.empty() : SolutionSet.allReals();
            }
            Expr negative = Expr.neg(h);
            Relation upper = new Relation(g, less ? (strict ? Op.LT : Op.LE) : (strict ? Op.GT : Op.GE), h);
            Relation lower = new Relation(g, less ? (strict ? Op.GT : Op.GE) : (strict ? Op.LT : Op.LE), negative);
            String cases = less ? negative + " " + op.ascii() + " " + g + " " + op.ascii() + " " + h
                    : g + " " + op.ascii() + " " + h + "  o  " + g + " " + (strict ? "<" : "<=") + " " + negative;
            return rec.group(Rule.INEQ_ABS_CASES, normalized, () -> {
                SolutionSet a = rec.group(Rule.ALG_ZERO_FACTOR, upper, () -> solve(upper, SolveMethod.AUTO));
                SolutionSet b = rec.group(Rule.ALG_ZERO_FACTOR, lower, () -> solve(lower, SolveMethod.AUTO));
                SolutionSet result = less ? intersect(a, b) : union(a, b);
                rec.rel(Rule.INEQ_COLLECT, normalized);
                return result;
            }, "cases", Expr.sym(cases));
        }

        // ---------------------------------------------------------------- Tabla de signos

        private SolutionSet signChart(Relation r) {
            Op op = r.op();
            Expr original = r.difference();
            Expr f = original;
            Expr together = Algebra.together(original);
            Expr shown = together.equals(original) ? Expand.expand(original) : together;
            f = shown;
            Relation moved = new Relation(shown, op, Expr.ZERO);
            boolean sameAsBefore = r.right().isZero() && r.left().equals(shown);
            if (!sameAsBefore) rec.rel(r.right().isZero() ? Rule.ALG_SIMPLIFY : Rule.ALG_MOVE_TO_ONE_SIDE, moved);

            // Forma factorizada de un polinomio, para ver dónde se anula
            Optional<Polynomial> polynomial = Polynomial.from(f, v);
            if (polynomial.isPresent() && polynomial.get().degree() >= 2) {
                Expr factored = Factoring.auto(Rec.scratch(), f);
                if (!factored.equals(f)) {
                    Expr target = f;
                    rec.group(Rule.ALG_FACTOR, null, () -> {
                        Factoring.auto(rec, target);
                        return null;
                    }, "f", f, "g", factored);
                }
            }

            // Los puntos donde la expresión se anula o deja de existir
            Expr function = f;
            List<Expr> points = criticalPoints(function, Rec.scratch());
            if (!points.isEmpty()) {
                rec.group(Rule.INEQ_CRITICAL_POINTS, null, () -> {
                    criticalPoints(function, rec);
                    return null;
                }, "list", Expr.sym(list(points)));
            }

            List<Interval> satisfied = new ArrayList<>();
            List<Expr> included = new ArrayList<>();
            List<Expr> excluded = new ArrayList<>();
            boolean[] intervalOk = new boolean[points.size() + 1];

            for (int i = 0; i <= points.size(); i++) {
                Expr lower = i == 0 ? null : points.get(i - 1);
                Expr upper = i == points.size() ? null : points.get(i);
                Rational test = testPoint(lower, upper);
                double value = valueAt(function, Expr.num(test));
                Interval open = Interval.open(lower, upper);
                if (Double.isNaN(value)) {
                    rec.note(Rule.INEQ_OUT_OF_DOMAIN, "interval", Expr.sym(open.toString()));
                    continue;
                }
                int sign = value > 0 ? 1 : value < 0 ? -1 : 0;
                rec.note(Rule.INEQ_SIGN_TEST, "interval", Expr.sym(open.toString()), "t", Expr.num(test),
                        "v", Expr.sym(SolutionSet.decimal(value)), "sign", Expr.sym(sign >= 0 ? "+" : "−"));
                intervalOk[i] = op.holdsForSign(sign);
            }

            // Cada punto crítico entra o no según el símbolo y si la expresión existe ahí
            boolean[] pointOk = new boolean[points.size()];
            for (int i = 0; i < points.size(); i++) {
                double value = valueAt(function, points.get(i));
                boolean defined = !Double.isNaN(value);
                int sign = !defined ? 0 : Math.abs(value) < 1e-9 ? 0 : value > 0 ? 1 : -1;
                pointOk[i] = defined && op.holdsForSign(sign);
                (pointOk[i] ? included : excluded).add(points.get(i));
            }
            if (!included.isEmpty()) {
                rec.note(Rule.INEQ_ENDPOINTS_INCLUDED, "op", Expr.sym(op.symbol()), "list", Expr.sym(list(included)));
            }
            if (!excluded.isEmpty()) {
                rec.note(Rule.INEQ_ENDPOINTS_EXCLUDED, "op", Expr.sym(op.symbol()), "list", Expr.sym(list(excluded)));
            }

            for (int i = 0; i <= points.size(); i++) {
                if (!intervalOk[i]) continue;
                boolean lowerClosed = i > 0 && pointOk[i - 1];
                boolean upperClosed = i < points.size() && pointOk[i];
                satisfied.add(new Interval(i == 0 ? null : points.get(i - 1), lowerClosed,
                        i == points.size() ? null : points.get(i), upperClosed));
            }
            for (int i = 0; i < points.size(); i++) {
                boolean covered = (i >= 0 && intervalOk[i]) || intervalOk[i + 1];
                if (pointOk[i] && !covered) satisfied.add(Interval.closed(points.get(i), points.get(i)));
            }

            SolutionSet answer = SolutionSet.intervals(satisfied);
            if (points.isEmpty()) {
                rec.rel(answer instanceof SolutionSet.AllReals ? Rule.INEQ_ALWAYS : Rule.INEQ_NEVER, moved);
            } else {
                rec.rel(Rule.INEQ_COLLECT, moved);
            }
            return answer;
        }

        /** Los puntos críticos de {@code f}; los pasos de resolver {@code f = 0} se registran en {@code target}. */
        private List<Expr> criticalPoints(Expr f, Rec target) {
            List<Expr> all = new ArrayList<>();
            if (!f.isFreeOf(v)) {
                EquationSolver solver = new EquationSolver(target, v, SolveMethod.AUTO);
                SolutionSet zeros = solver.solve(new Relation(f, Op.EQ, Expr.ZERO));
                approximate |= solver.approximate();
                if (zeros instanceof SolutionSet.Finite fin) all.addAll(fin.values());
                else if (zeros instanceof SolutionSet.Periodic) throw new ArithmeticException("Soluciones periódicas");
            }
            all.addAll(singularities(f));
            return distinctSorted(all);
        }

        private static String list(List<Expr> points) {
            return points.stream().map(Expr::toString).collect(Collectors.joining(", "));
        }

        /** Los ceros de los denominadores, de los radicandos y de los argumentos de logaritmos. */
        private List<Expr> singularities(Expr f) {
            List<Expr> bases = new ArrayList<>();
            collectSingular(f, bases);
            List<Expr> points = new ArrayList<>();
            for (Expr base : bases) {
                EquationSolver solver = new EquationSolver(Rec.scratch(), v, SolveMethod.AUTO);
                SolutionSet zeros = solver.solve(new Relation(base, Op.EQ, Expr.ZERO));
                approximate |= solver.approximate();
                if (zeros instanceof SolutionSet.Finite fin) points.addAll(fin.values());
            }
            return points;
        }

        private void collectSingular(Expr e, List<Expr> out) {
            switch (e) {
                case Expr.Add a -> a.terms().forEach(t -> collectSingular(t, out));
                case Expr.Mul m -> m.factors().forEach(t -> collectSingular(t, out));
                case Expr.Pow p -> {
                    boolean negative = p.exponent() instanceof Expr.Num n && n.value().signum() < 0;
                    boolean root = p.exponent() instanceof Expr.Num n && !n.value().isInteger();
                    if ((negative || root) && !p.base().isFreeOf(v) && !out.contains(p.base())) out.add(p.base());
                    collectSingular(p.base(), out);
                    collectSingular(p.exponent(), out);
                }
                case Expr.Func fn -> {
                    if ((fn.fn() == Fn.LN) && !fn.arg().isFreeOf(v) && !out.contains(fn.arg())) out.add(fn.arg());
                    collectSingular(fn.arg(), out);
                }
                default -> { }
            }
        }

        private List<Expr> distinctSorted(List<Expr> values) {
            List<Expr> sorted = new ArrayList<>();
            for (Expr value : values) {
                double d = Eval.value(value);
                if (Double.isNaN(d)) continue;
                boolean seen = sorted.stream().anyMatch(o -> Math.abs(Eval.value(o) - d) <= 1e-9 * Math.max(1, Math.abs(d)));
                if (!seen) sorted.add(value);
            }
            sorted.sort(Comparator.comparingDouble(Eval::value));
            return sorted;
        }

        /** Un punto de prueba dentro del intervalo abierto: un entero si cabe, si no el punto medio. */
        private static Rational testPoint(Expr lower, Expr upper) {
            if (lower == null && upper == null) return Rational.ZERO;
            if (lower == null) return Rational.of((long) Math.floor(Eval.value(upper)) - 1);
            if (upper == null) return Rational.of((long) Math.ceil(Eval.value(lower)) + 1);
            double a = Eval.value(lower);
            double b = Eval.value(upper);
            double integer = Math.floor(a) + 1;
            if (integer < b - 1e-9) return Rational.of((long) integer);
            double middle = Math.rint((a + b) / 2 * 1000) / 1000;
            if (middle <= a || middle >= b) middle = (a + b) / 2;
            return Rational.fromDouble(middle);
        }

        private double valueAt(Expr f, Expr point) {
            Map<String, Double> env = new HashMap<>();
            try {
                env.put(v, Eval.value(point));
                double value = Eval.value(f, env);
                return Double.isInfinite(value) ? Double.NaN : value;
            } catch (RuntimeException e) {
                return Double.NaN;
            }
        }

        // ---------------------------------------------------------------- Conjuntos

        private static List<Interval> parts(SolutionSet s) {
            return switch (s) {
                case SolutionSet.Intervals i -> i.parts();
                case SolutionSet.AllReals a -> List.of(new Interval(null, false, null, false));
                case SolutionSet.Finite f -> f.values().stream().map(e -> Interval.closed(e, e)).toList();
                default -> List.of();
            };
        }

        private static SolutionSet union(SolutionSet a, SolutionSet b) {
            List<Interval> all = new ArrayList<>(parts(a));
            all.addAll(parts(b));
            return SolutionSet.intervals(all);
        }

        private static SolutionSet intersect(SolutionSet a, SolutionSet b) {
            List<Interval> result = new ArrayList<>();
            for (Interval p : parts(a)) {
                for (Interval q : parts(b)) {
                    Expr lower;
                    boolean lowerClosed;
                    if (p.lower() == null) {
                        lower = q.lower();
                        lowerClosed = q.lowerClosed();
                    } else if (q.lower() == null) {
                        lower = p.lower();
                        lowerClosed = p.lowerClosed();
                    } else {
                        double x = Eval.value(p.lower());
                        double y = Eval.value(q.lower());
                        if (Math.abs(x - y) <= 1e-12) {
                            lower = p.lower();
                            lowerClosed = p.lowerClosed() && q.lowerClosed();
                        } else if (x > y) {
                            lower = p.lower();
                            lowerClosed = p.lowerClosed();
                        } else {
                            lower = q.lower();
                            lowerClosed = q.lowerClosed();
                        }
                    }
                    Expr upper;
                    boolean upperClosed;
                    if (p.upper() == null) {
                        upper = q.upper();
                        upperClosed = q.upperClosed();
                    } else if (q.upper() == null) {
                        upper = p.upper();
                        upperClosed = p.upperClosed();
                    } else {
                        double x = Eval.value(p.upper());
                        double y = Eval.value(q.upper());
                        if (Math.abs(x - y) <= 1e-12) {
                            upper = p.upper();
                            upperClosed = p.upperClosed() && q.upperClosed();
                        } else if (x < y) {
                            upper = p.upper();
                            upperClosed = p.upperClosed();
                        } else {
                            upper = q.upper();
                            upperClosed = q.upperClosed();
                        }
                    }
                    if (lower != null && upper != null) {
                        double lo = Eval.value(lower);
                        double hi = Eval.value(upper);
                        if (lo > hi + 1e-12 || (Math.abs(lo - hi) <= 1e-12 && !(lowerClosed && upperClosed))) continue;
                    }
                    result.add(new Interval(lower, lowerClosed, upper, upperClosed));
                }
            }
            return SolutionSet.intervals(result);
        }

        // ---------------------------------------------------------------- Forma |g| ⋈ h

        private static Optional<AbsoluteForm> absoluteForm(Relation r, String v) {
            return Inequalities.absoluteForm(r, v);
        }
    }

    /** La inecuación {@code |g| ⋈ h} con {@code h} constante, con el valor absoluto a la izquierda. */
    record AbsoluteForm(Expr inner, Op op, Expr bound) {
    }

    static Optional<AbsoluteForm> absoluteForm(Relation r, String v) {
        if (r.op() == Op.NE || r.op() == Op.EQ) return Optional.empty();
        if (r.left() instanceof Expr.Func fn && fn.fn() == Fn.ABS && !fn.arg().isFreeOf(v) && r.right().isFreeOf(v)
                && r.right().variables().isEmpty()) {
            return Optional.of(new AbsoluteForm(fn.arg(), r.op(), r.right()));
        }
        if (r.right() instanceof Expr.Func fn && fn.fn() == Fn.ABS && !fn.arg().isFreeOf(v) && r.left().isFreeOf(v)
                && r.left().variables().isEmpty()) {
            return Optional.of(new AbsoluteForm(fn.arg(), r.op().swapped(), r.left()));
        }
        return Optional.empty();
    }
}
