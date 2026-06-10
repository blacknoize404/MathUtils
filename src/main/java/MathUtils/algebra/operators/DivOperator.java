package MathUtils.algebra.operators;

import MathUtils.algebra.Evaluation;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.algebra.operators.primitives.Operator;
import MathUtils.exceptions.FunctionEvaluationException;

import java.util.Iterator;

public final class DivOperator extends Operator {

    public DivOperator(Number constant, MathElement secondElement, MathElement... elements) {
        super(constant, secondElement, elements);
    }

    public DivOperator(MathElement first, Number constant, Number... elements) {
        super(first, constant, elements);
    }

    public DivOperator(Number firstConstant, Number secondConstant, Number... constants) {
        super(firstConstant, secondConstant, constants);
    }

    public DivOperator(MathElement first, MathElement second, MathElement... elements) {
        super(first, second, elements);
    }

    @Override
    public double eval(Evaluation... evaluations) throws FunctionEvaluationException {
        if (elements.isEmpty()) return 0;

        Iterator<MathElement> it = elements.iterator();

        double result = it.next().eval(evaluations);

        while (it.hasNext()){
            double value = it.next().eval(evaluations);
            if (value == 0) throw new ArithmeticException("La división por 0 no está definida");
            result /= value;
        }

        return result;
    }

    @Override
    public String toString() {
        return generateStringRepresentation(" / ", elements);
    }

}
