package barnesinside.mathutils.cas.linalg;

import barnesinside.mathutils.cas.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * El subespacio de ℝⁿ generado por unos vectores: una base (los generadores que no sobran), su dimensión, si un vector pertenece a él y las
 * coordenadas de un vector respecto de la base.
 *
 * @param generators los vectores que lo generan, tal como se dieron
 * @param basis      una base: los generadores cuya columna tiene pivote al reducir
 * @since 0.10
 */
public record Subspace(List<List<Expr>> generators, List<List<Expr>> basis) {

    public static Subspace spanning(List<List<Expr>> generators) {
        ExactMatrix matrix = ExactMatrix.ofColumns(generators);
        RowReduction.Result reduction = RowReduction.reduce(matrix);
        List<List<Expr>> basis = new ArrayList<>();
        for (int c : reduction.pivots()) basis.add(generators.get(c));
        return new Subspace(generators, basis);
    }

    public int dimension() {
        return basis.size();
    }

    /** El tamaño de los vectores: el {@code n} de ℝⁿ. */
    public int ambient() {
        return generators.get(0).size();
    }

    /** Si el vector pertenece al subespacio: añadirlo a la base no sube el rango. */
    public boolean contains(List<Expr> vector) {
        return coordinates(vector) != null;
    }

    /**
     * Las coordenadas del vector respecto de la base (los coeficientes con {@code Σ cᵢ bᵢ = v}), o {@code null} si el vector no está en el
     * subespacio. Son únicas porque la base es independiente.
     */
    public List<Expr> coordinates(List<Expr> vector) {
        if (basis.isEmpty()) return vector.stream().allMatch(ExactMatrix::isZero) ? List.of() : null;
        List<List<Expr>> columns = new ArrayList<>(basis);
        columns.add(vector);
        RowReduction.Result reduction = RowReduction.reduce(ExactMatrix.ofColumns(columns));
        // Si la última columna tiene pivote, el sistema no tiene solución: el vector no está en el subespacio
        if (reduction.pivots().contains(basis.size())) return null;
        List<Expr> coordinates = new ArrayList<>();
        for (int i = 0; i < basis.size(); i++) coordinates.add(reduction.reduced().get(i, basis.size()));
        return coordinates;
    }

    /** Si los dos subespacios son el mismo: cada generador de uno está en el otro. */
    public boolean sameAs(Subspace other) {
        return ambient() == other.ambient() && generators.stream().allMatch(other::contains) && other.generators.stream().allMatch(this::contains);
    }
}
