package barnesinside.mathutils.cas.ode;

import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Messages;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class OdeSystemsTest {

    private static List<Rule> rules(List<Step> steps) {
        List<Rule> all = new ArrayList<>();
        for (Step s : steps) {
            all.add(s.rule());
            all.addAll(rules(s.substeps()));
        }
        return all;
    }

    private static void uses(Solution s, Rule... expected) {
        List<Rule> all = rules(s.steps());
        for (Rule r : expected) assert all.contains(r) : r + " falta en " + all;
    }

    @Test
    void aHigherOrderEquationInASystemIsReducedToFirstOrder() {
        OdeSystems.FirstOrder system = OdeSystems.reduce(List.of(Odes.parse("y'' = -y + z"), Odes.parse("z' = y")), "x");
        assert system.states().equals(List.of("y", "y'", "z")) : system.states();
        assert system.derivatives().get(0).equals(Expr.sym("y'")) : system.derivatives();
        assert system.derivatives().get(1).variables().containsAll(List.of("y", "z")) : system.derivatives();
        assert system.derivatives().get(2).equals(Expr.sym("y")) : system.derivatives();
        assert OdeSystems.equationsOf(system).size() == 3;
    }

    @Test
    void linearSystemsAreSolvedWithEigenvalues() {
        Solution real = OdeSystems.solve(List.of("y' = 2*y + z", "z' = y + 2*z"));
        assert real.isSolved() : real.status();
        uses(real, Rule.ODE_SYSTEM_MATRIX, Rule.ODE_SYSTEM_EIGEN, Rule.ODE_SYSTEM_SOLUTION);
        assert Odes.solutions(real).size() == 2 : Odes.solutions(real);

        Solution oscillator = OdeSystems.solve(List.of("y' = z", "z' = -y"));
        assert oscillator.isSolved() : oscillator.status();

        Solution defective = OdeSystems.solve(List.of("y' = y + z", "z' = z"));
        assert defective.isSolved() : defective.status();
    }

    @Test
    void theStepsOfASystemAreWrittenInBothLanguagesAndAllFormats() {
        Solution secondOrder = OdeSystems.solve(List.of("y'' = 3*z", "z' = y"));
        uses(secondOrder, Rule.ODE_REDUCE_ORDER);
        Solution solved = OdeSystems.solve(List.of("y' = 3*y", "z' = y + 2*z"));
        assert solved.isSolved() : solved.status();
        for (Solution s : List.of(secondOrder, solved)) {
            for (Locale locale : Messages.supportedLocales()) {
                for (Format f : Format.values()) assert !s.render(locale, f).isBlank();
            }
        }
    }
}
