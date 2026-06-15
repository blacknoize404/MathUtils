package MathUtils.point;

import MathUtils.vector.Vector3D;
import org.junit.jupiter.api.Test;

class Point3DTest {

    @Test
    void constructor() {
        Point3D p = new Point3D(1, 2, 3);
        assert p.getX() == 1 && p.getY() == 2 && p.getZ() == 3;
    }

    @Test
    void ZERO() {
        Point3D p = Point3D.ZERO();
        assert p.getX() == 0 && p.getY() == 0 && p.getZ() == 0;
    }

    @Test
    void AsVector() {
        Point3D p = new Point3D(1, 2, 3);
        Vector3D v = p.AsVector();
        assert v.getX() == 1 && v.getY() == 2 && v.getZ() == 3;
    }

    @Test
    void isEqualTo() {
        Point3D a = new Point3D(1, 2, 3);
        Point3D b = new Point3D(1, 2, 3);
        assert a.isEqualTo(b);
    }

    @Test
    void isNotEqualTo() {
        Point3D a = new Point3D(1, 2, 3);
        Point3D b = new Point3D(3, 2, 1);
        assert !a.isEqualTo(b);
    }

    @Test
    void setX() {
        Point3D p = new Point3D(1, 2, 3);
        p.setX(10);
        assert p.getX() == 10;
    }

    @Test
    void setY() {
        Point3D p = new Point3D(1, 2, 3);
        p.setY(20);
        assert p.getY() == 20;
    }

    @Test
    void setZ() {
        Point3D p = new Point3D(1, 2, 3);
        p.setZ(30);
        assert p.getZ() == 30;
    }

    @Test
    void testClone() {
        Point3D p = new Point3D(1, 2, 3);
        Point3D cloned = p.clone();
        assert cloned.getX() == 1 && cloned.getY() == 2 && cloned.getZ() == 3;
        cloned.setX(10);
        assert p.getX() == 1 : "Clone should be independent";
    }

    @Test
    void testToString() {
        Point3D p = new Point3D(1, 2, 3);
        String s = p.toString();
        assert s.contains("1") && s.contains("2") && s.contains("3");
    }
}
