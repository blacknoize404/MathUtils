package barnesinside.mathutils.cas.pde;

import barnesinside.mathutils.cas.diff.Derivatives;
import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.expr.Op;
import barnesinside.mathutils.cas.expr.Polish;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.integral.Integrals;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import barnesinside.mathutils.cas.steps.Trace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Ecuaciones en derivadas parciales cuasilineales de primer orden, {@code a(x,y,u) u_x + b(x,y,u) u_y = c(x,y,u)}, por el método de las
 * características: se plantea el sistema {@code dx/a = dy/b = du/c}, se buscan dos integrales primeras {@code φ} y {@code ψ} (constantes sobre
 * cada característica) y la solución general es {@code F(φ, ψ) = 0} con {@code F} arbitraria. Con un dato inicial sobre una curva se
 * determina la {@code F} y sale la solución particular.
 * <p>Solo se resuelven los sistemas que se integran en forma cerrada: cada pareja de ecuaciones tiene que ser de variables separables o lineal.
 * Cuando no se sabe, el resultado no se resuelve y su último paso dice por qué; nunca se devuelve una solución sin comprobar.
 *
 * @since 0.10
 */
public final class Pde {

    /**
     * El dato inicial: una curva {@code (x(s), y(s), u(s))} sobre la que se conoce la solución.
     *
     * @param parameter el nombre del parámetro {@code s} en las tres expresiones
     */
    public record Curve(Expr x, Expr y, Expr u, String parameter) {
    }

    /**
     * El resultado.
     *
     * @param solution   la resolución con sus pasos; si no se supo resolver, sin resultado
     * @param integrals  las dos integrales primeras {@code φ} (sin {@code u} si es posible) y {@code ψ}, o vacío si no se resolvió
     * @param general    la solución general {@code F(φ, ψ) = 0}, o {@code null}
     * @param explicit   la solución general con la incógnita despejada ({@code u = f(φ)}), o {@code null} si no se pudo despejar
     * @param particular la solución que cumple el dato inicial (con la incógnita despejada si se pudo), o {@code null} sin dato o sin solución
     */
    public record Result(Solution solution, List<Expr> integrals, Relation general, Relation explicit, Relation particular) {
        public boolean solved() {
            return solution.isSolved();
        }
    }

    private Pde() {
    }

    /** Los tres coeficientes de {@code a u_x + b u_y = c}. */
    public record Equation(Expr a, Expr b, Expr c) {
    }

    /**
     * Lee una ecuación escrita como {@code x*u_x + y*u_y = u}: las derivadas son los símbolos {@code u_x} y {@code u_y} (también
     * {@code u_{x}}), y puede haber términos a los dos lados. Los coeficientes no pueden llevar las derivadas, y la ecuación ha de ser de
     * primer orden y lineal en ellas.
     *
     * @throws IllegalArgumentException si no es de esa forma
     */
    public static Equation parse(String text, String x, String y, String u) {
        int sign = text.indexOf('=');
        if (sign < 0 || text.indexOf('=', sign + 1) >= 0) throw new IllegalArgumentException("Falta el signo igual: a·u_x + b·u_y = c");
        String dx = u + "_" + x;
        String dy = u + "_" + y;
        Expr left = ExprParser.parse(derivativeNames(text.substring(0, sign), u, x, y));
        Expr right = ExprParser.parse(derivativeNames(text.substring(sign + 1), u, x, y));
        Expr difference = Expr.sub(left, right);
        Expr a = Polish.best(Derivatives.differentiate(difference, dx, new Trace()));
        Expr b = Polish.best(Derivatives.differentiate(difference, dy, new Trace()));
        Expr rest = Polish.best(Expand.expand(Expr.sub(difference, Expr.add(Expr.mul(a, Expr.sym(dx)), Expr.mul(b, Expr.sym(dy))))));
        for (Expr e : List.of(a, b, rest)) {
            if (!e.isFreeOf(dx) || !e.isFreeOf(dy)) {
                throw new IllegalArgumentException("La ecuación tiene que ser de primer orden y lineal en " + dx + " y " + dy);
            }
        }
        if (a.isZero() && b.isZero()) throw new IllegalArgumentException("La ecuación no tiene derivadas de " + u);
        return new Equation(a, b, Polish.best(Expr.neg(rest)));
    }

