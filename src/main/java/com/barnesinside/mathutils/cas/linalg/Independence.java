package com.barnesinside.mathutils.cas.linalg;

import com.barnesinside.mathutils.cas.expr.Expand;
import com.barnesinside.mathutils.cas.expr.Expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Si unos vectores son linealmente independientes: se ponen como columnas de una matriz y se reduce por filas; lo son si cada columna tiene
 * pivote. Si no, de las columnas sin pivote sale una combinación lineal no trivial que da el vector cero.
 *
 * @since 0.10
 */
public final class Independence {

    /**
     * El resultado.
     *
     * @param independent si los vectores son linealmente independientes
     * @param combination los coeficientes {@code c} (uno por vector, no todos cero) con {@code Σ cᵢ vᵢ = 0}, o vacío si son independientes
     * @param reduction   la reducción por filas de la matriz de columnas
     */
    public record Result(boolean independent, List<Expr> combination, RowReduction.Result reduction) {
        /** La conclusión con palabras. */
        public String conclusion(Locale locale) {
            if (independent) return RowReduction.message(locale, "linalg.independent", reduction.rank());
            StringBuilder sum = new StringBuilder();
            for (int i = 0; i < combination.size(); i++) {
                if (ExactMatrix.isZero(combination.get(i))) continue;
                if (sum.length() > 0) sum.append(" + ");
                sum.append('(').append(combination.get(i)).append(")·v").append(i + 1);
            }
            return RowReduction.message(locale, "linalg.dependent", reduction.rank(), sum);
        }
    }

    private Independence() {
    }

    public static Result of(List<List<Expr>> vectors) {
        ExactMatrix matrix = ExactMatrix.ofColumns(vectors);
        RowReduction.Result reduction = RowReduction.reduce(matrix);
        int count = vectors.size();
        if (reduction.rank() == count) return new Result(true, List.of(), reduction);
        int free = -1;
        for (int c = 0; c < count && free < 0; c++) if (!reduction.pivots().contains(c)) free = c;
        List<Expr> coefficients = new ArrayList<>();
        for (int i = 0; i < count; i++) coefficients.add(i == free ? Expr.ONE : Expr.ZERO);
        for (int r = 0; r < reduction.pivots().size(); r++) {
            coefficients.set(reduction.pivots().get(r), Expand.expand(Expr.neg(reduction.reduced().get(r, free))));
        }
        return new Result(false, coefficients, reduction);
    }
}
