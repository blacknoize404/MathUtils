package MathUtils.vector;

import MathUtils.Angle;
import MathUtils.exceptions.ZeroVectorException;
import MathUtils.point.Point3D;
import org.junit.jupiter.api.Test;

class Vector3DTest {

    @Test
    void constructorXYZ() {
        Vector3D v = new Vector3D(1, 2, 3);
        assert v.getX() == 1 && v.getY() == 2 && v.getZ() == 3;
    }

    @Test
    void constructorPoint() {
        Vector3D v = new Vector3D(new Point3D(1, 2, 3));
        assert v.getX() == 1 && v.getY() == 2 && v.getZ() == 3;
    }

    @Test
    void constructorFromTo() {
        Vector3D v = new Vector3D(new Point3D(1, 1, 1), new Point3D(4, 5, 6));
        assert v.getX() == 3 && v.getY() == 4 && v.getZ() == 5;
    }

    @Test
    void of() {
        Vector3D v = Vector3D.of(1, 2, 3);
        assert v.getX() == 1 && v.getY() == 2 && v.getZ() == 3;
    }

    @Test
    void ZERO() {
        Vector3D v = Vector3D.ZERO();
        assert v.isZero();
    }

    @Test
    void FORWARD() {
        Vector3D v = Vector3D.FORWARD();
        assert v.getX() == 1 && v.getY() == 0 && v.getZ() == 0;
    }

    @Test
    void BACKWARD() {
        Vector3D v = Vector3D.BACKWARD();
        assert v.getX() == -1 && v.getY() == 0 && v.getZ() == 0;
    }

    @Test
    void LEFT() {
        Vector3D v = Vector3D.LEFT();
        assert v.getX() == 0 && v.getY() == -1 && v.getZ() == 0;
    }

    @Test
    void RIGHT() {
        Vector3D v = Vector3D.RIGHT();
        assert v.getX() == 0 && v.getY() == 1 && v.getZ() == 0;
    }

    @Test
    void UP() {
        Vector3D v = Vector3D.UP();
        assert v.getX() == 0 && v.getY() == 0 && v.getZ() == 1;
    }

    @Test
    void DOWN() {
        Vector3D v = Vector3D.DOWN();
        assert v.getX() == 0 && v.getY() == 0 && v.getZ() == -1;
    }

    @Test
    void isZero() {
        assert Vector3D.ZERO().isZero();
        assert !new Vector3D(1, 0, 0).isZero();
    }

    @Test
    void abs() {
        Vector3D v = new Vector3D(-1, -2, -3).abs();
        assert v.getX() == 1 && v.getY() == 2 && v.getZ() == 3;
    }

    @Test
    void sum() {
        Vector3D a = new Vector3D(1, 2, 3);
        Vector3D b = new Vector3D(4, 5, 6);
        a.sum(b);
        assert a.getX() == 5 && a.getY() == 7 && a.getZ() == 9;
    }

    @Test
    void subtract() {
        Vector3D a = new Vector3D(5, 7, 9);
        Vector3D b = new Vector3D(2, 3, 4);
        a.subtract(b);
        assert a.getX() == 3 && a.getY() == 4 && a.getZ() == 5;
    }

    @Test
    void multiply() {
        Vector3D v = new Vector3D(1, 2, 3);
        v.multiply(3);
        assert v.getX() == 3 && v.getY() == 6 && v.getZ() == 9;
    }

    @Test
    void multiplyByZero() {
        Vector3D v = new Vector3D(1, 2, 3);
        v.multiply(0);
        assert v.isZero();
    }

    @Test
    void divide() {
        Vector3D v = new Vector3D(10, 20, 30);
        v.divide(5);
        assert v.getX() == 2 && v.getY() == 4 && v.getZ() == 6;
    }

    @Test
    void getLength() {
        Vector3D v = new Vector3D(2, 3, 6);
        assert v.getLength() == 7.0;
    }

    @Test
    void getNorm() {
        Vector3D v = new Vector3D(2, 3, 6);
        assert v.getNorm() == 7.0;
    }

    @Test
    void getMagnitude() {
        Vector3D v = new Vector3D(2, 3, 6);
        assert v.getMagnitude() == 7.0;
    }

    @Test
    void getDotProduct() {
        Vector3D a = new Vector3D(1, 2, 3);
        Vector3D b = new Vector3D(4, 5, 6);
        assert a.getDotProduct(b) == 32;
    }

    @Test
    void getCrossProduct() {
        Vector3D a = new Vector3D(1, 0, 0);
        Vector3D b = new Vector3D(0, 1, 0);
        Vector3D cross = a.getCrossProduct(b);
        assert cross.getX() == 0 && cross.getY() == 0 && cross.getZ() == 1;
    }