    /** {@code u_{x}} y {@code u_ x} se escriben como {@code u_x}. */
    private static String derivativeNames(String text, String u, String x, String y) {
        String result = text;
        for (String v : List.of(x, y)) {
            result = result.replace(u + "_{" + v + "}", u + "_" + v).replace(u + "_ " + v, u + "_" + v);
        }
        return result;
    }

    /** La solución general, con las variables {@code x}, {@code y} y la incógnita {@code u}. */
    public static Result solve(Expr a, Expr b, Expr c) {
        return solve(a, b, c, null, "x", "y", "u");
    }

    /** La solución que pasa por la curva dada, con las variables {@code x}, {@code y} y la incógnita {@code u}. */
    public static Result solve(Expr a, Expr b, Expr c, Curve data) {
        return solve(a, b, c, data, "x", "y", "u");
    }

    public static Result solve(Expr a, Expr b, Expr c, Curve data, String x, String y, String u) {
        return new Engine(new String[]{x, y, u}, new Expr[]{a, b, c}).run(data);
    }

    // ================================================================ Motor

    private static final class Engine {

        private final String[] v;
        private final Expr[] r;
        private final Log log = new Log();
        private Step failure;
        private final String k1;
        private final String k2;

        Engine(String[] v, Expr[] r) {
            this.v = v;
            this.r = r;
            Set<String> taken = new HashSet<>(List.of(v));
            for (Expr e : r) taken.addAll(e.variables());
            List<String> names = new ArrayList<>();
            for (String n : List.of("k", "c", "m", "n", "p", "q")) if (!taken.contains(n)) names.add(n);
            k1 = names.get(0);
            k2 = names.get(1);
        }

        Result run(Curve data) {
            Relation statement = new Relation(Expr.add(Expr.mul(r[0], Expr.sym("u_" + v[0])), Expr.mul(r[1], Expr.sym("u_" + v[1]))), Op.EQ, r[2]);
            try {
                return solveAll(statement, data);
            } catch (Trace.BudgetExceededException | ArithmeticException | IllegalArgumentException | UnsupportedOperationException e) {
                return unsolved(statement);
            }
        }

        private Result solveAll(Relation statement, Curve data) {
            if (r[0].isZero() && r[1].isZero()) {
                log.leaf(Rule.PDE_DEGENERATE, null, "a", r[0], "b", r[1]);
                return unsolved(statement);
            }
            log.leaf(Rule.PDE_CHARACTERISTICS, null, "p1", rate(0), "p2", rate(1), "p3", rate(2));

            List<Expr> integrals = firstIntegrals();
            if (integrals == null) return unsolved(statement);
            // φ, sin u si se puede; ψ, la que tiene u una sola vez (y más corta)
            integrals.sort((p, q) -> {
                int byU = Integer.compare(occurrences(p, v[2]), occurrences(q, v[2]));
                return byU != 0 ? byU : Integer.compare(q.toString().length(), p.toString().length());
            });
            Expr phi = integrals.get(0);
            Expr psi = integrals.get(1);
            if (!constantAlong(phi) || !constantAlong(psi)) {
                log.leaf(Rule.PDE_BRANCH, null);
                return unsolved(statement);
            }
            log.leaf(Rule.PDE_CHECK, null, "phi", phi, "psi", psi);

            Relation general = eq(Expr.sym("F(" + phi + ", " + psi + ")"), Expr.ZERO);
            log.leaf(Rule.PDE_GENERAL, general);
            Relation explicit = explicit(phi, psi);
            if (explicit != null) log.leaf(Rule.PDE_EXPLICIT, explicit, "f", Expr.sym("f(" + phi + ")"));

            Relation particular = null;
            if (data != null) {
                particular = particular(phi, psi, data);
                if (particular == null) return unsolved(statement);
            }
            Relation shown = particular != null ? particular : explicit != null ? explicit : general;
            Map<String, Expr> extras = new LinkedHashMap<>();
            extras.put("l", shown.left());
            extras.put("phi", phi);
            extras.put("psi", psi);
            Solution solution = new Solution(Solution.Kind.PDE, Solution.Status.SOLVED, statement.difference(), shown.right(), log.steps(),
                    v[0], 1, extras, statement, null);
            return new Result(solution, List.of(phi, psi), general, explicit, particular);
        }

