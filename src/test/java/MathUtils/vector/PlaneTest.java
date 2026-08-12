package MathUtils.vector;

import MathUtils.point.Point2D;
import MathUtils.point.Point3D;
import org.junit.jupiter.api.Test;

class PlaneTest {

    @Test
    void constructorWithSlopes() {
        Plane p = new Plane(1, 0, 0);
        assert p.getXSlope() == 1;
        assert p.getYSlope() == 0;
        assert p.getZSlope() == 0;
    }

    @Test
    void constructorWithSlopesAndDisplacement() {
        Plane p = new Plane(1, 0, 0, 2, 3, 4);
        assert p.getXSlope() == 1;
        assert p.getXDisplacement() == 2;
    }

    @Test
    void constructorWithVectorAndPoint() {
        Vector3D normal = new Vector3D(1, 0, 0);
        Point3D disp = new Point3D(2, 3, 4);
        Plane p = new Plane(normal, disp);
        assert p.getXSlope() == 1;
        assert p.getXDisplacement() == 2;
    }

    @Test
    void constructorWithVector() {
        Plane p = new Plane(new Vector3D(1, 0, 0));
        assert p.getXSlope() == 1;
    }

    @Test
    void getUnitNormalVector() {
        Plane p = new Plane(3, 0, 0);
        Vector3D unit = p.getUnitNormalVector();
        assert Math.abs(unit.getLength() - 1.0) < 1e-10;
        assert unit.getX() == 1;
    }

    @Test
    void getNormalVector() {
        Plane p = new Plane(2, 3, 4);
        Vector3D n = p.getNormalVector();
        assert n.getX() == 2 && n.getY() == 3 && n.getZ() == 4;
    }

    @Test
    void getRelationCoincident() {
        Plane p1 = new Plane(1, 0, 0, 2, 3, 4);
        Plane p2 = new Plane(1, 0, 0, 2, 3, 4);
        assert p1.getRelationTo(p2) == PlaneState.COINCIDENT;
    }

    @Test
    void getRelationParallel() {
        Plane p1 = new Plane(1, 0, 0, 2, 3, 4);
        Plane p2 = new Plane(1, 0, 0, 5, 6, 7);
        assert p1.getRelationTo(p2) == PlaneState.PARALLEL;
    }

    @Test
    void getRelationPerpendicular() {
        Plane p1 = new Plane(1, 0, 0);
        Plane p2 = new Plane(0, 1, 0);
        assert p1.getRelationTo(p2) == PlaneState.PERPENDICULAR;
    }

    @Test
    void getRelationNoPerpendicular() {
        Plane p1 = new Plane(1, 0, 0);
        Plane p2 = new Plane(1, 1, 0);
        assert p1.getRelationTo(p2) == PlaneState.NO_PERPENDICULAR;
    }

    @Test
    void testToString() {
        Plane p = new Plane(1, 0, 0, 2, 3, 4);
        String s = p.toString();
        assert s.contains("x") && s.contains("y") && s.contains("z");
    }

    @Test
    void setXSlope() {
        Plane p = new Plane(1, 0, 0);
        p.setXSlope(5);
        assert p.getXSlope() == 5;
    }

    @Test
    void setYSlope() {
        Plane p = new Plane(0, 1, 0);
        p.setYSlope(5);
        assert p.getYSlope() == 5;
    }

    @Test
    void setZSlope() {
        Plane p = new Plane(0, 0, 1);
        p.setZSlope(5);
        assert p.getZSlope() == 5;
    }

    @Test
    void setXDisplacement() {
        Plane p = new Plane(1, 0, 0, 0, 0, 0);
        p.setXDisplacement(10);
        assert p.getXDisplacement() == 10;
    }

    @Test
    void setYDisplacement() {
        Plane p = new Plane(0, 1, 0, 0, 0, 0);
        p.setYDisplacement(10);
        assert p.getYDisplacement() == 10;
    }

    @Test
    void setZDisplacement() {
        Plane p = new Plane(0, 0, 1, 0, 0, 0);
        p.setZDisplacement(10);
        assert p.getZDisplacement() == 10;
    }
}
