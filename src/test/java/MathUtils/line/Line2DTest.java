package MathUtils.line;

import MathUtils.point.Point2D;
import MathUtils.vector.Vector2D;
import org.junit.jupiter.api.Test;

class Line2DTest {

    @Test
    void constructor() {
        Line2D line = new Line2D(new Point2D(1, 2), new Vector2D(3, 4));
        assert line != null;
    }

    @Test
    void eval() {
        Line2D line = new Line2D(new Point2D(1, 2), new Vector2D(3, 4));
        Point2D p = line.eval(0);
        assert p.getX() == 1 && p.getY() == 2;
    }

    @Test
    void evalT1() {
        Line2D line = new Line2D(new Point2D(1, 2), new Vector2D(3, 4));
        Point2D p = line.eval(1);
        assert p.getX() == 4 && p.getY() == 6;
    }

    @Test
    void evalT2() {
        Line2D line = new Line2D(new Point2D(1, 2), new Vector2D(3, 4));
        Point2D p = line.eval(2);
        assert p.getX() == 7 && p.getY() == 10;
    }

    @Test
    void isParallelTo() {
        Line2D a = new Line2D(new Point2D(0, 0), new Vector2D(1, 2));
        Line2D b = new Line2D(new Point2D(1, 1), new Vector2D(2, 4));
        assert a.isParallelTo(b);
    }

    @Test
    void isNotParallelTo() {
        Line2D a = new Line2D(new Point2D(0, 0), new Vector2D(1, 0));
        Line2D b = new Line2D(new Point2D(0, 0), new Vector2D(0, 1));
        assert !a.isParallelTo(b);
    }

    @Test
    void isPerpendicularTo() {
        Line2D a = new Line2D(new Point2D(0, 0), new Vector2D(1, 0));
        Line2D b = new Line2D(new Point2D(0, 0), new Vector2D(0, 1));
        assert a.isPerpendicularTo(b);
    }

    @Test
    void isNotPerpendicularTo() {
        Line2D a = new Line2D(new Point2D(0, 0), new Vector2D(1, 0));
        Line2D b = new Line2D(new Point2D(0, 0), new Vector2D(1, 1));
        assert !a.isPerpendicularTo(b);
    }

    @Test
    void isCoincidentTo() {
        Line2D a = new Line2D(new Point2D(0, 0), new Vector2D(1, 2));
        Line2D b = new Line2D(new Point2D(0, 0), new Vector2D(2, 4));
        assert a.isCoincidentTo(b);
    }

    @Test
    void isNotCoincidentTo() {
        Line2D a = new Line2D(new Point2D(0, 0), new Vector2D(1, 0));
        Line2D b = new Line2D(new Point2D(0, 1), new Vector2D(1, 0));
        assert !a.isCoincidentTo(b);
    }

    @Test
    void getUnitDirectionVector() {
        Line2D line = new Line2D(new Point2D(0, 0), new Vector2D(3, 0));
        Vector2D unit = line.getUnitDirectionVector();
        assert Math.abs(unit.getLength() - 1.0) < 1e-10;
    }

    @Test
    void getDirectionVector() {
        Vector2D dir = new Vector2D(3, 4);
        Line2D line = new Line2D(new Point2D(0, 0), dir);
        assert line.getDirectionVector() == dir;
    }

    @Test
    void getParametricEquation() {
        Line2D line = new Line2D(new Point2D(1, 2), new Vector2D(3, 4));
        String eq = line.getParametricEquation();
        assert eq.contains("x =") && eq.contains("y =");
    }
}
