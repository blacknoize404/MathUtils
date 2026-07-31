package barnesinside.mathutils.view;

import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.ExprParser;
import MathUtils.statistics.Binomial;
import MathUtils.statistics.Descriptive;
import MathUtils.statistics.DiscreteDistribution;
import MathUtils.statistics.Distribution;
import MathUtils.statistics.Exponential;
import MathUtils.statistics.Normal;
import MathUtils.statistics.Poisson;
import MathUtils.statistics.Uniform;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.math.ChartView;
import barnesinside.mathutils.ui.Numbers;
import barnesinside.mathutils.ui.Segmented;
import barnesinside.mathutils.ui.Ui;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Estadística: resumen de un conjunto de datos (con histograma o diagrama de dispersión si hay dos
 * series) y cálculo con las distribuciones normal, binomial, de Poisson, exponencial y uniforme.
 */
public final class StatisticsView extends ToolView {

    private enum Mode {DATA, DISTRIBUTION}

    /** Distribución disponible. */
    public enum Kind {
        NORMAL("normal", "mu", "sigma"), BINOMIAL("binomial", "n", "p"), POISSON("poisson", "lambda", null),
        EXPONENTIAL("exponential", "lambda", null), UNIFORM("uniform", "a", "b");

        private final String key;
        private final String first;
        private final String second;

        Kind(String key, String first, String second) {
            this.key = key;
            this.first = first;
            this.second = second;
        }
    }

    private final Segmented<Mode> mode = new Segmented<>(List.of(Mode.DATA, Mode.DISTRIBUTION), Mode.DATA,
            m -> t.get("stats.mode." + m.name().toLowerCase()));
    private final Segmented<Boolean> sample = new Segmented<>(List.of(true, false), true,
            s -> t.get(s ? "stats.sample" : "stats.population"));
    private final Segmented<Kind> kind = new Segmented<>(List.of(Kind.values()), Kind.NORMAL,
            k -> t.get("stats.dist." + k.key));

    private final TextArea dataX = new TextArea();
    private final TextArea dataY = new TextArea();
    private final TextField first = new TextField("0");
    private final TextField second = new TextField("1");
    private final TextField point = new TextField("1");
    private final TextField probability = new TextField("0.95");
    private final Label firstCaption = Ui.label("", "caption");
    private final Label secondCaption = Ui.label("", "caption");
    private final VBox secondBox = new VBox(4, secondCaption, second);

    public StatisticsView(Workspace workspace) {
        super(workspace);
        for (TextArea area : List.of(dataX, dataY)) {
            area.setPrefRowCount(4);
            area.getStyleClass().add("mono");
        }
        dataX.promptTextProperty().bind(t.bind("stats.data.placeholder"));
        dataY.promptTextProperty().bind(t.bind("stats.datay.placeholder"));
        for (TextField f : List.of(first, second, point, probability)) {
            f.getStyleClass().add("mono");
            f.setPrefWidth(110);
        }
        kind.valueProperty().addListener((obs, old, now) -> updateParameters(now));
        updateParameters(Kind.NORMAL);
    }

    @Override
    public String id() {
        return "statistics";
    }

    @Override
    public String icon() {
        return "percent";
    }

    @Override
    protected String keyPrefix() {
        return "stats";
    }

    private void updateParameters(Kind k) {
        firstCaption.setText(t.get("stats.param." + k.first).toUpperCase());
        boolean two = k.second != null;
        secondBox.setVisible(two);
        secondBox.setManaged(two);
        if (two) secondCaption.setText(t.get("stats.param." + k.second).toUpperCase());
        switch (k) {
            case NORMAL -> setParameters("0", "1", "1");
            case BINOMIAL -> setParameters("10", "0.5", "5");
            case POISSON -> setParameters("4", "", "3");
            case EXPONENTIAL -> setParameters("1.5", "", "1");
            case UNIFORM -> setParameters("0", "4", "1");
        }
    }

