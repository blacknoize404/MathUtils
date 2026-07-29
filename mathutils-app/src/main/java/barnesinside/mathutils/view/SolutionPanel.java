package barnesinside.mathutils.view;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.steps.Format;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Segment;
import MathUtils.cas.steps.Solution;
import MathUtils.cas.steps.Step;
import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.math.MathLayout;
import barnesinside.mathutils.math.MathView;
import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.ui.Toasts;
import barnesinside.mathutils.ui.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * Presenta una {@link Solution} (derivada, límite o integral): enunciado con notación matemática,
 * resultado con botones para copiarlo y el árbol de pasos, que se puede plegar. Se vuelve a
 * dibujar al cambiar el idioma.
 */
public final class SolutionPanel extends VBox {

    private final I18n t = AppContext.i18n();
    private final List<StepPane> panes = new ArrayList<>();
    private Solution solution;

    public SolutionPanel() {
        super(20);
        t.localeProperty().addListener((obs, old, now) -> {
            if (solution != null) render();
        });
    }

    /** Quita la solución mostrada. */
    public void clear() {
        solution = null;
        panes.clear();
        getChildren().clear();
    }

    public Solution solution() {
        return solution;
    }

    public void show(Solution newSolution) {
        this.solution = newSolution;
        render();
    }

    // ================================================================ Presentación

    private void render() {
        getChildren().clear();
        panes.clear();

        MathView statement = MathView.empty(22);
        statement.setBox(statement(solution));
        statement.setInteractive(false);
        getChildren().add(scrollable(statement));

        if (solution.isSolved()) {
            getChildren().add(resultCard());
            addPlot();
            getChildren().add(stepsSection());
            return;
        }

        String key = switch (solution.status()) {
            case NON_ELEMENTAL -> "solution.nonElemental";
            case DOES_NOT_EXIST -> isDefinite(solution) ? "solution.diverges" : "solution.doesNotExist";
            default -> "solution.unsolved";
        };
        Label message = new Label(t.get(key));
        message.setWrapText(true);
        boolean explanatory = solution.status() == Solution.Status.DOES_NOT_EXIST;
        message.getStyleClass().add(explanatory ? "step-title" : "message-error");
        VBox box = new VBox(message);
        box.getStyleClass().add(explanatory ? "card" : "message-error-box");
        getChildren().add(box);
        if (approximation() != null) getChildren().add(approximationLabel());
        addPlot();
        if ((explanatory || isDefinite(solution)) && !solution.steps().isEmpty()) getChildren().add(stepsSection());
    }

    private static boolean isDefinite(Solution s) {
        return s.kind() == Solution.Kind.INTEGRAL && s.extras().containsKey("lower") && s.extras().containsKey("upper");
    }

    /** Valor decimal calculado por la cuadratura numérica, o {@code null}. */
    private Double approximation() {
        if (!isDefinite(solution) || !(solution.extras().get("approximation") instanceof Expr.Num n)) return null;
        return n.value().doubleValue();
    }

    private Node approximationLabel() {
        Label label = new Label(t.get("solution.approximation") + " " + String.format(java.util.Locale.ROOT, "%.10g", approximation()));
        label.getStyleClass().addAll("mono", "text-muted");
        return label;
    }

    /** Gráfica del integrando con el área bajo la curva entre los límites (si es una integral definida). */
    private void addPlot() {
        if (solution.kind() != Solution.Kind.INTEGRAL) return;
        double lower = Double.NaN;
        double upper = Double.NaN;
        if (isDefinite(solution)) {
            lower = MathUtils.cas.expr.Eval.value(solution.extras().get("lower"));
            upper = MathUtils.cas.expr.Eval.value(solution.extras().get("upper"));
            if (lower > upper) {
                double swap = lower;
                lower = upper;
                upper = swap;
            }
        }
        barnesinside.mathutils.math.PlotView plot = new barnesinside.mathutils.math.PlotView(
                solution.problem(), solution.variable(), lower, upper);
        if (!plot.isValid()) return;
        VBox card = new VBox(10, Ui.caption(t.bind(isDefinite(solution) ? "solution.plot.area" : "solution.plot")), plot);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        getChildren().add(card);
    }

    private Node resultCard() {
        MathView equation = MathView.empty(26);
        equation.setBox(result(solution));
        equation.setInteractive(false);

        FlowPane actions = new FlowPane(8, 8);
        actions.getChildren().addAll(
                Ui.ghost(t.bind("solution.copyResult"), "copy", () -> copy(solution.result().toString())),
                Ui.ghost(t.bind("solution.copyLatex"), "copy", () -> copy(solution.render(t.locale(), Format.LATEX))),
                Ui.ghost(t.bind("solution.copyMarkdown"), "copy", () -> copy(solution.render(t.locale(), Format.MARKDOWN))),
                Ui.ghost(t.bind("solution.copyJson"), "copy", () -> copy(solution.render(t.locale(), Format.JSON))));

        VBox card = new VBox(14, Ui.caption(t.bind("solution.result")), scrollable(equation), actions);
        if (approximation() != null) card.getChildren().add(1, approximationLabel());
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        return card;
    }

    private void copy(String text) {
        Ui.copy(text);
        Toasts.show(t.get("solution.copied"));
    }

