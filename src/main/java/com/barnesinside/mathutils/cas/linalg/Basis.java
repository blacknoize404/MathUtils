package com.barnesinside.mathutils.cas.linalg;

import com.barnesinside.mathutils.cas.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Una base de ℝⁿ: coordenadas de un vector respecto de ella, matriz de cambio de base y cómo completar unos vectores independientes hasta
 * formar una base.
 *
 * @param vectors los {@code n} vectores de la base, independientes
 * @since 0.10
 */
public record Basis(List<List<Expr>> vectors) {

    public Basis {
        vectors = List.copyOf(vectors);
        int n = vectors.isEmpty() ? 0 : vectors.get(0).size();
        if (vectors.size() != n || !Independence.of(vectors).independent()) {
            throw new IllegalArgumentException("Los vectores no forman una base: hacen falta " + n + " vectores independientes");
        }
    }

    public int dimension() {
        return vectors.size();
    }

    /** Las coordenadas de {@code v} respecto de esta base: {@code [v]_B = B⁻¹ v}. */
    public List<Expr> coordinates(List<Expr> v) {
        return Subspace.spanning(vectors).coordinates(v);
    }

    /**
     * La matriz de cambio de base de esta base a {@code other}: la que lleva las coordenadas respecto de esta a las coordenadas respecto de
     * {@code other}. Sus columnas son las coordenadas de cada vector de esta base respecto de {@code other}.
     */
    public ExactMatrix changeTo(Basis other) {
        if (other.dimension() != dimension()) throw new IllegalArgumentException("Las bases son de espacios de distinta dimensión");
        List<List<Expr>> columns = new ArrayList<>();
        for (List<Expr> v : vectors) columns.add(other.coordinates(v));
        return ExactMatrix.ofColumns(columns);
    }

    /**
     * Completa unos vectores independientes hasta una base de ℝⁿ añadiendo vectores de la base canónica que no sobran.
     *
     * @throws IllegalArgumentException si los vectores dados son dependientes
     */
    public static Basis complete(List<List<Expr>> independent) {
        if (!Independence.of(independent).independent()) throw new IllegalArgumentException("Los vectores son linealmente dependientes");
        int n = independent.get(0).size();
        List<List<Expr>> all = new ArrayList<>(independent);
        for (int i = 0; i < n && all.size() < n; i++) {
            List<Expr> e = new ArrayList<>();
            for (int j = 0; j < n; j++) e.add(j == i ? Expr.ONE : Expr.ZERO);
            List<List<Expr>> trial = new ArrayList<>(all);
            trial.add(e);
            if (Independence.of(trial).independent()) all.add(e);
        }
        return new Basis(all);
    }
}
