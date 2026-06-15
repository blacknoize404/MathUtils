package MathUtils.point;

import MathUtils.vector.Vector3D;

public final class Point3D extends Point<Point3D, Vector3D> {

    private Double x;

    private Double y;

    private Double z;

    public static Point3D ZERO() {
        return new Point3D(0, 0, 0);
    }

    public Point3D(Number x, Number y, Number z) {
        this.x = x.doubleValue();
        this.y = y.doubleValue();
        this.z = z.doubleValue();
    }

    @Override
    public Vector3D AsVector() {
        return new Vector3D(x, y, z);
    }

    @Override
    public boolean isEqualTo(Point3D point) {
        return point.x.doubleValue() == getX().doubleValue() &&
                point.y.doubleValue() == getY().doubleValue() &&
                point.z.doubleValue() == getZ().doubleValue();
    }

    public Double getX() {
        return x;
    }

    public void setX(Number x) {
        this.x = x.doubleValue();
    }

    public Double getY() {
        return y;
    }

    public void setY(Number y) {
        this.y = y.doubleValue();
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Number z) {
        this.z = z.doubleValue();
    }

    @Override
    public String toString() {
        return "Point3D{" +
                "x = " + x + ", " +
                "y = " + y + ", " +
                "z = " + z +
                '}';
    }
}