    private Node stepsSection() {
        VBox steps = new VBox(0);
        List<Step> top = solution.steps();
        for (int i = 0; i < top.size(); i++) steps.getChildren().add(stepPane(top.get(i), String.valueOf(i + 1), 0));

        HBox header = new HBox(12, Ui.caption(t.bind("solution.steps")), Ui.spacer(),
                Ui.ghost(t.bind("solution.expandAll"), "chevron-down", () -> panes.forEach(p -> p.setExpanded(true))),
                Ui.ghost(t.bind("solution.collapseAll"), "chevron-right", () -> panes.forEach(p -> p.setExpanded(false))));
        header.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, header, steps);
    }

    private StepPane stepPane(Step step, String number, int depth) {
        StepPane pane = new StepPane(step, number, depth);
        panes.add(pane);
        return pane;
    }

    // ================================================================ Ecuaciones según el tipo de problema

    private static int directionOf(Solution s) {
        return s.extras().get("direction") instanceof Expr.Num n ? n.value().signum() : 0;
    }

    private static MathLayout.Box statement(Solution s) {
        Expr variable = Expr.sym(s.variable());
        return switch (s.kind()) {
            case DERIVATIVE -> MathLayout.derivativeEquation(variable, s.problem(), null, s.order(), 22);
            case LIMIT -> MathLayout.limitEquation(variable, s.extras().getOrDefault("a", Expr.ZERO), directionOf(s),
                    s.problem(), null, 22);
            case INTEGRAL -> MathLayout.integralEquation(variable, s.problem(), s.extras().get("lower"),
                    s.extras().get("upper"), null, false, 22);
        };
    }

    private static MathLayout.Box result(Solution s) {
        Expr variable = Expr.sym(s.variable());
        return switch (s.kind()) {
            case DERIVATIVE -> MathLayout.derivativeEquation(variable, s.problem(), s.result(), s.order(), 26);
            case LIMIT -> MathLayout.limitEquation(variable, s.extras().getOrDefault("a", Expr.ZERO), directionOf(s),
                    s.problem(), s.result(), 26);
            case INTEGRAL -> MathLayout.integralEquation(variable, s.problem(), s.extras().get("lower"),
                    s.extras().get("upper"), s.result(), !isDefinite(s), 26);
        };
    }

    private static MathLayout.Box stepEquation(Step step) {
        Expr x = step.variable();
        double size = 17;
        if (x == null) {
            return MathLayout.row(List.of(MathLayout.of(step.before(), size), MathLayout.text(" = ", size),
                    MathLayout.of(step.after(), size)));
        }
        return switch (step.rule().category()) {
            case DERIVATIVE -> MathLayout.derivativeEquation(x, step.before(), step.after(), 1, size);
            case LIMIT -> MathLayout.limitEquation(x, step.params().getOrDefault("a", Expr.ZERO), 0,
                    step.before(), step.after(), size);
            case INTEGRAL -> MathLayout.integralEquation(x, step.before(), step.after(), false, size);
            default -> MathLayout.row(List.of(MathLayout.of(step.before(), size), MathLayout.text(" = ", size),
                    MathLayout.of(step.after(), size)));
        };
    }

    // ================================================================ Pasos plegables

    /** Un paso con su explicación, su ecuación y sus subpasos, que se pliega al pulsar su título. */
    private final class StepPane extends VBox {
        private final VBox body = new VBox(10);
        private final VBox children = new VBox(0);
        private final Node chevronDown = Brand.icon("chevron-down", 16);
        private final Node chevronRight = Brand.icon("chevron-right", 16);

        StepPane(Step step, String number, int depth) {
            getStyleClass().add("step");

            HBox toggle = new HBox(8, chevronDown, chevronRight, Ui.label(number, "step-number"),
                    Ui.label(step.title(t.locale()), "step-title"));
            toggle.setAlignment(Pos.CENTER_LEFT);
            toggle.getStyleClass().add("toggle-chevron");
            toggle.setOnMouseClicked(e -> setExpanded(!body.isVisible()));
            getChildren().add(toggle);

            body.setPadding(new Insets(0, 0, 0, 24));
            body.getChildren().add(explanation(step));
            if (step.before() != null && step.after() != null) {
                MathView equation = MathView.empty(17);
                equation.setBox(stepEquation(step));
                equation.setInteractive(false);
                body.getChildren().add(scrollable(equation));
            }
            getChildren().add(body);

            children.setPadding(new Insets(0, 0, 0, 16));
            for (int i = 0; i < step.substeps().size(); i++) {
                children.getChildren().add(stepPane(step.substeps().get(i), number + "." + (i + 1), depth + 1));
            }
            if (!step.substeps().isEmpty()) getChildren().add(children);

            setExpanded(depth < 2);
        }

        void setExpanded(boolean value) {
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
                case Segment.Math math -> flow.getChildren().add(MathView.inline(math.expr(), 15));
            }
        }
        return flow;
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

    /** Referencia a las reglas, útil para que las vistas comprueben la categoría de un paso. */
    static boolean isDerivativeStep(Step step) {
        return step.rule().category() == Rule.Category.DERIVATIVE;
    }
}
