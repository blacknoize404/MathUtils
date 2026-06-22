package MathUtils.algebra;

import MathUtils.algebra.primitives.Expression;

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

}
