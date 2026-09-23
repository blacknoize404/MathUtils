package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.expr.SolutionSet;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

import java.util.List;

class ParametricEquationsTest {

    private static Solution solve(String text, String variable) {
        return Equations.solve(Relation.parse(text).orElseThrow(), variable);
    }

    /** Sustituye las letras por números en cada solución y comprueba que la ecuación se cumple. */
    private static void checks(String text, String variable, java.util.Map<String, Double> values) {
        Solution s = solve(text, variable);
        assert s.isSolved() : "sin resolver: " + text;
        Relation relation = Relation.parse(text).orElseThrow();
        List<Expr> roots = ((SolutionSet.Finite) s.answer()).values();
        assert !roots.isEmpty();
        for (Expr root : roots) {
            java.util.Map<String, Double> all = new java.util.HashMap<>(values);
            double x = barnesinside.mathutils.cas.expr.Eval.value(root, values);
            all.put(variable, x);
            double residual = barnesinside.mathutils.cas.expr.Eval.value(relation.difference(), all);
            assert Math.abs(residual) < 1e-9 : text + " con " + variable + " = " + x + " deja " + residual;
        }
    }

    @Test
    void aLinearEquationWithAParameterIsSolvedForTheVariable() {
        Solution s = solve("x + y = 5", "x");
        assert s.isSolved() && s.answer().toString().equals("{5 - y}") || s.answer().toString().contains("y") : s.answer();
        checks("x + y = 5", "x", java.util.Map.of("y", 2.0));
        checks("3*x - 2*y = 7", "x", java.util.Map.of("y", -4.0));
        checks("a*x + b = 0", "x", java.util.Map.of("a", 2.5, "b", 3.0));
        checks("a*x + b = c*x + d", "x", java.util.Map.of("a", 1.0, "b", 2.0, "c", 5.0, "d", -3.0));
        // La otra letra también se puede despejar
        checks("x + y = 5", "y", java.util.Map.of("x", 1.0));
    }

    @Test
    void aSymbolicCoefficientAddsTheNonZeroAssumption() {
        Solution s = solve("a*x + b = 0", "x");
        assert s.steps().stream().anyMatch(step -> step.rule() == Rule.ALG_PARAM_NONZERO) : "dice que a no puede ser cero";
        assert s.steps().stream().anyMatch(step -> step.rule() == Rule.ALG_PARAMETERS);
        Solution numeric = solve("2*x + y = 0", "x");
        assert numeric.steps().stream().noneMatch(step -> step.rule() == Rule.ALG_PARAM_NONZERO) : "con coeficiente numérico no hace falta";
    }

    @Test
    void aQuadraticWithParametersUsesTheFormulaAndSaysWhenItIsReal() {
        checks("x^2 + b*x + c = 0", "x", java.util.Map.of("b", -5.0, "c", 6.0));
        checks("a*x^2 + b*x + c = 0", "x", java.util.Map.of("a", 2.0, "b", -7.0, "c", 3.0));
        checks("x^2 - a = 0", "x", java.util.Map.of("a", 9.0));
        checks("x^2 + y = 4", "x", java.util.Map.of("y", -5.0));
        Solution s = solve("a*x^2 + b*x + c = 0", "x");
        assert s.steps().stream().anyMatch(step -> step.rule() == Rule.ALG_PARAM_REAL) : "avisa de que el discriminante tiene que ser no negativo";
        assert s.steps().stream().anyMatch(step -> step.rule() == Rule.ALG_QUADRATIC_FORMULA);
        assert ((SolutionSet.Finite) s.answer()).values().size() == 2;
    }

    @Test
    void whatDoesNotHaveThatShapeStaysUnsolvedWithoutInventingAnAnswer() {
        assert !solve("sin(x) = y", "x").isSolved();
        assert !solve("x^3 + y = 0", "x").isSolved() : "de tercer grado con parámetro no se resuelve";
        assert !solve("x*y = 1", "z").isSolved() : "la variable no está en la ecuación";
        // Y las inecuaciones con parámetros siguen sin resolverse
        assert !solve("x + y > 5", "x").isSolved();
    }

    @Test
    void theApplicableMethodsOfAnEquationWithParametersAreJustAuto() {
        Relation r = Relation.parse("a*x + b = 0").orElseThrow();
        assert Equations.applicableMethods(r, "x").equals(List.of(SolveMethod.AUTO));
        assert Equations.applicableMethods(Relation.parse("sin(x) = y").orElseThrow(), "x").isEmpty();
    }

    @Test
    void theNumericCasesAreNotAffected() {
        assert Equations.solve("x^2 - 5x + 6 = 0").answer().toString().equals("{2, 3}");
        assert ExprParser.parse("x").equals(Expr.sym("x"));
    }
}
