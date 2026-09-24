package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.SolutionSet;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * La búsqueda numérica tiene que acabar siempre y con una respuesta de tamaño razonable: cos(x^2) se anula
 * unas mil veces entre −50 y 50, y listar mil raíces con mil pasos cuelga a quien las pinte.
 */
@Timeout(value = 8, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class NumericSearchBoundsTest {

    private static boolean uses(List<Step> steps, Rule rule) {
        for (Step step : steps) if (step.rule() == rule || uses(step.substeps(), rule)) return true;
        return false;
    }

    /** Resuelve y comprueba que vuelve con un resultado acotado: nunca una excepción ni una lista enorme de pasos. */
    private static Solution assertReturns(String text) {
        Solution s = Equations.solve(text);
        assert s.status() == Solution.Status.SOLVED || s.status() == Solution.Status.UNSOLVED : text + " -> " + s.status();
        assert s.steps().size() <= 300 : text + " devolvió " + s.steps().size() + " pasos";
        if (s.isSolved() && s.answer() instanceof SolutionSet.Finite f) {
            assert f.values().size() <= EquationSolver.MAX_NUMERIC_ROOTS : text + " listó " + f.values().size() + " raíces";
        }
        // Se puede pintar entera en todos los formatos y los dos idiomas
        for (Format format : Format.values()) {
            assert !s.render(Locale.ENGLISH, format).isBlank() : text;
            assert !s.render(Locale.forLanguageTag("es"), format).isBlank() : text;
        }
        return s;
    }

    @Test
    void cosineSquaredEqualsCosineOfXSquaredReturns() {
        Solution s = assertReturns("cos^2(x) = cos(x^2)");
        // Demasiadas raíces para listarlas: se responde con el conjunto descrito por su condición
        assert s.isSolved() && s.answer() instanceof SolutionSet.Described : s.answer();
        assert uses(s.steps(), Rule.ALG_NUMERIC_TOO_MANY);
    }

    @Test
    void neighbouringTrigonometricEquationsReturn() {
        for (String text : List.of("sin(x^2) = sin(x)^2", "cos(x^2) = 0", "cos(x)^2 = cos(x^2)", "tan(x^2) = x",
                "sin(x^2) = 0", "cos(x^2) = sin(x)")) {
            assertReturns(text);
        }
    }

    @Test
    void inequalitiesWithManyCriticalPointsReturn() {
        for (String text : List.of("cos^2(x) > cos(x^2)", "cos^2(x) >= cos(x^2)", "cos(x^2) < cos(x)^2", "cos(x^2) <= 0",
                "cos(x^2) != cos(x)^2", "sin(x^2) > 0")) {
            Solution s = assertReturns(text);
            assert !(s.answer() instanceof SolutionSet.Finite) && !(s.answer() instanceof SolutionSet.Intervals) : text + " -> " + s.answer();
        }
        assert uses(Equations.solve("cos^2(x) > cos(x^2)").steps(), Rule.ALG_NUMERIC_TOO_MANY);
    }

    @Test
    void exponentialWithATrigonometricExponentDoesNotThrow() {
        Solution s = assertReturns("e^(sin(x)) = x^2");
        assert s.isSolved() && Equations.isApproximate(s) : s.status();
        assertReturns("2^(cos(x)) = x");
        assertReturns("e^(sin(x)) > x^2");
    }

    @Test
    void aFewNumericRootsAreStillListed() {
        Solution s = Equations.solve("sin(x) = x/10");
        assert s.isSolved() && Equations.isApproximate(s);
        assert s.answer() instanceof SolutionSet.Finite f && f.values().size() == 7 : s.answer();
        Solution cubic = Equations.solve("x = cos(x)");
        assert cubic.isSolved() && cubic.answer().toString().startsWith("{0.7390") : cubic.answer();
    }
}
