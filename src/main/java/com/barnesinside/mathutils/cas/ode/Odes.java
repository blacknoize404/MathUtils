package com.barnesinside.mathutils.cas.ode;

import com.barnesinside.mathutils.cas.algebra.Equations;
import com.barnesinside.mathutils.cas.expr.Expr;
import com.barnesinside.mathutils.cas.expr.Op;
import com.barnesinside.mathutils.cas.expr.Relation;
import com.barnesinside.mathutils.cas.expr.SolutionSet;
import com.barnesinside.mathutils.cas.ode.OdeSupport.Rec;
import com.barnesinside.mathutils.cas.steps.Rule;
import com.barnesinside.mathutils.cas.steps.Solution;
import com.barnesinside.mathutils.cas.steps.Step;
import com.barnesinside.mathutils.cas.steps.Trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.barnesinside.mathutils.cas.ode.OdeSupport.eq;
import static com.barnesinside.mathutils.cas.ode.OdeSupport.polish;
import static com.barnesinside.mathutils.cas.ode.OdeSupport.primed;

/**
 * Ecuaciones diferenciales ordinarias resueltas de forma exacta y con pasos, por tipos:
 * <ul>
 *   <li>de primer orden: separables, lineales, exactas (con factor integrante si hace falta), homogéneas y de Bernoulli; también en forma
 *   diferencial {@code M dx + N dy = 0} y en forma implícita {@code F(x, y, y') = 0};</li>
 *   <li>lineales de orden n con coeficientes constantes: la ecuación característica y, si no son homogéneas, coeficientes
 *   indeterminados o variación de parámetros.</li>
 * </ul>
 * La derivada se escribe con tildes: {@code y'}, {@code y''}… La solución general lleva constantes {@code C} (o {@code C1}, {@code C2}…).
 * Para cada solución, {@link Solution#extras()} trae {@code count} y, de {@code 1} a {@code count}, las igualdades {@code lhsN = rhsN}.
 *
 * @since 0.11
 */
public final class Odes {

    private static final Pattern PRIMED = Pattern.compile("(\\p{L})('+)");
    private static final Pattern ENCODED = Pattern.compile("(\\p{L})__p(\\d+)");
    /** {@code dx} y {@code dy} sueltos (no dentro de otro nombre): sin esto se leerían como un producto {@code d·x}. */
    private static final Pattern DIFFERENTIAL = Pattern.compile("(?<![\\p{L}\\p{N}_])d([xy])(?![\\p{L}\\p{N}_'])");
    private static final List<OdeMethod> FIRST_ORDER_AUTO = List.of(OdeMethod.SEPARABLE, OdeMethod.LINEAR, OdeMethod.BERNOULLI,
            OdeMethod.HOMOGENEOUS, OdeMethod.EXACT);

    private Odes() {
    }

    // ================================================================ Lectura

    /** Lee una ecuación con tildes: {@code y' = x*y}, {@code y'' - 3y' + 2y = 0}, {@code x dx + y dy = 0}. */
    public static Relation parse(String text) {
        Matcher m = PRIMED.matcher(text);
        String encoded = DIFFERENTIAL.matcher(m.replaceAll(r -> r.group(1) + "__p" + r.group(2).length())).replaceAll("d__$1");
        Relation relation = Relation.parse(encoded).orElseThrow(() -> new IllegalArgumentException("No es una ecuación: " + text));
        Map<String, Expr> names = new LinkedHashMap<>();
        for (String variable : relation.variables()) {
            if (variable.matches("d__[xy]")) names.put(variable, Expr.sym("d" + variable.charAt(3)));
            Matcher e = ENCODED.matcher(variable);
            if (e.matches()) names.put(variable, Expr.sym(e.group(1) + "'".repeat(Integer.parseInt(e.group(2)))));
        }
        if (names.isEmpty()) return relation;
        return new Relation(relation.left().substitute(names), relation.op(), relation.right().substitute(names));
    }

    /** La mayor derivada de {@code y} que aparece. */
    static int orderOf(Expr e, String y) {
        int order = 0;
        for (String name : e.variables()) {
            if (name.startsWith(y) && name.length() > y.length() && name.substring(y.length()).chars().allMatch(c -> c == '\'')) {
                order = Math.max(order, name.length() - y.length());
            }
        }
        return order;
    }

    /** La función incógnita: la letra que lleva tilde; en la forma diferencial, la de {@code dy}. */
    static String dependentOf(Relation ode) {
        for (String name : ode.variables()) {
            if (name.length() > 1 && name.endsWith("'")) return name.substring(0, name.indexOf('\''));
        }
        if (ode.variables().contains("dy") && ode.variables().contains("dx")) return "y";
        throw new IllegalArgumentException("No hay derivadas: la incógnita lleva tilde, por ejemplo y'");
    }

