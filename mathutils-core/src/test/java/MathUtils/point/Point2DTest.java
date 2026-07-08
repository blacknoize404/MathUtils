package MathUtils.point;

import MathUtils.vector.Vector2D;
import org.junit.jupiter.api.Test;

class Point2DTest {

    @Test
    void constructor() {
        Point2D p = new Point2D(3, 4);
        assert p.getX() == 3 && p.getY() == 4;
    }

    @Test
    void of() {
        Point2D p = Point2D.of(3, 4);
        assert p.getX() == 3 && p.getY() == 4;
    }

    @Test
    void ZERO() {
        Point2D p = Point2D.ZERO();
        assert p.getX() == 0 && p.getY() == 0;
    }

    @Test
    void AsVector() {
        Point2D p = new Point2D(3, 4);
        Vector2D v = p.AsVector();
        assert v.getX() == 3 && v.getY() == 4;
    }

    @Test
    void isEqualTo() {
        Point2D a = new Point2D(3, 4);
        Point2D b = new Point2D(3, 4);
        assert a.isEqualTo(b);
    }

    @Test
    void isNotEqualTo() {
        Point2D a = new Point2D(3, 4);
        Point2D b = new Point2D(4, 3);
        assert !a.isEqualTo(b);
    }

    @Test
    void setX() {
        Point2D p = new Point2D(1, 2);
        p.setX(10);
        assert p.getX() == 10;
    }

    @Test
    void setY() {
        Point2D p = new Point2D(1, 2);
        p.setY(20);
        assert p.getY() == 20;
    }

    @Test
    void testClone() {
        Point2D p = new Point2D(3, 4);
        Point2D cloned = p.clone();
        assert cloned.getX() == 3 && cloned.getY() == 4;
        cloned.setX(10);
        assert p.getX() == 3 : "Clone should be independent";
    }

    @Test
    void testToString() {
        Point2D p = new Point2D(3, 4);
        String s = p.toString();
        assert s.contains("3") && s.contains("4");
    }

    @Test
    void compareTo() {
        Point2D a = new Point2D(1, 5);
        Point2D b = new Point2D(3, 2);
        assert a.compareTo(b) < 0;
        assert b.compareTo(a) > 0;
    }

    @Test
    void compareToEqual() {
        Point2D a = new Point2D(1, 5);
        Point2D b = new Point2D(1, 2);
        assert a.compareTo(b) == 0;
    }
}
