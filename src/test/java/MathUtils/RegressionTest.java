package MathUtils;

import MathUtils.algebra.Const;
import MathUtils.combinatory.Factorial;
import MathUtils.informatics.Conversions;
import MathUtils.line.Line2D;
import MathUtils.point.Point2D;
import MathUtils.point.Point3D;
import MathUtils.statistics.WeightedMean;
import MathUtils.vector.Plane;
import MathUtils.vector.Vector2D;
import MathUtils.vector.Vector3D;
import org.junit.jupiter.api.Test;

class RegressionTest {

    @Test
    void unitDirectionDoesNotMutateLine() {
        Line2D line = new Line2D(new Point2D(0, 0), new Vector2D(3, 4));
        line.getUnitDirectionVector();
        line.isParallelTo(line);
        assert line.getDirectionVector().getX() == 3 && line.getDirectionVector().getY() == 4;
    }

    @Test
    void antiParallelLinesAreParallel() {
        Line2D a = new Line2D(new Point2D(0, 0), new Vector2D(1, 2));
        Line2D b = new Line2D(new Point2D(5, 5), new Vector2D(-2, -4));
        assert a.isParallelTo(b);
        assert !a.isCoincidentTo(b);
    }

    @Test
    void planeUnitNormalDoesNotMutate() {
        Plane p = new Plane(new Vector3D(0, 0, 5), Point3D.ZERO());
        p.getUnitNormalVector();
        assert p.getZSlope() == 5;
    }

    @Test
    void planeEvalUsesZDisplacement() {
        Plane p = new Plane(0, 0, 1, 0, 0, 7);
        assert p.eval(new Point2D(1, 1)) == 7;
    }

    @Test
    void vectorEqualityIsComponentWise() {
        assert !new Vector2D(1, 0).isEqualTo(new Vector2D(1, 5));
        assert new Vector3D(1, 2, 3).isEqualTo(new Vector3D(1, 2, 3));
    }

    @Test
    void vectorCompareToKeepsSmallDifferences() {
        assert new Vector2D(1, 0).compareTo(new Vector2D(1.001, 0)) < 0;
    }

    @Test
    void constKeepsLargeValues() {
        assert new Const(10_000_000_000L).eval() == 1.0e10;
    }

    @Test
    void factorialDoesNotOverflow() throws Exception {
        assert Factorial.of(15).eval() == 1307674368000d;
    }

    @Test
    void conversionsHandleNegativesAndLongStrings() {
        assert "-101".equals(Conversions.decimalToBinary(-5));
        assert Conversions.binaryToDecimal("-101") == -5;
    }

    @Test
    void weightedMeanFromTable() {
        WeightedMean m = new WeightedMean(new double[][]{{1, 6}, {1, 7}, {2, 5}});
        assert Math.abs(m.eval() - 5.75) < 1e-12;
    }
}
