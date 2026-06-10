package MathUtils.algebra.exponentials;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.operators.DivOperator;
import MathUtils.exceptions.FunctionEvaluationException;
import MathUtils.algebra.primitives.Expression;
import MathUtils.algebra.primitives.MathElement;

// TODO: 12/12/2023 ¿Debería usar Pow como un operador en vez de una expresión?
//  al final, una potencia puede admitir una base y n cantidad de exponentes elevando
//  uno al otro de forma recursiva.

/**
 * Representa la exponenciación
 */
public class Pow extends Expression<MathElement> {

    private final MathElement exponent;

    public Pow(Number argument, MathElement exponent) {
        super(new Const(argument));
        this.exponent = exponent;
    }

    public Pow(MathElement argument, Number exponent) {
        super(argument);
        this.exponent = new Const(exponent);
    }

    public Pow(Number argument, Number exponent) {
        super(new Const(argument));
        this.exponent = new Const(exponent);
    }

    public Pow(MathElement argument, MathElement exponent) {
        super(argument);
        this.exponent = exponent;
    }

    @Override
    public double eval(Evaluation... variables) throws FunctionEvaluationException {
        return Math.pow(argument.eval(variables), exponent.eval(variables));
    }

    public Root toRoot() {
        return new Root(argument, new DivOperator(1, exponent));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        return sb.append(argument.toString())
                .append(")^")
                .append(exponent.toString()).toString();
    }
}
