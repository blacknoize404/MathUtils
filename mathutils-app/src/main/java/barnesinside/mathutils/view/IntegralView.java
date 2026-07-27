package barnesinside.mathutils.view;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.integral.Integrals;
import MathUtils.cas.steps.Solution;
import barnesinside.mathutils.Workspace;
import javafx.beans.binding.StringBinding;

import java.util.List;

/**
 * Integrales indefinidas paso a paso: tabla, linealidad, sustitución, partes, fracciones parciales,
 * trigonométricas y radicales. Cada resultado
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
        return List.of("x*e^x", "x^2*sin(x)", "e^x*sin(x)", "x*e^(x^2)", "ln(x)", "atan(x)",
                "1/(x^2-3x+2)", "(3x+2)/(x^2+x+1)", "1/(x^2+1)^2",
                "sin(x)^3", "sin(x)^2*cos(x)^2", "sec(x)^3", "sin(3x)*cos(2x)", "1/(2+sin(x))",
                "sqrt(1-x^2)", "1/(x^2*sqrt(x^2+1))", "x*sqrt(x+1)");
    }

    @Override
    protected Solution compute(Expr f, String variableName) {
        return Integrals.indefinite(f, variableName);
    }
}