    static String independentOf(Relation ode, String y) {
        if (ode.variables().contains("d" + y) && ode.variables().contains("dx")) return "x";
        if (ode.variables().contains("x")) return "x";
        if (ode.variables().contains("t")) return "t";
        return "x";
    }

    // ================================================================ API

    /** La función incógnita de la ecuación: la letra que lleva tilde ({@code y} en {@code y' = x}). */
    public static String dependent(Relation ode) {
        return dependentOf(ode);
    }

    /** La variable independiente: {@code x}, o {@code t} si la ecuación no tiene {@code x} pero sí {@code t}. */
    public static String independent(Relation ode, String dependent) {
        return independentOf(ode, dependent);
    }

    public static Solution solve(String text) {
        return solve(text, OdeMethod.AUTO);
    }

    public static Solution solve(String text, OdeMethod method) {
        Relation ode = parse(text);
        String y = dependentOf(ode);
        return solve(ode, independentOf(ode, y), y, method);
    }

    public static Solution solve(Relation ode, String x, String y, OdeMethod method) {
        Rec rec = new Rec();
        try {
            List<Relation> general = solveInto(rec, ode, x, y, method);
            if (general.isEmpty()) return unsolved(ode, x, y, rec, method);
            return solved(ode, x, y, rec, general);
        } catch (Trace.BudgetExceededException | ArithmeticException | IllegalArgumentException e) {
            return unsolved(ode, x, y, rec, method);
        }
    }

    /** Los métodos con los que se puede resolver; vacío si ninguno sirve, y si no empieza por {@link OdeMethod#AUTO}. */
    public static List<OdeMethod> applicableMethods(Relation ode, String x, String y) {
        List<OdeMethod> methods = new ArrayList<>();
        if (!solve(ode, x, y, OdeMethod.AUTO).isSolved()) return methods;
        for (OdeMethod m : OdeMethod.values()) {
            if (m != OdeMethod.AUTO && solve(ode, x, y, m).isSolved()) methods.add(m);
        }
        methods.add(0, OdeMethod.AUTO);
        return methods;
    }

    // ================================================================ Resolución

    private static List<Relation> solveInto(Rec rec, Relation ode, String x, String y, OdeMethod method) {
        Expr difference = ode.difference();
        if (ode.op() != Op.EQ) throw new IllegalArgumentException("Una ecuación diferencial es una igualdad");
        int order = orderOf(difference, y);

        Optional<Expr> differential = differentialForm(difference, x, y);
        if (differential.isPresent()) return firstOrderBranches(rec, List.of(differential.get()), x, y, method, false);
        if (order == 0) throw new IllegalArgumentException("La ecuación no tiene derivadas");
        if (order >= 2 || method == OdeMethod.CONSTANT_COEFFICIENTS || method == OdeMethod.UNDETERMINED_COEFFICIENTS) {
            return constantCoefficients(rec, difference, x, y, order, method);
        }
        if (method == OdeMethod.VARIATION_OF_PARAMETERS) return List.of();

        // Primer orden: se despeja y' (si la ecuación no es lineal en y', hay una rama por cada raíz)
        Expr yp = primed(y, 1);
        String name = ((Expr.Sym) yp).name();
        Expr a = OdeSupport.diff(difference, name);
        List<Expr> branches = new ArrayList<>();
        Expr rest = polish(difference.substitute(name, Expr.ZERO));
        if (a.isFreeOf(name) && !a.isZero() && OdeSupport.isZero(Expr.sub(difference, Expr.add(Expr.mul(a, yp), rest)))) {
            branches.add(polish(Expr.div(Expr.neg(rest), a)));
            return firstOrderBranches(rec, branches, x, y, method, false);
        }
        Relation zero = new Relation(difference, Op.EQ, Expr.ZERO);
        if (Equations.solve(zero, name).answer() instanceof SolutionSet.Finite finite) {
            for (Expr value : finite.values()) {
                if (value.isFreeOf(name)) branches.add(polish(value));
            }
        }
        if (branches.isEmpty()) return List.of();
        rec.rel(Rule.ODE_SOLVE_FOR_DERIVATIVE, eq(Expr.sym(name), Expr.sym(branches.stream().map(Expr::toString).collect(java.util.stream.Collectors.joining(", ")))),
                "y", Expr.sym(y));
        return firstOrderBranches(rec, branches, x, y, method, true);
    }

    /** {@code M dx + N dy = 0}: da {@code y' = -M/N}. */
    private static Optional<Expr> differentialForm(Expr difference, String x, String y) {
        String dx = "d" + x;
        String dy = "d" + y;
        if (!difference.variables().contains(dx) || !difference.variables().contains(dy)) return Optional.empty();
        Expr m = OdeSupport.diff(difference, dx);
        Expr n = OdeSupport.diff(difference, dy);
        if (!m.isFreeOf(dx) || !m.isFreeOf(dy) || !n.isFreeOf(dx) || !n.isFreeOf(dy)) return Optional.empty();
        if (!OdeSupport.isZero(Expr.sub(difference, Expr.add(Expr.mul(m, Expr.sym(dx)), Expr.mul(n, Expr.sym(dy)))))) return Optional.empty();
        if (n.isZero()) return Optional.empty();
        return Optional.of(polish(Expr.div(Expr.neg(m), n)));
    }

