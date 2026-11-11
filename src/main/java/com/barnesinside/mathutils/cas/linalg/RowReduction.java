package com.barnesinside.mathutils.cas.linalg;

import com.barnesinside.mathutils.cas.expr.Expand;
import com.barnesinside.mathutils.cas.expr.Expr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * La forma escalonada reducida por filas (Gauss–Jordan) de una matriz, con cada operación entre filas como un paso: intercambiar dos filas,
 * dividir una fila por su pivote o restarle a otra un múltiplo de la fila del pivote.
 *
 * @since 0.10
 */
public final class RowReduction {

    /** Qué se hizo en un paso. */
    public enum Op {SWAP, SCALE, ELIMINATE}

    /**
     * Una operación entre filas y cómo quedó la matriz.
     *
     * @param op     la operación
     * @param row    la fila que cambia (empieza en 0; en un intercambio, la primera)
     * @param other  la otra fila: la del pivote al eliminar, la segunda al intercambiar; {@code -1} al dividir
     * @param factor el número por el que se divide o el múltiplo que se resta; {@code null} al intercambiar
     * @param after  la matriz tras la operación
     */
    public record Step(Op op, int row, int other, Expr factor, ExactMatrix after) {
        /** La operación dicha con palabras (las filas se cuentan desde 1). */
        public String describe(Locale locale) {
            return message(locale, "linalg.row." + op.name().toLowerCase(Locale.ROOT), row + 1, other + 1, factor == null ? "" : factor);
        }
    }

    /**
     * El resultado.
     *
     * @param start   la matriz de partida
     * @param steps   las operaciones, en orden
     * @param reduced la forma escalonada reducida
     * @param pivots  la columna del pivote de cada fila no nula (empieza en 0)
     */
    public record Result(ExactMatrix start, List<Step> steps, ExactMatrix reduced, List<Integer> pivots) {
        public int rank() {
            return pivots.size();
        }
    }

    private RowReduction() {
    }

    public static Result reduce(ExactMatrix matrix) {
        ExactMatrix current = matrix;
        List<Step> steps = new ArrayList<>();
        List<Integer> pivots = new ArrayList<>();
        int rank = 0;
        for (int c = 0; c < matrix.cols() && rank < matrix.rows(); c++) {
            int p = -1;
            for (int r = rank; r < matrix.rows(); r++) {
                if (!ExactMatrix.isZero(current.get(r, c))) {
                    p = r;
                    break;
                }
            }
            if (p < 0) continue;
            if (p != rank) {
                current = current.swapped(rank, p);
                steps.add(new Step(Op.SWAP, rank, p, null, current));
            }
            Expr pivot = current.get(rank, c);
            if (!pivot.equals(Expr.ONE)) {
                List<Expr> scaled = new ArrayList<>();
                for (Expr e : current.row(rank)) scaled.add(Expand.expand(Expr.div(e, pivot)));
                current = current.withRow(rank, scaled);
                steps.add(new Step(Op.SCALE, rank, -1, pivot, current));
            }
            for (int r = 0; r < matrix.rows(); r++) {
                if (r == rank || ExactMatrix.isZero(current.get(r, c))) continue;
                Expr factor = current.get(r, c);
                List<Expr> next = new ArrayList<>();
                for (int j = 0; j < matrix.cols(); j++) next.add(Expand.expand(Expr.sub(current.get(r, j), Expr.mul(factor, current.get(rank, j)))));
                current = current.withRow(r, next);
                steps.add(new Step(Op.ELIMINATE, r, rank, factor, current));
            }
            pivots.add(c);
            rank++;
        }
        return new Result(matrix, steps, current, pivots);
    }

    static String message(Locale locale, String key, Object... args) {
        try {
            String pattern = ResourceBundle.getBundle("barnesinside/mathutils/cas/linalg/messages", "en".equals(locale.getLanguage()) ? Locale.ENGLISH : Locale.forLanguageTag("es")).getString(key);
            return new MessageFormat(pattern).format(args);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
