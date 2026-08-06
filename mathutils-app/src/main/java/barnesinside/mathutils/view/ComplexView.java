package barnesinside.mathutils.view;

import MathUtils.complex.Complex;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.math.ChartView;
import barnesinside.mathutils.ui.Numbers;
import barnesinside.mathutils.ui.Ui;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Números complejos: operaciones entre dos números, módulo, argumento, forma polar, potencias,
 * raíces n-ésimas y funciones elementales, con el plano de Argand para ver los resultados.
 */
public final class ComplexView extends ToolView {

    /** Operación disponible. */
    public enum Op {
        ADD("add"), SUBTRACT("subtract"), MULTIPLY("multiply"), DIVIDE("divide"), CONJUGATE("conjugate"),
        MODULUS("modulus"), ARGUMENT("argument"), POLAR("polar"), POWER("power"), ROOTS("roots"), EXP("exp"),
        LN("ln"), SQRT("sqrt"), SIN("sin"), COS("cos");

        private final String key;

        Op(String key) {
            this.key = key;
        }
    }

    private final TextField z1 = field("3+4i");
    private final TextField z2 = field("1-2i");
    private final TextField n = field("3");

    public ComplexView(Workspace workspace) {
        super(workspace);
        z1.promptTextProperty().bind(t.bind("complex.placeholder"));
        z2.promptTextProperty().bind(t.bind("complex.placeholder"));
        for (TextField f : List.of(z1, z2, n)) {
            f.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) run(Op.POLAR);
            });
        }
    }

    private static TextField field(String initial) {
        TextField field = new TextField(initial);
        field.getStyleClass().addAll("mono", "expression-field");
        return field;
    }

    @Override
    public String id() {
        return "complex";
    }

    @Override
    public String icon() {
        return "pi";
    }

    @Override
    protected String keyPrefix() {
        return "complex";
    }

    private final barnesinside.mathutils.math.LivePreview preview = new barnesinside.mathutils.math.LivePreview();

    @Override
    protected Node previewNode() {
        return preview.follow(List.of(z1, z2), () -> {
            boolean hasFirst = z1.getText() != null && !z1.getText().isBlank();
            boolean hasSecond = z2.getText() != null && !z2.getText().isBlank();
            if (!hasFirst && !hasSecond) return null;
            List<barnesinside.mathutils.math.MathLayout.Box> items = new java.util.ArrayList<>();
            if (hasFirst) items.add(barnesinside.mathutils.math.MathLayout.text("z1 = " + format(Complex.parse(z1.getText())), 20));
            if (hasFirst && hasSecond) items.add(barnesinside.mathutils.math.MathLayout.text("      ", 20));
            if (hasSecond) items.add(barnesinside.mathutils.math.MathLayout.text("z2 = " + format(Complex.parse(z2.getText())), 20));
            return barnesinside.mathutils.math.MathLayout.row(items);
        });
    }

    @Override
    protected boolean imaginaryUnit() {
        return true;
    }

    @Override
    protected List<javafx.scene.control.TextInputControl> inputs() {
        return List.of(z1, z2, n);
    }

    @Override
    protected Node form() {
        VBox first = field("complex.z1", z1);
        VBox second = field("complex.z2", z2);
        n.setPrefWidth(90);
        VBox exponent = field("complex.n", n);
        HBox.setHgrow(first, Priority.ALWAYS);
        HBox.setHgrow(second, Priority.ALWAYS);

        FlowPane operations = new FlowPane(8, 8);
        for (Op op : Op.values()) operations.getChildren().add(Ui.secondary(t.bind("complex.op." + op.key), null, () -> run(op)));
        HBox row = new HBox(16, first, second, exponent);
        return new VBox(16, row, operations);
    }

    @Override
    protected List<Example> examples() {
        return List.of(
                new Example("3+4i", () -> set("3+4i", "1-2i", "3")),
                new Example("(1+i)^2", () -> set("(1+i)^2", "2-i", "4")),
                new Example("-8", () -> set("-8", "i", "3")),
                new Example("1", () -> set("1", "-1", "6")));
    }

    private void set(String first, String second, String exponent) {
        z1.setText(first);
        z2.setText(second);
        n.setText(exponent);
        run(Op.POLAR);
    }

    /** Rellena los campos desde fuera (para pruebas). */
    public void setValues(String first, String second, String exponent) {
        z1.setText(first);
        z2.setText(second);
        n.setText(exponent);
    }

    // ---------------------------------------------------------------- Cálculo

    /** Ejecuta una operación con los números escritos y muestra el resultado o el error. */
    public void run(Op op) {
        try {
            Complex first = Complex.parse(z1.getText());
            Complex second = needsSecond(op) ? Complex.parse(z2.getText()) : null;
            int exponent = needsExponent(op) ? integer(n.getText()) : 0;
            show(result(op, first, second, exponent));
        } catch (IllegalArgumentException | ArithmeticException e) {
            fail(e.getMessage());
        }
    }

    private static boolean needsSecond(Op op) {
        return op == Op.ADD || op == Op.SUBTRACT || op == Op.MULTIPLY || op == Op.DIVIDE;
    }

    private static boolean needsExponent(Op op) {
        return op == Op.POWER || op == Op.ROOTS;
    }

    private int integer(String text) {
        try {
            int value = Integer.parseInt(text.strip());
            if (value < 1 || value > 360) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(t.get("complex.error.integer"));
        }
    }

    private Node result(Op op, Complex first, Complex second, int exponent) {
        List<String[]> lines = new ArrayList<>();
        ChartView chart = new ChartView(340).equalAspect(true);
        List<Complex> results = new ArrayList<>();

        vector(chart, "z1", first, 0);
        if (second != null) vector(chart, "z2", second, 1);

        switch (op) {
            case ADD -> results.add(first.add(second));
            case SUBTRACT -> results.add(first.subtract(second));
            case MULTIPLY -> results.add(first.multiply(second));
            case DIVIDE -> {
                if (second.isZero()) throw new IllegalArgumentException(t.get("complex.error.zero"));
                results.add(first.divide(second));
            }
            case CONJUGATE -> results.add(first.conjugate());
            case MODULUS -> lines.add(new String[]{"|z1|", real(first.abs())});
            case ARGUMENT -> {
                lines.add(new String[]{"arg z1", real(first.arg()) + " rad"});
                lines.add(new String[]{"", real(Math.toDegrees(first.arg())) + "°"});
            }
            case POLAR -> {
                lines.add(new String[]{"|z1|", real(first.abs())});
                lines.add(new String[]{"arg z1", real(first.arg()) + " rad = " + real(Math.toDegrees(first.arg())) + "°"});
                lines.add(new String[]{t.get("complex.polar"), real(first.abs()) + "·(cos " + real(first.arg())
                        + " + i·sin " + real(first.arg()) + ")"});
                lines.add(new String[]{t.get("complex.exponential"), real(first.abs()) + "·e^(" + real(first.arg()) + "i)"});
            }
            case POWER -> results.add(first.pow(exponent));
            case ROOTS -> {
                if (first.isZero()) throw new IllegalArgumentException(t.get("complex.error.zero"));
                Complex[] roots = first.roots(exponent);
                results.addAll(List.of(roots));
                double[] cx = new double[181];
                double[] cy = new double[181];
                for (int i = 0; i <= 180; i++) {
                    cx[i] = Math.pow(first.abs(), 1.0 / exponent) * Math.cos(Math.PI * i / 90);
                    cy[i] = Math.pow(first.abs(), 1.0 / exponent) * Math.sin(Math.PI * i / 90);
                }
                chart.add(new ChartView.Series(null, cx, cy, ChartView.Kind.LINE, -1));
            }
            case EXP -> results.add(first.exp());
            case LN -> {
                if (first.isZero()) throw new IllegalArgumentException(t.get("complex.error.zero"));
                results.add(first.log());
            }
            case SQRT -> results.add(first.sqrt());
            case SIN -> results.add(first.sin());
            case COS -> results.add(first.cos());
        }

        String label = t.get("complex.op." + op.key);
        for (int i = 0; i < results.size(); i++) {
            String name = results.size() > 1 ? "w" + (i + 1) : "w";
            lines.add(new String[]{name, format(results.get(i))});
        }
        if (!results.isEmpty()) {
            double[] xs = results.stream().mapToDouble(Complex::re).toArray();
            double[] ys = results.stream().mapToDouble(Complex::im).toArray();
            chart.add(new ChartView.Series(results.size() == 1 ? "w" : t.get("complex.roots"), xs, ys, ChartView.Kind.POINTS, 4));
        }

        VBox rows = new VBox(8);
        for (String[] line : lines) {
            Label name = Ui.label(line[0], "mono", "text-muted");
            name.setMinWidth(70);
            Label value = Ui.label(line[1], "mono");
            value.setStyle("-fx-font-size: 17px;");
            value.setWrapText(true);
            HBox row = new HBox(14, name, value);
            rows.getChildren().add(row);
        }
        VBox box = new VBox(18, rows, chart);
        return card(label, box);
    }

    private static void vector(ChartView chart, String name, Complex z, int color) {
        chart.add(new ChartView.Series(name, new double[]{z.re()}, new double[]{z.im()}, ChartView.Kind.VECTOR, color));
    }

    private static String real(double v) {
        return Numbers.exact(v);
    }

    /** Forma rectangular legible: {@code 3 + 4i}, {@code −2i}, {@code 5}. */
    static String format(Complex z) {
        double re = Math.abs(z.re()) < 1e-12 ? 0 : z.re();
        double im = Math.abs(z.im()) < 1e-12 ? 0 : z.im();
        if (im == 0) return Numbers.exact(re);
        String imaginary = Math.abs(Math.abs(im) - 1) < 1e-12 ? "i" : Numbers.exact(Math.abs(im)) + "i";
        if (re == 0) return (im < 0 ? "−" : "") + imaginary;
        return Numbers.exact(re) + (im < 0 ? " − " : " + ") + imaginary;
    }
}
