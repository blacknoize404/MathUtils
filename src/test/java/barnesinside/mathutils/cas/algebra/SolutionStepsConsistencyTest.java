package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Op;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Los pasos que muestran una relación no pueden cambiarla: comprueba que la expresión que enseñan es la del
 * enunciado (o su desarrollo exacto) evaluándolas en varios puntos.
 */
class SolutionStepsConsistencyTest {

    private static final double[] POINTS = {-3.5, -2.25, -1.5, -0.75, 0.5, 1.25, 2.0, 3.75};

    private static double difference(Relation r, double x) {
        return Eval.value(r.difference(), Map.of("x", x));
    }

    /** Los pasos de primer nivel que traen una relación, en orden. */
    private static List<Relation> shownRelations(Solution s) {
        List<Relation> out = new ArrayList<>();
        for (Step step : s.steps()) Relation.fromParams(step.params()).ifPresent(out::add);
        return out;
    }

    private static void assertSameDifference(Relation expected, Relation shown, String where) {
        for (double x : POINTS) {
            double a = difference(expected, x);
            double b = difference(shown, x);
            assert Math.abs(a - b) <= 1e-9 * Math.max(1, Math.abs(a)) : where + ": en x = " + x + " el enunciado vale " + a
                    + " pero el paso (" + shown + ") vale " + b;
        }
    }

    private static void assertFirstAndLastStepsKeepTheExpression(String text) {
        Solution s = Equations.solve(text);
        Relation statement = s.statement();
        assert statement != null : text;
        List<Relation> shown = shownRelations(s);
        assert !shown.isEmpty() : text + " sin pasos con relación";
        // El primer paso desarrolla el enunciado: tiene que ser igual a él
        assertSameDifference(statement, shown.get(0), text + " primer paso");
        assert shown.get(0).op() == statement.op() : text + " cambia el sentido en el primer paso: " + shown.get(0);
        // Las inecuaciones terminan reuniendo los intervalos de la misma expresión
        if (statement.op() != Op.EQ) {
            Relation last = shown.get(shown.size() - 1);
            assertSameDifference(statement, last, text + " último paso");
        }
    }

    @Test
    void theExpansionStepOfABinomialPowerKeepsEveryTerm() {
        Solution s = Inequalities.solve("(x+2)^3 > 0");
        Relation first = Relation.fromParams(s.steps().get(0).params()).orElseThrow();
        assert s.steps().get(0).rule() == Rule.ALG_SIMPLIFY;
        assert first.left().toString().equals("x^3 + 6*x^2 + 12*x + 8") : first;
        assert "(-2, ∞)".equals(s.answer().toString()) : s.answer();
    }

    @Test
    void stepsOfBinomialPowersAgreeWithTheStatement() {
        String[] texts = {
                "(x+2)^3 > 0", "(x+2)^3 >= 0", "(x+2)^3 < 0", "(x+2)^3 = 0",
                "(x-1)^4 > 0", "(x-1)^4 = 0", "(x-1)^4 <= 0", "(2x+3)^3 <= 0", "(2x+3)^3 = 0", "(2x+3)^3 > 0",
                "(3x-1)^5 >= 0", "(x+1)^6 > 0", "(1-x)^3 > 0", "(x+2)^3 > 8"
        };
        for (String text : texts) assertFirstAndLastStepsKeepTheExpression(text);
    }

    @Test
    void everyBinomialPowerKeepsItsTermsInTheSteps() {
        int[] slopes = {1, 2, 3, -1};
        for (int a : slopes) {
            for (int b = -3; b <= 3; b++) {
                if (b == 0) continue;
                for (int n = 2; n <= 6; n++) {
                    for (String op : new String[]{">", "<=", "="}) {
                        assertFirstAndLastStepsKeepTheExpression("(" + a + "x" + (b > 0 ? "+" : "") + b + ")^" + n + " " + op + " 0");
                    }
                }
            }
        }
    }

    @Test
    void theAnswersOfBinomialPowersAreRight() {
        assertAnswer("(-2, ∞)", "(x+2)^3 > 0");
        assertAnswer("{-2}", "(x+2)^3 = 0");
        assertAnswer("(-∞, 1) ∪ (1, ∞)", "(x-1)^4 > 0");
        assertAnswer("(-∞, -3/2]", "(2x+3)^3 <= 0");
    }

    private static void assertAnswer(String expected, String text) {
        Solution s = Equations.solve(text);
        assert s.isSolved() : text;
        assert expected.equals(s.answer().toString()) : text + " -> " + s.answer();
    }
}
