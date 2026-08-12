package MathUtils.point;

import MathUtils.vector.Vector2D;

public final class Point2D extends Point<Point2D, Vector2D> implements Comparable<Point2D> {

    private double x;
    private double y;

    public static Point2D ZERO() {
        return new Point2D(0, 0);
    }

    public Point2D(Number x, Number y) {

        this.x = x.doubleValue();
        this.y = y.doubleValue();
    }

    public static Point2D of(Number x, Number y) {
        return new Point2D(x, y);
    }

    @Override
    public Vector2D AsVector() {
        return new Vector2D(x, y);
    }

    @Override
    public boolean isEqualTo(Point2D point) {
        return point.x == getX()
                && point.y == getY();
    }

    public double getX() {
        return x;
    }

    public void setX(Number x) {
        this.x = x.doubleValue();
    }

    public double getY() {
        return y;
    }

    public void setY(Number y) {
        this.y = y.doubleValue();
    }


    @Override
    public String toString() {
        return "Point2D{" +
                "x = " + x + ", " +
                "y = " + y +
                '}';
    }

    @Override
    public int compareTo(Point2D o) {
        return Double.compare(this.getX(), o.getX());
    }
}
