package barnesinside.mathutils.view;

import MathUtils.cas.diff.Derivatives;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.steps.Format;
import MathUtils.cas.steps.Segment;
import MathUtils.cas.steps.Solution;
import MathUtils.cas.steps.Step;
import MathUtils.exceptions.ExpressionParseException;
import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.math.MathLayout;
import barnesinside.mathutils.math.MathView;
import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.ui.Toasts;
import barnesinside.mathutils.ui.Ui;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Derivadas paso a paso: cada regla aplicada se muestra con su fórmula, los valores concretos
 * que toma en el problema y el resultado parcial. Los pasos son un árbol que se puede plegar.
 */
public final class DerivativeView implements Module {

    private static final Pattern VARIABLE = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]*");
    private static final List<String> EXAMPLES = List.of(
            "x^2 sin(x)", "sin(x^2)", "(x^2+1)/(x-3)", "e^(2x) cos(x)", "x^x", "ln(x)/x", "atan(2x)", "sqrt(1+x^2)");

    private final Workspace workspace;
    private final I18n t = AppContext.i18n();

    private final TextField function = new TextField();
    private final TextField variable = new TextField("x");
    private final TextField order = new TextField("1");
    private final Label error = new Label();
    private final VBox output = new VBox(20);
    private final List<StepPane> panes = new ArrayList<>();

    private Solution solution;
    private Node content;
    private String lastShared = "";

    public DerivativeView(Workspace workspace) {
        this.workspace = workspace;
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

    /** Resuelve desde fuera (para pruebas). */
    public void solve(String expression) {
        function.setText(expression);
        solve();
    }

    public Solution solution() {
        return solution;
    }

    // ================================================================ Construcción

    private Node build() {
        function.getStyleClass().add("expression-field");
        function.promptTextProperty().bind(t.bind("derivative.function.placeholder"));
        HBox.setHgrow(function, Priority.ALWAYS);
        function.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) solve();
        });
        variable.setPrefWidth(90);
        variable.getStyleClass().add("mono");
        order.setPrefWidth(90);
        order.getStyleClass().add("mono");

        VBox functionBox = new VBox(4, Ui.caption(t.bind("derivative.function")), function);
        HBox.setHgrow(functionBox, Priority.ALWAYS);
        HBox form = new HBox(12, functionBox,
                new VBox(4, Ui.caption(t.bind("derivative.variable")), variable),
                new VBox(4, Ui.caption(t.bind("derivative.order")), order));
        form.setAlignment(Pos.BOTTOM_LEFT);

        Button solve = Ui.primary(t.bind("derivative.solve"), "play", this::solve);
        HBox actions = new HBox(12, solve);

        error.getStyleClass().add("message-error");
        error.setWrapText(true);
        error.setManaged(false);
        error.setVisible(false);

        FlowPane examples = new FlowPane(8, 8);
        for (String example : EXAMPLES) {
            Label chip = new Label(example);
            HBox box = new HBox(chip);
            box.getStyleClass().add("chip");
            box.setOnMouseClicked(e -> {
                function.setText(example);
                solve();
            });
            examples.getChildren().add(box);
        }
        VBox exampleBox = new VBox(8, Ui.caption(t.bind("derivative.examples")), examples);

        Label empty = Ui.label(t.bind("derivative.empty"), "text-muted");
        output.getChildren().add(empty);

        t.localeProperty().addListener((obs, old, now) -> {
            if (solution != null) render();
        });

        VBox main = new VBox(20, Ui.pageHeader(t.bind("derivative.title"), t.bind("derivative.subtitle")),
                form, actions, error, exampleBox, output);
        main.setPadding(new Insets(32));
        main.setMaxWidth(900);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        return scroll;
    }

    // ================================================================ Lógica

    private void solve() {
        String text = function.getText();
        if (text == null || text.isBlank()) return;

        String name = variable.getText() == null ? "" : variable.getText().trim();
        if (!VARIABLE.matcher(name).matches()) {
            showError(t.get("derivative.invalidVariable"));
            return;
        }
        int derivativeOrder;
        try {
            derivativeOrder = Integer.parseInt(order.getText().trim());
            if (derivativeOrder < 1 || derivativeOrder > 10) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError(t.get("derivative.invalidOrder"));
            return;
        }

        Expr parsed;
        try {
            parsed = ExprParser.parse(text);
        } catch (ExpressionParseException e) {
            showError(e.getMessage());
            return;
        }

        error.setVisible(false);
        error.setManaged(false);
        solution = Derivatives.nth(parsed, name, derivativeOrder);
        render();
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisible(true);
        error.setManaged(true);
    }

    // ================================================================ Presentación

    private void render() {
        output.getChildren().clear();
        panes.clear();

        Expr variableExpr = Expr.sym(solution.variable());
        MathView statement = MathView.empty(22);
        statement.setBox(MathLayout.derivativeEquation(variableExpr, solution.problem(), null, solution.order(), 22));
        statement.setInteractive(false);
        output.getChildren().add(statement);

        if (!solution.isSolved()) {
            Label message = new Label(t.get("derivative.unsolved"));
            message.getStyleClass().add("message-error");
            VBox box = new VBox(message);
            box.getStyleClass().add("message-error-box");
            output.getChildren().add(box);
            return;
        }

        output.getChildren().add(resultCard(variableExpr));
        output.getChildren().add(stepsSection());
    }

    private Node resultCard(Expr variableExpr) {
        MathView equation = MathView.empty(26);
        equation.setBox(MathLayout.derivativeEquation(variableExpr, solution.problem(), solution.result(),
                solution.order(), 26));
        equation.setInteractive(false);

        FlowPane actions = new FlowPane(8, 8);
        actions.getChildren().addAll(
                Ui.ghost(t.bind("derivative.copyResult"), "copy", () -> copy(solution.result().toString())),
                Ui.ghost(t.bind("derivative.copyLatex"), "copy", () -> copy(solution.render(t.locale(), Format.LATEX))),
                Ui.ghost(t.bind("derivative.copyMarkdown"), "copy", () -> copy(solution.render(t.locale(), Format.MARKDOWN))),
                Ui.ghost(t.bind("derivative.copyJson"), "copy", () -> copy(solution.render(t.locale(), Format.JSON))));

        VBox card = new VBox(14, Ui.caption(t.bind("derivative.result")), scrollable(equation), actions);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        return card;
    }

    private void copy(String text) {
        Ui.copy(text);
        Toasts.show(t.get("derivative.copied"));
    }

    private Node stepsSection() {
        VBox steps = new VBox(0);
        List<Step> top = solution.steps();
        for (int i = 0; i < top.size(); i++) {
            steps.getChildren().add(stepPane(top.get(i), String.valueOf(i + 1), 0));
        }

        HBox header = new HBox(12, Ui.caption(t.bind("derivative.steps")), Ui.spacer(),
                Ui.ghost(t.bind("derivative.expandAll"), "chevron-down", () -> panes.forEach(p -> p.setExpanded(true))),
                Ui.ghost(t.bind("derivative.collapseAll"), "chevron-right", () -> panes.forEach(p -> p.setExpanded(false))));
        header.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, header, steps);
    }

    private StepPane stepPane(Step step, String number, int depth) {
        StepPane pane = new StepPane(step, number, depth);
        panes.add(pane);
        return pane;
    }

    /** Un paso plegable con su explicación, su ecuación y sus subpasos. */
    private final class StepPane extends VBox {
        private final VBox body = new VBox(10);
        private final VBox children = new VBox(0);
        private final javafx.scene.layout.Region chevron = new javafx.scene.layout.Region();
        private final Node chevronDown = Brand.icon("chevron-down", 16);
        private final Node chevronRight = Brand.icon("chevron-right", 16);
        private final HBox toggle = new HBox(8);
        private boolean expanded;

        StepPane(Step step, String number, int depth) {
            getStyleClass().add("step");

            Label numberLabel = Ui.label(number, "step-number");
            Label titleLabel = Ui.label(step.title(t.locale()), "step-title");
            toggle.getChildren().addAll(chevronDown, chevronRight, numberLabel, titleLabel);
            toggle.setAlignment(Pos.CENTER_LEFT);
            toggle.getStyleClass().add("toggle-chevron");
            toggle.setOnMouseClicked(e -> setExpanded(!expanded));
            getChildren().add(toggle);

            body.setPadding(new Insets(0, 0, 0, 24));
            body.getChildren().add(explanation(step));
            Node equation = equation(step);
            if (equation != null) body.getChildren().add(equation);
            getChildren().add(body);

            children.setPadding(new Insets(0, 0, 0, 16));
            for (int i = 0; i < step.substeps().size(); i++) {
                children.getChildren().add(stepPane(step.substeps().get(i), number + "." + (i + 1), depth + 1));
            }
            if (!step.substeps().isEmpty()) getChildren().add(children);

            setExpanded(depth < 2);
        }

        void setExpanded(boolean value) {
            expanded = value;
            body.setVisible(value);
            body.setManaged(value);
            children.setVisible(value);
            children.setManaged(value);
            chevronDown.setVisible(value);
            chevronDown.setManaged(value);
            chevronRight.setVisible(!value);
            chevronRight.setManaged(!value);
        }
    }

    private TextFlow explanation(Step step) {
        TextFlow flow = new TextFlow();
        flow.setLineSpacing(4);
        for (Segment segment : step.explanation(t.locale())) {
            switch (segment) {
                case Segment.Text text -> {
                    Text node = new Text(text.value());
                    node.getStyleClass().add("explanation-text");
                    flow.getChildren().add(node);
                }
                case Segment.Math math -> {
                    MathView view = new MathView(math.expr(), 15);
                    flow.getChildren().add(view);
                }
            }
        }
        return flow;
    }

    private Node equation(Step step) {
        if (step.before() == null || step.after() == null) return null;
        MathView view = MathView.empty(17);
        if (step.rule().category() == MathUtils.cas.steps.Rule.Category.DERIVATIVE && step.variable() != null) {
            view.setBox(MathLayout.derivativeEquation(step.variable(), step.before(), step.after(), 1, 17));
        } else {
            view.setBox(MathLayout.row(List.of(MathLayout.of(step.before(), 17), MathLayout.text(" = ", 17),
                    MathLayout.of(step.after(), 17))));
        }
        view.setInteractive(false);
        return scrollable(view);
    }

    /** Ecuación con desplazamiento horizontal cuando no cabe en el ancho disponible. */
    private static ScrollPane scrollable(MathView view) {
        ScrollPane pane = new ScrollPane(view);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pane.setFitToHeight(true);
        pane.setPannable(true);
        pane.setMinHeight(Region.USE_PREF_SIZE);
        pane.prefHeightProperty().bind(view.heightProperty().add(14));
        return pane;
    }
}
