package barnesinside.mathutils.view;

import MathUtils.cas.expr.Eval;
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
import barnesinside.mathutils.math.PlotView;
import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.ui.Collapsible;
import barnesinside.mathutils.ui.Segmented;
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
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Presenta una {@link Solution} (derivada, límite o integral): enunciado con notación matemática,
 * resultado con botones para copiarlo y los pasos organizados en tres niveles de detalle.
 * <p>Los pasos forman un recorrido: arriba, el camino con los pasos principales; debajo, una tarjeta
 * por paso con su explicación y su ecuación, y dentro de cada tarjeta sus subpasos. Elegir un paso
 * lo selecciona en el {@link StepSession} compartido, así que el animador lo muestra al instante.
 * Se vuelve a dibujar al cambiar el idioma.
 */
public final class SolutionPanel extends VBox {

    /** Cuánto de la estructura de pasos se muestra abierto. */
    enum Level {
        /** Solo los títulos de los pasos principales. */
        SUMMARY(-1),
        /** Los pasos principales abiertos, con los subpasos como títulos. */
        MAIN(0),
        /** Todo abierto. */
        FULL(Integer.MAX_VALUE);

        private final int openDepth;

        Level(int openDepth) {
            this.openDepth = openDepth;
        }
    }

    private final I18n t = AppContext.i18n();
    private final StepSession session;
    private final List<StepPane> panes = new ArrayList<>();
    private final Segmented<Level> level = new Segmented<>(List.of(Level.values()), Level.MAIN,
            l -> AppContext.i18n().get("steps.level." + l.name().toLowerCase()));
    private Solution solution;
    private Consumer<Node> revealer = node -> { };

    public SolutionPanel(StepSession session) {
        super(20);
        this.session = session;
        t.localeProperty().addListener((obs, old, now) -> {
            if (solution != null) render();
        });
        level.valueProperty().addListener((obs, old, now) -> applyLevel(now));
        session.positionProperty().addListener((obs, old, now) -> highlight(now.intValue()));
    }

    /** Quita la solución mostrada. */
    public void clear() {
        solution = null;
        panes.clear();
        session.clear();
        getChildren().clear();
    }

    public Solution solution() {
        return solution;
    }

    public void show(Solution newSolution) {
        this.solution = newSolution;
        session.load(newSolution);
        render();
    }

    /** Quién desplaza la vista hasta un paso cuando se selecciona desde el animador. */
    void setRevealer(Consumer<Node> revealer) {
        this.revealer = revealer;
    }

    // ================================================================ Presentación

    private void render() {
        getChildren().clear();
        panes.clear();

        // Con resultado, la tarjeta ya muestra el problema completo; el enunciado solo hace falta si no lo hay
        if (!solution.isSolved()) {
            MathView statement = MathView.empty(22);
            statement.setBox(statement(solution));
            statement.setInteractive(false);
            getChildren().add(scrollable(statement));
        }

        if (solution.isSolved()) {
            getChildren().add(resultCard());
            addPlot();
            getChildren().add(stepsSection());
            highlight(session.positionProperty().get());
            return;
        }

        Expr candidate = solution.extras().get("candidate");
        String key = switch (solution.status()) {
            case NON_ELEMENTAL -> "solution.nonElemental";
            case DOES_NOT_EXIST -> isDefinite(solution) ? "solution.diverges" : "solution.doesNotExist";
            default -> candidate != null ? "solution.candidate" : "solution.unsolved";
        };
        Label message = new Label(candidate != null ? t.get(key, candidate) : t.get(key));
        message.setWrapText(true);
        boolean explanatory = solution.status() == Solution.Status.DOES_NOT_EXIST;
        message.getStyleClass().add(explanatory ? "step-title" : "message-error");
        VBox box = new VBox(message);
        box.getStyleClass().add(explanatory ? "card" : "message-error-box");
        getChildren().add(box);
        if (approximation() != null) getChildren().add(approximationLabel());
        addPlot();
        if ((explanatory || isDefinite(solution)) && !solution.steps().isEmpty()) {
            getChildren().add(stepsSection());
            highlight(session.positionProperty().get());
        }
    }

