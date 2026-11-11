package com.barnesinside.mathutils.cas.ode;

import com.barnesinside.mathutils.cas.algebra.Eigen;
import com.barnesinside.mathutils.cas.expr.Expand;
import com.barnesinside.mathutils.cas.expr.Expr;
import com.barnesinside.mathutils.cas.expr.Op;
import com.barnesinside.mathutils.cas.expr.Rational;
import com.barnesinside.mathutils.cas.expr.Relation;
import com.barnesinside.mathutils.cas.ode.OdeSupport.Rec;
import com.barnesinside.mathutils.cas.steps.Rule;
import com.barnesinside.mathutils.cas.steps.Solution;
import com.barnesinside.mathutils.cas.steps.Trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.barnesinside.mathutils.cas.ode.OdeSupport.eq;
import static com.barnesinside.mathutils.cas.ode.OdeSupport.polish;
import static com.barnesinside.mathutils.cas.ode.OdeSupport.primed;

/**
 * Sistemas de n ecuaciones diferenciales. Las ecuaciones de orden superior se reducen a primer orden con una variable por cada derivada
 * ({@code y, y', …, y⁽ᵐ⁻¹⁾}); el sistema lineal con coeficientes constantes {@code X' = A·X} se resuelve con los autovalores y autovectores
 * de {@code A}.
 *
 * @since 0.11
 */
public final class OdeSystems {

    private OdeSystems() {
    }

    /**
     * El sistema de primer orden equivalente.
     *
     * @param independent la variable independiente
     * @param states      las variables del sistema: de cada incógnita, ella y sus derivadas hasta la anterior a la mayor
     * @param derivatives la derivada de cada variable, en función de las variables y de la independiente
     * @param unknowns    las incógnitas originales, en el orden de las ecuaciones
     */
    public record FirstOrder(String independent, List<String> states, List<Expr> derivatives, List<String> unknowns) {
    }

    // ================================================================ Reducción

    /** La incógnita de una ecuación: la que tiene la derivada de mayor orden. */
    private static String leading(Expr difference, List<String> candidates) {
        String best = null;
        int order = 0;
        for (String c : candidates) {
            int k = Odes.orderOf(difference, c);
            if (k > order) {
                order = k;
                best = c;
            }
        }
        return best;
    }

    private static List<String> unknownsOf(List<Relation> equations) {
        List<String> names = new ArrayList<>();
        for (Relation r : equations) {
            for (String name : r.variables()) {
                if (name.length() > 1 && name.endsWith("'")) {
                    String base = name.substring(0, name.indexOf('\''));
                    if (!names.contains(base)) names.add(base);
                }
            }
        }
        return names;
    }

    /**
     * Reduce el sistema a primer orden, dejando en {@code rec} un paso por cada ecuación de orden superior.
     *
     * @throws IllegalArgumentException si las ecuaciones no se pueden despejar una a una en la mayor derivada de su incógnita
     */
    public static FirstOrder reduce(List<Relation> equations, String x) {
        return reduce(new Rec(), equations, x);
    }

    private static FirstOrder reduce(Rec rec, List<Relation> equations, String x) {
        List<String> unknowns = unknownsOf(equations);
        if (unknowns.size() != equations.size()) throw new IllegalArgumentException("Tiene que haber una ecuación por incógnita");
        Map<String, Expr> highest = new LinkedHashMap<>();
        Map<String, Integer> orders = new LinkedHashMap<>();
        for (Relation r : equations) {
            Expr d = r.difference();
            String v = leading(d, unknowns);
            if (v == null || highest.containsKey(v)) throw new IllegalArgumentException("Cada ecuación define la derivada de una incógnita distinta");
            int m = Odes.orderOf(d, v);
            Expr top = primed(v, m);
            String name = ((Expr.Sym) top).name();
            Expr a = OdeSupport.diff(d, name);
            Expr rest = polish(d.substitute(name, Expr.ZERO));
            if (!a.isFreeOf(name) || a.isZero() || !OdeSupport.isZero(Expr.sub(d, Expr.add(Expr.mul(a, top), rest)))) {
                throw new IllegalArgumentException("La derivada de mayor orden de " + v + " tiene que aparecer despejable");
            }
            highest.put(v, polish(Expr.div(Expr.neg(rest), a)));
            orders.put(v, m);
        }
        List<String> states = new ArrayList<>();
        List<Expr> derivatives = new ArrayList<>();
        for (String v : unknowns) {
            int m = orders.get(v);
            for (int j = 0; j < m; j++) states.add(j == 0 ? v : ((Expr.Sym) primed(v, j)).name());
            if (m > 1) {
                rec.rel(Rule.ODE_REDUCE_ORDER, eq(primed(v, m), highest.get(v)), "y", Expr.sym(v), "k", Expr.num(m));
            }
        }
        for (String v : unknowns) {
            int m = orders.get(v);
            for (int j = 1; j < m; j++) derivatives.add(primed(v, j));
            derivatives.add(highest.get(v));
        }
        for (Expr d : derivatives) {
            for (String name : d.variables()) {
                if (name.endsWith("'") && !states.contains(name)) throw new IllegalArgumentException("Aparece una derivada de orden demasiado alto: " + name);
            }
        }
        return new FirstOrder(x, states, derivatives, unknowns);
    }

