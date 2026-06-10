package MathUtils.algebra.operators;

import MathUtils.algebra.Evaluation;
import MathUtils.exceptions.FunctionEvaluationException;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.algebra.operators.primitives.Operator;

import java.util.Iterator;

public final class SubOperator extends Operator {

    public SubOperator(Number constant, MathElement secondElement, MathElement... elements) {
        super(constant, secondElement, elements);
    }

    public SubOperator(MathElement first, Number constant, Number... elements) {
        super(first, constant, elements);
    }

    public SubOperator(Number firstConstant, Number secondConstant, Number... constants) {
        super(firstConstant, secondConstant, constants);
    }

    public SubOperator(MathElement first, MathElement second, MathElement... elements) {
        super(first, second, elements);
    }

    @Override
    public double eval(Evaluation... variables) throws FunctionEvaluationException {
        if (elements.isEmpty()) return 0;
        Iterator<MathElement> it = elements.iterator();

        double result = it.next().eval(variables);

        while (it.hasNext()){
            result -= it.next().eval(variables);
        }

        return result;
    }

    @Override
    public String toString() {
        return generateStringRepresentation(" - ", elements);
    }
}
