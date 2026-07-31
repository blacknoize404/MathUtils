package barnesinside.mathutils.view;

import MathUtils.cas.calc.CalculatorSession;
import MathUtils.cas.expr.AngleUnit;
import MathUtils.cas.expr.UserFunction;
import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.math.MathView;
import barnesinside.mathutils.ui.Segmented;
import barnesinside.mathutils.ui.Toasts;
import barnesinside.mathutils.ui.SymbolPalette;
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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

/**
 * Calculadora: resultado exacto y decimal, variables, funciones definidas por el usuario,
 * historial y teclado. Los resultados se pueden enviar al árbol, al orden de operaciones o al
 * módulo de derivadas.
 */
public final class CalculatorView implements Module {

    private static final int HISTORY_LIMIT = 40;

    private final Workspace workspace;
    private final I18n t = AppContext.i18n();
    private final CalculatorSession session;

    private final TextField input = new TextField();
    private final SymbolPalette symbols = new SymbolPalette(false);
    private final Label validation = new Label();
    private final VBox resultBox = new VBox(12);
    private final VBox historyBox = new VBox(8);
    private final VBox variablesBox = new VBox(6);
    private Node content;

    private CalculatorSession.Result lastResult;

    public CalculatorView(Workspace workspace) {
        this.workspace = workspace;
        this.session = workspace.session();
    }

    @Override
    public String id() {
        return "calculator";
    }

    @Override
    public StringBinding title() {
        return t.bind("nav.calculator");
    }

    @Override
    public String icon() {
        return "calculator";
    }

    @Override
    public void onShow() {
        input.requestFocus();
    }

    @Override
    public Node content() {
        if (content == null) content = build();
        return content;
    }

    // ================================================================ Construcción

    private Node build() {
        VBox main = new VBox(20);
        main.setPadding(new Insets(32));
        main.getChildren().addAll(
                Ui.pageHeader(t.bind("calc.title"), t.bind("calc.subtitle"), symbols.attach(input).trigger()),
                inputRow(),
                validationLabel(),
                resultBox,
                keypad());
        showEmptyResult();

        ScrollPane left = new ScrollPane(main);
        left.setFitToWidth(true);
        HBox.setHgrow(left, Priority.ALWAYS);

        VBox side = new VBox(24, historySection(), variablesSection());
        side.setPadding(new Insets(32, 24, 32, 24));
        ScrollPane right = new ScrollPane(side);
        right.setFitToWidth(true);
        right.setPrefWidth(340);
        right.setMinWidth(300);
        right.setMaxWidth(380);
        right.getStyleClass().add("hairline-left");
        right.setStyle("-fx-border-color: transparent transparent transparent -bi-border; -fx-border-width: 0 0 0 1;");

        refreshSide();
        return new HBox(left, right);
    }

