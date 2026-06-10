package MathUtils.algebra.trigonometrics;

import MathUtils.algebra.Evaluation;
import MathUtils.exceptions.FunctionEvaluationException;
import MathUtils.algebra.primitives.Expression;
import MathUtils.algebra.primitives.MathElement;

public final class Cos extends Expression<MathElement> {

    public Cos(MathElement body) {
        super(body);
    }

    @Override
    public double eval(Evaluation... variables) throws FunctionEvaluationException {
        return Math.cos(argument.eval(variables));
    }
}
