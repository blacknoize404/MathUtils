package MathUtils.vector;

import MathUtils.Angle;
import MathUtils.exceptions.ZeroVectorException;
import MathUtils.point.Point2D;
import org.junit.jupiter.api.Test;

class Vector2DTest {

    @Test
    void constructorXY() {
        Vector2D v = new Vector2D(3, 4);
        assert v.getX() == 3;
        assert v.getY() == 4;
    }

    @Test
    void constructorPoint() {
        Vector2D v = new Vector2D(new Point2D(3, 4));
        assert v.getX() == 3;
        assert v.getY() == 4;
    }

    @Test
    void constructorFromTo() {
        Vector2D v = new Vector2D(new Point2D(1, 1), new Point2D(4, 5));
        assert v.getX() == 3;
        assert v.getY() == 4;
    }

    @Test
    void constructorAngle() {
        Vector2D v = new Vector2D(Angle.fromAngle(0));
        double len = v.getLength();
        assert Math.abs(len - 1.0) < 1e-10 : "Expected unit vector but got length " + len;
    }

    @Test
    void ZERO() {
        Vector2D v = Vector2D.ZERO();
        assert v.isZero();
    }

    @Test
    void FORWARD() {
        Vector2D v = Vector2D.FORWARD();
        assert v.getX() == 1 && v.getY() == 0;
    }

    @Test
    void BACKWARD() {
        Vector2D v = Vector2D.BACKWARD();
        assert v.getX() == -1 && v.getY() == 0;
    }

    @Test
    void LEFT() {
        Vector2D v = Vector2D.LEFT();
        assert v.getX() == 0 && v.getY() == -1;
    }

    @Test
    void RIGHT() {
        Vector2D v = Vector2D.RIGHT();
        assert v.getX() == 0 && v.getY() == 1;
    }

    @Test
    void of() {
        Vector2D v = Vector2D.of(3, 4);
        assert v.getX() == 3 && v.getY() == 4;
    }

    @Test
    void isZero() {
        assert Vector2D.ZERO().isZero();
        assert !new Vector2D(1, 0).isZero();
    }

    @Test
    void abs() {
        Vector2D v = new Vector2D(-3, -4).abs();
        assert v.getX() == 3 && v.getY() == 4;
    }

    @Test
    void sum() {
        Vector2D a = new Vector2D(1, 2);
        Vector2D b = new Vector2D(3, 4);
        a.sum(b);
        assert a.getX() == 4 && a.getY() == 6;
    }

    @Test
    void subtract() {
        Vector2D a = new Vector2D(5, 7);
        Vector2D b = new Vector2D(2, 3);
        a.subtract(b);
        assert a.getX() == 3 && a.getY() == 4;
    }

    @Test
    void multiply() {
        Vector2D v = new Vector2D(2, 3);
        v.multiply(4);
        assert v.getX() == 8 && v.getY() == 12;
    }

    @Test
    void multiplyByZero() {
        Vector2D v = new Vector2D(2, 3);
        v.multiply(0);
        assert v.isZero();
    }

    @Test
    void divide() {
        Vector2D v = new Vector2D(10, 20);
        v.divide(5);
        assert v.getX() == 2 && v.getY() == 4;
    }

    @Test
    void getLength() {
        Vector2D v = new Vector2D(3, 4);
        assert v.getLength() == 5.0;
    }

    @Test
    void getNorm() {
        Vector2D v = new Vector2D(3, 4);
        assert v.getNorm() == 5.0;
    }

    @Test
    void getMagnitude() {
        Vector2D v = new Vector2D(3, 4);
        assert v.getMagnitude() == 5.0;
    }

    @Test
    void getDotProduct() {
        Vector2D a = new Vector2D(1, 2);
        Vector2D b = new Vector2D(3, 4);
        assert a.getDotProduct(b) == 11;
    }

    @Test
    void getScalarProduct() {
        Vector2D a = new Vector2D(1, 2);
        Vector2D b = new Vector2D(3, 4);
        assert a.getScalarProduct(b) == 11;
    }

    @Test
    void getCrossProduct() {
        Vector2D a = new Vector2D(1, 0);
        Vector2D b = new Vector2D(0, 1);
        Vector3D cross = a.getCrossProduct(b);
        assert cross.getX() == 0 && cross.getY() == 0 && cross.getZ() == 1;
    }

    @Test
    void isEqualTo() {
        Vector2D a = new Vector2D(3, 4);
        Vector2D b = new Vector2D(3, 4);
        assert a.isEqualTo(b);
    }

    @Test
    void isNotEqualTo() {
        Vector2D a = new Vector2D(3, 4);
        Vector2D b = new Vector2D(4, 3);
        assert !a.isEqualTo(b);
    }

    @Test
    void getAngleBetween() throws ZeroVectorException {
        Vector2D a = new Vector2D(1, 0);
        Vector2D b = new Vector2D(0, 1);
        Angle angle = a.getAngleBetween(b);
        assert Math.abs(angle.getRadian() - Math.PI / 2) < 1e-10;
    }

    @Test
    void getAngleBetweenParallel() throws ZeroVectorException {
        Vector2D a = new Vector2D(1, 0);
        Vector2D b = new Vector2D(2, 0);
        Angle angle = a.getAngleBetween(b);
        assert Math.abs(angle.getRadian()) < 1e-10;
    }

    @Test
    void getAngleBetweenZeroVector() {
        Vector2D a = new Vector2D(1, 0);
        Vector2D zero = Vector2D.ZERO();
        try {
            a.getAngleBetween(zero);
            assert false : "Should have thrown ZeroVectorException";
        } catch (ZeroVectorException e) {
            assert true;
        }
    }

    @Test
    void isOrthogonalTo() {
        Vector2D a = new Vector2D(1, 0);
        Vector2D b = new Vector2D(0, 1);
        assert a.isOrthogonalTo(b);
    }

    @Test
    void isNotOrthogonalTo() {
        Vector2D a = new Vector2D(1, 0);
        Vector2D b = new Vector2D(1, 1);
        assert !a.isOrthogonalTo(b);
    }

    @Test
    void testClone() {
        Vector2D v = new Vector2D(3, 4);
        Vector2D cloned = v.clone();
        assert cloned.getX() == 3 && cloned.getY() == 4;
        cloned.setX(10);
        assert v.getX() == 3 : "Clone should be independent";
    }

    @Test
    void AsPoint() {
        Vector2D v = new Vector2D(3, 4);
        Point2D p = v.AsPoint();
        assert p.getX() == 3 && p.getY() == 4;
    }

    @Test
    void compareTo() {
        Vector2D a = new Vector2D(3, 4);
        Vector2D b = new Vector2D(6, 8);
        assert a.compareTo(b) < 0;
        assert b.compareTo(a) > 0;
    }

    @Test
    void testToString() {
        Vector2D v = new Vector2D(3, 4);
        String s = v.toString();
        assert s.contains("3") && s.contains("4");
    }

    @Test
    void setTo() {
        Vector2D v = new Vector2D(1, 1);
        v.setTo(new Point2D(4, 5));
        assert v.getX() == 4 && v.getY() == 5;
    }

    @Test
    void setX() {
        Vector2D v = new Vector2D(1, 2);
        v.setX(10);
        assert v.getX() == 10;
    }

    @Test
    void setY() {
        Vector2D v = new Vector2D(1, 2);
        v.setY(20);
        assert v.getY() == 20;
    }
}
