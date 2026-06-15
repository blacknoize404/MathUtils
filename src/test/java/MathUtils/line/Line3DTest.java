package MathUtils.line;

import MathUtils.point.Point3D;
import MathUtils.vector.Vector3D;
import org.junit.jupiter.api.Test;

class Line3DTest {

    @Test
    void constructor() {
        Line3D line = new Line3D(new Point3D(1, 2, 3), new Vector3D(4, 5, 6));
        assert line != null;
    }

    @Test
    void eval() {
        Line3D line = new Line3D(new Point3D(1, 2, 3), new Vector3D(4, 5, 6));
        Point3D p = line.eval(0);
        assert p.getX() == 1 && p.getY() == 2 && p.getZ() == 3;
    }

    @Test
    void evalT1() {
        Line3D line = new Line3D(new Point3D(1, 2, 3), new Vector3D(4, 5, 6));
        Point3D p = line.eval(1);
        assert p.getX() == 5 && p.getY() == 7 && p.getZ() == 9;
    }

    @Test
    void evalT2() {
        Line3D line = new Line3D(new Point3D(1, 2, 3), new Vector3D(4, 5, 6));
        Point3D p = line.eval(2);
        assert p.getX() == 9 && p.getY() == 12 && p.getZ() == 15;
    }

    @Test
    void isParallelTo() {
        Line3D a = new Line3D(new Point3D(0, 0, 0), new Vector3D(1, 2, 3));
        Line3D b = new Line3D(new Point3D(1, 1, 1), new Vector3D(2, 4, 6));
        assert a.isParallelTo(b);
    }

    @Test
    void isNotParallelTo() {
        Line3D a = new Line3D(new Point3D(0, 0, 0), new Vector3D(1, 0, 0));
        Line3D b = new Line3D(new Point3D(0, 0, 0), new Vector3D(0, 1, 0));
        assert !a.isParallelTo(b);
    }

    @Test
    void isPerpendicularTo() {
        Line3D a = new Line3D(new Point3D(0, 0, 0), new Vector3D(1, 0, 0));
        Line3D b = new Line3D(new Point3D(0, 0, 0), new Vector3D(0, 1, 0));
        assert a.isPerpendicularTo(b);
    }

    @Test
    void isNotPerpendicularTo() {
        Line3D a = new Line3D(new Point3D(0, 0, 0), new Vector3D(1, 0, 0));
        Line3D b = new Line3D(new Point3D(0, 0, 0), new Vector3D(1, 1, 0));
        assert !a.isPerpendicularTo(b);
    }

    @Test
    void getUnitDirectionVector() {
        Line3D line = new Line3D(new Point3D(0, 0, 0), new Vector3D(2, 0, 0));
        Vector3D unit = line.getUnitDirectionVector();
        assert Math.abs(unit.getLength() - 1.0) < 1e-10;
    }

    @Test
    void getDirectionVector() {
        Vector3D dir = new Vector3D(1, 2, 3);
        Line3D line = new Line3D(new Point3D(0, 0, 0), dir);
        assert line.getDirectionVector() == dir;
    }

    @Test
    void getParametricEquation() {
        Line3D line = new Line3D(new Point3D(1, 2, 3), new Vector3D(4, 5, 6));
        String eq = line.getParametricEquation();
        assert eq.contains("x =") && eq.contains("y =") && eq.contains("z =");
    }
}
