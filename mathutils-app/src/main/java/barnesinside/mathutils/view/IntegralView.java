package barnesinside.mathutils.view;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.integral.Integrals;
import MathUtils.cas.steps.Solution;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.ui.Ui;
import javafx.beans.binding.StringBinding;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Integrales indefinidas, definidas e impropias paso a paso: tabla, linealidad, sustitución, partes, fracciones parciales,
 * trigonométricas y radicales. Cada resultado
 * se verifica derivándolo antes de mostrarse.
 */
public final class IntegralView extends SolverView {

    private final TextField lower = new TextField();
    private final TextField upper = new TextField();

    public IntegralView(Workspace workspace) {
        super(workspace);
        for (TextField field : List.of(lower, upper)) {
            field.setPrefWidth(110);
            field.getStyleClass().add("mono");
        }
        lower.promptTextProperty().bind(t.bind("integral.lower.placeholder"));
        upper.promptTextProperty().bind(t.bind("integral.upper.placeholder"));
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
                "sqrt(1-x^2)", "1/(x^2*sqrt(x^2+1))", "x*sqrt(x+1)", "sin(x)/x", "e^(-x^2)",
                "x^2|0|1", "sin(x)|0|pi", "e^(-x)|0|oo", "1/(1+x^2)|-oo|oo", "ln(x)|0|1", "1/x^2|1|oo", "1/x|0|1");
    }

    /** Cada ejemplo es {@code función} o {@code función|inferior|superior}. */
    @Override
    protected void applyExample(String example) {
        String[] parts = example.split("\\|");
        function.setText(parts[0]);
        lower.setText(parts.length > 2 ? parts[1] : "");
        upper.setText(parts.length > 2 ? parts[2] : "");
    }

    @Override
    protected String exampleLabel(String example) {
        String[] parts = example.split("\\|");
        return parts.length > 2 ? parts[0] + "   [" + parts[1] + ", " + parts[2] + "]" : parts[0];
    }

    /** Resuelve una integral definida desde fuera (para pruebas). */
    public void solve(String expression, String lowerText, String upperText) {
        lower.setText(lowerText);
        upper.setText(upperText);
        solve(expression);
    }

    @Override
    protected List<Node> extraFields() {
        return List.of(
                new VBox(4, Ui.caption(t.bind("integral.lower")), lower),
                new VBox(4, Ui.caption(t.bind("integral.upper")), upper));
    }

    @Override
    protected Solution compute(Expr f, String variableName) {
        boolean hasLower = lower.getText() != null && !lower.getText().isBlank();
        boolean hasUpper = upper.getText() != null && !upper.getText().isBlank();
        if (hasLower != hasUpper) throw new IllegalArgumentException(t.get("integral.bounds.both"));
        if (!hasLower) return Integrals.indefinite(f, variableName);
        return Integrals.definite(f, variableName, ExprParser.parse(lower.getText()), ExprParser.parse(upper.getText()));
    }
}
