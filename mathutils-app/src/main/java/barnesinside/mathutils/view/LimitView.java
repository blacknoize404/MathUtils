package barnesinside.mathutils.view;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.limit.Limits;
import MathUtils.cas.limit.MultiLimits;
import MathUtils.cas.steps.Solution;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.math.MathLayout;
import barnesinside.mathutils.ui.Segmented;
import barnesinside.mathutils.ui.Ui;
import javafx.beans.binding.StringBinding;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Límites paso a paso: sustitución directa, límites en el infinito, L'Hôpital y formas
 * indeterminadas, con límites laterales.
 */
public final class LimitView extends SolverView {

    private final TextField point = new TextField("0");
    private final Segmented<Limits.Direction> direction = new Segmented<>(
            List.of(Limits.Direction.BOTH, Limits.Direction.LEFT, Limits.Direction.RIGHT), Limits.Direction.BOTH,
            d -> switch (d) {
                case BOTH -> t.get("limit.direction.both");
                case LEFT -> t.get("limit.direction.left");
                case RIGHT -> t.get("limit.direction.right");
            });

    public LimitView(Workspace workspace) {
        super(workspace);
        point.setPrefWidth(130);
        point.getStyleClass().add("mono");
        point.promptTextProperty().bind(t.bind("limit.point.placeholder"));
        direction.valueProperty().addListener((obs, old, now) -> refreshPreview());
    }

    @Override
    public String id() {
        return "limits";
    }

    @Override
    public StringBinding title() {
        return t.bind("nav.limits");
    }

    @Override
    public String icon() {
        return "infinity";
    }

    @Override
    protected String keyPrefix() {
        return "limit";
    }

    /** Cada ejemplo es {@code función|punto|lado} con el lado {@code B}, {@code L} o {@code R}. */
    @Override
    protected List<String> examples() {
        return List.of("sin(x)/x|0|B", "(1+1/x)^x|oo|B", "(1-cos(x))/x^2|0|B", "x*ln(x)|0|R", "1/x|0|R",
                "sqrt(x^2+x)-x|oo|B", "(3x^2+x)/(2x^2-5)|oo|B", "abs(x)/x|0|B",
                "x^2*y/(x^2+y^2)|0; 0|B", "x*y/(x^2+y^2)|0; 0|B", "(x^2-y^2)/(x^2+y^2)|0; 0|B",
                "x^2*y/(x^4+y^2)|0; 0|B");
    }

    @Override
    protected String exampleLabel(String example) {
        String[] parts = example.split("\\|");
        String side = parts[2].equals("R") ? "+" : parts[2].equals("L") ? "-" : "";
        if (parts[1].contains(";")) return parts[0] + "   ((x, y) → (" + parts[1].replace(";", ",") + "))";
        return parts[0] + "   (x → " + parts[1] + side + ")";
    }

    @Override
    protected void applyExample(String example) {
        String[] parts = example.split("\\|");
        function.setText(parts[0]);
        point.setText(parts[1]);
        int count = parts[1].split(";").length;
        variable.setText(String.join(", ", java.util.Arrays.asList("x", "y", "z").subList(0, Math.min(3, count))));
        direction.setValue(switch (parts[2]) {
            case "R" -> Limits.Direction.RIGHT;
            case "L" -> Limits.Direction.LEFT;
            default -> Limits.Direction.BOTH;
        });
    }

    /** Resuelve un límite desde fuera (para pruebas). */
    public void solve(String expression, String pointText, Limits.Direction side) {
        point.setText(pointText);
        direction.setValue(side);
        solve(expression);
    }

    @Override
    protected List<javafx.scene.control.TextInputControl> extraInputs() {
        return List.of(point);
    }

    @Override
    protected List<Node> extraFields() {
        return List.of(new VBox(4, Ui.caption(t.bind("limit.point")), point));
    }

    @Override
    protected Node optionsRow() {
        return new VBox(4, Ui.caption(t.bind("limit.direction")), direction);
    }

    @Override
    protected StringBinding variableCaption() {
        return t.bind("limit.variables");
    }

    @Override
    protected MathLayout.Box previewBox(String functionText, String variablesText) {
        List<String> variables = variableList(variablesText);
        Expr f = ExprParser.parse(functionText);
        String pointText = point.getText() == null ? "" : point.getText();
        if (variables.size() > 1) {
            String[] pieces = pointText.contains(";") ? pointText.split(";", -1) : pointText.split(",", -1);
            List<Expr> values = new java.util.ArrayList<>();
            for (String piece : pieces) values.add(ExprParser.parse(piece.isBlank() ? "0" : piece));
            return MathLayout.limitEquation(Expr.sym("(" + String.join(", ", variables) + ")"),
                    Expr.sym("(" + String.join(", ", values.stream().map(Expr::toString).toList()) + ")"), 0, f, null, 22);
        }
        Expr target = ExprParser.parse(pointText.isBlank() ? "0" : pointText);
        int side = direction.getValue() == Limits.Direction.RIGHT ? 1 : direction.getValue() == Limits.Direction.LEFT ? -1 : 0;
        return MathLayout.limitEquation(Expr.sym(variables.isEmpty() ? "x" : variables.get(0)), target, side, f, null, 22);
    }

    /** Con varias variables, el punto se escribe con un valor por variable, separados por «;» o por comas. */
    @Override
    protected Computation computeAll(String functionText, String variablesText) {
        List<String> variables = variableList(variablesText);
        if (variables.size() < 2) return super.computeAll(functionText, variablesText);

        String pointText = point.getText() == null ? "" : point.getText();
        String[] pieces = pointText.contains(";") ? pointText.split(";", -1) : pointText.split(",", -1);
        if (pieces.length != variables.size()) throw new IllegalArgumentException(t.get("limit.error.point"));
        List<Expr> values = new java.util.ArrayList<>();
        for (String piece : pieces) values.add(ExprParser.parse(piece.isBlank() ? "0" : piece));
        return Computation.single(MultiLimits.limit(ExprParser.parse(functionText), variables, values));
    }

    @Override
    protected Solution compute(Expr f, String variableName) {
        Expr target = ExprParser.parse(point.getText() == null || point.getText().isBlank() ? "0" : point.getText());
        return Limits.limit(f, variableName, target, direction.getValue());
    }
}
