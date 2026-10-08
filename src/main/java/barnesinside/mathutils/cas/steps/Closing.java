package barnesinside.mathutils.cas.steps;

import java.util.ArrayList;
import java.util.List;

/**
 * Cierra los pasos que se anidan: un paso con subpasos termina con un subpaso que vuelve a él y reúne su resultado, para que quien lee
 * no se pierda al salir de un nivel. Si el último subpaso ya acaba en el resultado del paso, no hace falta repetirlo.
 */
final class Closing {

    private Closing() {
    }

    /** Los mismos pasos con el cierre de cada nivel anidado (idempotente: no añade uno donde ya lo hay). */
    static List<Step> nested(List<Step> steps) {
        List<Step> closed = new ArrayList<>(steps.size());
        for (Step step : steps) {
            if (!noChange(step)) closed.add(named(close(step)));
        }
        return closed;
    }

    /** Un «Simplificar» recibe el nombre de lo que cambió. */
    private static Step named(Step step) {
        if (step.rule() != Rule.SIMPLIFY || !step.substeps().isEmpty()) return step;
        Rule rule = Simplifications.classify(step.before(), step.after());
        return rule == Rule.SIMPLIFY ? step : new Step(rule, step.before(), step.after(), step.params(), step.substeps());
    }

    /** Un «Simplificar» que no cambia nada a la vista (solo se reescribe {@code + -x} como {@code - x}) no enseña nada. */
    private static boolean noChange(Step step) {
        if (step.rule() != Rule.SIMPLIFY || !step.substeps().isEmpty() || step.before() == null || step.after() == null) return false;
        return shown(step.before()).equals(shown(step.after()));
    }

    private static String shown(barnesinside.mathutils.cas.expr.Expr e) {
        return e.toString().replace(" + -", " - ").replace(" ", "");
    }

    private static Step close(Step step) {
        if (step.substeps().isEmpty()) return step;
        List<Step> substeps = nested(step.substeps());
        // Con un solo subpaso no hay nada que reunir: su resultado ya se ve en «Cómo queda»
        if (substeps.size() < 2) return new Step(step.rule(), step.before(), step.after(), step.params(), substeps);
        Step last = substeps.getLast();
        boolean already = isReturn(last.rule()) || (last.after() != null && last.after().equals(step.after()));
        // Los pasos del álgebra no siempre traen expresiones: llevan la ecuación en que quedó la resolución
        boolean hasEquation = (step.before() != null && step.after() != null)
                || barnesinside.mathutils.cas.expr.Relation.fromParams(step.params()).isPresent();
        if (!already && hasEquation) {
            substeps.add(new Step(returnRule(step.rule().category()), step.before(), step.after(), step.params(), List.of()));
        }
        return new Step(step.rule(), step.before(), step.after(), step.params(), substeps);
    }

    private static boolean isReturn(Rule rule) {
        return rule == Rule.NEST_RETURN || rule == Rule.NEST_RETURN_DERIVATIVE || rule == Rule.NEST_RETURN_LIMIT
                || rule == Rule.NEST_RETURN_INTEGRAL;
    }

    /** La regla de cierre con la misma notación que el paso que cierra (derivada, límite o integral). */
    private static Rule returnRule(Rule.Category category) {
        return switch (category) {
            case DERIVATIVE -> Rule.NEST_RETURN_DERIVATIVE;
            case LIMIT -> Rule.NEST_RETURN_LIMIT;
            case INTEGRAL -> Rule.NEST_RETURN_INTEGRAL;
            default -> Rule.NEST_RETURN;
        };
    }
}