        private Result unsolved(Relation statement) {
            List<Step> steps = new ArrayList<>(log.steps());
            if (failure != null) steps.add(failure);
            else if (steps.stream().noneMatch(s -> s.rule() == Rule.PDE_DEGENERATE || s.rule() == Rule.PDE_BRANCH
                    || s.rule() == Rule.PDE_DATA_CHARACTERISTIC || s.rule() == Rule.PDE_CANNOT_ISOLATE)) {
                steps.add(new Step(Rule.PDE_NO_PAIR, null, null, Map.of(), List.of()));
            }
            Solution solution = new Solution(Solution.Kind.PDE, Solution.Status.UNSOLVED, statement.difference(), null, steps, v[0], 1,
                    Map.of(), statement, null);
            return new Result(solution, List.of(), null, null, null);
        }

        // ---------------------------------------------------------------- Integrales primeras

        /** Dos integrales primeras independientes, o {@code null} (con el motivo en {@link #failure}). */
        private List<Expr> firstIntegrals() {
            List<Integer> zero = new ArrayList<>();
            for (int k = 0; k < 3; k++) if (r[k].isZero()) zero.add(k);
            List<Expr> found = new ArrayList<>();
            for (int k : zero) {
                Expr var = Expr.sym(v[k]);
                log.leaf(Rule.PDE_ZERO_RATE, eq(var, Expr.sym(k1)), "v", var);
                found.add(var);
            }
            if (found.size() >= 2) return found;

            if (found.size() == 1) {
                int z = zero.get(0);
                int i = z == 0 ? 1 : 0;
                int j = z == 2 ? 1 : 2;
                for (int[] order : new int[][]{{i, j}, {j, i}}) {
                    if (r[order[0]].isZero()) continue;
                    int mark = log.mark();
                    log.open();
                    Expr integral = solvePair(order[0], order[1], z, r[order[0]], r[order[1]], k1, true);
                    if (integral == null) {
                        log.discard();
                        log.rollback(mark);
                        continue;
                    }
                    log.close(Rule.PDE_FIRST_INTEGRAL, eq(integral, Expr.sym(k1)));
                    found.add(integral);
                    return found;
                }
                return null;
            }

            for (int w : new int[]{2, 0, 1}) {
                int p = w == 0 ? 1 : 0;
                int q = w == 2 ? 1 : 2;
                if (!r[p].isFreeOf(v[w]) || !r[q].isFreeOf(v[w])) continue;
                for (int[] order : new int[][]{{p, q}, {q, p}}) {
                    List<Expr> both = withElimination(order[0], order[1], w);
                    if (both != null) return both;
                }
            }
            return null;
        }

        /** La pareja {@code (p, q)} y, con su integral, la que sale de la tercera variable {@code w} y la que falta. */
        private List<Expr> withElimination(int p, int q, int w) {
            int mark = log.mark();
            log.open();
            Expr first = solvePair(p, q, w, r[p], r[q], k1, true);
            if (first == null) {
                log.discard();
                log.rollback(mark);
                return null;
            }
            log.close(Rule.PDE_FIRST_INTEGRAL, eq(first, Expr.sym(k1)));
            Expr constant = Expr.sym(k1);
            for (int z : new int[]{q, p}) {
                int o = z == q ? p : q;
                int inner = log.mark();
                log.open();
                Expr value = isolate(first, v[z], constant);
                if (value == null || occurrences(first, v[z]) != 1) {
                    fail(Rule.PDE_CANNOT_ISOLATE, eq(first, constant), "z", Expr.sym(v[z]));
                    log.discard();
                    log.rollback(inner);
                    continue;
                }
                log.leaf(Rule.PDE_ELIMINATE, eq(Expr.sym(v[z]), value), "phi", first, "z", Expr.sym(v[z]), "k", constant);
                Expr rw = Polish.best(r[w].substitute(v[z], value));
                Expr ro = Polish.best(r[o].substitute(v[z], value));
                if (ro.isZero()) {
                    log.discard();
                    log.rollback(inner);
                    continue;
                }
                Relation substituted = eq(Expr.div(dd(v[w]), dd(v[o])), Polish.best(Expr.div(rw, ro)));
                log.leaf(Rule.PDE_SUBSTITUTE, substituted, "z", Expr.sym(v[z]), "Z", value);
                Expr second = solvePair(o, w, z, ro, rw, k2, false);
                if (second == null) {
                    log.discard();
                    log.rollback(inner);
                    continue;
                }
                Expr psi = second;
                if (!second.isFreeOf(k1)) {
                    psi = Polish.best(second.substitute(k1, first));
                    log.leaf(Rule.PDE_BACK_SUBSTITUTE, eq(psi, Expr.sym(k2)), "k", constant, "phi", first);
                }
                log.close(Rule.PDE_FIRST_INTEGRAL, eq(psi, Expr.sym(k2)));
                List<Expr> both = new ArrayList<>();
                both.add(first);
                both.add(psi);
                return both;
            }
            log.rollback(mark);
            return null;
        }

