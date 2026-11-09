package barnesinside.mathutils.cas.linalg;

import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * La factorización QR de una matriz de columnas linealmente independientes: {@code A = Q R}, con {@code Q} de columnas ortonormales (las del proceso
 * de {@link GramSchmidt}) y {@code R} triangular superior con {@code R[i][j] = qᵢ · aⱼ}.
 *
 * @param q            las columnas ortonormales
 * @param r            la matriz triangular superior
 * @param gramSchmidt  el proceso del que sale {@code Q}, con sus pasos
 * @since 0.10
 */
public record QR(ExactMatrix q, ExactMatrix r, GramSchmidt.Result gramSchmidt) {

    /**
     * Factoriza la matriz.
     *
     * @throws IllegalArgumentException si tiene más columnas que filas o sus columnas son linealmente dependientes
     */
    public static QR of(ExactMatrix a) {
        if (a.cols() > a.rows()) throw new IllegalArgumentException("Hace falta que no haya más columnas que filas");
        List<List<Expr>> columns = new ArrayList<>();
        for (int j = 0; j < a.cols(); j++) columns.add(column(a, j));
        GramSchmidt.Result gs = GramSchmidt.of(columns);
        ExactMatrix q = ExactMatrix.ofColumns(gs.orthonormal());
        List<List<Expr>> r = new ArrayList<>();
        for (int i = 0; i < a.cols(); i++) {
            List<Expr> row = new ArrayList<>();
            for (int j = 0; j < a.cols(); j++) row.add(i <= j ? Expand.expand(GramSchmidt.dot(gs.orthonormal().get(i), columns.get(j))) : Expr.ZERO);
            r.add(row);
        }
        return new QR(q, ExactMatrix.of(r), gs);
    }

    /** Si {@code Q·R} da otra vez la matriz de partida. */
    public boolean verifies(ExactMatrix a) {
        return q.multiply(r).sameAs(a);
    }

    /** Si las columnas de {@code Q} son ortonormales: {@code QᵀQ = I}. */
    public boolean orthonormal() {
        return q.transpose().multiply(q).sameAs(ExactMatrix.identity(q.cols()));
    }

    /** La conclusión con palabras. */
    public String summary(Locale locale) {
        return RowReduction.message(locale, "linalg.qr.summary", q, r);
    }

    private static List<Expr> column(ExactMatrix a, int j) {
        List<Expr> out = new ArrayList<>();
        for (int i = 0; i < a.rows(); i++) out.add(a.get(i, j));
        return out;
    }
}