    private void setParameters(String a, String b, String x) {
        first.setText(a);
        second.setText(b);
        point.setText(x);
    }

    @Override
    protected List<javafx.scene.control.TextInputControl> inputs() {
        return List.of(dataX, dataY, first, second, point, probability);
    }

    @Override
    protected Node form() {
        VBox dataForm = new VBox(14,
                new HBox(16, grow(field("stats.data.x", dataX)), grow(field("stats.data.y", dataY))),
                new HBox(16, field("stats.variance", sample)));
        VBox distributionForm = new VBox(14, field("stats.distribution", kind),
                new HBox(16, new VBox(4, firstCaption, first), secondBox, field("stats.point", point),
                        field("stats.probability", probability)));
        distributionForm.setVisible(false);
        distributionForm.setManaged(false);

        mode.valueProperty().addListener((obs, old, now) -> {
            dataForm.setVisible(now == Mode.DATA);
            dataForm.setManaged(now == Mode.DATA);
            distributionForm.setVisible(now == Mode.DISTRIBUTION);
            distributionForm.setManaged(now == Mode.DISTRIBUTION);
        });
        return new VBox(16, mode, new StackPane(dataForm, distributionForm),
                Ui.primary(t.bind("stats.calculate"), "play", this::calculate));
    }

    private static VBox grow(VBox box) {
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    @Override
    protected List<Example> examples() {
        return List.of(
                new Example(t.get("stats.example.data"), () -> {
                    mode.setValue(Mode.DATA);
                    dataX.setText("4 8 15 16 23 42 8 15 16 4 12 9");
                    dataY.setText("");
                    calculate();
                }),
                new Example(t.get("stats.example.correlation"), () -> {
                    mode.setValue(Mode.DATA);
                    dataX.setText("1 2 3 4 5 6 7 8");
                    dataY.setText("2.1 3.9 6.2 7.8 10.1 12.2 13.8 16.1");
                    calculate();
                }),
                new Example(t.get("stats.example.normal"), () -> {
                    mode.setValue(Mode.DISTRIBUTION);
                    kind.setValue(Kind.NORMAL);
                    setParameters("100", "15", "115");
                    calculate();
                }),
                new Example(t.get("stats.example.binomial"), () -> {
                    mode.setValue(Mode.DISTRIBUTION);
                    kind.setValue(Kind.BINOMIAL);
                    setParameters("20", "0.3", "7");
                    calculate();
                }));
    }

    /** Escribe los datos desde fuera y calcula (para pruebas). */
    public void calculateData(String x, String y) {
        mode.setValue(Mode.DATA);
        dataX.setText(x);
        dataY.setText(y);
        calculate();
    }

    /** Calcula una distribución desde fuera (para pruebas). */
    public void calculateDistribution(Kind distribution, String a, String b, String x) {
        mode.setValue(Mode.DISTRIBUTION);
        kind.setValue(distribution);
        first.setText(a);
        second.setText(b);
        point.setText(x);
        calculate();
    }

    // ---------------------------------------------------------------- Cálculo

    private void calculate() {
        try {
            show(mode.getValue() == Mode.DATA ? summary() : distribution());
        } catch (IllegalArgumentException | ArithmeticException e) {
            fail(e.getMessage());
        }
    }

    private Node summary() {
        double[] x = numbers(dataX.getText(), t.get("stats.data.x"));
        double[] y = dataY.getText() == null || dataY.getText().isBlank() ? null : numbers(dataY.getText(), t.get("stats.data.y"));
        if (y != null && y.length != x.length) throw new IllegalArgumentException(t.get("stats.error.lengths"));
        boolean isSample = sample.getValue();
        if (isSample && x.length < 2) throw new IllegalArgumentException(t.get("stats.error.few"));

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{t.get("stats.count"), String.valueOf(x.length)});
        rows.add(new String[]{t.get("stats.mean"), Numbers.both(Descriptive.mean(x))});
        rows.add(new String[]{t.get("stats.median"), Numbers.both(Descriptive.median(x))});
        double[] modes = Descriptive.modes(x);
        rows.add(new String[]{t.get("stats.mode"), modes.length == x.length ? t.get("stats.noMode")
                : Arrays.stream(modes).mapToObj(Numbers::decimal).reduce((p, q) -> p + ", " + q).orElse("")});
        rows.add(new String[]{t.get("stats.min"), Numbers.decimal(Descriptive.min(x))});
        rows.add(new String[]{t.get("stats.max"), Numbers.decimal(Descriptive.max(x))});
        rows.add(new String[]{t.get("stats.range"), Numbers.decimal(Descriptive.max(x) - Descriptive.min(x))});
        rows.add(new String[]{t.get("stats.variance.label"), Numbers.both(Descriptive.variance(x, isSample))});
        rows.add(new String[]{t.get("stats.sd"), Numbers.decimal(Descriptive.standardDeviation(x, isSample))});
        double q1 = Descriptive.percentile(x, 25);
        double q3 = Descriptive.percentile(x, 75);
        rows.add(new String[]{"Q1 / Q3", Numbers.decimal(q1) + " / " + Numbers.decimal(q3)});
        rows.add(new String[]{t.get("stats.iqr"), Numbers.decimal(q3 - q1)});
        if (x.length >= 3) rows.add(new String[]{t.get("stats.skewness"), Numbers.decimal(Descriptive.skewness(x))});
        if (x.length >= 4) rows.add(new String[]{t.get("stats.kurtosis"), Numbers.decimal(Descriptive.kurtosis(x))});

        ChartView chart;
        if (y != null) {
            rows.add(new String[]{t.get("stats.covariance"), Numbers.decimal(Descriptive.covariance(x, y))});
            rows.add(new String[]{t.get("stats.correlation"), Numbers.decimal(Descriptive.correlation(x, y))});
            chart = new ChartView(320).add(new ChartView.Series(null, x, y, ChartView.Kind.POINTS, 0));
        } else {
            chart = histogram(x);
        }
        return card(t.get("stats.summary"), new VBox(20, table(rows), chart));
    }