        /**
         * Resuelve {@code dv_q/dv_p = r_q/r_p}: la expresión {@code I(v_p, v_q)} que vale la constante {@code kn} a lo largo de las soluciones, o
         * {@code null} (con el motivo en {@link #failure}). Registra la pareja, el método y, si hace falta, el paso de las exponenciales.
         */
        private Expr solvePair(int p, int q, int w, Expr rp, Expr rq, String kn, boolean announce) {
            String vp = v[p];
            String vq = v[q];
            Expr f = Polish.best(Expr.div(rq, rp));
            Relation ode = eq(Expr.div(dd(vq), dd(vp)), f);
            if (announce) {
                log.leaf(Rule.PDE_PAIR, ode, "vi", Expr.sym(vp), "vj", Expr.sym(vq), "ri", rp, "rj", rq, "w", Expr.sym(v[w]));
            }
            Expr integral = separable(vp, vq, f, kn);
            if (integral == null) integral = linear(vp, vq, f, kn);
            if (integral == null) {
                fail(Rule.PDE_NOT_SOLVABLE, ode);
                return null;
            }
            Expr tidy = exponentiate(integral);
            if (tidy != null) {
                log.leaf(Rule.PDE_EXPONENTIATE, eq(tidy, Expr.sym(kn)));
                integral = tidy;
            }
            return integral;
        }

        private Expr separable(String vp, String vq, Expr f, String kn) {
            List<Expr> p = new ArrayList<>();
            List<Expr> q = new ArrayList<>();
            for (Expr factor : Algebra.factors(f)) {
                boolean inP = !factor.isFreeOf(vp);
                boolean inQ = !factor.isFreeOf(vq);
                if (inP && inQ) return null;
                (inQ ? q : p).add(factor);
            }
            Expr pPart = p.isEmpty() ? Expr.ONE : Expr.mul(p);
            Expr qPart = q.isEmpty() ? Expr.ONE : Expr.mul(q);
            Expr left = integrate(Expr.div(Expr.ONE, qPart), vq);
            Expr right = integrate(pPart, vp);
            if (left == null || right == null) return null;
            log.leaf(Rule.PDE_SEPARABLE, eq(left, Expr.add(right, Expr.sym(kn))), "p", pPart, "q", qPart, "vi", Expr.sym(vp), "vj", Expr.sym(vq));
            return Polish.best(Expr.sub(left, right));
        }

        private Expr linear(String vp, String vq, Expr f, String kn) {
            Expr coefficient = Polish.best(Derivatives.differentiate(f, vq, new Trace()));
            if (coefficient.isZero() || !coefficient.isFreeOf(vq)) return null;
            Expr rest = Polish.best(Expand.expand(Expr.sub(f, Expr.mul(coefficient, Expr.sym(vq)))));
            if (!rest.isFreeOf(vq)) return null;
            Expr antiderivative = integrate(coefficient, vp);
            if (antiderivative == null) return null;
            Expr factor = Polish.best(Expr.exp(Expr.neg(antiderivative)));
            Expr right = integrate(Polish.best(Expr.mul(rest, factor)), vp);
            if (right == null) return null;
            Expr left = Polish.best(Expr.mul(factor, Expr.sym(vq)));
            log.leaf(Rule.PDE_LINEAR, eq(left, Expr.add(right, Expr.sym(kn))), "P", coefficient, "Q", rest, "mu", factor,
                    "vi", Expr.sym(vp), "vj", Expr.sym(vq));
            return Polish.best(Expr.sub(left, right));
        }