    private static List<Relation> firstOrderBranches(Rec rec, List<Expr> fs, String x, String y, OdeMethod method, boolean branches) {
        List<Relation> all = new ArrayList<>();
        int number = 0;
        for (Expr f : fs) {
            number++;
            Rec branch = branches ? new Rec() : rec;
            Optional<Relation> solved = firstOrder(branch, f, x, y, method);
            if (solved.isEmpty()) return List.of();
            all.add(solved.get());
            if (branches) {
                rec.trace().add(new Step(Rule.ODE_BRANCH, null, null, branchParams(eq(primed(y, 1), f), number), branch.steps()));
            }
        }
        return all;
    }

    private static Map<String, Expr> branchParams(Relation relation, int number) {
        Map<String, Expr> params = new LinkedHashMap<>(relation.toParams());
        params.put("k", Expr.num(number));
        return params;
    }

    private static Optional<Relation> firstOrder(Rec rec, Expr f, String x, String y, OdeMethod method) {
        List<OdeMethod> candidates = method == OdeMethod.AUTO ? FIRST_ORDER_AUTO : List.of(method);
        for (OdeMethod m : candidates) {
            Rec attempt = new Rec();
            Optional<Relation> result;
            try {
                result = switch (m) {
                    case SEPARABLE -> FirstOrder.separable(attempt, f, x, y);
                    case LINEAR -> FirstOrder.linear(attempt, f, x, y);
                    case EXACT -> FirstOrder.exact(attempt, f, x, y);
                    case HOMOGENEOUS -> FirstOrder.homogeneous(attempt, f, x, y);
                    case BERNOULLI -> FirstOrder.bernoulli(attempt, f, x, y);
                    default -> Optional.empty();
                };
            } catch (ArithmeticException | IllegalArgumentException | Trace.BudgetExceededException e) {
                // Un método que se tropieza (una división por cero al probar un punto, el tiempo) no sirve; se prueba el siguiente
                result = Optional.empty();
            }
            if (result.isPresent()) {
                for (Step s : attempt.steps()) rec.trace().add(s);
                return result;
            }
        }
        return Optional.empty();
    }

    private static List<Relation> constantCoefficients(Rec rec, Expr difference, String x, String y, int order, OdeMethod method) {
        if (method == OdeMethod.SEPARABLE || method == OdeMethod.LINEAR || method == OdeMethod.EXACT
                || method == OdeMethod.HOMOGENEOUS || method == OdeMethod.BERNOULLI) {
            return List.of();
        }
        Rec attempt = new Rec();
        Expr[] answer = new Expr[1];
        if (ConstantCoefficients.solve(attempt, difference, x, y, order, method, answer).isEmpty()) return List.of();
        for (Step s : attempt.steps()) rec.trace().add(s);
        return List.of(eq(Expr.sym(y), answer[0]));
    }

    // ================================================================ Resultado

    private static Solution solved(Relation ode, String x, String y, Rec rec, List<Relation> general) {
        Map<String, Expr> extras = new LinkedHashMap<>();
        extras.put("count", Expr.num(general.size()));
        for (int i = 0; i < general.size(); i++) {
            extras.put("lhs" + (i + 1), general.get(i).left());
            extras.put("rhs" + (i + 1), general.get(i).right());
        }
        Relation first = general.getFirst();
        Expr result = first.left().equals(Expr.sym(y)) ? first.right() : first.left();
        return new Solution(Solution.Kind.ODE, Solution.Status.SOLVED, ode.difference(), result, rec.steps(), x,
                orderOf(ode.difference(), y), extras, ode, null);
    }

    private static Solution unsolved(Relation ode, String x, String y, Rec rec, OdeMethod method) {
        if (method != OdeMethod.AUTO) rec.note(Rule.ODE_NOT_APPLICABLE, "y", Expr.sym(y));
        return new Solution(Solution.Kind.ODE, Solution.Status.UNSOLVED, ode.difference(), null, rec.steps(), x,
                Math.max(1, orderOf(ode.difference(), y)), Map.of(), ode, null);
    }

    /** Las soluciones de una resolución de ecuación diferencial como igualdades, de la primera a la última. */
    public static List<Relation> solutions(Solution solution) {
        List<Relation> list = new ArrayList<>();
        int count = solution.extras().get("count") instanceof Expr.Num n ? n.value().intValueExact() : 0;
        for (int i = 1; i <= count; i++) list.add(eq(solution.extras().get("lhs" + i), solution.extras().get("rhs" + i)));
        return list;
    }
}
