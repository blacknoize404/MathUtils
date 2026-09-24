package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.SolutionSet;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

class DescribedSolutionTest {

    @Test
    void anInequationWithInfinitelyManyRootsIsAnsweredWithItsCondition() {
        Solution s = Equations.solve("2+2x^(2)*cos(2 x)≠0");
        assert s.isSolved() : "ya no se queda sin respuesta";
        assert s.answer() instanceof SolutionSet.Described d && d.variable().equals("x") : s.answer();
        assert s.answer().toString().startsWith("{x ∈ ℝ : ") && s.answer().toString().contains("!=") : s.answer();
        assert s.steps().stream().anyMatch(step -> step.rule() == Rule.ALG_NUMERIC_TOO_MANY);
        assert s.steps().stream().anyMatch(step -> step.rule() == Rule.ALG_NUMERIC_SAMPLES) : "da unas raíces de ejemplo";
        assert Equations.isApproximate(s) == false;
    }

    @Test
    void theSamplesAreRootsCloseToTheOriginAndTheSetKnowsItsMembers() {
        Solution s = Equations.solve("2+2x^(2)*cos(2 x)≠0");
        SolutionSet.Described d = (SolutionSet.Described) s.answer();
        assert !d.samples().isEmpty() && d.samples().size() <= 12;
        for (var sample : d.samples()) {
            double x = barnesinside.mathutils.cas.expr.Eval.value(sample);
            assert Math.abs(x) <= 10 : x;
            double residual = Math.abs(2 + 2 * x * x * Math.cos(2 * x));
            assert residual < 0.05 : "debe ser una raíz de la expresión: " + x + " deja " + residual;
        }
        assert d.contains(0) : "en 0 la expresión vale 2";
        assert d.contains(0.5);
    }

    @Test
    void equationsAndInequalitiesWithTooManyRootsAreDescribedToo() {
        Solution equation = Equations.solve("2+2x^(2)*cos(2 x)=0");
        assert equation.isSolved() && equation.answer().toString().contains("= 0}") : equation.answer();
        Solution inequality = Equations.solve("2+2x^(2)*cos(2 x)>0");
        assert inequality.isSolved() && inequality.answer() instanceof SolutionSet.Described : inequality.answer();
        assert inequality.answer().toLatex().contains("\\in \\mathbb{R}") : inequality.answer().toLatex();
    }

    @Test
    void whatCanBeListedIsStillListed() {
        assert Equations.solve("x*sin(x)=1").answer() instanceof SolutionSet.Finite;
        assert Equations.solve("x^2 - 5x + 6 >= 0").answer() instanceof SolutionSet.Intervals;
    }
}
