package barnesinside.mathutils.sequences;

import barnesinside.mathutils.algebra.Evaluation;
import barnesinside.mathutils.algebra.primitives.MathElement;
import barnesinside.mathutils.exceptions.FunctionEvaluationException;

/**
 * Representa una sucesión matemática (similar a una función, pero el dominio son los naturales
 * incluyendo el 0)
 */
public class Sequence extends MathElement {

    private final MathElement content;

    public Sequence(MathElement content) {
        this.content = content;
    }

    @Override
    public double eval(Evaluation... evaluations) throws FunctionEvaluationException {
        return content.eval(evaluations);
    }

    public double eval(int n) throws FunctionEvaluationException {
        return content.eval(new Evaluation("n", n));
    }

    @Override
    public boolean equalsTo(MathElement mathElement) {
        return false;
    }
}
