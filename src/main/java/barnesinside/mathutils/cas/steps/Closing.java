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
        for (Step step : steps) closed.add(close(step));
        return closed;
    }

    private static Step close(Step step) {
        if (step.substeps().isEmpty()) return step;
        List<Step> substeps = nested(step.substeps());
        Step last = substeps.getLast();
        boolean already = isReturn(last.rule()) || (last.after() != null && last.after().equals(step.after()));
        if (!already && step.before() != null && step.after() != null) {
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
