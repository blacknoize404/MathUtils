package MathUtils.algebra.operators;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.exceptions.FunctionEvaluationException;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.algebra.operators.primitives.Operator;

import java.util.Iterator;

public final class SumOperator extends Operator {

    public SumOperator(MathElement first, MathElement second, MathElement... elements) {
        super(first, second, elements);
    }

    public SumOperator(Number constant, MathElement secondElement, MathElement... elements) {
        super(constant, secondElement, elements);
    }

    public SumOperator(MathElement first, Number constant, Number... elements) {
        super(first, constant, elements);
    }

    public SumOperator(Number firstConstant, Number secondConstant, Number... constants) {
        super(firstConstant, secondConstant, constants);
    }

    @Override
    public double eval(Evaluation... evaluations) throws FunctionEvaluationException {
        double result = 0;
        for (MathElement n: elements) {
            result += n.eval(evaluations);
        }

        return result;
    }

    @Override
    public String toString() {
        return generateStringRepresentation(" + ", elements);
    }

}
