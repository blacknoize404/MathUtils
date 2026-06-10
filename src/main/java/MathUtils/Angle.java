package MathUtils;

// TODO: 9/12/2023 Documentar la clase de ángulos
// TODO: 9/12/2023 Citar al libro de Matemáticas Simplificadas de Conamat
public class Angle {

    private double radian;
    private double angle;

    private static double radianToAngle(double radian) {
        return radian * 180D / Math.PI;
    }

    private static double angleToRadian(double angle) {
        return angle * Math.PI / 180D;
    }
    public static Angle fromAngle(double angle) {
        return new Angle() {{setAngle(angle);}};
    }
    public static Angle fromRadians(double radians) {
        return new Angle() {{setRadian(radians);}};
    }

    public double getRadian() {
        return radian;
    }

    public void setRadian(double radian) {
        this.radian = radian;
        this.angle = radianToAngle(radian);
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
        this.radian = angleToRadian(angle);
    }

    @Override
    public String toString() {
        return "Vectorial.Angle {" +
                "radian=" + radian +
                ", angle=" + angle +
                '}';
    }
}