    private ChartView histogram(double[] x) {
        double min = Descriptive.min(x);
        double max = Descriptive.max(x);
        int bins = Math.max(3, (int) Math.ceil(Math.log(x.length) / Math.log(2)) + 1);
        double width = (max - min) / bins;
        if (width == 0) width = 1;
        double[] centers = new double[bins];
        double[] counts = new double[bins];
        for (int i = 0; i < bins; i++) centers[i] = min + width * (i + 0.5);
        for (double v : x) counts[Math.min(bins - 1, (int) ((v - min) / width))]++;
        return new ChartView(300).add(new ChartView.Series(null, centers, counts, ChartView.Kind.BARS, 0));
    }

    private Node distribution() {
        Kind k = kind.getValue();
        double a = number(first.getText(), t.get("stats.param." + k.first));
        double b = k.second == null ? 0 : number(second.getText(), t.get("stats.param." + k.second));
        double x = number(point.getText(), t.get("stats.point"));
        double p = number(probability.getText(), t.get("stats.probability"));
        if (!(p >= 0 && p <= 1)) throw new IllegalArgumentException(t.get("stats.error.probability"));

        List<String[]> rows = new ArrayList<>();
        ChartView chart;
        switch (k) {
            case NORMAL -> chart = continuous(new Normal(a, b), x, p, rows, a - 4 * b, a + 4 * b);
            case EXPONENTIAL -> chart = continuous(new Exponential(a), x, p, rows, 0, 5 / a);
            case UNIFORM -> chart = continuous(new Uniform(a, b), x, p, rows, a - (b - a) * 0.1, b + (b - a) * 0.1);
            case BINOMIAL -> {
                if (a != Math.rint(a) || a < 1 || a > 2000) throw new IllegalArgumentException(t.get("stats.error.trials"));
                chart = discrete(new Binomial((int) a, b), x, p, rows, 0, (int) a);
            }
            default -> {
                Poisson poisson = new Poisson(a);
                chart = discrete(poisson, x, p, rows, 0, (int) Math.ceil(a + 5 * Math.sqrt(a) + 3));
            }
        }
        return card(t.get("stats.dist." + k.key), new VBox(20, table(rows), chart));
    }