    private static boolean isDefinite(Solution s) {
        return s.kind() == Solution.Kind.INTEGRAL && s.extras().containsKey("lower") && s.extras().containsKey("upper");
    }

    /** Valor decimal calculado por la cuadratura numérica, o {@code null}. */
    private Double approximation() {
        if (solution.kind() != Solution.Kind.INTEGRAL || !(solution.extras().get("approximation") instanceof Expr.Num n)) return null;
        return n.value().doubleValue();
    }

    private Node approximationLabel() {
        Label label = new Label(t.get("solution.approximation") + " " + String.format(Locale.ROOT, "%.10g", approximation()));
        label.getStyleClass().addAll("mono", "text-muted");
        return label;
    }

    /** Gráfica del integrando con el área bajo la curva entre los límites (si es una integral definida). */
    private void addPlot() {
        if (solution.kind() != Solution.Kind.INTEGRAL || solution.problem().variables().size() > 1
                || extraVariables(solution).size() > 1) return;
        double lower = Double.NaN;
        double upper = Double.NaN;
        if (isDefinite(solution)) {
            lower = Eval.value(solution.extras().get("lower"));
            upper = Eval.value(solution.extras().get("upper"));
            if (lower > upper) {
                double swap = lower;
                lower = upper;
                upper = swap;
            }
        }
        PlotView plot = new PlotView(solution.problem(), solution.variable(), lower, upper);
        if (!plot.isValid()) return;
        VBox card = new VBox(new Collapsible(t.bind(isDefinite(solution) ? "solution.plot.area" : "solution.plot"), plot,
                isDefinite(solution)));
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

    // ================================================================ Pasos organizados

    private Node stepsSection() {
        VBox cards = new VBox(8);
        List<StepSession.Frame> frames = session.frames();
        // Se crean todas las tarjetas y luego se cuelgan de su tarjeta madre, que ya existe porque va antes
        List<StepPane> created = new ArrayList<>();
        for (StepSession.Frame frame : frames) {
            StepPane pane = new StepPane(frame);
            created.add(pane);
            if (frame.parent() < 0) cards.getChildren().add(pane);
            else created.get(frame.parent()).addChild(pane);
        }
        panes.addAll(created);

        HBox header = new HBox(12, Ui.caption(t.bind("solution.steps")), Ui.spacer(), level);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox section = new VBox(12, header, path(), cards);
        applyLevel(level.getValue());
        return section;
    }

    /** El camino: los pasos principales como una fila de botones numerados, para saltar a cada uno. */
    private Node path() {
        FlowPane flow = new FlowPane(6, 6);
        flow.setAlignment(Pos.CENTER_LEFT);
        List<StepSession.Frame> top = session.topLevel();
        for (int i = 0; i < top.size(); i++) {
            StepSession.Frame frame = top.get(i);
            Label chip = new Label(frame.number() + "  " + frame.step().title(t.locale()));
            HBox box = new HBox(chip);
            box.getStyleClass().add("chip");
            box.setOnMouseClicked(e -> {
                session.go(frame.index());
                StepPane pane = panes.get(frame.index());
                pane.setExpanded(true);
                revealer.accept(pane);
            });
            flow.getChildren().add(box);
            if (i + 1 < top.size()) {
                Label arrow = Ui.label("›", "text-muted");
                flow.getChildren().add(arrow);
            }
        }
        VBox box = new VBox(6, Ui.caption(t.bind("steps.path")), flow);
        box.getStyleClass().add("panel");
        box.setPadding(new Insets(12, 14, 14, 14));
        return box;
    }

    private void applyLevel(Level target) {
        for (StepPane pane : panes) pane.setExpanded(pane.frame.depth() <= target.openDepth);
    }

    /** Resalta el paso seleccionado, abre las tarjetas que lo contienen y lo muestra en pantalla. */
    private void highlight(int index) {
        if (panes.isEmpty()) return;
        for (StepPane pane : panes) pane.setCurrent(pane.frame.index() == index);
        if (index < 0 || index >= panes.size()) return;
        StepPane current = panes.get(index);
        int parent = current.frame.parent();
        while (parent >= 0) {
            panes.get(parent).setExpanded(true);
            parent = panes.get(parent).frame.parent();
        }
        revealer.accept(current);
    }

    /** Una tarjeta de paso: número, título, resultado resumido, explicación, ecuación y subpasos. */
    private final class StepPane extends VBox {
        final StepSession.Frame frame;
        private final VBox body = new VBox(10);
        private final VBox children = new VBox(8);
        private final Node chevronDown = Brand.icon("chevron-down", 16);
        private final Node chevronRight = Brand.icon("chevron-right", 16);
        private final HBox header;
        private final Label summary;
        private boolean expanded;

        StepPane(StepSession.Frame frame) {
            super(0);
            this.frame = frame;
            getStyleClass().add("step-card");
            if (frame.depth() > 0) getStyleClass().add("step-card-nested");

            Label number = Ui.label(frame.number(), "step-number");
            Label title = Ui.label(frame.step().title(t.locale()), "step-title");
            summary = Ui.label(summaryText(frame.step()), "mono", "text-muted");
            summary.setStyle("-fx-font-size: 12px;");
            header = new HBox(10, number, title, Ui.spacer(), summary, chevronDown, chevronRight);
            header.setAlignment(Pos.CENTER_LEFT);
            header.getStyleClass().add("toggle-chevron");
            header.setOnMouseClicked(e -> {
                setExpanded(!expanded);
                session.go(frame.index());
            });
            getChildren().add(header);

            body.getChildren().add(explanation(frame.step(), t.locale()));
            if (frame.step().before() != null && frame.step().after() != null) {
                MathView equation = MathView.empty(17);
                equation.setBox(stepEquation(frame.step()));
                equation.setInteractive(false);
                body.getChildren().add(scrollable(equation));
            }
            body.setPadding(new Insets(10, 0, 0, 26));
            getChildren().add(body);

            children.setPadding(new Insets(10, 0, 0, 18));
            children.getStyleClass().add("step-rail");
            if (!frame.step().substeps().isEmpty()) getChildren().add(children);
            setExpanded(false);
        }

        void addChild(StepPane child) {
            children.getChildren().add(child);
        }

        void setExpanded(boolean value) {
            expanded = value;
            body.setVisible(value);
            body.setManaged(value);
            boolean showChildren = value && !frame.step().substeps().isEmpty();
            children.setVisible(showChildren);
            children.setManaged(showChildren);
            chevronDown.setVisible(value);
            chevronDown.setManaged(value);
            chevronRight.setVisible(!value);
            chevronRight.setManaged(!value);
            summary.setVisible(!value);
            summary.setManaged(!value);
        }

        void setCurrent(boolean value) {
            if (value) {
                if (!getStyleClass().contains("current")) getStyleClass().add("current");
            } else {
                getStyleClass().remove("current");
            }
        }
    }

    /** El resultado del paso en una línea corta, para leer el recorrido sin abrir las tarjetas. */
    private static String summaryText(Step step) {
        if (step.after() == null) return "";
        String text = "= " + step.after();
        return text.length() > 46 ? text.substring(0, 45) + "…" : text;
    }

    // ================================================================ Ecuaciones según el tipo de problema

    private static int directionOf(Solution s) {
        return s.extras().get("direction") instanceof Expr.Num n ? n.value().signum() : 0;
    }

    /** Variables de un cálculo con varias variables ({@code var1}, {@code var2}…), en el orden de los extras. */
    private static List<Expr> extraVariables(Solution s) {
        List<Expr> variables = new ArrayList<>();
        for (int i = 1; s.extras().get("var" + i) != null; i++) variables.add(s.extras().get("var" + i));
        return variables;
    }

    private static List<MathLayout.IntegralDimension> dimensions(Solution s) {
        List<MathLayout.IntegralDimension> dimensions = new ArrayList<>();
        List<Expr> variables = extraVariables(s);
        for (int i = 0; i < variables.size(); i++) {
            dimensions.add(new MathLayout.IntegralDimension(variables.get(i), s.extras().get("lower" + (i + 1)),
                    s.extras().get("upper" + (i + 1))));
        }
        return dimensions;
    }

    private static Expr listExpr(List<Expr> items) {
        return Expr.sym("(" + String.join(", ", items.stream().map(Expr::toString).toList()) + ")");
    }

    private static Expr limitPoint(Solution s) {
        List<Expr> point = new ArrayList<>();
        for (int i = 1; s.extras().get("point" + i) != null; i++) point.add(s.extras().get("point" + i));
        return listExpr(point);
    }

    /** Si el resultado lleva constante de integración (indefinidas e iteradas con alguna variable indefinida). */
    private static boolean hasConstant(Solution s) {
        if (extraVariables(s).size() > 1) return dimensions(s).stream().anyMatch(d -> d.lower() == null);
        return !isDefinite(s);
    }

    private static MathLayout.Box equation(Solution s, Expr result, double size) {
        Expr variable = Expr.sym(s.variable());
        List<Expr> variables = extraVariables(s);
        return switch (s.kind()) {
            case DERIVATIVE -> variables.isEmpty()
                    ? MathLayout.derivativeEquation(variable, s.problem(), result, s.order(), size)
                    : MathLayout.partialEquation(variables, s.problem(), result, size);
            case LIMIT -> variables.size() > 1
                    ? MathLayout.limitEquation(listExpr(variables), limitPoint(s), 0, s.problem(), result, size)
                    : MathLayout.limitEquation(variable, s.extras().getOrDefault("a", Expr.ZERO), directionOf(s),
                    s.problem(), result, size);
            case INTEGRAL -> variables.size() > 1
                    ? MathLayout.multipleIntegralEquation(dimensions(s), s.problem(), result, hasConstant(s) && result != null, size)
                    : MathLayout.integralEquation(variable, s.problem(), s.extras().get("lower"),
                    s.extras().get("upper"), result, !isDefinite(s), size);
        };
    }

    private static MathLayout.Box statement(Solution s) {
        return equation(s, null, 22);
    }

    private static MathLayout.Box result(Solution s) {
        return equation(s, s.result(), 26);
    }

    /** La ecuación de un paso con la notación de su tipo (derivada, límite o integral). */
    static MathLayout.Box stepEquation(Step step) {
        Expr x = step.variable();
        double size = 17;
        if (x == null) {
            return MathLayout.row(List.of(MathLayout.of(step.before(), size), MathLayout.text(" = ", size),
                    MathLayout.of(step.after(), size)));
        }
        return switch (step.rule().category()) {
            case DERIVATIVE -> step.rule() == Rule.DIFF_PARTIAL
                    ? MathLayout.partialEquation(List.of(x), step.before(), step.after(), size)
                    : MathLayout.derivativeEquation(x, step.before(), step.after(), 1, size);
            case LIMIT -> MathLayout.limitEquation(x, step.params().getOrDefault("a", Expr.ZERO), 0,
                    step.before(), step.after(), size);
            case INTEGRAL -> MathLayout.integralEquation(x, step.before(), step.params().get("lower"),
                    step.params().get("upper"), step.after(), false, size);
            default -> MathLayout.row(List.of(MathLayout.of(step.before(), size), MathLayout.text(" = ", size),
                    MathLayout.of(step.after(), size)));
        };
    }

    /** La explicación de un paso con las expresiones intercaladas como fórmulas. */
    static TextFlow explanation(Step step, Locale locale) {
        TextFlow flow = new TextFlow();
        flow.setLineSpacing(4);
        for (Segment segment : step.explanation(locale)) {
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
