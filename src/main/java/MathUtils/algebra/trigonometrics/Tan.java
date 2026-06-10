package MathUtils.algebra.trigonometrics;

import MathUtils.algebra.Evaluation;
import MathUtils.algebra.primitives.Expression;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.exceptions.FunctionEvaluationException;

public final class Tan extends Expression<MathElement> {
    public Tan(MathElement body) {
        super(body);
    }
    @Override
    public double eval(Evaluation... variables) throws FunctionEvaluationException {

        return Math.tan(argument.eval(variables));
    }
}
