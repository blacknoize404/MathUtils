package barnesinside.mathutils.view;

import MathUtils.cas.order.OrderOfOperations;
import MathUtils.cas.syntax.SyntaxPrinter;
import MathUtils.cas.tree.TreeModel;
import MathUtils.cas.tree.TreeNode;
import MathUtils.exceptions.ExpressionParseException;
import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.ui.SymbolPalette;
import barnesinside.mathutils.ui.Ui;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Orden de operaciones paso a paso: cada paso resuelve una sola operación, resaltada en la
 * expresión y en el árbol, con la regla de jerarquía que explica por qué va en ese orden.
 */
public final class OrderView implements Module {

    private static final double PLAY_INTERVAL_MILLIS = 1100;

    private final Workspace workspace;
    private final I18n t = AppContext.i18n();

    private final TextField input = new TextField();
    private final SymbolPalette symbols = new SymbolPalette(false);
    private final Label error = new Label();
    private final HBox expressionRow = new HBox(0);
    private final VBox stepsBox = new VBox(0);
    private final VBox notes = new VBox(8);
    private final Label progress = new Label();
    private final TreeCanvas canvas = new TreeCanvas();
    private final StackPane stage = new StackPane();
    private final Label emptyHint = new Label();
    private final HBox controls = new HBox(8);

    private final Timeline player = new Timeline();
    private Button playButton;
    private boolean playing;

    private OrderOfOperations.Reduction reduction;
    private TreeModel model;
    private int position;
    private Node content;
    private String lastShared = "";

