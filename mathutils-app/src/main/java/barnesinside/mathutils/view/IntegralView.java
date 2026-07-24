package barnesinside.mathutils.view;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.integral.Integrals;
import MathUtils.cas.steps.Solution;
import barnesinside.mathutils.Workspace;
import javafx.beans.binding.StringBinding;

import java.util.List;

/**
 * Integrales indefinidas paso a paso: tabla, linealidad, sustitución y partes. Cada resultado
 * se verifica derivándolo antes de mostrarse.
 */
public final class IntegralView extends SolverView {

    public IntegralView(Workspace workspace) {
        super(workspace);
    }

    @Override
    public String id() {
        return "integrals";
    }

    @Override
    public StringBinding title() {
        return t.bind("nav.integrals");
    }

    @Override
    public String icon() {
        return "chart-line";
    }

    @Override
    protected String keyPrefix() {
        return "integral";
    }

    @Override
    protected List<String> examples() {
        return List.of("x*e^x", "x^2*sin(x)", "sin(x)^2*cos(x)", "x*e^(x^2)", "1/(x^2+4)", "ln(x)", "e^x*sin(x)",
                "atan(x)", "x/(x^2+1)", "(2x+1)^5");
    }

    @Override
    protected Solution compute(Expr f, String variableName) {
        return Integrals.indefinite(f, variableName);
    }
}
