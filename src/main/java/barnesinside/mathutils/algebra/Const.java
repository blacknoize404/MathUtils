package barnesinside.mathutils.algebra;

import barnesinside.mathutils.algebra.primitives.Expression;

/**
 * Representa a una constante
 */
public final class Const extends Expression<Number> {

    public Const(Number value) {
        super(value);
    }

    @Override
    public double eval(Evaluation... variables) {
        return argument.doubleValue();
    }

    @Override
    public String toString() {
        double value = argument.doubleValue();
        if (value == Math.E) return "E";
        if (value % 1 == 0 && Math.abs(value) < 1e15) return String.valueOf((long) value);
        return argument.toString();
    }

}