    public OrderView(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public String id() {
        return "order";
    }

    @Override
    public StringBinding title() {
        return t.bind("nav.order");
    }

    @Override
    public String icon() {
        return "list-ordered";
    }

    @Override
    public void onShow() {
        String shared = workspace.expressionProperty().get();
        if (shared != null && !shared.isBlank() && !shared.equals(lastShared)) {
            lastShared = shared;
            input.setText(shared);
            solve();
        }
        input.requestFocus();
    }

    @Override
    public Node content() {
        if (content == null) content = build();
        return content;
    }

    /** Resuelve una expresión desde fuera (para pruebas). */
    public void solve(String expression) {
        input.setText(expression);
        solve();
    }

    // ================================================================ Construcción


    /** La vista previa de lo que se escribe; mientras no sea una expresión válida no muestra nada (los errores ya se avisan aparte). */
    private static barnesinside.mathutils.math.LivePreview livePreview(javafx.scene.control.TextField field) {
        return new barnesinside.mathutils.math.LivePreview().follow(java.util.List.of(field), () -> {
            String text = field.getText();
            if (text == null || text.isBlank()) return null;
            return MathUtils.cas.expr.ExprParser.tryParse(text)
                    .map(e -> barnesinside.mathutils.math.MathLayout.of(e, 22)).orElse(null);
        });
    }

    private Node build() {
        input.getStyleClass().add("expression-field");
        input.promptTextProperty().bind(t.bind("order.placeholder"));
        HBox.setHgrow(input, Priority.ALWAYS);
        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) solve();
        });

        symbols.attach(input);
        HBox inputRow = new HBox(12, input, Ui.primary(t.bind("order.solve"), "play", this::solve));
        inputRow.setAlignment(Pos.CENTER_LEFT);

        error.getStyleClass().add("message-error");
        error.setWrapText(true);
        error.setManaged(false);
        error.setVisible(false);

        playButton = Ui.secondary(t.bind("order.play"), "play", this::togglePlay);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getChildren().addAll(
                Ui.iconButton("rotate-ccw", t.bind("order.restart"), () -> go(0)),
                Ui.iconButton("skip-back", t.bind("order.prev"), () -> go(position - 1)),
                Ui.iconButton("skip-forward", t.bind("order.next"), () -> go(position + 1)),
                playButton,
                Ui.spacer(),
                progress);
        progress.getStyleClass().addAll("mono", "text-muted");
        controls.setVisible(false);
        controls.setManaged(false);

        expressionRow.setAlignment(Pos.CENTER_LEFT);
        expressionRow.setPadding(new Insets(16, 20, 16, 20));
        expressionRow.getStyleClass().add("panel");
        expressionRow.setVisible(false);
        expressionRow.setManaged(false);

        VBox main = new VBox(18, Ui.pageHeader(t.bind("order.title"), t.bind("order.subtitle"), symbols.trigger()), inputRow, livePreview(input), error,
                controls, expressionRow, stepsBox, notes, hierarchy());
        main.setPadding(new Insets(32));
        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(560);
        scroll.setMinWidth(480);
        scroll.setMaxWidth(640);

        emptyHint.textProperty().bind(t.bind("order.empty"));
        emptyHint.getStyleClass().add("text-muted");
        emptyHint.setWrapText(true);
        emptyHint.setMaxWidth(340);
        stage.getChildren().addAll(canvas, emptyHint);
        stage.getStyleClass().add("panel");
        StackPane.setAlignment(emptyHint, Pos.CENTER);

        VBox right = new VBox(stage);
        right.setPadding(new Insets(32, 32, 32, 8));
        VBox.setVgrow(stage, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        t.localeProperty().addListener((obs, old, now) -> {
            if (reduction != null) render();
        });
        return new HBox(scroll, right);
    }

    private Node hierarchy() {
        VBox box = new VBox(6, Ui.caption(t.bind("order.hierarchy")));
        for (int level = 1; level <= 4; level++) {
            HBox row = new HBox(10, Ui.label(String.valueOf(level), "badge", "mono"),
                    Ui.label(t.bind("order.hierarchy." + level), "body-sm"));
            row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }
        box.setPadding(new Insets(8, 0, 0, 0));
        return box;
    }

    // ================================================================ Lógica

    private void solve() {
        String text = input.getText();
        stopPlaying();

        if (text == null || text.isBlank()) {
            reduction = null;
            model = null;
            canvas.setModel(null);
            emptyHint.setVisible(true);
            hide();
            return;
        }

        try {
            reduction = OrderOfOperations.reduce(text);
            model = TreeModel.of(text);
        } catch (ExpressionParseException e) {
            error.setText(e.getMessage());
            error.setVisible(true);
            error.setManaged(true);
            return;
        }

        error.setVisible(false);
        error.setManaged(false);
        emptyHint.setVisible(false);
        canvas.setModel(model);
        canvas.setShowOrder(true);
        position = 0;
        render();
    }

    private void hide() {
        stepsBox.getChildren().clear();
        notes.getChildren().clear();
        expressionRow.setVisible(false);
        expressionRow.setManaged(false);
        controls.setVisible(false);
        controls.setManaged(false);
    }

    private void go(int target) {
        if (reduction == null) return;
        position = Math.max(0, Math.min(reduction.steps().size(), target));
        render();
    }

    private void togglePlay() {
        if (playing) {
            stopPlaying();
            return;
        }
        if (reduction == null || reduction.steps().isEmpty()) return;
        if (position >= reduction.steps().size()) position = 0;

        playing = true;
        updatePlayButton();
        player.getKeyFrames().setAll(new KeyFrame(Duration.millis(PLAY_INTERVAL_MILLIS), e -> {
            if (position >= reduction.steps().size()) {
                stopPlaying();
            } else {
                go(position + 1);
                if (position >= reduction.steps().size()) stopPlaying();
            }
        }));
        player.setCycleCount(Timeline.INDEFINITE);
        player.playFromStart();
        render();
    }

    private void stopPlaying() {
        player.stop();
        playing = false;
        if (playButton != null) updatePlayButton();
    }

    private void updatePlayButton() {
        playButton.textProperty().unbind();
        playButton.textProperty().bind(t.bind(playing ? "order.pause" : "order.play"));
        playButton.setGraphic(Brand.icon(playing ? "pause" : "play", 16));
    }

    // ================================================================ Presentación

    private void render() {
        List<OrderOfOperations.OrderStep> steps = reduction.steps();
        int total = steps.size();

        controls.setVisible(total > 0);
        controls.setManaged(total > 0);
        progress.setText(t.get("order.step", Math.min(position + 1, total), total));
        if (position >= total && total > 0) progress.setText(t.get("order.step", total, total));

        renderExpression();
        renderSteps();
        renderNotes();
        renderTree();
    }

    private void renderExpression() {
        expressionRow.getChildren().clear();
        expressionRow.setVisible(true);
        expressionRow.setManaged(true);

        List<SyntaxPrinter.Piece> pieces = position < reduction.steps().size()
                ? reduction.steps().get(position).beforePieces()
                : SyntaxPrinter.pieces(reduction.result(), null);
        if (reduction.steps().isEmpty()) pieces = SyntaxPrinter.pieces(reduction.original(), null);

        for (SyntaxPrinter.Piece piece : pieces) {
            Label label = new Label(piece.text());
            label.getStyleClass().add("mono");
            label.setStyle("-fx-font-size: 22px;");
            if (piece.highlighted()) {
                label.getStyleClass().add("highlight");
                label.setPadding(new Insets(2, 6, 2, 6));
            }
            expressionRow.getChildren().add(label);
        }
    }

    private void renderSteps() {
        stepsBox.getChildren().clear();
        List<OrderOfOperations.OrderStep> steps = reduction.steps();

        for (int i = 0; i < steps.size(); i++) {
            OrderOfOperations.OrderStep step = steps.get(i);
            boolean done = i < position;
            boolean current = i == position;

            VBox card = new VBox(6);
            card.getStyleClass().add("step");
            if (current) card.getStyleClass().add("current");
            if (!done && !current) card.setOpacity(0.4);

            HBox header = new HBox(10, Ui.label(String.valueOf(step.number()), "step-number"),
                    Ui.label(step.title(t.locale()), "step-title"));
            header.setAlignment(Pos.CENTER_LEFT);

            Label explanation = Ui.label(step.explanation(t.locale()), "body-sm");
            explanation.setWrapText(true);

            HBox equation = new HBox(8);
            equation.setAlignment(Pos.CENTER_LEFT);
            appendPieces(equation, step.beforePieces(), done);
            equation.getChildren().add(Brand.icon("arrow-right", 16));
            appendPieces(equation, step.afterPieces(), done);

            card.getChildren().addAll(header, explanation, equation);
            int target = i;
            card.setOnMouseClicked(e -> go(target));
            stepsBox.getChildren().add(card);
        }
    }

    private void appendPieces(HBox row, List<SyntaxPrinter.Piece> pieces, boolean done) {
        HBox text = new HBox(0);
        text.setAlignment(Pos.CENTER_LEFT);
        for (SyntaxPrinter.Piece piece : pieces) {
            Label label = new Label(piece.text());
            label.getStyleClass().add("mono");
            label.setStyle("-fx-font-size: 14px;");
            if (piece.highlighted()) {
                label.getStyleClass().add("highlight");
                label.setPadding(new Insets(1, 4, 1, 4));
            }
            text.getChildren().add(label);
        }
        row.getChildren().add(text);
    }

    private void renderNotes() {
        notes.getChildren().clear();
        if (reduction.problem() != null) {
            Label problem = new Label(t.get("order.stopped", reduction.problemMessage(t.locale())));
            problem.setWrapText(true);
            problem.getStyleClass().add("message-error");
            VBox box = new VBox(problem);
            box.getStyleClass().add("message-error-box");
            notes.getChildren().add(box);
            return;
        }
        if (reduction.steps().isEmpty()) {
            notes.getChildren().add(wrapped(Ui.label(t.get("order.nothing"), "text-muted")));
        } else if (!reduction.isFullyEvaluated() && position >= reduction.steps().size()) {
            notes.getChildren().add(wrapped(Ui.label(t.get("order.partial"), "text-muted")));
        }
    }

    private static Label wrapped(Label label) {
        label.setWrapText(true);
        return label;
    }

    private void renderTree() {
        Set<Integer> dimmed = new HashSet<>();
        List<OrderOfOperations.OrderStep> steps = reduction.steps();
        for (int i = 0; i < position && i < steps.size(); i++) {
            model.find(steps.get(i).nodeId()).ifPresent(node -> collect(node, dimmed));
        }
        Integer current = position < steps.size() ? steps.get(position).nodeId() : null;
        canvas.setStepState(dimmed, current);
    }

    private static void collect(TreeNode node, Set<Integer> out) {
        out.add(node.id());
        for (TreeNode child : node.children()) collect(child, out);
    }
}
