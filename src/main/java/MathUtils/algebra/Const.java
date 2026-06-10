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

        // Si el argumento es entero, lo devuelve como entero
        if (argument.doubleValue() % 1 == 0) return argument.intValue();

        // caso contrario, lo devuelve como double
        return argument.doubleValue();
    }

}
