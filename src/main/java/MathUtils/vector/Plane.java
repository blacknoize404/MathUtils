package MathUtils.vector;

import MathUtils.line.Line3D;
import MathUtils.point.Point2D;
import MathUtils.point.Point3D;

/**
 * Se encargará de realizar operaciones entre planos
 */
public class Plane {
    private Vector3D normalVector;
    private Point3D displacement;

    public Plane(double xSlope, double ySlope, double zSlope) {
        this.normalVector = new Vector3D(xSlope, ySlope, zSlope);
    }

    public Plane(double xSlope, double ySlope, double zSlope, double xDisplacement, double yDisplacement, double zDisplacement, double ...zdi) {
        this.normalVector = new Vector3D(xSlope, ySlope, zSlope);
        this.displacement = new Point3D(xDisplacement, yDisplacement, zDisplacement);
    }

    public Plane(Vector3D normalVector, Point3D displacement) {
        this.normalVector = normalVector;
        this.displacement = displacement;
    }

    // TODO: 6/3/2024
    /**
     * Construir un plano a partir de dos rectas que se corten
     * @param l1 recta 1
     * @param l2 recta 2
     */
    public Plane(Line3D l1, Line3D l2){

    }

    /**
     * Crea un plano sin desplazamientos rotado en dirección al vector normal
     * @param normalVector el vector normal
     */
    public Plane(Vector3D normalVector) {
        this.normalVector = normalVector;
        this.displacement = Point3D.ZERO();
    }

//     TODO: Comprobar que funcione apropiadamente.

    /**
     * @param point punto el cual necesita ser evaluado en el plano.
     * @return devuelve el resultado de la evaluación del punto (valor de z)
     */
    public double eval(Point2D point) {
        return (-( getXSlope() * ( point.getX() - getXDisplacement() ) + getYSlope() * ( point.getY() - getYDisplacement() )) / getZSlope()) + getYDisplacement();
    }

    /**
     * TODO: Comprobar que funcione apropiadamente.
     * @param point punto el cual necesita saber si está contenido en el plano.
     * @return un booleano en true en caso de que el punto se encuentre dentro del plano.
     */
    public boolean isInPlane(Point3D point) {
        return point.getZ() == eval(new Point2D(point.getX(), point.getY()));
    }

    /**
     * <p>Dos planos son perpendiculares si sus vectores normales son perpendiculares
     * <p>Dos planos son paralelos si sus vectores normales son paralelos
     * @param p plano a evaluar relación
     * @return devuelve la relación entre dichos planos
     */
    public PlaneState getRelationTo(Plane p) {
        if (p.getUnitNormalVector().isEqualTo(this.getUnitNormalVector())) {

            if (getXDisplacement() == p.getXDisplacement() && getYDisplacement() == p.getYDisplacement() && getZDisplacement() == p.getZDisplacement()) {
                return PlaneState.COINCIDENT;
            }

            return PlaneState.PARALLEL;

        } else if (p.getUnitNormalVector().isOrthogonalTo(this.getUnitNormalVector())) {

            return PlaneState.PERPENDICULAR;

        } else {

            return PlaneState.NO_PERPENDICULAR;

        }
    }

    public Vector3D getUnitNormalVector() {
        return normalVector.divide(normalVector.getLength());
    }
    public Vector3D getNormalVector() {
        return normalVector;
    }

    public double getXSlope() {
        return normalVector.getX();
    }

    public void setXSlope(double xSlope) {
        normalVector.setX(xSlope);
    }

    public double getYSlope() {
        return normalVector.getY();
    }

    public void setYSlope(double ySlope) {
        normalVector.setY(ySlope);
    }

    public double getZSlope() {
        return normalVector.getZ();
    }

    public void setZSlope(double zSlope) {
        normalVector.setZ(zSlope);
    }

    public double getXDisplacement() {
        return displacement.getX();
    }

    public void setXDisplacement(double xDisplacement) {
        displacement.setX(xDisplacement);
    }

    public double getYDisplacement() {
        return displacement.getY();
    }

    public void setYDisplacement(double yDisplacement) {
        displacement.setY(yDisplacement);
    }

    public double getZDisplacement() {
        return displacement.getZ();
    }

    public void setZDisplacement(double zDisplacement) {
        displacement.setZ(zDisplacement);
    }

    @Override
    public String toString() {
        return "0 = " +
                getXSlope() + "(x - " + getXDisplacement() + ") + " +
                getYSlope() + "(y - " + getYDisplacement() + ") + " +
                getZSlope() + "(z - " + getZDisplacement() + ")";

    }
}
