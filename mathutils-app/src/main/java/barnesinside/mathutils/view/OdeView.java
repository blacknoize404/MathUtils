package barnesinside.mathutils.view;

import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.exceptions.ExpressionParseException;
import MathUtils.ode.DormandPrince;
import MathUtils.ode.Euler;
import MathUtils.ode.FixedStepSolver;
import MathUtils.ode.Heun;
import MathUtils.ode.Midpoint;
import MathUtils.ode.OdeFunction;
import MathUtils.ode.OdeSolution;
import MathUtils.ode.RK4;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.math.ChartView;
import barnesinside.mathutils.ui.Numbers;
import barnesinside.mathutils.ui.Segmented;
import barnesinside.mathutils.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ecuaciones diferenciales ordinarias con valor inicial: una ecuación {@code y' = f(t, y)} o un
 * sistema de dos, {@code y' = f(t, y, z)} y {@code z' = g(t, y, z)}. Se resuelven con Euler, Heun,
 * punto medio, Runge-Kutta de orden 4 o Dormand-Prince con paso adaptativo, y se grafican.
 */
public final class OdeView extends ToolView {

    /** Método de integración. */
    public enum Method {
        EULER("euler"), HEUN("heun"), MIDPOINT("midpoint"), RK4("rk4"), DORMAND_PRINCE("dopri");

        private final String key;

        Method(String key) {
            this.key = key;
        }
    }

    private static final Set<String> ALLOWED = Set.of("t", "y", "z");

    private final TextField fy = new TextField("-2*y + sin(t)");
    private final TextField fz = new TextField();
    private final TextField t0 = new TextField("0");
    private final TextField y0 = new TextField("1");
    private final TextField z0 = new TextField("0");
    private final TextField tEnd = new TextField("6");
    private final TextField steps = new TextField("200");
    private final Segmented<Method> method = new Segmented<>(List.of(Method.values()), Method.RK4,
            m -> t.get("ode.method." + m.key));

    public OdeView(Workspace workspace) {
        super(workspace);
        for (TextField f : List.of(fy, fz)) {
            f.getStyleClass().addAll("mono", "expression-field");
            f.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) solve();
            });
        }
        fy.promptTextProperty().bind(t.bind("ode.fy.placeholder"));
        fz.promptTextProperty().bind(t.bind("ode.fz.placeholder"));
        for (TextField f : List.of(t0, y0, z0, tEnd, steps)) {
            f.getStyleClass().add("mono");
            f.setPrefWidth(100);
        }
    }

    @Override
    public String id() {
        return "ode";
    }

    @Override
    public String icon() {
        return "workflow";
    }

    @Override
    protected String keyPrefix() {
        return "ode";
    }

    private final barnesinside.mathutils.math.LivePreview preview = new barnesinside.mathutils.math.LivePreview();

    @Override
    protected Node previewNode() {
        return preview.follow(List.of(fy, fz), () -> {
            boolean hasY = fy.getText() != null && !fy.getText().isBlank();
            boolean hasZ = fz.getText() != null && !fz.getText().isBlank();
            if (!hasY && !hasZ) return null;
            List<barnesinside.mathutils.math.MathLayout.Box> items = new java.util.ArrayList<>();
            if (hasY) {
                items.add(barnesinside.mathutils.math.MathLayout.text("y' = ", 22));
                items.add(barnesinside.mathutils.math.MathLayout.of(ExprParser.parse(fy.getText()), 22));
            }
            if (hasY && hasZ) items.add(barnesinside.mathutils.math.MathLayout.text("        ", 22));
            if (hasZ) {
                items.add(barnesinside.mathutils.math.MathLayout.text("z' = ", 22));
                items.add(barnesinside.mathutils.math.MathLayout.of(ExprParser.parse(fz.getText()), 22));
            }
            return barnesinside.mathutils.math.MathLayout.row(items);
        });
    }

    @Override
    protected List<javafx.scene.control.TextInputControl> inputs() {
        return List.of(fy, fz, t0, y0, z0, tEnd, steps);
    }

    @Override
    protected Node form() {
        VBox equationY = new VBox(4, Ui.caption("y' ="), fy);
        VBox equationZ = new VBox(4, Ui.caption("z' ="), fz);
        HBox.setHgrow(equationY, Priority.ALWAYS);
        HBox.setHgrow(equationZ, Priority.ALWAYS);

        HBox conditions = new HBox(16, field("ode.t0", t0), new VBox(4, Ui.caption("y(t0)"), y0),
                new VBox(4, Ui.caption("z(t0)"), z0), field("ode.tEnd", tEnd), field("ode.steps", steps));
        return new VBox(16, new HBox(16, equationY, equationZ), conditions, field("ode.method", method),
                Ui.primary(t.bind("ode.solve"), "play", this::solve));
    }

    @Override
    protected List<Example> examples() {
        return List.of(
                example("decay", "-2*y", "", "0", "1", "0", "3"),
                example("forced", "-2*y + sin(t)", "", "0", "1", "0", "10"),
                example("logistic", "y*(1 - y/5)", "", "0", "0.5", "0", "8"),
                example("oscillator", "z", "-y", "0", "1", "0", "12.5"),
                example("damped", "z", "-y - 0.3*z", "0", "1", "0", "20"));
    }

    private Example example(String key, String f, String g, String start, String y, String z, String end) {
        return new Example(t.get("ode.example." + key), () -> {
            fy.setText(f);
            fz.setText(g);
            t0.setText(start);
            y0.setText(y);
            z0.setText(z);
            tEnd.setText(end);
            solve();
        });
    }

    /** Rellena el problema desde fuera y resuelve (para pruebas). */
    public void solve(String f, String g, String start, String y, String z, String end, Method m) {
        fy.setText(f);
        fz.setText(g);
        t0.setText(start);
        y0.setText(y);
        z0.setText(z);
        tEnd.setText(end);
        method.setValue(m);
        solve();
    }

    // ---------------------------------------------------------------- Cálculo

    private void solve() {
        try {
            Expr dy = expression(fy.getText(), "y'");
            boolean system = fz.getText() != null && !fz.getText().isBlank();
            Expr dz = system ? expression(fz.getText(), "z'") : null;

            double start = number(t0.getText(), "t0");
            double end = number(tEnd.getText(), t.get("ode.tEnd"));
            if (start == end) throw new IllegalArgumentException(t.get("ode.error.interval"));
            double[] initial = system
                    ? new double[]{number(y0.getText(), "y(t0)"), number(z0.getText(), "z(t0)")}
                    : new double[]{number(y0.getText(), "y(t0)")};

            OdeFunction f = (time, state) -> {
                double zValue = state.length > 1 ? state[1] : 0;
                Map<String, Double> env = Map.of("t", time, "y", state[0], "z", zValue);
                return system
                        ? new double[]{Eval.value(dy, env), Eval.value(dz, env)}
                        : new double[]{Eval.value(dy, env)};
            };

            OdeSolution solution = integrate(f, start, initial, end);
            double[] last = solution.finalState();
            for (double v : last) {
                if (!Double.isFinite(v)) throw new IllegalArgumentException(t.get("ode.error.diverges"));
            }
            show(result(solution, system));
        } catch (IllegalArgumentException | ArithmeticException | ExpressionParseException e) {
            fail(e.getMessage());
        }
    }

    private OdeSolution integrate(OdeFunction f, double start, double[] initial, double end) {
        Method m = method.getValue();
        if (m == Method.DORMAND_PRINCE) return new DormandPrince(1e-8).solve(f, start, initial, end);
        int n = (int) number(steps.getText(), t.get("ode.steps"));
        if (n < 1 || n > 200_000) throw new IllegalArgumentException(t.get("ode.error.steps"));
        FixedStepSolver solver = switch (m) {
            case EULER -> new Euler();
            case HEUN -> new Heun();
            case MIDPOINT -> new Midpoint();
            default -> new RK4();
        };
        return solver.solve(f, start, initial, end, n);
    }

    private Node result(OdeSolution solution, boolean system) {
        ChartView chart = new ChartView(340)
                .add(new ChartView.Series("y(t)", solution.t(), solution.component(0), ChartView.Kind.LINE, 0));
        if (system) chart.add(new ChartView.Series("z(t)", solution.t(), solution.component(1), ChartView.Kind.LINE, 1));

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{t.get("ode.method"), t.get("ode.method." + method.getValue().key)});
        rows.add(new String[]{t.get("ode.points"), String.valueOf(solution.t().length)});
        double[] last = solution.finalState();
        String time = Numbers.decimal(solution.finalTime());
        rows.add(new String[]{"y(" + time + ")", Numbers.decimal(last[0])});
        if (system) rows.add(new String[]{"z(" + time + ")", Numbers.decimal(last[1])});

        GridPane grid = new GridPane();
        grid.setHgap(32);
        grid.setVgap(8);
        for (int i = 0; i < rows.size(); i++) {
            grid.add(Ui.label(rows.get(i)[0], "text-muted"), 0, i);
            grid.add(Ui.label(rows.get(i)[1], "mono"), 1, i);
        }
        return card(t.get("ode.solution"), new VBox(20, grid, chart));
    }

    // ---------------------------------------------------------------- Lectura

    private Expr expression(String text, String name) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException(t.get("ode.error.empty").replace("{0}", name));
        Expr e = ExprParser.parse(text);
        for (String variable : e.variables()) {
            if (!ALLOWED.contains(variable)) {
                throw new IllegalArgumentException(t.get("ode.error.variable").replace("{0}", variable));
            }
        }
        return e;
    }

    private double number(String text, String name) {
        try {
            double value = Eval.value(ExprParser.parse(text.strip().replace(',', '.')));
            if (!Double.isFinite(value)) throw new IllegalArgumentException();
            return value;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(t.get("stats.error.number").replace("{0}", String.valueOf(text)).replace("{1}", name));
        }
    }
}
