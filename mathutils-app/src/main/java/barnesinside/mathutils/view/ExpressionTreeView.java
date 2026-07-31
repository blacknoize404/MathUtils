package barnesinside.mathutils.view;

import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.tree.TreeModel;
import MathUtils.cas.tree.TreeNode;
import MathUtils.exceptions.ExpressionParseException;
import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.ui.Segmented;
import barnesinside.mathutils.ui.Toasts;
import barnesinside.mathutils.ui.SymbolPalette;
import barnesinside.mathutils.ui.Ui;
import javafx.beans.binding.StringBinding;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Árbol de una expresión: cómo se agrupa tal como se escribió (o en su forma canónica), con el
 * valor de cada subárbol, la precedencia de cada operador y el orden en que se resuelve.
 */
public final class ExpressionTreeView implements Module {

    /** Qué árbol se dibuja. */
    private enum Mode {SYNTAX, CANONICAL}

    private final Workspace workspace;
    private final I18n t = AppContext.i18n();

    private final TextField input = new TextField();
    private final SymbolPalette symbols = new SymbolPalette(false);
    private final TextField values = new TextField();
    private final Label error = new Label();
    private final Label hoverStatus = new Label();
    private final TreeCanvas canvas = new TreeCanvas();
    private final VBox details = new VBox(12);
    private final Label emptyHint = new Label();
    private final Segmented<Mode> mode = new Segmented<>(List.of(Mode.SYNTAX, Mode.CANONICAL), Mode.SYNTAX,
            m -> m == Mode.SYNTAX ? t.get("tree.mode.syntax") : t.get("tree.mode.canonical"));
    private Node content;
    private String lastShared = "";

