package barnesinside.mathutils.cas.linalg;

import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Una matriz de expresiones, inmutable: sus entradas pueden ser números exactos ({@code 1/2}), radicales ({@code sqrt(2)}) o letras.
 *
 * @since 0.10
 */
public final class ExactMatrix {

    private final Expr[][] cells;
    private final int rows;
    private final int cols;

    private ExactMatrix(Expr[][] cells, int rows, int cols) {
        this.cells = cells;
        this.rows = rows;
        this.cols = cols;
    }

    /** Una matriz con las filas dadas, todas del mismo largo. */
    public static ExactMatrix of(List<List<Expr>> values) {
        if (values.isEmpty() || values.get(0).isEmpty()) throw new IllegalArgumentException("La matriz está vacía");
        int cols = values.get(0).size();
        Expr[][] cells = new Expr[values.size()][cols];
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).size() != cols) throw new IllegalArgumentException("Todas las filas deben tener el mismo número de columnas");
            for (int j = 0; j < cols; j++) cells[i][j] = values.get(i).get(j);
        }
        return new ExactMatrix(cells, values.size(), cols);
    }

    /** Lee una matriz escrita con una fila por línea (o separadas por «;») y las entradas separadas por espacios o comas. */
    public static ExactMatrix parse(String text) {
        List<List<Expr>> rows = new ArrayList<>();
        for (String line : text.strip().split("[\\n;]+")) {
            if (line.isBlank()) continue;
            List<Expr> row = new ArrayList<>();
            for (String token : line.strip().split("[\\s,]+")) row.add(ExprParser.parse(token));
            rows.add(row);
        }
        return of(rows);
    }

    /** La matriz cuyas columnas son los vectores dados. */
    public static ExactMatrix ofColumns(List<List<Expr>> vectors) {
        if (vectors.isEmpty()) throw new IllegalArgumentException("No hay vectores");
        int size = vectors.get(0).size();
        List<List<Expr>> rows = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            List<Expr> row = new ArrayList<>();
            for (List<Expr> v : vectors) {
                if (v.size() != size) throw new IllegalArgumentException("Todos los vectores deben tener las mismas componentes");
                row.add(v.get(i));
            }
            rows.add(row);
        }
        return of(rows);
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public Expr get(int row, int col) {
        return cells[row][col];
    }

    public List<Expr> row(int row) {
        return List.of(cells[row]);
    }

    public boolean isSquare() {
        return rows == cols;
    }

    public ExactMatrix transpose() {
        Expr[][] out = new Expr[cols][rows];
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) out[j][i] = cells[i][j];
        return new ExactMatrix(out, cols, rows);
    }

    public ExactMatrix multiply(ExactMatrix other) {
        if (cols != other.rows) throw new IllegalArgumentException("Las dimensiones no coinciden: " + rows + "×" + cols + " por " + other.rows + "×" + other.cols);
        Expr[][] out = new Expr[rows][other.cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < other.cols; j++) {
                List<Expr> terms = new ArrayList<>();
                for (int k = 0; k < cols; k++) terms.add(Expr.mul(cells[i][k], other.cells[k][j]));
                out[i][j] = Expand.expand(Expr.add(terms));
            }
        }
        return new ExactMatrix(out, rows, other.cols);
    }

    /** Una copia con la fila {@code row} cambiada. */
    ExactMatrix withRow(int row, List<Expr> values) {
        Expr[][] out = new Expr[rows][];
        for (int i = 0; i < rows; i++) out[i] = i == row ? values.toArray(new Expr[0]) : cells[i];
        return new ExactMatrix(out, rows, cols);
    }

    /** Una copia con dos filas intercambiadas. */
    ExactMatrix swapped(int a, int b) {
        Expr[][] out = new Expr[rows][];
        for (int i = 0; i < rows; i++) out[i] = i == a ? cells[b] : i == b ? cells[a] : cells[i];
        return new ExactMatrix(out, rows, cols);
    }

    /** Si la entrada vale cero: exactamente, o (con números) dentro de la precisión de los decimales. Una letra suelta nunca vale cero. */
    static boolean isZero(Expr e) {
        Expr expanded = Expand.expand(e);
        if (expanded.equals(Expr.ZERO)) return true;
        if (!expanded.variables().isEmpty()) return false;
        try {
            return Math.abs(Eval.value(expanded)) < 1e-9;
        } catch (RuntimeException notANumber) {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ExactMatrix m) || m.rows != rows || m.cols != cols) return false;
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) if (!cells[i][j].equals(m.cells[i][j])) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.deepHashCode(cells);
    }

    @Override
    public String toString() {
        StringJoiner out = new StringJoiner("; ", "[", "]");
        for (Expr[] row : cells) {
            StringJoiner line = new StringJoiner(", ");
            for (Expr e : row) line.add(e.toString());
            out.add(line.toString());
        }
        return out.toString();
    }
}