    private ChartView continuous(Distribution d, double x, double p, List<String[]> rows, double from, double to) {
        rows.add(new String[]{t.get("stats.mean"), Numbers.decimal(d.mean())});
        rows.add(new String[]{t.get("stats.variance.label"), Numbers.decimal(d.variance())});
        rows.add(new String[]{t.get("stats.sd"), Numbers.decimal(d.standardDeviation())});
        rows.add(new String[]{"f(" + Numbers.decimal(x) + ")", Numbers.decimal(d.pdf(x))});
        rows.add(new String[]{"P(X ≤ " + Numbers.decimal(x) + ")", Numbers.decimal(d.cdf(x))});
        rows.add(new String[]{"P(X > " + Numbers.decimal(x) + ")", Numbers.decimal(1 - d.cdf(x))});
        if (p > 0 && p < 1) rows.add(new String[]{t.get("stats.quantile") + " " + Numbers.decimal(p), Numbers.decimal(d.quantile(p))});

        int n = 400;
        double[] xs = new double[n + 1];
        double[] ys = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            xs[i] = from + (to - from) * i / n;
            ys[i] = d.pdf(xs[i]);
        }
        return new ChartView(320).add(new ChartView.Series("f(x)", xs, ys, ChartView.Kind.LINE, 0))
                .add(new ChartView.Series("x", new double[]{x}, new double[]{d.pdf(x)}, ChartView.Kind.POINTS, 4));
    }

    private ChartView discrete(DiscreteDistribution d, double x, double p, List<String[]> rows, int from, int to) {
        int k = (int) Math.rint(x);
        rows.add(new String[]{t.get("stats.mean"), Numbers.decimal(d.mean())});
        rows.add(new String[]{t.get("stats.variance.label"), Numbers.decimal(d.variance())});
        rows.add(new String[]{t.get("stats.sd"), Numbers.decimal(d.standardDeviation())});
        rows.add(new String[]{"P(X = " + k + ")", Numbers.decimal(d.pmf(k))});
        rows.add(new String[]{"P(X ≤ " + k + ")", Numbers.decimal(d.cdf(k))});
        rows.add(new String[]{"P(X > " + k + ")", Numbers.decimal(1 - d.cdf(k))});
        if (p > 0 && p < 1) rows.add(new String[]{t.get("stats.quantile") + " " + Numbers.decimal(p), String.valueOf(d.quantile(p))});

        int count = to - from + 1;
        double[] xs = new double[count];
        double[] ys = new double[count];
        for (int i = 0; i < count; i++) {
            xs[i] = from + i;
            ys[i] = d.pmf(from + i);
        }
        return new ChartView(320).add(new ChartView.Series("P(X = k)", xs, ys, ChartView.Kind.BARS, 0));
    }

    // ---------------------------------------------------------------- Lectura y presentación

    private double[] numbers(String text, String name) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException(t.get("stats.error.empty"));
        String[] tokens = text.strip().split("[\\s,;]+");
        double[] values = new double[tokens.length];
        for (int i = 0; i < tokens.length; i++) values[i] = number(tokens[i], name);
        return values;
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

    private static Node table(List<String[]> rows) {
        GridPane grid = new GridPane();
        grid.setHgap(32);
        grid.setVgap(8);
        grid.setPadding(new Insets(0));
        for (int i = 0; i < rows.size(); i++) {
            grid.add(Ui.label(rows.get(i)[0], "text-muted"), 0, i);
            grid.add(Ui.label(rows.get(i)[1], "mono"), 1, i);
        }
        return grid;
    }
}
