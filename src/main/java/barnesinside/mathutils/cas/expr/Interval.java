package barnesinside.mathutils.cas.expr;

/**
 * Intervalo de números reales. Un extremo {@code null} es infinito (el intervalo no está acotado por ese lado).
 */
public record Interval(Expr lower, boolean lowerClosed, Expr upper, boolean upperClosed) {

    public Interval {
        // Un extremo infinito nunca es cerrado
        if (lower == null) lowerClosed = false;
        if (upper == null) upperClosed = false;
    }

    public static Interval open(Expr lower, Expr upper) {
        return new Interval(lower, false, upper, false);
    }

    public static Interval closed(Expr lower, Expr upper) {
        return new Interval(lower, true, upper, true);
    }

    /** Todos los reales a la izquierda de {@code upper}. */
    public static Interval below(Expr upper, boolean closed) {
        return new Interval(null, false, upper, closed);
    }

    /** Todos los reales a la derecha de {@code lower}. */
    public static Interval above(Expr lower, boolean closed) {
        return new Interval(lower, closed, null, false);
    }

    public boolean contains(double x) {
        if (lower != null) {
            double a = Eval.value(lower);
            if (x < a || (x == a && !lowerClosed)) return false;
        }
        if (upper != null) {
            double b = Eval.value(upper);
            if (x > b || (x == b && !upperClosed)) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return (lowerClosed ? "[" : "(") + (lower == null ? "-∞" : lower.toString()) + ", "
                + (upper == null ? "∞" : upper.toString()) + (upperClosed ? "]" : ")");
    }

    public String toLatex() {
        return (lowerClosed ? "[" : "(") + (lower == null ? "-\\infty" : lower.toLatex()) + ", "
                + (upper == null ? "\\infty" : upper.toLatex()) + (upperClosed ? "]" : ")");
    }
}
