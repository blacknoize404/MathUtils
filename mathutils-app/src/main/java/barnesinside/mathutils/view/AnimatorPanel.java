package barnesinside.mathutils.view;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.tree.TreeModel;
import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.math.MathView;
import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.ui.Segmented;
import barnesinside.mathutils.ui.Ui;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * Animador de una solución: recorre los pasos uno a uno y dibuja el árbol de la expresión antes y
 * después de cada transformación, con la explicación del paso y los mandos de reproducción.
 * Comparte el {@link StepSession} con la lista de pasos, así que ambos se mueven a la vez.
 */
final class AnimatorPanel extends VBox {

    private static final double BEFORE_MILLIS = 1000;
    private static final double AFTER_MILLIS = 1100;

    private enum Side {BEFORE, AFTER}

    private final I18n t = AppContext.i18n();
    private final StepSession session;

    private final Label counter = Ui.label("", "mono", "text-muted");
    private final Label title = Ui.label("", "step-title");
    private final VBox explanation = new VBox();
    private final MathView equation = MathView.empty(17);
    private final TreeCanvas canvas = new TreeCanvas();
    private final Label empty = Ui.label("", "text-muted");
    private final Label treeNote = Ui.label("", "text-muted");
    private final Segmented<Side> side = new Segmented<>(List.of(Side.BEFORE, Side.AFTER), Side.AFTER,
            s -> AppContext.i18n().get("animator." + s.name().toLowerCase()));
    private final Timeline player = new Timeline();
    private final Button play;
    private final VBox content = new VBox(14);
    private boolean playing;
    private TreeModel shown;

    AnimatorPanel(StepSession session) {
        super(14);
        this.session = session;
        getStyleClass().add("animator");
        setPadding(new Insets(20, 20, 20, 16));

        empty.textProperty().bind(t.bind("animator.empty"));
        empty.setWrapText(true);
        treeNote.setWrapText(true);
        treeNote.textProperty().bind(t.bind("animator.noTree"));
        treeNote.setManaged(false);
        treeNote.setVisible(false);
        title.setWrapText(true);

        ScrollPane equationScroll = new ScrollPane(equation);
        equationScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        equationScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        equationScroll.setFitToHeight(true);
        equationScroll.setPannable(true);
        equationScroll.setMinHeight(Region.USE_PREF_SIZE);
        equationScroll.prefHeightProperty().bind(equation.heightProperty().add(14));
        equation.setInteractive(false);

        canvas.setShowOrder(false);
        canvas.setPrefHeight(300);
        canvas.setMinHeight(240);
        canvas.getStyleClass().add("panel");
        VBox.setVgrow(canvas, Priority.ALWAYS);

        play = Ui.secondary(t.bind("order.play"), "play", this::togglePlay);
        HBox controls = new HBox(8,
                Ui.iconButton("rotate-ccw", t.bind("order.restart"), () -> go(0)),
                Ui.iconButton("skip-back", t.bind("order.prev"), () -> go(session.positionProperty().get() - 1)),
                Ui.iconButton("skip-forward", t.bind("order.next"), () -> go(session.positionProperty().get() + 1)),
                play);
        controls.setAlignment(Pos.CENTER_LEFT);

        HBox head = new HBox(10, Ui.caption(t.bind("animator.title")), Ui.spacer(), counter);
        head.setAlignment(Pos.CENTER_LEFT);
        HBox treeHead = new HBox(10, Ui.caption(t.bind("animator.tree")), Ui.spacer(), side);
        treeHead.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(title, explanation, equationScroll, treeHead, canvas, treeNote);
        getChildren().addAll(head, controls, empty, content);
        VBox.setVgrow(content, Priority.ALWAYS);

        session.positionProperty().addListener((obs, old, now) -> update());
        session.solutionProperty().addListener((obs, old, now) -> {
            stop();
            shown = null;
            update();
        });
        side.valueProperty().addListener((obs, old, now) -> drawTree());
        t.localeProperty().addListener((obs, old, now) -> update());
        update();
    }

    // ---------------------------------------------------------------- Reproducción

    private void go(int index) {
        session.go(index);
    }

    private void togglePlay() {
        if (playing) {
            stop();
            return;
        }
        if (session.size() == 0) return;
        if (session.positionProperty().get() >= session.size() - 1) session.go(0);
        playing = true;
        updatePlayButton();
        player.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO, e -> side.setValue(Side.BEFORE)),
                new KeyFrame(Duration.millis(BEFORE_MILLIS), e -> side.setValue(Side.AFTER)),
                new KeyFrame(Duration.millis(BEFORE_MILLIS + AFTER_MILLIS), e -> advance()));
        player.setCycleCount(Timeline.INDEFINITE);
        player.playFromStart();
    }

    private void advance() {
        int next = session.positionProperty().get() + 1;
        if (next >= session.size()) {
            stop();
        } else {
            session.go(next);
        }
    }

    void stop() {
        player.stop();
        playing = false;
        updatePlayButton();
    }

    private void updatePlayButton() {
        play.textProperty().unbind();
        play.textProperty().bind(t.bind(playing ? "order.pause" : "order.play"));
        play.setGraphic(Brand.icon(playing ? "pause" : "play", 16));
    }

    // ---------------------------------------------------------------- Presentación

    private void update() {
        StepSession.Frame frame = session.current();
        boolean has = frame != null;
        content.setVisible(has);
        content.setManaged(has);
        empty.setVisible(!has);
        empty.setManaged(!has);
        if (!has) {
            counter.setText("");
            return;
        }

        counter.setText(t.get("order.step", frame.index() + 1, session.size()));
        title.setText(frame.number() + "  " + frame.step().title(t.locale()));
        explanation.getChildren().setAll(SolutionPanel.explanation(frame.step(), t.locale()));

        boolean both = frame.step().before() != null && frame.step().after() != null;
        equation.setVisible(both);
        equation.setManaged(both);
        if (both) equation.setBox(SolutionPanel.stepEquation(frame.step()));

        side.setDisable(frame.before() == null || frame.after() == null);
        if (!playing) side.setValue(frame.after() != null ? Side.AFTER : Side.BEFORE);
        drawTree();
    }

    /** Dibuja el árbol de la expresión del lado elegido; si el paso no tiene expresiones deja el anterior. */
    private void drawTree() {
        StepSession.Frame frame = session.current();
        if (frame == null) {
            canvas.setModel(null);
            return;
        }
        Expr expr = side.getValue() == Side.BEFORE ? frame.before() : frame.after();
        if (expr == null) expr = frame.after() != null ? frame.after() : frame.before();
        boolean drawable = expr != null;
        treeNote.setVisible(!drawable && shown == null);
        treeNote.setManaged(!drawable && shown == null);
        canvas.setVisible(drawable || shown != null);
        canvas.setManaged(drawable || shown != null);
        if (!drawable) return;
        try {
            shown = TreeModel.ofExpr(expr);
            canvas.setModel(shown);
        } catch (RuntimeException e) {
            treeNote.setVisible(true);
            treeNote.setManaged(true);
        }
    }

    /** El árbol que se está dibujando, o {@code null} (para pruebas). */
    TreeModel shownTree() {
        return shown;
    }

    /** Si el animador está reproduciendo (para pruebas). */
    boolean isPlaying() {
        return playing;
    }
}
