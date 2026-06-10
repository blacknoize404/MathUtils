package MathUtils.line;

import MathUtils.point.Point3D;
import MathUtils.vector.Vector3D;

// TODO: 9/12/2023 Implementar todo lo de recta 3D
// TODO: 9/12/2023 Hacer las citas al libro de Stewart Epígrafe 12.5
public class Line3D extends Line<Line3D, Point3D, Vector3D> {

    /**
     * Crea una nueva recta
     *
     * @param displacement punto que define el desplazamiento de la recta
     * @param direction    dirección en la cual apunta la recta
     */
    public Line3D(Point3D displacement, Vector3D direction) {
        super(displacement, direction);
    }

    @Override
    public Point3D eval(double t) {
        double X = displacement.getX() + direction.getX() * t;
        double Y = displacement.getY() + direction.getY() * t;
        double Z = displacement.getZ() + direction.getZ() * t;
        return new Point3D(X, Y, Z);
    }

    @Override
    public boolean isCoincidentTo(Line3D line) {
        return false;
    }

    @Override
    public boolean isIntersectedBy(Line3D line) {
        return false;
    }

    @Override
    public String getParametricEquation() {
        return "x = " + displacement.getX() + " + " + direction.getX() + " t, " +
               "y = " + displacement.getY() + " + " + direction.getY() + " t, " +
               "z = " + displacement.getZ() + " + " + direction.getZ() + " t";
    }

    @Override
    public String getSymmetricEquation() {
        return null;
    }

    @Override
    public String getGeneralEquation() {
        return null;
    }
}
