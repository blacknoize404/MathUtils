package barnesinside.mathutils.cas.integral;

import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Integrales iteradas (dobles, triples…): se integra de dentro hacia fuera, una variable cada vez,
 * y las demás se tratan como constantes. Los límites de una integral interna pueden depender de las
 * variables exteriores, como en {@code ∫₀¹ ∫₀ʸ x·y dx dy}.
 * <pre>{@code
 * Solution s = MultipleIntegrals.iterated("x*y", List.of(
 *         new MultipleIntegrals.Dimension("x", "0", "y"),
 *         new MultipleIntegrals.Dimension("y", "0", "1")));              // 1/8
 * }</pre>
 * Cada etapa se comprueba antes de seguir (la definida con cuadratura numérica), así que el motor
 * responde "no resuelta" en vez de dar un resultado dudoso.
 */
public final class MultipleIntegrals {

    /**
     * Una variable de integración.
     *
     * @param variable nombre de la variable
     * @param lower    límite inferior, o {@code null} si la integral es indefinida en esta variable
     * @param upper    límite superior, o {@code null} si la integral es indefinida en esta variable
     */
    public record Dimension(String variable, Expr lower, Expr upper) {

        public Dimension {
            if ((lower == null) != (upper == null)) {
                throw new IllegalArgumentException("Indica los dos límites de " + variable + " o ninguno");
            }
        }

        public Dimension(String variable, String lower, String upper) {
            this(variable, lower == null || lower.isBlank() ? null : ExprParser.parse(lower),
                    upper == null || upper.isBlank() ? null : ExprParser.parse(upper));
        }

        public Dimension(String variable) {
            this(variable, (Expr) null, (Expr) null);
        }

        public boolean isDefinite() {
            return lower != null;
        }
    }

    private MultipleIntegrals() {
    }

    public static Solution iterated(String integrand, List<Dimension> dimensions) {
        return iterated(ExprParser.parse(integrand), dimensions);
    }

    /**
     * @param dimensions variables de integración de la más interna a la más externa
     * @return la solución; los extras {@code var1}, {@code lower1}, {@code upper1}, {@code var2}…
     * describen cada integral
     */
    public static Solution iterated(Expr f, List<Dimension> dimensions) {
        if (dimensions.isEmpty()) throw new IllegalArgumentException("Indica al menos una variable de integración");

        Map<String, Expr> extras = new LinkedHashMap<>();
        for (int i = 0; i < dimensions.size(); i++) {
            Dimension d = dimensions.get(i);
            extras.put("var" + (i + 1), Expr.sym(d.variable()));
            if (d.isDefinite()) {
                extras.put("lower" + (i + 1), d.lower());
                extras.put("upper" + (i + 1), d.upper());
            }
        }

        List<Step> steps = new ArrayList<>();
        Expr current = f;
        for (int i = 0; i < dimensions.size(); i++) {
            Dimension d = dimensions.get(i);
            Solution stage = d.isDefinite() ? definiteStage(current, d) : Integrals.indefinite(current, d.variable());
            if (!stage.isSolved()) {
                Solution.Status status = stage.status() == Solution.Status.DOES_NOT_EXIST
                        ? Solution.Status.DOES_NOT_EXIST : Solution.Status.UNSOLVED;
                steps.add(stageStep(current, null, d, i, dimensions.size(), stage.steps()));
                return new Solution(Solution.Kind.INTEGRAL, status, f, null, steps, dimensions.get(0).variable(),
                        dimensions.size(), extras);
            }
            steps.add(stageStep(current, stage.result(), d, i, dimensions.size(), stage.steps()));
            current = stage.result();
        }

        if (current.variables().isEmpty()) {
            double value = Eval.value(current);
            if (Double.isFinite(value)) extras.put("approximation", Expr.num(barnesinside.mathutils.cas.expr.Rational.fromDouble(value)));
        }
        return new Solution(Solution.Kind.INTEGRAL, Solution.Status.SOLVED, f, current, steps,
                dimensions.get(0).variable(), dimensions.size(), extras);
    }

    private static Solution definiteStage(Expr f, Dimension d) {
        // Con parámetros (otras variables en el integrando o en los límites) no hay cuadratura exacta de
        // referencia: se integra con la antiderivada y se contrasta en varios valores de los parámetros
        boolean parametric = !freeParameters(f, d).isEmpty() || !d.lower().variables().isEmpty()
                || !d.upper().variables().isEmpty();
        return parametric
                ? DefiniteIntegrals.parametric(f, d.variable(), d.lower(), d.upper())
                : DefiniteIntegrals.solve(f, d.variable(), d.lower(), d.upper());
    }

    private static List<String> freeParameters(Expr f, Dimension d) {
        List<String> names = new ArrayList<>(f.variables());
        names.remove(d.variable());
        return names;
    }

    private static Step stageStep(Expr before, Expr after, Dimension d, int index, int count, List<Step> substeps) {
        Map<String, Expr> params = new LinkedHashMap<>();
        params.put("x", Expr.sym(d.variable()));
        params.put("k", Expr.num(index + 1));
        params.put("n", Expr.num(count));
        if (d.isDefinite()) {
            params.put("lower", d.lower());
            params.put("upper", d.upper());
        }
        return new Step(d.isDefinite() ? Rule.INT_ITERATED_DEFINITE : Rule.INT_ITERATED, before, after, params, substeps);
    }
}
