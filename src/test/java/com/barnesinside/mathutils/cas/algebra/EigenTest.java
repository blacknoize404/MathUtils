package com.barnesinside.mathutils.cas.algebra;

import com.barnesinside.mathutils.cas.expr.Eval;
import com.barnesinside.mathutils.cas.expr.Expr;
import com.barnesinside.mathutils.cas.expr.ExprParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class EigenTest {

    private static List<List<Expr>> matrix(String... rows) {
        List<List<Expr>> m = new ArrayList<>();
        for (String row : rows) {
            List<Expr> cells = new ArrayList<>();
            for (String cell : row.split(" ")) cells.add(ExprParser.parse(cell));
            m.add(cells);
        }
        return m;
    }

    private static double value(Expr e) {
        return Eval.value(e, Map.of());
    }

    @Test
    void aSymmetricTwoByTwoHasTwoSimpleEigenvalues() {
        Eigen.Result r = Eigen.of(matrix("2 1", "1 2"));
        assert r.complete() && r.diagonalizable() : r;
        assert r.pairs().size() == 2 && value(r.pairs().get(0).value()) == 1 && value(r.pairs().get(1).value()) == 3 : r.pairs();
        assert r.pairs().get(0).vectors().get(0).toString().equals("[1, -1]") : r.pairs().get(0).vectors();
        assert r.pairs().get(1).vectors().get(0).toString().equals("[1, 1]") : r.pairs().get(1).vectors();
    }

    @Test
    void theCharacteristicPolynomialIsExpanded() {
        Expr p = Eigen.characteristic(matrix("2 1", "1 2"));
        for (double l : new double[]{0, 1, 2, 3}) {
            double expected = (2 - l) * (2 - l) - 1;
            assert Math.abs(Eval.value(p, Map.of("λ", l)) - expected) < 1e-9 : l + ": " + p;
        }
    }

    @Test
    void aRepeatedEigenvalueWithTooFewVectorsIsNotDiagonalizable() {
        Eigen.Result jordan = Eigen.of(matrix("2 1", "0 2"));
        assert jordan.pairs().size() == 1 && jordan.pairs().get(0).algebraic() == 2 && jordan.pairs().get(0).geometric() == 1 : jordan;
        assert !jordan.diagonalizable();
        Eigen.Result identity = Eigen.of(matrix("3 0", "0 3"));
        assert identity.pairs().get(0).algebraic() == 2 && identity.pairs().get(0).geometric() == 2 && identity.diagonalizable() : identity;
    }

    @Test
    void aThreeByThreeAndIrrationalEigenvalues() {
        Eigen.Result three = Eigen.of(matrix("2 0 0", "0 3 4", "0 4 9"));
        assert three.complete() && three.pairs().size() == 3 : three;
        assert Math.abs(value(three.pairs().get(0).value()) - 1) < 1e-9 && Math.abs(value(three.pairs().get(2).value()) - 11) < 1e-9 : three.pairs();
        Eigen.Result irrational = Eigen.of(matrix("1 1", "1 0"));
        assert irrational.complete() && Math.abs(value(irrational.pairs().get(1).value()) - (1 + Math.sqrt(5)) / 2) < 1e-9 : irrational.pairs();
        // Cada vector cumple A v = λ v
        for (Eigen.Pair pair : irrational.pairs()) {
            List<Expr> v = pair.vectors().get(0);
            double x = value(v.get(0));
            double y = value(v.get(1));
            double l = value(pair.value());
            assert Math.abs(x + y - l * x) < 1e-9 && Math.abs(x - l * y) < 1e-9 : pair;
        }
    }

    @Test
    void complexEigenvaluesAreReportedAsIncomplete() {
        Eigen.Result rotation = Eigen.of(matrix("0 -1", "1 0"));
        assert !rotation.complete() && rotation.pairs().isEmpty() : rotation;
    }

    @Test
    void nonSquareAndHugeMatricesAreRefused() {
        try {
            Eigen.of(matrix("1 2 3", "4 5 6"));
            assert false : "no es cuadrada";
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("cuadrada");
        }
    }
}
