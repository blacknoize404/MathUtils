package barnesinside.mathutils.cas.steps;

import barnesinside.mathutils.cas.diff.Derivatives;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

/** Los pasos que se anidan terminan con un subpaso que vuelve al paso que los pedía. */
class ClosingTest {

    private static boolean returns(List<Step> steps) {
        for (Step step : steps) {
            if (step.substeps().isEmpty()) continue;
            // Con un solo subpaso no hay nada que reunir y no se añade cierre
            if (step.substeps().size() < 2) {
                if (!returns(step.substeps())) return false;
                continue;
            }
            Step last = step.substeps().getLast();
            boolean endsInResult = last.after() != null && last.after().equals(step.after());
            boolean isReturn = last.rule().name().startsWith("NEST_RETURN");
            if (!endsInResult && !isReturn) return false;
            if (!returns(step.substeps())) return false;
        }
        return true;
    }

    @Test
    void everyNestedStepEndsInItsResultOrAReturnStep() {
        for (String f : List.of("ln(2*x)", "sin(x^2)*e^(3*x)", "x^2+5*x+ln(2*x)", "(x^2+1)/(x-3)")) {
            Solution s = Derivatives.nth(barnesinside.mathutils.cas.expr.ExprParser.parse(f), "x", 1);
            assert returns(s.steps()) : f;
        }
    }

    @Test
    void theChainRuleOfALogarithmGetsItsReturnStepAndItIsNotAddedTwice() {
        Solution s = Derivatives.nth(barnesinside.mathutils.cas.expr.ExprParser.parse("ln(2*x)"), "x", 1);
        Step chain = s.steps().getFirst();
        assert chain.substeps().size() >= 2 : chain.substeps().size();
        assert chain.substeps().getLast().rule() == Rule.NEST_RETURN_DERIVATIVE || chain.substeps().getLast().after().equals(chain.after());
        // Volver a construir la solución no repite el cierre
        Solution again = s.withPrelude(List.of());
        assert again.steps().equals(s.steps());
        Solution twice = new Solution(s.kind(), s.status(), s.problem(), s.result(), s.steps(), s.variable(), s.order(), s.extras());
        assert twice.steps().equals(s.steps()) : "cerrar dos veces no añade nada";
        assert s.render(Locale.of("es"), Format.PLAIN).length() > 0;
    }
}
