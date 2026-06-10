package MathUtils.sequences;

import MathUtils.algebra.Evaluation;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.exceptions.FunctionEvaluationException;

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
