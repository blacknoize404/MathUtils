package MathUtils.algebra.trigonometrics;

import MathUtils.algebra.Evaluation;
import MathUtils.exceptions.FunctionEvaluationException;
import MathUtils.algebra.primitives.Expression;
import MathUtils.algebra.primitives.MathElement;

public class Cot extends Expression<MathElement> {

    public Cot(MathElement content) {
        super(content);
    }

    /**
     * Evaluación de la cotangente usando la identidad trigonométrica.
     */
    @Override
    public double eval(Evaluation... evaluations) throws FunctionEvaluationException {
        double value = argument.eval(evaluations);

        return Math.cos(value) / Math.sin(value);
    }

}
