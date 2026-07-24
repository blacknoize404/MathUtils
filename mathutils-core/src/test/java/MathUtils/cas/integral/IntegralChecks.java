package MathUtils.cas.integral;

import MathUtils.calculus.Integration;
import MathUtils.cas.diff.Derivatives;
import MathUtils.cas.expr.Equivalence;
import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Solution;
import MathUtils.cas.steps.Step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Oráculos compartidos por los tests de integrales. */
final class IntegralChecks {

    private IntegralChecks() {
    }

    static List<Step> flatten(List<Step> steps) {
        List<Step> all = new ArrayList<>();
        for (Step s : steps) {
            all.add(s);
            all.addAll(flatten(s.substeps()));
        }
        return all;
    }

    static boolean uses(Solution s, Rule rule) {
        return flatten(s.steps()).stream().anyMatch(step -> step.rule() == rule);
    }

    /**
     * Comprueba con dos oráculos independientes: la derivada del resultado debe ser el integrando
     * y la integral definida por Barrow debe coincidir con la cuadratura numérica en [a, b].
     */
    static Solution check(String integrand, double a, double b) {
        Solution s = Integrals.indefinite(integrand, "x");
        assert s.isSolved() : integrand + " -> " + s.status() + "\n" + s;

        Expr f = ExprParser.parse(integrand);
        Expr derivative = Derivatives.derivative(s.result(), "x").result();
        assert Equivalence.check(derivative, f) != Equivalence.Result.DIFFERENT
                : integrand + ": la derivada de " + s.result() + " es " + derivative;

        double exact = Eval.value(s.result(), Map.of("x", b)) - Eval.value(s.result(), Map.of("x", a));
        double numeric = Integration.simpson13(t -> Eval.value(f, Map.of("x", t)), a, b, 2000);
        assert Math.abs(exact - numeric) <= 1e-6 * (1 + Math.abs(numeric))
                : integrand + " en [" + a + ", " + b + "]: Barrow " + exact + " vs numérica " + numeric + " con " + s.result();
        return s;
    }
}
