package barnesinside.mathutils.view;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.integral.Integrals;
import MathUtils.cas.integral.MultipleIntegrals;
import MathUtils.cas.steps.Solution;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.math.MathLayout;
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
                "x^2|0|1", "sin(x)|0|pi", "e^(-x)|0|oo", "1/(1+x^2)|-oo|oo", "ln(x)|0|1", "1/x^2|1|oo", "1/x|0|1",
                "x*y|0; 0|y; 1|x, y", "x^2 + y^2|0; 0|1; 1|x, y", "1|0; -1|sqrt(1-x^2); 1|y, x",
                "x*y*z|0; 0; 0|1; 1; 1|x, y, z", "x*y|||x, y");
    }

    /** Cada ejemplo es {@code función}, {@code función|inferior|superior} o con las variables al final. */
    @Override
    protected void applyExample(String example) {
        String[] parts = example.split("\\|", -1);
        function.setText(parts[0]);
        lower.setText(parts.length > 2 ? parts[1] : "");
        upper.setText(parts.length > 2 ? parts[2] : "");
        variable.setText(parts.length > 3 ? parts[3].trim() : "x");
    }

    @Override
    protected String exampleLabel(String example) {
        String[] parts = example.split("\\|", -1);
        if (parts.length > 3) {
            boolean definite = !parts[1].isBlank();
            return "∬ " + parts[0] + (definite ? "   [" + parts[1] + " → " + parts[2] + "]" : "");
        }
        return parts.length > 2 ? parts[0] + "   [" + parts[1] + ", " + parts[2] + "]" : parts[0];
    }

    /** Resuelve una integral definida desde fuera (para pruebas). */
    public void solve(String expression, String lowerText, String upperText) {
        lower.setText(lowerText);
        upper.setText(upperText);
        solve(expression);
    }

    @Override
    protected List<javafx.scene.control.TextInputControl> extraInputs() {
        return List.of(lower, upper);
    }

    @Override
    protected List<Node> extraFields() {
        return List.of(
                new VBox(4, Ui.caption(t.bind("integral.lower")), lower),
                new VBox(4, Ui.caption(t.bind("integral.upper")), upper));
    }

    @Override
    protected StringBinding variableCaption() {
        return t.bind("integral.variables");
    }

    @Override
    protected MathLayout.Box previewBox(String functionText, String variablesText) {
        List<String> variables = variableList(variablesText);
        Expr f = ExprParser.parse(functionText);
        String[] lows = splitBounds(lower.getText());
        String[] highs = splitBounds(upper.getText());
        if (variables.size() > 1) {
            List<MathLayout.IntegralDimension> dimensions = new java.util.ArrayList<>();
            for (int i = 0; i < variables.size(); i++) {
                boolean has = i < lows.length && i < highs.length && !lows[i].isBlank() && !highs[i].isBlank();
                dimensions.add(new MathLayout.IntegralDimension(Expr.sym(variables.get(i)),
                        has ? ExprParser.parse(lows[i]) : null, has ? ExprParser.parse(highs[i]) : null));
            }
            return MathLayout.multipleIntegralEquation(dimensions, f, null, false, 22);
        }
        boolean definite = lows.length == 1 && highs.length == 1 && !lows[0].isBlank() && !highs[0].isBlank();
        return MathLayout.integralEquation(Expr.sym(variables.isEmpty() ? "x" : variables.get(0)), f,
                definite ? ExprParser.parse(lows[0]) : null, definite ? ExprParser.parse(highs[0]) : null, null, false, 22);
    }

    /**
     * Con varias variables es una integral iterada: se integra de la primera a la última (la primera es
     * la más interna) y los límites se escriben con uno por variable, separados por «;».
     */
    @Override
    protected Computation computeAll(String functionText, String variablesText) {
        List<String> variables = variableList(variablesText);
        if (variables.size() < 2) return super.computeAll(functionText, variablesText);

        String[] lows = splitBounds(lower.getText());
        String[] highs = splitBounds(upper.getText());
        boolean indefinite = lows.length == 0 && highs.length == 0;
        if (!indefinite && (lows.length != variables.size() || highs.length != variables.size())) {
            throw new IllegalArgumentException(t.get("integral.error.bounds"));
        }
        List<MultipleIntegrals.Dimension> dimensions = new java.util.ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            String low = indefinite ? "" : lows[i].trim();
            String high = indefinite ? "" : highs[i].trim();
            if (low.isEmpty() != high.isEmpty()) throw new IllegalArgumentException(t.get("integral.error.bounds"));
            dimensions.add(new MultipleIntegrals.Dimension(variables.get(i), low, high));
        }
        return Computation.single(MultipleIntegrals.iterated(ExprParser.parse(functionText), dimensions));
    }

    private static String[] splitBounds(String text) {
        return text == null || text.isBlank() ? new String[0] : text.split(";", -1);
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
