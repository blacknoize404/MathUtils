package barnesinside.mathutils.cas.linalg;

import barnesinside.mathutils.cas.algebra.Eigen;
import barnesinside.mathutils.cas.expr.Expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * La diagonalización {@code A = P D P⁻¹} de una matriz cuadrada: los autovalores reales con sus autoespacios ({@link Eigen}), {@code P} con los
 * autovectores por columnas, {@code D} con los autovalores en la diagonal y la inversa de {@code P} con los pasos de su cálculo. Si la matriz no se
 * puede diagonalizar sobre los reales, dice por qué.
 *
 * @param diagonalizable si se pudo diagonalizar
 * @param eigen          los autovalores y autoespacios
 * @param p              los autovectores por columnas, o {@code null} si no es diagonalizable
 * @param d              la matriz diagonal, o {@code null}
 * @param inversion      la reducción de {@code [P | I]}, con la inversa de {@code P} a la derecha, o {@code null}
 * @param reason         si no es diagonalizable, la clave del motivo
 * @since 0.10
 */
public record Diagonalization(boolean diagonalizable, Eigen.Result eigen, ExactMatrix p, ExactMatrix d, RowReduction.Result inversion, String reason) {

    /**
     * Diagonaliza la matriz.
     *
     * @throws IllegalArgumentException si no es cuadrada o es demasiado grande
     */
    public static Diagonalization of(ExactMatrix a) {
        if (!a.isSquare()) throw new IllegalArgumentException("La matriz tiene que ser cuadrada");
        List<List<Expr>> rows = new ArrayList<>();
        for (int i = 0; i < a.rows(); i++) rows.add(a.row(i));
        Eigen.Result eigen = Eigen.of(rows);
        if (!eigen.complete()) return new Diagonalization(false, eigen, null, null, null, "linalg.diag.noReal");
        if (!eigen.diagonalizable()) return new Diagonalization(false, eigen, null, null, null, "linalg.diag.defective");
        List<List<Expr>> vectors = new ArrayList<>();
        List<Expr> values = new ArrayList<>();
        for (Eigen.Pair pair : eigen.pairs()) {
            for (List<Expr> vector : pair.vectors()) {
                vectors.add(vector);
                values.add(pair.value());
            }
        }
        List<List<Expr>> diagonal = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            List<Expr> row = new ArrayList<>();
            for (int j = 0; j < values.size(); j++) row.add(i == j ? values.get(i) : Expr.ZERO);
            diagonal.add(row);
        }
        ExactMatrix p = ExactMatrix.ofColumns(vectors);
        return new Diagonalization(true, eigen, p, ExactMatrix.of(diagonal), p.inversion(), null);
    }

    /** La inversa de {@code P}, o {@code null} si no es diagonalizable. */
    public ExactMatrix inverse() {
        return inversion == null ? null : inversion.reduced().columns(p.cols(), 2 * p.cols());
    }

    /** Si {@code P·D·P⁻¹} da otra vez la matriz de partida. */
    public boolean verifies(ExactMatrix a) {
        return diagonalizable && inverse() != null && p.multiply(d).multiply(inverse()).sameAs(a);
    }

    /** La conclusión con palabras. */
    public String conclusion(Locale locale) {
        if (!diagonalizable) return RowReduction.message(locale, reason);
        return RowReduction.message(locale, "linalg.diag.yes", p, d, inverse());
    }
}
