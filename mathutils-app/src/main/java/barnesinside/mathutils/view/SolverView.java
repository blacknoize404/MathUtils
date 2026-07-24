package barnesinside.mathutils.view;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.steps.Solution;
import MathUtils.exceptions.ExpressionParseException;
import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.ui.Ui;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Base de los módulos que resuelven un problema paso a paso (derivadas, límites, integrales):
 * formulario con la función y la variable, ejemplos para probar y el panel con la solución.
 * Cada módulo aporta sus campos extra y el cálculo.
 */
abstract class SolverView implements Module {

    private static final Pattern VARIABLE = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]*");

    protected final Workspace workspace;
    protected final I18n t = AppContext.i18n();

    protected final TextField function = new TextField();
    protected final TextField variable = new TextField("x");
    private final Label error = new Label();
    private final SolutionPanel panel = new SolutionPanel();
    private Node content;
    private String lastShared = "";

    protected SolverView(Workspace workspace) {
        this.workspace = workspace;
    }

    // ---------------------------------------------------------------- Lo que aporta cada módulo

    /** Prefijo de las claves de texto del módulo: {@code derivative}, {@code limit} o {@code integral}. */
    protected abstract String keyPrefix();

    protected abstract List<String> examples();

    /** Texto con el que se muestra un ejemplo (por defecto, el propio ejemplo). */
    protected String exampleLabel(String example) {
        return example;
    }

    /** Rellena el formulario con un ejemplo (por defecto, solo la función). */
    protected void applyExample(String example) {
        function.setText(example);
    }

    /** Campos adicionales del formulario (orden, punto…), o una lista vacía. */
    protected List<Node> extraFields() {
        return List.of();
    }

    /**
     * Calcula la solución.
     * @throws IllegalArgumentException con un mensaje para el usuario si los campos adicionales no son válidos
     */
    protected abstract Solution compute(Expr f, String variableName);

    // ---------------------------------------------------------------- Módulo

    @Override
    public void onShow() {
        String shared = workspace.expressionProperty().get();
        if (shared != null && !shared.isBlank() && !shared.equals(lastShared)) {
            lastShared = shared;
            function.setText(shared);
            solve();
        }
        function.requestFocus();
    }

    @Override
    public Node content() {
        if (content == null) content = build();
        return content;
    }

    /** Resuelve desde fuera (para pruebas y para recibir una expresión de otro módulo). */
    public void solve(String expression) {
        function.setText(expression);
        solve();
    }

    public Solution solution() {
        return panel.solution();
    }

    protected StringBinding text(String suffix) {
        return t.bind(keyPrefix() + "." + suffix);
    }

    // ---------------------------------------------------------------- Construcción

    private Node build() {
        function.getStyleClass().add("expression-field");
        function.promptTextProperty().bind(text("function.placeholder"));
        function.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) solve();
        });
        variable.setPrefWidth(90);
        variable.getStyleClass().add("mono");
        variable.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) solve();
        });

        VBox functionBox = new VBox(4, Ui.caption(text("function")), function);
        HBox.setHgrow(functionBox, Priority.ALWAYS);
        HBox form = new HBox(12, functionBox, new VBox(4, Ui.caption(t.bind("solver.variable")), variable));
        form.getChildren().addAll(extraFields());
        form.setAlignment(Pos.BOTTOM_LEFT);

        error.getStyleClass().add("message-error");
        error.setWrapText(true);
        error.setManaged(false);
        error.setVisible(false);

        FlowPane examples = new FlowPane(8, 8);
        for (String example : examples()) {
            Label chip = new Label(exampleLabel(example));
            HBox box = new HBox(chip);
            box.getStyleClass().add("chip");
            box.setOnMouseClicked(e -> {
                applyExample(example);
                solve();
            });
            examples.getChildren().add(box);
        }

        Label empty = Ui.label(text("empty"), "text-muted");
        empty.managedProperty().bind(empty.visibleProperty());
        VBox output = new VBox(20, panel);
        output.getChildren().add(0, empty);
        panel.getChildren().addListener((javafx.collections.ListChangeListener<Node>) c ->
                empty.setVisible(panel.getChildren().isEmpty()));

        VBox main = new VBox(20, Ui.pageHeader(text("title"), text("subtitle")), form,
                Ui.primary(t.bind("solver.solve"), "play", this::solve), error,
                new VBox(8, Ui.caption(t.bind("solver.examples")), examples), output);
        main.setPadding(new Insets(32));
        main.setMaxWidth(920);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        return scroll;
    }

    // ---------------------------------------------------------------- Lógica

    private void solve() {
        String text = function.getText();
        if (text == null || text.isBlank()) return;

        String name = variable.getText() == null ? "" : variable.getText().trim();
        if (!VARIABLE.matcher(name).matches()) {
            showError(t.get("solver.invalidVariable"));
            return;
        }

        Expr parsed;
        try {
            parsed = ExprParser.parse(text);
        } catch (ExpressionParseException e) {
            showError(e.getMessage());
            return;
        }

        Solution result;
        try {
            result = compute(parsed, name);
        } catch (IllegalArgumentException | ExpressionParseException e) {
            showError(e.getMessage());
            return;
        }

        error.setVisible(false);
        error.setManaged(false);
        panel.show(result);
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisible(true);
        error.setManaged(true);
    }
}