    @Test
    void getCrossProductAntiCommutative() {
        Vector3D a = new Vector3D(1, 0, 0);
        Vector3D b = new Vector3D(0, 1, 0);
        Vector3D crossAB = a.getCrossProduct(b);
        Vector3D crossBA = b.getCrossProduct(a);
        assert crossAB.getX() == -crossBA.getX();
        assert crossAB.getY() == -crossBA.getY();
        assert crossAB.getZ() == -crossBA.getZ();
    }

    @Test
    void isEqualTo() {
        Vector3D a = new Vector3D(1, 2, 3);
        Vector3D b = new Vector3D(1, 2, 3);
        assert a.isEqualTo(b);
    }

    @Test
    void isNotEqualTo() {
        Vector3D a = new Vector3D(1, 2, 3);
        Vector3D b = new Vector3D(3, 2, 1);
        assert !a.isEqualTo(b);
    }

    @Test
    void getAngleBetween() throws ZeroVectorException {
        Vector3D a = new Vector3D(1, 0, 0);
        Vector3D b = new Vector3D(0, 1, 0);
        Angle angle = a.getAngleBetween(b);
        assert Math.abs(angle.getRadian() - Math.PI / 2) < 1e-10;
    }

    @Test
    void getAngleBetweenZeroVector() {
        Vector3D a = new Vector3D(1, 0, 0);
        Vector3D zero = Vector3D.ZERO();
        try {
            a.getAngleBetween(zero);
            assert false : "Should have thrown ZeroVectorException";
        } catch (ZeroVectorException e) {
            assert true;
        }
    }

    @Test
    void isOrthogonalTo() {
        Vector3D a = new Vector3D(1, 0, 0);
        Vector3D b = new Vector3D(0, 1, 0);
        assert a.isOrthogonalTo(b);
    }

    @Test
    void getAngleBetweenX() throws ZeroVectorException {
        Vector3D v = new Vector3D(1, 0, 0);
        Angle angle = v.getAngleBetweenX();
        assert Math.abs(angle.getRadian()) < 1e-10;
    }

    @Test
    void getAngleBetweenY() throws ZeroVectorException {
        Vector3D v = new Vector3D(0, 1, 0);
        Angle angle = v.getAngleBetweenY();
        assert Math.abs(angle.getRadian()) < 1e-10;
    }

    @Test
    void getAngleBetweenZ() throws ZeroVectorException {
        Vector3D v = new Vector3D(0, 0, 1);
        Angle angle = v.getAngleBetweenZ();
        assert Math.abs(angle.getRadian()) < 1e-10;
    }

    @Test
    void getAngleBetweenXOnZero() {
        Vector3D v = Vector3D.ZERO();
        try {
            v.getAngleBetweenX();
            assert false : "Should have thrown ZeroVectorException";
        } catch (ZeroVectorException e) {
            assert true;
        }
    }

    @Test
    void getNormalVector() {
        Vector3D a = new Vector3D(1, 0, 0);
        Vector3D b = new Vector3D(0, 1, 0);
        Vector3D normal = a.getNormalVector(b);
        assert normal.getZ() == 1;
    }

    @Test
    void getUnitNormalVector() {
        Vector3D a = new Vector3D(2, 0, 0);
        Vector3D b = new Vector3D(0, 2, 0);
        Vector3D unit = a.getUnitNormalVector(b);
        double len = unit.getLength();
        assert Math.abs(len - 1.0) < 1e-10;
    }

    @Test
    void testClone() {
        Vector3D v = new Vector3D(1, 2, 3);
        Vector3D cloned = v.clone();
        assert cloned.getX() == 1 && cloned.getY() == 2 && cloned.getZ() == 3;
        cloned.setX(10);
        assert v.getX() == 1 : "Clone should be independent";
    }

    @Test
    void setX() {
        Vector3D v = new Vector3D(1, 2, 3);
        v.setX(10);
        assert v.getX() == 10;
    }

    @Test
    void setY() {
        Vector3D v = new Vector3D(1, 2, 3);
        v.setY(20);
        assert v.getY() == 20;
    }

    @Test
    void setZ() {
        Vector3D v = new Vector3D(1, 2, 3);
        v.setZ(30);
        assert v.getZ() == 30;
    }

    @Test
    void compareTo() {
        Vector3D a = new Vector3D(1, 0, 0);
        Vector3D b = new Vector3D(2, 0, 0);
        assert a.compareTo(b) < 0;
        assert b.compareTo(a) > 0;
    }

    @Test
    void testToString() {
        Vector3D v = new Vector3D(1, 2, 3);
        String s = v.toString();
        assert s.contains("1") && s.contains("2") && s.contains("3");
    }
}