    // ================================================================ Resolución

    public static Solution solve(List<String> texts) {
        List<Relation> equations = texts.stream().map(Odes::parse).toList();
        String x = "x";
        for (Relation r : equations) {
            if (r.variables().contains("t") && !r.variables().contains("x")) x = "t";
        }
        return solve(equations, x);
    }

    public static Solution solve(List<Relation> equations, String x) {
        Rec rec = new Rec();
        Relation statement = eq(Expr.sym(equations.stream().map(Relation::toString).collect(java.util.stream.Collectors.joining(";  "))), Expr.ZERO);
        Map<String, Expr> marker = new LinkedHashMap<>();
        marker.put("system", Expr.ONE);
        try {
            FirstOrder system = reduce(rec, equations, x);
            Optional<Map<String, Expr>> general = linearSolution(rec, system, x);
            if (general.isPresent()) {
                int count = 0;
                for (String u : system.unknowns()) {
                    count++;
                    marker.put("lhs" + count, Expr.sym(u));
                    marker.put("rhs" + count, general.get().get(u));
                }
                marker.put("count", Expr.num(count));
                return new Solution(Solution.Kind.ODE, Solution.Status.SOLVED, Expr.ZERO, marker.get("rhs1"), rec.steps(), x, 1, marker, statement, null);
            }
        } catch (Trace.BudgetExceededException | ArithmeticException | IllegalArgumentException e) {
            // Se informa como no resuelto
        }
        return new Solution(Solution.Kind.ODE, Solution.Status.UNSOLVED, Expr.ZERO, null, rec.steps(), x, 1, marker, statement, null);
    }

    /** Solución de {@code X' = A·X} (A de números): las componentes de cada incógnita, o vacío si no es de ese tipo o no se sabe resolver. */
    private static Optional<Map<String, Expr>> linearSolution(Rec rec, FirstOrder system, String x) {
        int n = system.states().size();
        List<List<Expr>> a = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Expr f = Expand.expand(system.derivatives().get(i));
            List<Expr> row = new ArrayList<>();
            List<Expr> rebuilt = new ArrayList<>();
            for (String s : system.states()) {
                Expr c = OdeSupport.diff(f, s);
                if (!(c instanceof Expr.Num)) return Optional.empty();
                row.add(c);
                rebuilt.add(Expr.mul(c, Expr.sym(s)));
            }
            if (!OdeSupport.isZero(Expr.sub(f, Expr.add(rebuilt)))) return Optional.empty();
            a.add(row);
        }
        rec.note(Rule.ODE_SYSTEM_MATRIX, "A", Expr.sym(matrixText(a)), "X", Expr.sym("(" + String.join(", ", system.states()) + ")"));

        Expr xx = Expr.sym(x);
        List<Expr> columns = new ArrayList<>(); // una solución (vector de n componentes) por constante
        Eigen.Result eigen = Eigen.of(a);
        List<List<Expr>> solutions = new ArrayList<>();
        if (eigen.diagonalizable()) {
            for (Eigen.Pair pair : eigen.pairs()) {
                for (List<Expr> v : pair.vectors()) {
                    rec.note(Rule.ODE_SYSTEM_EIGEN, "lambda", pair.value(), "v", Expr.sym(vectorText(v)));
                    List<Expr> solution = new ArrayList<>();
                    Expr e = Expr.exp(Expr.mul(pair.value(), xx));
                    for (Expr c : v) solution.add(polish(Expr.mul(c, e)));
                    solutions.add(solution);
                }
            }
        } else if (n == 2) {
            Optional<List<List<Expr>>> two = twoByTwo(rec, a, xx);
            if (two.isEmpty()) return Optional.empty();
            solutions.addAll(two.get());
        } else {
            return Optional.empty();
        }
        if (solutions.size() != n) return Optional.empty();

