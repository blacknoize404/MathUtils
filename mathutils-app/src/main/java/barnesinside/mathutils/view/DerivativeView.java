package barnesinside.mathutils.view;

import MathUtils.cas.diff.Derivatives;
import MathUtils.cas.diff.MultiDerivatives;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.steps.Solution;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.ui.Segmented;
import barnesinside.mathutils.ui.Ui;
import javafx.beans.binding.StringBinding;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Collections;
import java.util.List;

/**
 * Derivadas paso a paso: cada regla aplicada se muestra con su fórmula, los valores concretos
 * que toma en el problema y el resultado parcial. Con varias variables calcula derivadas parciales
 * (también mixtas), el gradiente, la hessiana, la jacobiana, la divergencia y el laplaciano.
 */
public final class DerivativeView extends SolverView {

    /** Qué se calcula. */
    public enum Mode {DERIVATIVE, GRADIENT, HESSIAN, JACOBIAN, DIVERGENCE, LAPLACIAN}

    private final TextField order = new TextField("1");
    private final Segmented<Mode> mode = new Segmented<>(List.of(Mode.values()), Mode.DERIVATIVE,
            m -> barnesinside.mathutils.AppContext.i18n().get("derivative.mode." + m.name().toLowerCase()));

    public DerivativeView(Workspace workspace) {
        super(workspace);
        order.setPrefWidth(90);
        order.getStyleClass().add("mono");
        variable.setPrefWidth(120);
        variable.promptTextProperty().bind(t.bind("derivative.variables.placeholder"));
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
    protected StringBinding variableCaption() {
        return t.bind("derivative.variables");
    }

    /** Cada ejemplo es {@code función|variables|modo}; las funciones de una jacobiana van separadas por «;». */
    @Override
    protected List<String> examples() {
        return List.of("x^2 sin(x)||D", "sin(x^2)||D", "(x^2+1)/(x-3)||D", "e^(2x) cos(x)||D", "x^x||D", "ln(x)/x||D",
                "atan(2x)||D", "sqrt(1+x^2)||D",
                "x^2*y^3 + sin(x*y)|x, y|D", "x^2*y + y^3||G", "x^2*y + y^3||H", "x*y; x + y^2||J",
                "x^2; x*y; z|x, y, z|V", "x^2 + y^3 + z||L");
    }

    @Override
    protected String exampleLabel(String example) {
        String[] parts = example.split("\\|", -1);
        String tag = switch (parts[2]) {
            case "G" -> "∇ ";
            case "H" -> "H ";
            case "J" -> "J ";
            case "V" -> "∇· ";
            case "L" -> "∇² ";
            default -> parts[1].isEmpty() ? "" : "∂ ";
        };
        return tag + parts[0];
    }

    @Override
    protected void applyExample(String example) {
        String[] parts = example.split("\\|", -1);
        function.setText(parts[0]);
        variable.setText(parts[1].isEmpty() && parts[2].equals("D") ? "x" : parts[1]);
        mode.setValue(switch (parts[2]) {
            case "G" -> Mode.GRADIENT;
            case "H" -> Mode.HESSIAN;
            case "J" -> Mode.JACOBIAN;
            case "V" -> Mode.DIVERGENCE;
            case "L" -> Mode.LAPLACIAN;
            default -> Mode.DERIVATIVE;
        });
    }

    @Override
    protected List<javafx.scene.control.TextInputControl> extraInputs() {
        return List.of(order);
    }

    @Override
    protected List<Node> extraFields() {
        return List.of(new VBox(4, Ui.caption(t.bind("derivative.order")), order));
    }

    @Override
    protected Node optionsRow() {
        // Seis opciones no caben en el ancho de la columna: se desplazan en vez de recortarse
        javafx.scene.control.ScrollPane scroller = new javafx.scene.control.ScrollPane(mode);
        scroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setFitToHeight(true);
        scroller.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        scroller.getStyleClass().add("animator-scroll");
        mode.getChildren().forEach(n -> ((javafx.scene.control.ToggleButton) n).setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE));
        return new VBox(4, Ui.caption(t.bind("derivative.mode")), scroller);
    }

    /** Elige el tipo de cálculo desde fuera (para pruebas). */
    public void setMode(Mode newMode) {
        mode.setValue(newMode);
    }

    /** Resuelve con un modo y variables concretos desde fuera (para pruebas). */
    public void solve(String expression, String variables, Mode newMode) {
        mode.setValue(newMode);
        variable.setText(variables);
        solve(expression);
    }

    @Override
    protected Solution compute(Expr f, String variableName) {
        return Derivatives.nth(f, variableName, derivativeOrder());
    }

    private int derivativeOrder() {
        try {
            int value = Integer.parseInt(order.getText().trim());
            if (value < 1 || value > 10) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(t.get("derivative.invalidOrder"));
        }
    }

    @Override
    protected Computation computeAll(String functionText, String variablesText) {
        List<String> variables = variableList(variablesText);
        Mode selected = mode.getValue();

        if (selected == Mode.JACOBIAN || selected == Mode.DIVERGENCE) {
            List<Expr> functions = MultiDerivatives.parseFunctions(functionText);
            if (functions.isEmpty()) throw new IllegalArgumentException(t.get("derivative.error.functions"));
            if (variables.isEmpty()) variables = MultiDerivatives.variablesOf(functions.toArray(new Expr[0]));
            if (selected == Mode.DIVERGENCE && functions.size() != variables.size()) {
                throw new IllegalArgumentException(t.get("derivative.error.divergence"));
            }
            return MultiResults.of(selected == Mode.JACOBIAN
                    ? MultiDerivatives.jacobian(functions, variables) : MultiDerivatives.divergence(functions, variables));
        }

        Expr f = ExprParser.parse(functionText);
        if (selected != Mode.DERIVATIVE && variables.isEmpty()) variables = MultiDerivatives.variablesOf(f);
        switch (selected) {
            case GRADIENT -> {
                return MultiResults.of(MultiDerivatives.gradient(f, variables));
            }
            case HESSIAN -> {
                return MultiResults.of(MultiDerivatives.hessian(f, variables));
            }
            case LAPLACIAN -> {
                return MultiResults.of(MultiDerivatives.laplacian(f, variables));
            }
            default -> {
                if (variables.isEmpty()) throw new IllegalArgumentException(t.get("solver.invalidVariable"));
                if (variables.size() > 1) return Computation.single(MultiDerivatives.mixed(f, variables));
                // Con otras variables en la función es una derivada parcial; con una sola, la derivada de siempre
                if (f.variables().size() > 1) {
                    return Computation.single(MultiDerivatives.mixed(f, Collections.nCopies(derivativeOrder(), variables.get(0))));
                }
                return Computation.single(compute(f, variables.get(0)));
            }
        }
    }
}
