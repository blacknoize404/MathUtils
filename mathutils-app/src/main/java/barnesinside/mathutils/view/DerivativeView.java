package barnesinside.mathutils.view;

import MathUtils.cas.diff.Derivatives;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.steps.Solution;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.ui.Ui;
import javafx.beans.binding.StringBinding;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Derivadas paso a paso: cada regla aplicada se muestra con su fórmula, los valores concretos
 * que toma en el problema y el resultado parcial.
 */
public final class DerivativeView extends SolverView {

    private final TextField order = new TextField("1");

    public DerivativeView(Workspace workspace) {
        super(workspace);
        order.setPrefWidth(90);
        order.getStyleClass().add("mono");
    }

    @Override
    public String id() {
        return "derivatives";
    }

    @Override
    public StringBinding title() {
        return t.bind("nav.derivatives");
    }

    @Override
    public String icon() {
        return "sigma";
    }

    @Override
    protected String keyPrefix() {
        return "derivative";
    }

    @Override
    protected List<String> examples() {
        return List.of("x^2 sin(x)", "sin(x^2)", "(x^2+1)/(x-3)", "e^(2x) cos(x)", "x^x", "ln(x)/x", "atan(2x)",
                "sqrt(1+x^2)");
    }

    @Override
    protected List<Node> extraFields() {
        return List.of(new VBox(4, Ui.caption(t.bind("derivative.order")), order));
    }

    @Override
    protected Solution compute(Expr f, String variableName) {
        int derivativeOrder;
        try {
            derivativeOrder = Integer.parseInt(order.getText().trim());
            if (derivativeOrder < 1 || derivativeOrder > 10) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(t.get("derivative.invalidOrder"));
        }
        return Derivatives.nth(f, variableName, derivativeOrder);
    }
}