        // ---------------------------------------------------------------- Solución general

        /** {@code u} despejada de {@code ψ = f(φ)} si {@code φ} no lleva {@code u}, y {@code u = f(φ)} si {@code ψ} es la propia {@code u}. */
        private Relation explicit(Expr phi, Expr psi) {
            Expr arbitrary = Expr.sym("f(" + phi + ")");
            Expr u = Expr.sym(v[2]);
            if (psi.equals(u)) return eq(u, arbitrary);
            if (phi.isFreeOf(v[2]) && occurrences(psi, v[2]) == 1) {
                Expr solved = isolate(psi, v[2], arbitrary);
                if (solved != null) return eq(u, solved);
            }
            return null;
        }

        // ---------------------------------------------------------------- Dato inicial

        private Relation particular(Expr phi, Expr psi, Curve data) {
            String s = data.parameter();
            Expr sym = Expr.sym(s);
            log.leaf(Rule.PDE_DATA, null, "x", Expr.sym(v[0]), "y", Expr.sym(v[1]), "u", Expr.sym(v[2]),
                    "x0", data.x(), "y0", data.y(), "u0", data.u(), "s", sym);
            Map<String, Expr> onCurve = Map.of(v[0], data.x(), v[1], data.y(), v[2], data.u());
            Expr dx = Polish.best(Derivatives.differentiate(data.x(), s, new Trace()));
            Expr dy = Polish.best(Derivatives.differentiate(data.y(), s, new Trace()));
            Expr j = Polish.best(Expr.sub(Expr.mul(r[0].substitute(onCurve), dy), Expr.mul(r[1].substitute(onCurve), dx)));
            if (zeroEverywhere(j, s)) {
                log.leaf(Rule.PDE_DATA_CHARACTERISTIC, null, "J", j);
                return null;
            }
            log.leaf(Rule.PDE_TRANSVERSALITY, null, "J", j);

            Expr c1 = Polish.best(phi.substitute(onCurve));
            Expr c2 = Polish.best(psi.substitute(onCurve));
            log.leaf(Rule.PDE_ON_CURVE, null, "phi", phi, "psi", psi, "c1", c1, "c2", c2, "s", sym);
            if (c1.isFreeOf(s)) {
                log.leaf(Rule.PDE_DATA_CHARACTERISTIC, null, "J", j);
                return null;
            }
            Expr value = occurrences(c1, s) == 1 ? isolate(c1, s, phi) : null;
            if (value == null) {
                fail(Rule.PDE_CANNOT_ISOLATE, eq(c1, phi), "z", sym);
                return null;
            }
            log.leaf(Rule.PDE_ELIMINATE_PARAMETER, eq(sym, value), "phi", phi, "c1", c1, "s", sym);
            Expr g = Polish.best(c2.substitute(s, value));
            Relation particular = eq(psi, g);
            log.leaf(Rule.PDE_PARTICULAR, particular, "phi", phi, "psi", psi);
            if (!satisfiedOnCurve(psi, g, onCurve, s)) {
                log.leaf(Rule.PDE_BRANCH, null);
                return null;
            }
            if (phi.isFreeOf(v[2]) && g.isFreeOf(v[2]) && occurrences(psi, v[2]) == 1) {
                Expr solved = isolate(psi, v[2], g);
                if (solved != null && matchesData(solved, data)) {
                    Relation explicit = eq(Expr.sym(v[2]), Polish.best(solved));
                    log.leaf(Rule.PDE_PARTICULAR_EXPLICIT, explicit);
                    return explicit;
                }
            }
            return particular;
        }

        /** Si {@code ψ − G} se anula sobre la curva: el dato cumple la relación que se ha armado. */
        private boolean satisfiedOnCurve(Expr psi, Expr g, Map<String, Expr> onCurve, String s) {
            Expr residual = Expr.sub(psi.substitute(onCurve), g.substitute(onCurve));
            return zeroEverywhere(residual, s);
        }

        /** Si la solución despejada toma el valor {@code u(s)} en el punto {@code (x(s), y(s))}. */
        private boolean matchesData(Expr solved, Curve data) {
            Expr atCurve = solved.substitute(Map.of(v[0], data.x(), v[1], data.y()));
            return zeroEverywhere(Expr.sub(atCurve, data.u()), data.parameter());
        }

