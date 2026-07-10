package MathUtils.cas.diff;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Solution;
import MathUtils.cas.steps.Trace;

import java.util.HashSet;
import java.util.Set;

/**
 * Derivación simbólica paso a paso.
 * <pre>{@code
 * Solution s = Derivatives.derivative("x^2*sin(x)", "x");
 * s.result();                                   // 2*x*sin(x) + x^2*cos(x)
 * s.render(Locale.forLanguageTag("es"), Format.LATEX);
 * }</pre>
 * Las derivadas respecto a una variable de una expresión con varias variables son derivadas
 * parciales: las demás variables se tratan como constantes.
 */
public final class Derivatives {

    private Derivatives() {
    }

    /**
     * Deriva respecto a {@code variable}.
     * @return la solución con el resultado y los pasos; si se agota el tiempo o la profundidad
     * el estado es {@link Solution.Status#UNSOLVED}
     */
    public static Solution derivative(Expr f, String variable) {
        return nth(f, variable, 1);
    }

    /**
     * Interpreta el texto y lo deriva.
     * @throws MathUtils.exceptions.ExpressionParseException si el texto no es una expresión válida
     */
    public static Solution derivative(String source, String variable) {
        return derivative(ExprParser.parse(source), variable);
    }

    /**
     * Derivada de orden {@code order} (≥ 1). Cada derivación sucesiva es un paso de nivel superior.
     */
    public static Solution nth(Expr f, String variable, int order) {
        if (order < 1) throw new IllegalArgumentException("El orden debe ser al menos 1");

        Trace trace = new Trace();
        Set<String> taken = new HashSet<>(f.variables());
        taken.add(variable);
        Differentiator differentiator = new Differentiator(variable, taken, trace);
        Expr variableExpr = Expr.sym(variable);

        try {
            Expr current = f;
            for (int k = 1; k <= order; k++) {
                Expr before = current;
                int degree = k;
                current = order == 1
                        ? differentiator.diff(before)
                        : trace.step(Rule.DIFF_ORDER, before, Trace.params("x", variableExpr, "k", Expr.num(degree)),
                        () -> differentiator.diff(before));
            }
            return new Solution(Solution.Kind.DERIVATIVE, Solution.Status.SOLVED, f, current,
                    trace.steps(), variable, order);
        } catch (Trace.BudgetExceededException e) {
            return new Solution(Solution.Kind.DERIVATIVE, Solution.Status.UNSOLVED, f, null,
                    trace.steps(), variable, order);
        }
    }
}
