package barnesinside.mathutils.cas.linalg;

import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Una transformación lineal dada por su matriz {@code A} (de {@code m} filas y {@code n} columnas, de ℝⁿ en ℝᵐ): su núcleo, su imagen, el rango y
 * la nulidad, y si es inyectiva o sobreyectiva. Todo sale de la forma escalonada reducida de {@code A}.
 *
 * @param matrix    la matriz
 * @param reduction su reducción por filas, con los pasos
 * @param kernel    una base del núcleo (vectores de ℝⁿ con {@code A v = 0})
 * @param image     una base de la imagen: las columnas de {@code A} que tienen pivote
 * @since 0.10
 */
public record LinearMap(ExactMatrix matrix, RowReduction.Result reduction, List<List<Expr>> kernel, List<List<Expr>> image) {

    public static LinearMap of(ExactMatrix matrix) {
        RowReduction.Result reduction = RowReduction.reduce(matrix);
        int n = matrix.cols();
        List<List<Expr>> kernel = new ArrayList<>();
        for (int f = 0; f < n; f++) {
            if (reduction.pivots().contains(f)) continue;
            List<Expr> v = new ArrayList<>();
            for (int i = 0; i < n; i++) v.add(i == f ? Expr.ONE : Expr.ZERO);
            for (int r = 0; r < reduction.pivots().size(); r++) {
                v.set(reduction.pivots().get(r), Expand.expand(Expr.neg(reduction.reduced().get(r, f))));
            }
            kernel.add(v);
        }
        List<List<Expr>> image = new ArrayList<>();
        for (int c : reduction.pivots()) {
            List<Expr> column = new ArrayList<>();
            for (int i = 0; i < matrix.rows(); i++) column.add(matrix.get(i, c));
            image.add(column);
        }
        return new LinearMap(matrix, reduction, kernel, image);
    }

    /** La dimensión de la imagen. */
    public int rank() {
        return reduction.rank();
    }

    /** La dimensión del núcleo: {@code n − rango} (teorema de la dimensión). */
    public int nullity() {
        return matrix.cols() - rank();
    }

    /** Inyectiva: el núcleo es solo el cero. */
    public boolean injective() {
        return nullity() == 0;
    }

    /** Sobreyectiva: la imagen es todo ℝᵐ. */
    public boolean surjective() {
        return rank() == matrix.rows();
    }

    /** El resumen con palabras: dimensiones, teorema de la dimensión y si es inyectiva o sobreyectiva. */
    public String summary(Locale locale) {
        return RowReduction.message(locale, "linalg.map.summary", matrix.cols(), matrix.rows(), rank(), nullity())
                + " " + RowReduction.message(locale, injective() ? "linalg.map.injective" : "linalg.map.notInjective")
                + " " + RowReduction.message(locale, surjective() ? "linalg.map.surjective" : "linalg.map.notSurjective");
    }
}