        // ---------------------------------------------------------------- Comprobaciones

        /** Si {@code a φ_x + b φ_y + c φ_u} se anula en puntos de ejemplo: {@code φ} es constante sobre las características. */
        private boolean constantAlong(Expr integral) {
            Expr residual = Expr.ZERO;
            for (int k = 0; k < 3; k++) {
                Expr partial = Derivatives.differentiate(integral, v[k], new Trace());
                residual = Expr.add(residual, Expr.mul(r[k], partial));
            }
            return zeroEverywhere(residual, null);
        }

        private Expr rate(int k) {
            return r[k].isZero() ? Expr.sym("d" + v[k] + "/0") : Expr.div(dd(v[k]), r[k]);
        }

        private Expr dd(String name) {
            return Expr.sym("d" + name);
        }

        private void fail(Rule rule, Relation relation, Object... extras) {
            if (failure == null) failure = Log.step(rule, relation, extras);
        }
    }

    // ================================================================ Ayudas

    private static Relation eq(Expr left, Expr right) {
        return new Relation(left, Op.EQ, right);
    }

    private static Expr integrate(Expr f, String variable) {
        if (f.isZero()) return Expr.ZERO;
        try {
            Solution s = Integrals.indefinite(f, variable);
            return s.isSolved() ? s.result() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Cuántas veces aparece la variable en la expresión. */
    private static int occurrences(Expr e, String variable) {
        return switch (e) {
            case Expr.Num n -> 0;
            case Expr.Const c -> 0;
            case Expr.Sym s -> s.name().equals(variable) ? 1 : 0;
            case Expr.Add a -> a.terms().stream().mapToInt(t -> occurrences(t, variable)).sum();
            case Expr.Mul m -> m.factors().stream().mapToInt(t -> occurrences(t, variable)).sum();
            case Expr.Pow p -> occurrences(p.base(), variable) + occurrences(p.exponent(), variable);
            case Expr.Func f -> occurrences(f.arg(), variable);
        };
    }

    /**
     * Despeja {@code z} de {@code e = rhs} cuando aparece una sola vez, deshaciendo las operaciones de fuera adentro. Con raíces de índice par
     * se toma la rama positiva. Si no se sabe, {@code null}.
     */
    private static Expr isolate(Expr e, String z, Expr rhs) {
        if (occurrences(e, z) != 1) return null;
        switch (e) {
            case Expr.Sym s -> {
                return rhs;
            }
            case Expr.Add a -> {
                Expr target = null;
                List<Expr> others = new ArrayList<>();
                for (Expr t : a.terms()) {
                    if (t.isFreeOf(z)) others.add(t);
                    else target = t;
                }
                return isolate(target, z, Expr.sub(rhs, Expr.add(others)));
            }
            case Expr.Mul m -> {
                Expr target = null;
                List<Expr> others = new ArrayList<>();
                for (Expr t : m.factors()) {
                    if (t.isFreeOf(z)) others.add(t);
                    else target = t;
                }
                return isolate(target, z, Expr.div(rhs, Expr.mul(others)));
            }
            case Expr.Pow p -> {
                if (p.exponent().isFreeOf(z)) return isolate(p.base(), z, Expr.pow(rhs, Expr.div(Expr.ONE, p.exponent())));
                if (p.base().isFreeOf(z)) return isolate(p.exponent(), z, Expr.div(Expr.ln(rhs), Expr.ln(p.base())));
                return null;
            }
            case Expr.Func f -> {
                Expr inverse = switch (f.fn()) {
                    case LN -> Expr.exp(rhs);
                    case ABS -> rhs;
                    case SIN -> Expr.func(Fn.ASIN, rhs);
                    case COS -> Expr.func(Fn.ACOS, rhs);
                    case TAN -> Expr.func(Fn.ATAN, rhs);
                    case ASIN -> Expr.func(Fn.SIN, rhs);
                    case ACOS -> Expr.func(Fn.COS, rhs);
                    case ATAN -> Expr.func(Fn.TAN, rhs);
                    default -> null;
                };
                return inverse == null ? null : isolate(f.arg(), z, inverse);
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * Si la integral lleva logaritmos, la misma con exponenciales ({@code ln y − ln x} pasa a {@code y/x}): como la constante pasa a ser
     * {@code e^k}, que también es una constante, la relación {@code I = k} sigue siendo cierta. {@code null} si no hay logaritmos.
     */
    private static Expr exponentiate(Expr integral) {
        List<Expr> factors = new ArrayList<>();
        boolean any = false;
        for (Expr term : Algebra.terms(Expand.expand(integral))) {
            Expr argument = null;
            Expr coefficient = Expr.ONE;
            if (term instanceof Expr.Func f && f.fn() == Fn.LN) {
                argument = f.arg();
            } else if (term instanceof Expr.Mul m) {
                List<Expr> rest = new ArrayList<>();
                for (Expr factor : m.factors()) {
                    if (argument == null && factor instanceof Expr.Func f && f.fn() == Fn.LN) argument = f.arg();
                    else rest.add(factor);
                }
                coefficient = Expr.mul(rest);
                if (!(coefficient instanceof Expr.Num)) argument = null;
            }
            if (argument != null) {
                any = true;
                Expr base = argument instanceof Expr.Func a && a.fn() == Fn.ABS ? a.arg() : argument;
                factors.add(Expr.pow(base, coefficient));
            } else {
                factors.add(Expr.exp(term));
            }
        }
        return any ? Polish.best(Expr.mul(factors)) : null;
    }

    /**
     * Si la expresión vale cero en puntos de ejemplo (los valores de la variable de la curva, si hay, y los de las demás letras). Los puntos
     * donde no se puede evaluar no cuentan, pero tiene que haber varios que sí.
     */
    private static boolean zeroEverywhere(Expr e, String curveParameter) {
        Set<String> names = new HashSet<>(e.variables());
        Random random = new Random(11);
        int good = 0;
        for (int i = 0; i < 16; i++) {
            Map<String, Double> env = new HashMap<>();
            for (String name : names) env.put(name, 0.4 + random.nextDouble() * 2.1);
            double value;
            double scale;
            try {
                value = Eval.value(e, env);
                scale = magnitude(e, env);
            } catch (RuntimeException ex) {
                continue;
            }
            if (Double.isNaN(value) || Double.isInfinite(value) || Double.isNaN(scale) || Double.isInfinite(scale)) continue;
            if (Math.abs(value) > 1e-7 * (1 + scale)) return false;
            good++;
        }
        return good >= 3;
    }

    /** La suma de los valores absolutos de los sumandos: la escala con la que comparar un resto. */
    private static double magnitude(Expr e, Map<String, Double> env) {
        double sum = 0;
        for (Expr term : Algebra.terms(e)) sum += Math.abs(Eval.value(term, env));
        return sum;
    }

    // ================================================================ Registro de pasos

    /** Los pasos que se van armando, con niveles: cada {@code open}/{@code close} es un paso con subpasos. */
    private static final class Log {

        private final Deque<List<Step>> frames = new ArrayDeque<>();

        Log() {
            frames.push(new ArrayList<>());
        }

        static Step step(Rule rule, Relation relation, Object... extras) {
            return step(rule, relation, List.of(), extras);
        }

        static Step step(Rule rule, Relation relation, List<Step> children, Object... extras) {
            Map<String, Expr> params = new LinkedHashMap<>();
            if (relation != null) params.putAll(relation.toParams());
            if (extras.length % 2 != 0) throw new IllegalArgumentException("Se esperan pares nombre/valor");
            for (int i = 0; i < extras.length; i += 2) params.put((String) extras[i], (Expr) extras[i + 1]);
            return new Step(rule, null, null, params, children);
        }

        void leaf(Rule rule, Relation relation, Object... extras) {
            frames.peek().add(step(rule, relation, extras));
        }

        void open() {
            frames.push(new ArrayList<>());
        }

        void close(Rule rule, Relation relation, Object... extras) {
            List<Step> children = frames.pop();
            frames.peek().add(step(rule, relation, children, extras));
        }

        void discard() {
            frames.pop();
        }

        int mark() {
            return frames.peek().size();
        }

        void rollback(int mark) {
            List<Step> top = frames.peek();
            while (top.size() > mark) top.removeLast();
        }

        List<Step> steps() {
            return List.copyOf(frames.peekLast());
        }
    }
}