    private Node inputRow() {
        input.getStyleClass().add("expression-field");
        input.promptTextProperty().bind(t.bind("calc.placeholder"));
        HBox.setHgrow(input, Priority.ALWAYS);
        input.textProperty().addListener((obs, old, now) -> validate(now));
        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) evaluate();
        });

        Button evaluate = Ui.primary(t.bind("calc.evaluate"), "equal", this::evaluate);
        HBox row = new HBox(12, input, evaluate);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node validationLabel() {
        validation.getStyleClass().add("message-error");
        validation.setWrapText(true);
        validation.setManaged(false);
        validation.setVisible(false);
        return validation;
    }

    private void validate(String text) {
        CalculatorSession.Validation v = session.validate(text);
        boolean invalid = !v.valid();
        input.getStyleClass().remove("invalid");
        if (invalid) {
            input.getStyleClass().add("invalid");
            String where = v.position() == null ? "" : "  (" + t.get("common.errorAt", v.position()) + ")";
            validation.setText(v.message() + where);
        }
        validation.setVisible(invalid);
        validation.setManaged(invalid);
    }

    // ================================================================ Resultado

    private void showEmptyResult() {
        resultBox.getChildren().setAll();
    }

    /** Escribe y evalúa una entrada (para pruebas y para enviarla desde otro módulo). */
    public void evaluate(String text) {
        input.setText(text);
        evaluate();
    }

    private void evaluate() {
        String text = input.getText();
        CalculatorSession.Result result = session.evaluate(text);
        lastResult = result;
        showResult(result);
        refreshSide();
    }

    private void showResult(CalculatorSession.Result r) {
        resultBox.getChildren().clear();

        if (r.isError()) {
            Label message = new Label(r.message());
            message.setWrapText(true);
            message.getStyleClass().add("message-error");
            VBox box = new VBox(message);
            box.getStyleClass().add("message-error-box");
            resultBox.getChildren().add(box);
            return;
        }

        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));

        if (r.status() == CalculatorSession.Status.DEFINED) {
            Label badge = Ui.label(t.get("calc.defined", r.message()), "mono");
            card.getChildren().add(badge);
        }

        card.getChildren().add(labelled(t.bind("calc.exact"), new MathView(r.exact(), 28)));

        if (r.decimalText() != null && !r.decimalText().equals(r.exactText())) {
            Label decimal = Ui.label(r.decimalText(), "mono");
            decimal.setStyle("-fx-font-size: 20px;");
            card.getChildren().add(labelled(t.bind("calc.decimal"), decimal));
        }
        if (r.status() == CalculatorSession.Status.SYMBOLIC) {
            Label note = Ui.label(t.bind("calc.symbolic"), "text-muted", "body-sm");
            card.getChildren().add(note);
        }

        FlowPane actions = new FlowPane(8, 8);
        actions.getChildren().addAll(
                Ui.secondary(t.bind("calc.sendTree"), "network", () -> workspace.sendTo("tree", input.getText())),
                Ui.secondary(t.bind("calc.sendOrder"), "list-ordered", () -> workspace.sendTo("order", input.getText())),
                Ui.secondary(t.bind("calc.sendDerivative"), "sigma", () -> workspace.sendTo("derivatives", input.getText())),
                Ui.secondary(t.bind("calc.sendIntegral"), "chart-line", () -> workspace.sendTo("integrals", input.getText())),
                Ui.ghost(t.bind("calc.copyResult"), "copy", () -> {
                    Ui.copy(r.decimalText() != null ? r.decimalText() : r.exactText());
                    Toasts.show(t.get("calc.copied"));
                }),
                Ui.ghost(t.bind("calc.copyLatex"), "copy", () -> {
                    Ui.copy(r.latex());
                    Toasts.show(t.get("calc.copied"));
                }));
        card.getChildren().add(actions);
        resultBox.getChildren().add(card);
    }

    private static VBox labelled(StringBinding caption, Node body) {
        VBox box = new VBox(4, Ui.caption(caption), body);
        return box;
    }

    // ================================================================ Teclado

    private Node keypad() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        for (int i = 0; i < 5; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(20);
            grid.getColumnConstraints().add(column);
        }

        String[][] rows = {
                {"sin", "sin(", "cos", "cos(", "tan", "tan(", "ln", "ln(", "√", "sqrt("},
                {"asin", "asin(", "acos", "acos(", "atan", "atan(", "log", "log(", "^", "^"},
                {"(", "(", ")", ")", "π", "pi", "e", "e", "ans", "ans"},
                {"7", "7", "8", "8", "9", "9", "÷", "/", "⌫", "<back>"},
                {"4", "4", "5", "5", "6", "6", "×", "*", "C", "<clear>"},
                {"1", "1", "2", "2", "3", "3", "−", "-", "=", "<eval>"},
                {"0", "0", ".", ".", "x", "x", "+", "+", ",", ","},
        };

        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < 5; c++) {
                String label = rows[r][c * 2];
                String action = rows[r][c * 2 + 1];
                grid.add(key(label, action), c, r);
            }
        }

        Segmented<AngleUnit> angle = new Segmented<>(List.of(AngleUnit.RADIANS, AngleUnit.DEGREES, AngleUnit.GRADIANS),
                session.angleUnit(), unit -> switch (unit) {
            case RADIANS -> "RAD";
            case DEGREES -> "DEG";
            case GRADIANS -> "GRAD";
        });
        angle.valueProperty().addListener((obs, old, now) -> session.setAngleUnit(now));

        HBox angleRow = new HBox(12, Ui.caption(t.bind("calc.angle")), angle);
        angleRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(12, angleRow, grid);
        box.setMaxWidth(560);
        return box;
    }

    private Button key(String label, String action) {
        Button button = new Button(label);
        button.getStyleClass().add("key");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setFocusTraversable(false);

        boolean operator = "÷×−+^".contains(label);
        boolean control = action.startsWith("<");
        if (operator) button.getStyleClass().add("key-operator");
        if (action.equals("<back>")) {
            button.setText("");
            button.setGraphic(barnesinside.mathutils.theme.Brand.icon("delete", 18));
        }
        if (action.equals("<eval>")) button.getStyleClass().add("key-accent");

        button.setOnAction(e -> {
            switch (action) {
                case "<back>" -> backspace();
                case "<clear>" -> {
                    input.clear();
                    resultBox.getChildren().clear();
                }
                case "<eval>" -> evaluate();
                default -> input.insertText(input.getCaretPosition(), action);
            }
            input.requestFocus();
            input.positionCaret(input.getText().length() == 0 ? 0 : input.getCaretPosition());
        });
        if (control) Ui.pressEffect(button);
        return button;
    }

    private void backspace() {
        if (!input.getSelectedText().isEmpty()) {
            input.replaceSelection("");
        } else {
            input.deletePreviousChar();
        }
    }

    // ================================================================ Panel lateral

    private Node historySection() {
        VBox section = new VBox(12, Ui.caption(t.bind("calc.history")), historyBox);
        return section;
    }

    private Node variablesSection() {
        VBox section = new VBox(12, Ui.caption(t.bind("calc.variables")), variablesBox);
        return section;
    }

    private void refreshSide() {
        historyBox.getChildren().clear();
        List<CalculatorSession.Result> history = session.history();
        if (history.isEmpty()) {
            Label empty = Ui.label(t.bind("calc.history.empty"), "text-muted", "body-sm");
            empty.setWrapText(true);
            historyBox.getChildren().add(empty);
        }
        for (int i = history.size() - 1, shown = 0; i >= 0 && shown < HISTORY_LIMIT; i--, shown++) {
            historyBox.getChildren().add(historyItem(history.get(i)));
        }

        variablesBox.getChildren().clear();
        Map<String, MathUtils.cas.expr.Expr> variables = session.variables();
        Map<String, UserFunction> functions = session.functions();
        if (variables.isEmpty() && functions.isEmpty()) {
            Label empty = Ui.label(t.bind("calc.variables.empty"), "text-muted", "body-sm");
            empty.setWrapText(true);
            variablesBox.getChildren().add(empty);
        }
        variables.forEach((name, value) -> variablesBox.getChildren().add(Ui.label(name + " = " + value, "mono", "body-sm")));
        functions.forEach((name, f) -> variablesBox.getChildren().add(Ui.label(
                name + "(" + String.join(", ", f.parameters()) + ") = " + f.body(), "mono", "body-sm")));
    }

    private Node historyItem(CalculatorSession.Result r) {
        Label expression = Ui.label(r.input(), "mono", "body-sm", "text-muted");
        expression.setWrapText(true);
        String outcome = r.isError() ? r.message() : (r.decimalText() != null ? r.decimalText() : r.exactText());
        Label value = Ui.label(r.isError() ? t.get("calc.status.error") + ": " + outcome : "= " + outcome, "mono");
        if (r.isError()) value.getStyleClass().add("message-error");
        value.setWrapText(true);

        VBox item = new VBox(2, expression, value);
        item.getStyleClass().addAll("card", "card-clickable");
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setOnMouseClicked(e -> {
            input.setText(r.input());
            input.requestFocus();
            input.positionCaret(r.input().length());
        });
        return item;
    }
}
