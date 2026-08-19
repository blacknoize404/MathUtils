package barnesinside.mathutils.algebra.exponentials;

import barnesinside.mathutils.algebra.Const;
import barnesinside.mathutils.algebra.Evaluation;
import barnesinside.mathutils.algebra.operators.DivOperator;
import barnesinside.mathutils.exceptions.FunctionEvaluationException;
import barnesinside.mathutils.algebra.primitives.Expression;
import barnesinside.mathutils.algebra.primitives.MathElement;



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

    @Override
    public boolean equalsTo(MathElement mathElement) {
        if (mathElement == null) return false;
        if (!(mathElement instanceof Pow pow)) return false;
        return super.equalsTo(mathElement) && exponent.equalsTo(pow.exponent);
    }

    public MathElement getExponent() {
        return exponent;
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
