package MathUtils.combinatory;

import MathUtils.algebra.Evaluation;
import MathUtils.algebra.primitives.Expression;
import MathUtils.exceptions.FunctionEvaluationException;

/**
 * Representa el factorial de una expresión matemática
 */
public class Factorial extends Expression<Number> {

    public Factorial(Integer argument) {
        super(argument);
    }

    public static Factorial of(Integer argument)     {
        return new Factorial(argument);
    }

    @Override
    public double eval(Evaluation... evaluations) throws FunctionEvaluationException {

        int factSum = 1;
        int value = argument.intValue();
        for (int i = 2; i <= value; i++) {
            factSum *= i;
        }

        return factSum;
    }
}