        Map<String, Expr> byState = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            List<Expr> parts = new ArrayList<>();
            for (int k = 0; k < n; k++) parts.add(Expr.mul(Expr.sym("C" + (k + 1)), solutions.get(k).get(i)));
            Expr component = polish(Expr.add(parts));
            byState.put(system.states().get(i), component);
            rec.rel(Rule.ODE_SYSTEM_SOLUTION, eq(Expr.sym(system.states().get(i)), component), "x", xx);
        }
        // Se comprueba X' = A·X componente a componente
        for (int i = 0; i < n; i++) {
            List<Expr> terms = new ArrayList<>();
            for (int j = 0; j < n; j++) terms.add(Expr.mul(a.get(i).get(j), byState.get(system.states().get(j))));
            if (OdeSupport.different(OdeSupport.diff(byState.get(system.states().get(i)), x), Expr.add(terms))) return Optional.empty();
        }
        Map<String, Expr> byUnknown = new LinkedHashMap<>();
        for (String u : system.unknowns()) byUnknown.put(u, byState.get(u));
        columns.clear();
        return Optional.of(byUnknown);
    }

    /** Los casos de 2×2 que no tienen dos autovectores reales: autovalores complejos y autovalor doble con un solo autovector. */
    private static Optional<List<List<Expr>>> twoByTwo(Rec rec, List<List<Expr>> a, Expr x) {
        Rational[] m = new Rational[4];
        for (int i = 0; i < 4; i++) {
            if (!(a.get(i / 2).get(i % 2) instanceof Expr.Num num)) return Optional.empty();
            m[i] = num.value();
        }
        Rational trace = m[0].add(m[3]);
        Rational det = m[0].multiply(m[3]).subtract(m[1].multiply(m[2]));
        Optional<List<ConstantCoefficients.Root>> roots = ConstantCoefficients.roots(new Rational[]{det, trace.negate(), Rational.ONE});
        if (roots.isEmpty() || roots.get().size() != 1) return Optional.empty();
        ConstantCoefficients.Root root = roots.get().getFirst();
        Expr alpha = root.re();
        List<List<Expr>> solutions = new ArrayList<>();
        if (root.isComplex()) {
            Expr beta = root.im();
            List<Expr> u;
            List<Expr> v;
            if (!m[1].isZero()) {
                u = List.of(Expr.num(m[1]), polish(Expr.sub(alpha, Expr.num(m[0]))));
                v = List.of(Expr.ZERO, beta);
            } else {
                u = List.of(polish(Expr.sub(alpha, Expr.num(m[3]))), Expr.num(m[2]));
                v = List.of(beta, Expr.ZERO);
            }
            rec.note(Rule.ODE_SYSTEM_EIGEN, "lambda", polish(Expr.add(alpha, Expr.mul(beta, Expr.sym("i")))),
                    "v", Expr.sym(vectorText(List.of(polish(Expr.add(u.get(0), Expr.mul(v.get(0), Expr.sym("i")))),
                            polish(Expr.add(u.get(1), Expr.mul(v.get(1), Expr.sym("i"))))))));
            Expr e = Expr.exp(Expr.mul(alpha, x));
            Expr cos = Expr.cos(Expr.mul(beta, x));
            Expr sin = Expr.sin(Expr.mul(beta, x));
            List<Expr> first = new ArrayList<>();
            List<Expr> second = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                first.add(polish(Expr.mul(e, Expr.sub(Expr.mul(u.get(i), cos), Expr.mul(v.get(i), sin)))));
                second.add(polish(Expr.mul(e, Expr.add(Expr.mul(u.get(i), sin), Expr.mul(v.get(i), cos)))));
            }
            solutions.add(first);
            solutions.add(second);
            return Optional.of(solutions);
        }
        if (root.multiplicity() != 2) return Optional.empty();
        // (A − λI)² = 0: una columna de A − λI es el autovector v, y e (con (A − λI)e = v) completa la solución
        Expr lambda = alpha;
        Rational l = ((Expr.Num) lambda).value();
        Rational[] n = {m[0].subtract(l), m[1], m[2], m[3].subtract(l)};
        List<Expr> e;
        List<Expr> v;
        if (!n[0].isZero() || !n[2].isZero()) {
            e = List.of(Expr.ONE, Expr.ZERO);
            v = List.of(Expr.num(n[0]), Expr.num(n[2]));
        } else {
            e = List.of(Expr.ZERO, Expr.ONE);
            v = List.of(Expr.num(n[1]), Expr.num(n[3]));
        }
        if (v.get(0).isZero() && v.get(1).isZero()) return Optional.empty();
        rec.note(Rule.ODE_SYSTEM_EIGEN, "lambda", lambda, "v", Expr.sym(vectorText(v)));
        Expr exp = Expr.exp(Expr.mul(lambda, x));
        List<Expr> first = new ArrayList<>();
        List<Expr> second = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            first.add(polish(Expr.mul(v.get(i), exp)));
            second.add(polish(Expr.mul(Expr.add(e.get(i), Expr.mul(x, v.get(i))), exp)));
        }
        solutions.add(first);
        solutions.add(second);
        return Optional.of(solutions);
    }

    private static String vectorText(List<Expr> v) {
        return "(" + String.join(", ", v.stream().map(Expr::toString).toList()) + ")";
    }

    private static String matrixText(List<List<Expr>> a) {
        return "[" + String.join("; ", a.stream().map(row -> String.join(", ", row.stream().map(Expr::toString).toList())).toList()) + "]";
    }

    /** Las ecuaciones de un sistema ya reducido, como {@code y' = f} con primas, para mostrarlas o integrarlas numéricamente. */
    public static List<Relation> equationsOf(FirstOrder system) {
        List<Relation> list = new ArrayList<>();
        for (int i = 0; i < system.states().size(); i++) {
            list.add(new Relation(Expr.sym(system.states().get(i) + "'"), Op.EQ, system.derivatives().get(i)));
        }
        return list;
    }
}
