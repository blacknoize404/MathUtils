package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Pattern;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sustitución de subexpresiones por patrón como una solución con su paso.
 * <pre>{@code
 * Rewriting.rewrite(ExprParser.parse("sin(3x)^2 + cos(3x)^2 + y"), ExprParser.parse("sin(a)^2 + cos(a)^2"),
 *         Expr.ONE, Set.of("a"));                          // 1 + y
 * }</pre>
 */
public final class Rewriting {

    private Rewriting() {
    }

    /**
     * Sustituye cada parte de {@code f} con la forma de {@code pattern} por {@code replacement}; los comodines son las
     * variables del patrón que se listan. Si el patrón no aparece, el estado es {@link Solution.Status#UNSOLVED}.
     */
    public static Solution rewrite(Expr f, Expr pattern, Expr replacement, Set<String> wildcards) {
        Expr result = Pattern.rewrite(f, pattern, replacement, wildcards);
        String variable = f.variables().contains("x") || f.variables().isEmpty() ? "x" : f.variables().iterator().next();
        Map<String, Expr> extras = Map.of("p", pattern, "q", replacement);
        if (result.equals(f)) {
            return new Solution(Solution.Kind.REWRITE, Solution.Status.UNSOLVED, f, null, List.of(), variable, 1, extras);
        }
        Step step = new Step(Rule.POLY_REWRITE, f, result, extras, List.of());
        return new Solution(Solution.Kind.REWRITE, Solution.Status.SOLVED, f, result, List.of(step), variable, 1, extras);
    }
}
