package MathUtils.cas.expr;

/**
 * Unidad en la que se interpretan los ángulos de las funciones trigonométricas.
 */
public enum AngleUnit {
    RADIANS,
    DEGREES,
    GRADIANS;

    /**
     * Factor que convierte esta unidad a radianes: {@code 1}, {@code π/180} o {@code π/200}.
     */
    public Expr toRadiansFactor() {
        return switch (this) {
            case RADIANS -> Expr.ONE;
            case DEGREES -> Expr.mul(Expr.PI, Expr.num(1, 180));
            case GRADIANS -> Expr.mul(Expr.PI, Expr.num(1, 200));
        };
    }
}