    public ExpressionTreeView(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public String id() {
        return "tree";
    }

    @Override
    public StringBinding title() {
        return t.bind("nav.tree");
    }

    @Override
    public String icon() {
        return "network";
    }

    @Override
    public void onShow() {
        String shared = workspace.expressionProperty().get();
        if (shared != null && !shared.isBlank() && !shared.equals(lastShared)) {
            lastShared = shared;
            input.setText(shared);
        }
        input.requestFocus();
    }

    @Override
    public Node content() {
        if (content == null) content = build();
        return content;
    }

    /** Dibuja el árbol de una expresión (para pruebas y para enviarla desde otro módulo). */
    public void show(String expression) {
        input.setText(expression);
    }

    public TreeCanvas canvas() {
        return canvas;
    }

    // ================================================================ Construcción

    private Node build() {
        input.getStyleClass().add("expression-field");
        input.promptTextProperty().bind(t.bind("tree.placeholder"));
        HBox.setHgrow(input, Priority.ALWAYS);

        values.promptTextProperty().bind(t.bind("tree.values.placeholder"));
        values.setPrefWidth(200);

        error.getStyleClass().add("message-error");
        error.setWrapText(true);
        error.setManaged(false);
        error.setVisible(false);

        input.textProperty().addListener((obs, old, now) -> rebuild());
        values.textProperty().addListener((obs, old, now) -> rebuild());
        mode.valueProperty().addListener((obs, old, now) -> rebuild());
        t.localeProperty().addListener((obs, old, now) -> showDetails(canvas.selectedProperty().get()));

        canvas.selectedProperty().addListener((obs, old, now) -> showDetails(now));
        canvas.hoveredProperty().addListener((obs, old, now) -> hoverStatus.setText(now == null ? ""
                : now.text() + (now.value() == null ? "" : "  =  " + format(now.value()))));

        HBox inputRow = new HBox(12, input, Ui.secondary(t.bind("tree.fit"), "maximize", canvas::fit));
        inputRow.setAlignment(Pos.CENTER_LEFT);

        HBox valuesBox = new HBox(8, Ui.label(t.bind("tree.values"), "text-muted"), values);
        valuesBox.setAlignment(Pos.CENTER_LEFT);
        HBox zoom = new HBox(4, Ui.iconButton("zoom-out", t.bind("tree.fit"), canvas::zoomOut),
                Ui.iconButton("zoom-in", t.bind("tree.fit"), canvas::zoomIn));
        zoom.setAlignment(Pos.CENTER_LEFT);
        FlowPane options = new FlowPane(16, 8, mode, valuesBox, zoom,
                Ui.ghost(t.bind("tree.export"), "image", this::export));
        options.setAlignment(Pos.CENTER_LEFT);
        options.setRowValignment(javafx.geometry.VPos.CENTER);

        symbols.attach(input, values);
        VBox top = new VBox(16, Ui.pageHeader(t.bind("tree.title"), t.bind("tree.subtitle"), symbols.trigger()), inputRow, error, options);
        top.setPadding(new Insets(32, 32, 16, 32));

        emptyHint.textProperty().bind(t.bind("tree.empty"));
        emptyHint.getStyleClass().add("text-muted");
        emptyHint.setWrapText(true);
        emptyHint.setMaxWidth(380);

        hoverStatus.getStyleClass().addAll("mono", "body-sm");
        hoverStatus.setPadding(new Insets(0, 16, 12, 16));
        Label legend = Ui.label(t.bind("tree.legend"), "text-muted", "body-sm");
        legend.setPadding(new Insets(0, 16, 12, 16));

        StackPane stage = new StackPane(canvas, emptyHint);
        stage.getStyleClass().add("panel");
        StackPane.setAlignment(emptyHint, Pos.CENTER);
        VBox.setVgrow(stage, Priority.ALWAYS);
        stage.setMinHeight(280);

        VBox statusBar = new VBox(hoverStatus, legend);
        VBox left = new VBox(0, top, wrap(stage), statusBar);
        VBox.setVgrow(stage.getParent(), Priority.ALWAYS);
        HBox.setHgrow(left, Priority.ALWAYS);

        details.setPadding(new Insets(32, 24, 32, 24));
        details.setPrefWidth(320);
        details.setMinWidth(300);
        details.setMaxWidth(340);
        details.setStyle("-fx-border-color: transparent transparent transparent -bi-border; -fx-border-width: 0 0 0 1;");
        showDetails(null);

        rebuild();
        return new HBox(left, details);
    }

    private static Node wrap(StackPane stage) {
        VBox box = new VBox(stage);
        box.setPadding(new Insets(0, 32, 8, 32));
        VBox.setVgrow(stage, Priority.ALWAYS);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    // ================================================================ Lógica

    private void rebuild() {
        String text = input.getText();
        if (text == null || text.isBlank()) {
            canvas.setModel(null);
            emptyHint.setVisible(true);
            hide(error);
            return;
        }

        try {
            Map<String, Double> variables = parseValues(values.getText());
            TreeModel model = mode.getValue() == Mode.SYNTAX
                    ? TreeModel.of(text, variables)
                    : TreeModel.ofExpr(ExprParser.parse(text), variables);
            canvas.setModel(model);
            canvas.setShowOrder(mode.getValue() == Mode.SYNTAX);
            emptyHint.setVisible(false);
            hide(error);
        } catch (ExpressionParseException e) {
            error.setText(e.getMessage());
            error.setVisible(true);
            error.setManaged(true);
        }
    }

    private static void hide(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    /** Interpreta {@code x=2, y=3.5}; ignora los pares inválidos. */
    static Map<String, Double> parseValues(String text) {
        Map<String, Double> map = new HashMap<>();
        if (text == null) return map;
        for (String pair : text.split("[,;]")) {
            String[] parts = pair.split("=");
            if (parts.length != 2) continue;
            try {
                map.put(parts[0].trim(), Double.parseDouble(parts[1].trim().replace(',', '.')));
            } catch (NumberFormatException e) {
                // Se ignora el par
            }
        }
        return map;
    }

    private static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e12) return String.valueOf((long) value);
        return String.format(Locale.ROOT, "%.10g", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // ================================================================ Detalle del nodo

    private void showDetails(TreeNode node) {
        details.getChildren().clear();
        details.getChildren().add(Ui.caption(t.bind("tree.details")));

        if (node == null) {
            Label hint = Ui.label(t.bind("tree.details.empty"), "text-muted", "body-sm");
            hint.setWrapText(true);
            details.getChildren().add(hint);
            return;
        }

        Label label = Ui.label(node.label(), "mono");
        label.setStyle("-fx-font-size: 28px;");
        details.getChildren().add(label);

        details.getChildren().add(row("tree.field.kind", t.get("tree.kind." + node.kind().name())));
        details.getChildren().add(row("tree.field.expression", node.text()));
        details.getChildren().add(row("tree.field.value", node.value() == null ? t.get("tree.noValue") : format(node.value())));
        if (node.associativity() != null) {
            details.getChildren().add(row("tree.field.precedence", String.valueOf(node.precedence())));
            details.getChildren().add(row("tree.field.associativity", t.get("tree.assoc." + node.associativity().name())));
        } else if (node.kind() == TreeNode.Kind.NEGATION || node.kind() == TreeNode.Kind.FUNCTION) {
            details.getChildren().add(row("tree.field.precedence", String.valueOf(node.precedence())));
        }
        if (node.evaluationOrder() != null) {
            details.getChildren().add(row("tree.field.order", String.valueOf(node.evaluationOrder())));
        }
        if (node.parentheses() > 0) {
            details.getChildren().add(row("tree.field.parentheses", String.valueOf(node.parentheses())));
        }
    }

    private Node row(String captionKey, String value) {
        Label text = Ui.label(value, "mono", "body-sm");
        text.setWrapText(true);
        return new VBox(2, Ui.caption(t.get(captionKey)), text);
    }

    // ================================================================ Exportación

    private void export() {
        WritableImage image = canvas.exportImage();
        if (image == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("arbol.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        File file = chooser.showSaveDialog(canvas.getScene() == null ? null : canvas.getScene().getWindow());
        if (file == null) return;

        try {
            javax.imageio.ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            Toasts.show(t.get("tree.exported"));
        } catch (IOException e) {
            Toasts.show(e.getMessage());
        }
    }
}
