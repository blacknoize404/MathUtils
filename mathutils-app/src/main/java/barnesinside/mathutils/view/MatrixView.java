package barnesinside.mathutils.view;

import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.ExprParser;
import MathUtils.matrix.Matrix;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.ui.Numbers;
import barnesinside.mathutils.ui.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Álgebra lineal con dos matrices: suma, producto, transpuesta, determinante, rango, traza,
 * inversa, descomposición LU y sistemas {@code A·x = b}. Las entradas pueden ser números,
 * fracciones ({@code 1/2}) o expresiones ({@code sqrt(2)}); los resultados muestran la fracción
 * exacta cuando existe.
 */
public final class MatrixView extends ToolView {

    /** Operación disponible. */
    public enum Op {
        ADD("add", true), SUBTRACT("subtract", true), MULTIPLY("multiply", true), TRANSPOSE("transpose", false),
        DETERMINANT("determinant", false), RANK("rank", false), TRACE("trace", false), INVERSE("inverse", false),
        LU("lu", false), SOLVE("solve", true);

        private final String key;
        private final boolean needsB;

        Op(String key, boolean needsB) {
            this.key = key;
            this.needsB = needsB;
        }
    }

    private final TextArea a = area("matrix.a.placeholder");
    private final TextArea b = area("matrix.b.placeholder");

    public MatrixView(Workspace workspace) {
        super(workspace);
    }

    private TextArea area(String promptKey) {
        TextArea area = new TextArea();
        area.setPrefRowCount(5);
        area.getStyleClass().add("mono");
        area.promptTextProperty().bind(t.bind(promptKey));
        return area;
    }

    @Override
    public String id() {
        return "matrices";
    }

    @Override
    public String icon() {
        return "layout-grid";
    }

    @Override
    protected String keyPrefix() {
        return "matrix";
    }

    @Override
    protected Node form() {
        VBox left = field("matrix.a", a);
        VBox right = field("matrix.b", b);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        FlowPane operations = new FlowPane(8, 8);
        for (Op op : Op.values()) operations.getChildren().add(Ui.secondary(t.bind("matrix.op." + op.key), null, () -> run(op)));
        return new VBox(16, new HBox(16, left, right), operations);
    }

    @Override
    protected List<Example> examples() {
        return List.of(
                new Example("2×2", () -> set("2 1\n1 3", "1 0\n4 -1")),
                new Example("3×3", () -> set("2 -1 0\n-1 2 -1\n0 -1 2", "1 2 3\n4 5 6\n7 8 10")),
                new Example("A·x = b", () -> set("2 1 -1\n-3 -1 2\n-2 1 2", "8\n-11\n-3")),
                new Example("1/2", () -> set("1/2 1/3\n1/4 1/5", "1 0\n0 1")));
    }

    private void set(String matrixA, String matrixB) {
        a.setText(matrixA);
        b.setText(matrixB);
    }

    /** Rellena las dos matrices desde fuera (para pruebas). */
    public void setMatrices(String matrixA, String matrixB) {
        set(matrixA, matrixB);
    }

    // ---------------------------------------------------------------- Cálculo

    /** Ejecuta una operación con las matrices escritas y muestra el resultado o el error. */
    public void run(Op op) {
        try {
            Matrix first = parse(a.getText());
            Matrix second = op.needsB ? parse(b.getText()) : null;
            show(result(op, first, second));
        } catch (IllegalArgumentException | ArithmeticException e) {
            fail(e.getMessage());
        }
    }

    private Node result(Op op, Matrix first, Matrix second) {
        String title = t.get("matrix.op." + op.key);
        return switch (op) {
            case ADD -> card(title, grid(first.add(second)));
            case SUBTRACT -> card(title, grid(first.subtract(second)));
            case MULTIPLY -> card(title, grid(first.multiply(second)));
            case TRANSPOSE -> card(title, grid(first.transpose()));
            case INVERSE -> card(title, grid(first.inverse()));
            case DETERMINANT -> card(title, scalar(first.determinant()));
            case TRACE -> card(title, scalar(first.trace()));
            case RANK -> card(title, scalar(first.rank()));
            case LU -> {
                Matrix.LU lu = first.lu();
                yield card(title, new VBox(18, named("L", grid(lu.l())), named("U", grid(lu.u())), named("P", grid(lu.p()))));
            }
            case SOLVE -> {
                if (second.cols() != 1) throw new IllegalArgumentException(t.get("matrix.error.vector"));
                double[] column = new double[second.rows()];
                for (int i = 0; i < column.length; i++) column[i] = second.get(i, 0);
                double[] x = first.solve(column);
                double[][] values = new double[x.length][1];
                for (int i = 0; i < x.length; i++) values[i][0] = x[i];
                yield card(title, grid(Matrix.of(values)));
            }
        };
    }

    // ---------------------------------------------------------------- Lectura

    /**
     * Lee una matriz: una fila por línea (o separadas por «;»), entradas separadas por espacios o
     * comas. Cada entrada puede ser una expresión ({@code 1/2}, {@code sqrt(2)}).
     * @throws IllegalArgumentException con un mensaje para el usuario si el texto no es una matriz
     */
    static Matrix parse(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Escribe la matriz. Ejemplo: 1 2 en una fila y 3 4 en la siguiente.");
        List<double[]> rows = new ArrayList<>();
        for (String line : text.strip().split("[\\n;]+")) {
            if (line.isBlank()) continue;
            String[] tokens = line.strip().split("[\\s,]+");
            double[] row = new double[tokens.length];
            for (int j = 0; j < tokens.length; j++) {
                try {
                    row[j] = Eval.value(ExprParser.parse(tokens[j]));
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException("«" + tokens[j] + "» no es un número. Usa por ejemplo 2, -1.5 o 1/3.");
                }
                if (!Double.isFinite(row[j])) throw new IllegalArgumentException("«" + tokens[j] + "» no es un número finito.");
            }
            rows.add(row);
        }
        int columns = rows.get(0).length;
        for (double[] row : rows) {
            if (row.length != columns) throw new IllegalArgumentException("Todas las filas deben tener el mismo número de columnas.");
        }
        return Matrix.of(rows.toArray(new double[0][]));
    }

    // ---------------------------------------------------------------- Presentación

    private static Node named(String name, Node matrix) {
        Label label = Ui.label(name + " =", "mono", "text-muted");
        HBox box = new HBox(14, label, matrix);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Node scalar(double value) {
        Label label = Ui.label(Numbers.both(value), "mono");
        label.setStyle("-fx-font-size: 26px;");
        return label;
    }

    /** Matriz entre corchetes (dos líneas verticales), con fracciones exactas cuando las hay. */
    private static Node grid(Matrix m) {
        GridPane grid = new GridPane();
        grid.setHgap(22);
        grid.setVgap(8);
        for (int i = 0; i < m.rows(); i++) {
            for (int j = 0; j < m.cols(); j++) {
                Label cell = Ui.label(Numbers.exact(m.get(i, j)), "mono");
                cell.setStyle("-fx-font-size: 16px;");
                GridPane.setHalignment(cell, javafx.geometry.HPos.RIGHT);
                grid.add(cell, j, i);
            }
        }
        HBox brackets = new HBox(grid);
        brackets.setPadding(new Insets(8, 16, 8, 16));
        brackets.setStyle("-fx-border-color: -bi-fg; -fx-border-width: 0 2 0 2;");
        brackets.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        return brackets;
    }
}
